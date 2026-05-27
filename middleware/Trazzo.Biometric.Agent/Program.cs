using Trazzo.Biometric.Agent;
using Trazzo.Biometric.Agent.Queue;
using Trazzo.Biometric.Agent.Security;
using Trazzo.Biometric.Agent.Services;
using Trazzo.Biometric.Agent.WebSocket;
using Trazzo.Biometric.Agent.ZKTeco;

var builder = Host.CreateApplicationBuilder(args);

builder.Services.AddWindowsService(options =>
{
    options.ServiceName = "Trazzo Biometric Agent";
});

builder.Services.AddSingleton<IAgentHealthService, AgentHealthService>();
builder.Services.AddSingleton<IZKTecoNativeSdk, ZKTecoNativeSdk>();
builder.Services.AddSingleton<ICryptographyService, HybridCryptographyService>();
builder.Services.AddSingleton<IBiometricScannerService, ZKTecoScannerService>();
builder.Services.AddSingleton<IEventQueue, SqliteEventQueue>();
builder.Services.AddSingleton<IWebSocketServerService, LocalWebSocketServerService>();
builder.Services.AddHostedService<Worker>();
builder.Services.AddHostedService<EventForwarderService>();

var host = builder.Build();
host.Run();
