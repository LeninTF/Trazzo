using System.Text.Json;
using System.Text.Json.Serialization;

namespace Trazzo.Biometric.Agent.Contracts;

public sealed record WebSocketMessage(
    [property: JsonPropertyName("type")] string Type,
    [property: JsonPropertyName("payload")] JsonElement? Payload = null,
    // Enrolamiento: referencia del usuario (para el padrón local de identificación 1:N) y dedo.
    [property: JsonPropertyName("userRef")] string? UserRef = null,
    [property: JsonPropertyName("fingerIndex")] int FingerIndex = 0);
