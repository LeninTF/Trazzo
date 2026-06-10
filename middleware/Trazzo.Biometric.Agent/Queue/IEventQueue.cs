namespace Trazzo.Biometric.Agent.Queue;

public interface IEventQueue
{
    Task EnqueueAsync(BiometricEvent biometricEvent, CancellationToken cancellationToken = default);

    Task<IReadOnlyList<BiometricEvent>> GetPendingAsync(int limit = 50, CancellationToken cancellationToken = default);

    Task MarkSentAsync(IEnumerable<long> ids, CancellationToken cancellationToken = default);

    Task MarkFailedAsync(long id, CancellationToken cancellationToken = default);

    Task<int> GetPendingCountAsync(CancellationToken cancellationToken = default);

    Task PruneAsync(TimeSpan maxAge, CancellationToken cancellationToken = default);
}
