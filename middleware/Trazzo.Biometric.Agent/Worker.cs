using Trazzo.Biometric.Agent.Services;

namespace Trazzo.Biometric.Agent;

public sealed class Worker(
    IWebSocketServerService webSocketServer,
    IBiometricScannerService scannerService,
    ICryptographyService cryptoService,
    ILogger<Worker> logger) : BackgroundService
{
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        logger.LogInformation("Agente biométrico de Trazzo iniciado.");

        try
        {
            await cryptoService.InitializeAsync(stoppingToken);
            await scannerService.InitializeAsync(stoppingToken);
            await webSocketServer.StartAsync(stoppingToken);
        }
        catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
        {
            logger.LogInformation("Detención solicitada para el agente biométrico de Trazzo.");
        }
        catch (Exception ex)
        {
            logger.LogCritical(ex, "El agente biométrico de Trazzo se detuvo inesperadamente.");
            throw;
        }
    }

    public override async Task StopAsync(CancellationToken cancellationToken)
    {
        logger.LogInformation("Deteniendo el agente biométrico de Trazzo.");

        await webSocketServer.StopAsync(cancellationToken);

        await base.StopAsync(cancellationToken);
    }
}

public sealed class AgentHealthService : IAgentHealthService
{
    public object GetHealthResult()
    {
        return new
        {
            type = "health.check.result",
            success = true,
            message = "El agente biométrico de Trazzo está en ejecución."
        };
    }
}
