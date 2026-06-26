using System.Text.Json;
using System.Text.Json.Serialization;

namespace Trazzo.Biometric.Agent.Contracts;

public sealed record WebSocketMessage(
    [property: JsonPropertyName("type")] string Type,
    [property: JsonPropertyName("payload")] JsonElement? Payload = null);
