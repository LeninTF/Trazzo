namespace Trazzo.Biometric.Agent.ZKTeco;

public interface IZKTecoNativeSdk
{
    bool IsAvailable { get; }

    string? LoadError { get; }

    int Init();

    int Terminate();

    int GetDeviceCount();

    IntPtr OpenDevice(int index);

    int CloseDevice(IntPtr deviceHandle);

    int AcquireFingerprint(IntPtr deviceHandle, byte[] imageBuffer, byte[] template, ref int size);

    bool TryGetParameter(IntPtr deviceHandle, int parameterCode, out int value);

    bool TryGetParameterString(IntPtr deviceHandle, int parameterCode, out string value);

    IntPtr DBInit();

    int DBFree(IntPtr databaseHandle);

    int DBMatch(IntPtr databaseHandle, byte[] template1, byte[] template2);

    int DBIdentify(IntPtr databaseHandle, byte[] template, ref int fingerId, ref int score);

    int DBMerge(IntPtr databaseHandle, byte[] template1, byte[] template2, byte[] template3, byte[] registeredTemplate, ref int registeredTemplateSize)
    {
        registeredTemplateSize = 0;
        return -1;
    }
}
