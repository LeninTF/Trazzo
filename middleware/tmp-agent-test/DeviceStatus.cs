using System;
using System.Net.WebSockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

using var ws = new ClientWebSocket();
await ws.ConnectAsync(new Uri("ws://localhost:9001/"), CancellationToken.None);
byte[] payload = Encoding.UTF8.GetBytes("{\"type\":\"device.status\"}");
await ws.SendAsync(payload, WebSocketMessageType.Text, true, CancellationToken.None);
byte[] buffer = new byte[8192];
using var ms = new System.IO.MemoryStream();
WebSocketReceiveResult result;
do
{
    result = await ws.ReceiveAsync(buffer, CancellationToken.None);
    ms.Write(buffer, 0, result.Count);
}
while (!result.EndOfMessage);
Console.WriteLine(Encoding.UTF8.GetString(ms.ToArray()));
await ws.CloseAsync(WebSocketCloseStatus.NormalClosure, "done", CancellationToken.None);
