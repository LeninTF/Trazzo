using Trazzo.Biometric.Agent.Contracts;

namespace Trazzo.Biometric.Agent.Utilities;

public sealed record FingerprintQualityCriteria(
    double MinimumForegroundCoveragePercent,
    double MaximumForegroundCoveragePercent,
    double MinimumContrastScore,
    bool RequireCenteredFingerprint,
    double CenterTolerancePercent,
    double ContrastThresholdOffset = 15);

public static class FingerprintQualityAnalyzer
{
    private const double MaximumRejectedScorePercent = 50;

    public static FingerprintQualityResult Analyze(
        byte[] imageBuffer,
        int width,
        int height,
        FingerprintQualityCriteria criteria)
    {
        if (width <= 0 || height <= 0 || imageBuffer.Length < width * height)
        {
            return new FingerprintQualityResult(false, 0, 0, 0, false, "Imagen de huella inválida.");
        }

        int totalPixels = width * height;
        double average = CalculateAverage(imageBuffer, totalPixels);
        double threshold = Math.Max(0, average - criteria.ContrastThresholdOffset);

        int foregroundCount = 0;
        long foregroundSum = 0;
        long backgroundSum = 0;
        int backgroundCount = 0;
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++)
        {
            int rowOffset = y * width;
            for (int x = 0; x < width; x++)
            {
                byte pixel = imageBuffer[rowOffset + x];
                if (pixel < threshold)
                {
                    foregroundCount++;
                    foregroundSum += pixel;
                    minX = Math.Min(minX, x);
                    minY = Math.Min(minY, y);
                    maxX = Math.Max(maxX, x);
                    maxY = Math.Max(maxY, y);
                }
                else
                {
                    backgroundCount++;
                    backgroundSum += pixel;
                }
            }
        }

        double coveragePercent = foregroundCount * 100d / totalPixels;
        double foregroundAverage = foregroundCount > 0 ? foregroundSum / (double)foregroundCount : 0;
        double backgroundAverage = backgroundCount > 0 ? backgroundSum / (double)backgroundCount : average;
        double contrastScore = Math.Max(0, backgroundAverage - foregroundAverage);
        bool isCentered = IsForegroundCentered(width, height, minX, minY, maxX, maxY, criteria.CenterTolerancePercent);

        return EvaluateQuality(criteria, foregroundCount, coveragePercent, contrastScore, isCentered);
    }

    private static FingerprintQualityResult EvaluateQuality(
        FingerprintQualityCriteria criteria,
        int foregroundCount,
        double coveragePercent,
        double contrastScore,
        bool isCentered)
    {
        if (coveragePercent < criteria.MinimumForegroundCoveragePercent)
        {
            return CreateResult(criteria, false, foregroundCount, coveragePercent, contrastScore, isCentered, "Área de huella insuficiente.");
        }

        if (coveragePercent > criteria.MaximumForegroundCoveragePercent)
        {
            return CreateResult(criteria, false, foregroundCount, coveragePercent, contrastScore, isCentered, "Área de huella excesiva.");
        }

        if (contrastScore < criteria.MinimumContrastScore)
        {
            return CreateResult(criteria, false, foregroundCount, coveragePercent, contrastScore, isCentered, "Contraste de huella insuficiente.");
        }

        if (criteria.RequireCenteredFingerprint && !isCentered)
        {
            return CreateResult(criteria, false, foregroundCount, coveragePercent, contrastScore, false, "La huella no está centrada.");
        }

        return CreateResult(criteria, true, foregroundCount, coveragePercent, contrastScore, isCentered, "Calidad de huella aceptable.");
    }

    private static double CalculateAverage(byte[] imageBuffer, int totalPixels)
    {
        long sum = 0;
        for (int index = 0; index < totalPixels; index++)
        {
            sum += imageBuffer[index];
        }

        return sum / (double)totalPixels;
    }

    private static bool IsForegroundCentered(
        int width,
        int height,
        int minX,
        int minY,
        int maxX,
        int maxY,
        double tolerancePercent)
    {
        if (maxX < minX || maxY < minY)
        {
            return false;
        }

        double imageCenterX = width / 2d;
        double imageCenterY = height / 2d;
        double foregroundCenterX = (minX + maxX) / 2d;
        double foregroundCenterY = (minY + maxY) / 2d;
        double toleranceX = width * tolerancePercent / 100d;
        double toleranceY = height * tolerancePercent / 100d;

        return Math.Abs(foregroundCenterX - imageCenterX) <= toleranceX
            && Math.Abs(foregroundCenterY - imageCenterY) <= toleranceY;
    }

    private static FingerprintQualityResult CreateResult(
        FingerprintQualityCriteria criteria,
        bool isAcceptable,
        int foregroundCount,
        double coveragePercent,
        double contrastScore,
        bool isCentered,
        string message)
    {
        return new FingerprintQualityResult(
            isAcceptable,
            foregroundCount,
            Math.Round(coveragePercent, 2),
            Math.Round(contrastScore, 2),
            isCentered,
            message)
        {
            ScorePercent = Math.Round(CalculateScorePercent(criteria, coveragePercent, contrastScore, isCentered, isAcceptable), 2)
        };
    }

    private static double CalculateScorePercent(
        FingerprintQualityCriteria criteria,
        double coveragePercent,
        double contrastScore,
        bool isCentered,
        bool isAcceptable)
    {
        if (coveragePercent <= 0)
        {
            return 0;
        }

        double coverageScore = CalculateCoverageScore(
            coveragePercent,
            criteria.MinimumForegroundCoveragePercent,
            criteria.MaximumForegroundCoveragePercent);
        double contrastScorePercent = CalculateContrastScore(contrastScore, criteria.MinimumContrastScore);
        double centerScore = !criteria.RequireCenteredFingerprint || isCentered ? 100 : 45;

        double scorePercent = Clamp(coverageScore * 0.45 + contrastScorePercent * 0.45 + centerScore * 0.10, 0, 100);
        return isAcceptable ? scorePercent : Math.Min(scorePercent, MaximumRejectedScorePercent);
    }

    private static double CalculateCoverageScore(double coveragePercent, double minimumCoverage, double maximumCoverage)
    {
        if (coveragePercent <= 0)
        {
            return 0;
        }

        double safeMinimum = Math.Max(1, minimumCoverage);
        double safeMaximum = Math.Max(safeMinimum + 1, maximumCoverage);
        double idealMaximum = Math.Min(safeMaximum, Math.Max(safeMinimum + 20, safeMinimum * 2.5));

        if (coveragePercent < safeMinimum)
        {
            return Clamp(coveragePercent / safeMinimum * 75, 0, 75);
        }

        if (coveragePercent <= idealMaximum)
        {
            double range = Math.Max(1, idealMaximum - safeMinimum);
            return Clamp(85 + ((coveragePercent - safeMinimum) / range * 15), 85, 100);
        }

        if (coveragePercent <= safeMaximum)
        {
            double range = Math.Max(1, safeMaximum - idealMaximum);
            return Clamp(100 - ((coveragePercent - idealMaximum) / range * 25), 75, 100);
        }

        double excessRange = Math.Max(1, 100 - safeMaximum);
        return Clamp(75 - ((coveragePercent - safeMaximum) / excessRange * 75), 0, 75);
    }

    private static double CalculateContrastScore(double contrastScore, double minimumContrast)
    {
        if (contrastScore <= 0)
        {
            return 0;
        }

        double safeMinimum = Math.Max(1, minimumContrast);
        if (contrastScore < safeMinimum)
        {
            return Clamp(contrastScore / safeMinimum * 75, 0, 75);
        }

        double targetContrast = safeMinimum * 1.5;
        if (contrastScore >= targetContrast)
        {
            return 100;
        }

        return Clamp(75 + ((contrastScore - safeMinimum) / Math.Max(1, targetContrast - safeMinimum) * 25), 75, 100);
    }

    private static double Clamp(double value, double minimum, double maximum)
    {
        return Math.Min(maximum, Math.Max(minimum, value));
    }
}
