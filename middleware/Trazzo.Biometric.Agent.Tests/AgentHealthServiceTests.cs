using System.Text.Json;
using Trazzo.Biometric.Agent;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class AgentHealthServiceTests
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

    [Fact]
    public void GetHealthResult_ReturnsObjectWithTypeHealthCheckResult()
    {
        AgentHealthService service = new();

        object result = service.GetHealthResult();

        using JsonDocument doc = JsonDocument.Parse(JsonSerializer.SerializeToUtf8Bytes(result, JsonOptions));
        Assert.Equal("health.check.result", doc.RootElement.GetProperty("type").GetString());
    }

    [Fact]
    public void GetHealthResult_ReturnsSuccessTrue()
    {
        AgentHealthService service = new();

        object result = service.GetHealthResult();

        using JsonDocument doc = JsonDocument.Parse(JsonSerializer.SerializeToUtf8Bytes(result, JsonOptions));
        Assert.True(doc.RootElement.GetProperty("success").GetBoolean());
    }

    [Fact]
    public void GetHealthResult_ReturnsExpectedMessage()
    {
        AgentHealthService service = new();

        object result = service.GetHealthResult();

        using JsonDocument doc = JsonDocument.Parse(JsonSerializer.SerializeToUtf8Bytes(result, JsonOptions));
        Assert.Equal(
            "El agente biométrico de Trazzo está en ejecución.",
            doc.RootElement.GetProperty("message").GetString());
    }

    [Fact]
    public void GetHealthResult_CalledTwice_ReturnsSameShape()
    {
        AgentHealthService service = new();

        object first = service.GetHealthResult();
        object second = service.GetHealthResult();

        byte[] json1 = JsonSerializer.SerializeToUtf8Bytes(first, JsonOptions);
        byte[] json2 = JsonSerializer.SerializeToUtf8Bytes(second, JsonOptions);
        Assert.Equal(json1, json2);
    }
}
