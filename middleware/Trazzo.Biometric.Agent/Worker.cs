using Trazzo.Biometric.Agent.Contracts;
using Trazzo.Biometric.Agent.Backend;
using Trazzo.Biometric.Agent.Services;

namespace Trazzo.Biometric.Agent;

public sealed class Worker(
    IWebSocketServerService webSocketServer,
    IBiometricScannerService scannerService,
    ICryptographyService cryptoService,
    IConfiguration configuration,
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
            await LogStartupReportAsync(stoppingToken);
        }
        catch (OperationCanceledException ex) when (stoppingToken.IsCancellationRequested)
        {
            logger.LogInformation(ex, "Detención solicitada para el agente biométrico de Trazzo.");
        }
    }

    public override async Task StopAsync(CancellationToken cancellationToken)
    {
        logger.LogInformation("Deteniendo el agente biométrico de Trazzo.");

        await webSocketServer.StopAsync(cancellationToken);

        await base.StopAsync(cancellationToken);
    }

    private async Task LogStartupReportAsync(CancellationToken cancellationToken)
    {
        logger.LogInformation("=== Trazzo Biometric Agent — Reporte de inicio ===");

        if (cryptoService.IsConfigured)
            logger.LogInformation("  Cifrado RSA:       ACTIVO — templates cifrados con AES-256-GCM + RSA-2048.");
        else
            logger.LogWarning("  Cifrado RSA:       INACTIVO — configure Backend:BaseUrl antes de produccion.");

        FingerprintDeviceStatus deviceStatus = await scannerService.GetStatusAsync(cancellationToken);
        if (deviceStatus.IsConnected)
            logger.LogInformation("  Lector biometrico: CONECTADO — {DeviceCount} dispositivo(s) detectado(s).", deviceStatus.DeviceCount);
        else
            logger.LogWarning("  Lector biometrico: SIN LECTOR — conecte el ZK9500 y reinicie el servicio.");

        string? backendUrl = BackendEndpointResolver.ResolveAttendanceSyncUrl(configuration);
        if (!string.IsNullOrWhiteSpace(backendUrl))
            logger.LogInformation("  Cola de eventos:   ACTIVA — reenvio automatico al backend habilitado.");
        else
            logger.LogWarning("  Cola de eventos:   INACTIVA — configure Backend:BaseUrl para habilitar el reenvio.");

        string[] origins = configuration.GetSection("Agent:AllowedOrigins").Get<string[]>() ?? [];
        if (origins.Length > 0)
            logger.LogInformation("  WebSocket CORS:    RESTRINGIDO — {Count} origen(es) permitido(s).", origins.Length);
        else
            logger.LogWarning("  WebSocket CORS:    ABIERTO — configure Agent:AllowedOrigins en produccion.");

        string? tenantId = configuration["Agent:TenantId"];
        if (!string.IsNullOrWhiteSpace(tenantId))
            logger.LogInformation("  Tenant ID:         CONFIGURADO — X-Tenant-ID enviado en cada solicitud al backend.");
        else
            logger.LogWarning("  Tenant ID:         NO CONFIGURADO — configure Agent:TenantId antes de produccion.");

        string? deviceCode = configuration["Agent:DeviceCode"];
        if (!string.IsNullOrWhiteSpace(deviceCode))
            logger.LogInformation("  Device Code:       CONFIGURADO - usado para CoreHR biometria.");
        else
            logger.LogWarning("  Device Code:       NO CONFIGURADO - configure Agent:DeviceCode para enrolamiento remoto CoreHR.");

        bool autoUpdateEnabled = configuration.GetValue("AutoUpdate:Enabled", false);
        string? manifestUrl = configuration["AutoUpdate:ManifestUrl"];
        if (autoUpdateEnabled && !string.IsNullOrWhiteSpace(manifestUrl))
        {
            int checkIntervalMinutes = configuration.GetValue("AutoUpdate:CheckIntervalMinutes", 60);
            logger.LogInformation("  Auto-Update:       ACTIVO — verificacion cada {Minutes} min.", checkIntervalMinutes);
        }
        else if (autoUpdateEnabled)
            logger.LogWarning("  Auto-Update:       HABILITADO pero sin ManifestUrl — configure AutoUpdate:ManifestUrl.");
        else
            logger.LogInformation("  Auto-Update:       DESHABILITADO — habilite AutoUpdate:Enabled en produccion.");

        logger.LogInformation("=================================================");
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
