namespace Trazzo.Biometric.Agent.AutoUpdate;

internal sealed record UpdateManifest(
    string Version,
    string DownloadUrl,
    string Sha256);
