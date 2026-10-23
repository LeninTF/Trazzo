namespace Trazzo.Biometric.Agent.Services;

public interface IWebSocketServerService
{
    Task StartAsync(CancellationToken cancellationToken);

    Task StopAsync(CancellationToken cancellationToken);

    /// <summary>Envía el mensaje (serializado como JSON) a todos los clientes conectados.</summary>
    Task BroadcastAsync(object message, CancellationToken cancellationToken);
}
