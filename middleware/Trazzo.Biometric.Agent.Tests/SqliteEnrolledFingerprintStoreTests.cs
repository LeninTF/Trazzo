using Microsoft.Extensions.Logging.Abstractions;
using Trazzo.Biometric.Agent.Enrollment;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class SqliteEnrolledFingerprintStoreTests
{
    private static SqliteEnrolledFingerprintStore CreateStore()
    {
        // SQLite en memoria: la conexión keep-alive mantiene el esquema vivo durante el test.
        string connectionString = $"Data Source=enrolled-{Guid.NewGuid():N};Mode=Memory;Cache=Shared";
        return new SqliteEnrolledFingerprintStore(connectionString, NullLogger<SqliteEnrolledFingerprintStore>.Instance);
    }

    [Fact]
    public async Task SaveAsync_LuegoGetAll_DevuelveLaHuellaConTemplateIntacto()
    {
        using var store = CreateStore();
        byte[] template = Enumerable.Range(0, 300).Select(v => (byte)(v % 256)).ToArray();

        await store.SaveAsync(new EnrolledFingerprint("user-42", 1, template));
        var all = await store.GetAllAsync();

        var fp = Assert.Single(all);
        Assert.Equal("user-42", fp.UserReference);
        Assert.Equal(1, fp.FingerIndex);
        Assert.Equal(template, fp.Template);
    }

    [Fact]
    public async Task SaveAsync_ReEnrolarMismoUsuarioYDedo_ReemplazaElTemplate()
    {
        using var store = CreateStore();
        await store.SaveAsync(new EnrolledFingerprint("user-1", 2, [1, 2, 3]));
        await store.SaveAsync(new EnrolledFingerprint("user-1", 2, [9, 8, 7, 6]));

        var all = await store.GetAllAsync();

        var fp = Assert.Single(all);
        Assert.Equal(new byte[] { 9, 8, 7, 6 }, fp.Template);
    }

    [Fact]
    public async Task GetAllAsync_ConVariosUsuarios_DevuelveTodos()
    {
        using var store = CreateStore();
        await store.SaveAsync(new EnrolledFingerprint("user-1", 1, [1]));
        await store.SaveAsync(new EnrolledFingerprint("user-2", 1, [2]));
        await store.SaveAsync(new EnrolledFingerprint("user-1", 2, [3]));

        var all = await store.GetAllAsync();

        Assert.Equal(3, all.Count);
    }
}
