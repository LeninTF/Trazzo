using Trazzo.Biometric.Agent;
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
builder.Services.AddSingleton<IBiometricScannerService, ZKTecoScannerService>();
builder.Services.AddSingleton<IWebSocketServerService, LocalWebSocketServerService>();
builder.Services.AddHostedService<Worker>();

var host = builder.Build();
host.Run();
