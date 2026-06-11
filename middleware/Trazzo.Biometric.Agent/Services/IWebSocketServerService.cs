namespace Trazzo.Biometric.Agent.Services;

public interface IWebSocketServerService
{
    Task StartAsync(CancellationToken cancellationToken);

    Task StopAsync(CancellationToken cancellationToken);
}
