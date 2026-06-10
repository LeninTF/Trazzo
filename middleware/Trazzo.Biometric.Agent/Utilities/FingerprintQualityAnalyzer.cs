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

        if (coveragePercent < criteria.MinimumForegroundCoveragePercent)
        {
            return CreateResult(false, foregroundCount, coveragePercent, contrastScore, isCentered, "Área de huella insuficiente.");
        }

        if (coveragePercent > criteria.MaximumForegroundCoveragePercent)
        {
            return CreateResult(false, foregroundCount, coveragePercent, contrastScore, isCentered, "Área de huella excesiva.");
        }

        if (contrastScore < criteria.MinimumContrastScore)
        {
            return CreateResult(false, foregroundCount, coveragePercent, contrastScore, isCentered, "Contraste de huella insuficiente.");
        }

        if (criteria.RequireCenteredFingerprint && !isCentered)
        {
            return CreateResult(false, foregroundCount, coveragePercent, contrastScore, false, "La huella no está centrada.");
        }

        return CreateResult(true, foregroundCount, coveragePercent, contrastScore, isCentered, "Calidad de huella aceptable.");
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
            message);
    }
}
