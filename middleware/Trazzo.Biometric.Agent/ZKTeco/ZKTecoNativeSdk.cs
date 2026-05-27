using System.Reflection;
using System.Runtime.Loader;

namespace Trazzo.Biometric.Agent.ZKTeco;

public sealed class ZKTecoNativeSdk : IZKTecoNativeSdk
{
    private readonly Type? _zkfp2Type;

    public ZKTecoNativeSdk(ILogger<ZKTecoNativeSdk> logger)
    {
        try
        {
            _zkfp2Type = LoadSdkType();
            if (_zkfp2Type is null)
            {
                LoadError = "No se encontró la DLL del SDK ZKTeco o no se pudo cargar.";
                logger.LogWarning("{Message}", LoadError);
            }
        }
        catch (Exception ex)
        {
            LoadError = $"No se encontró la DLL del SDK ZKTeco o no se pudo cargar. {ex.Message}";
            logger.LogWarning(ex, "No se pudo cargar el SDK ZKTeco.");
        }
    }

    public bool IsAvailable => _zkfp2Type is not null;

    public string? LoadError { get; }

    public int Init() => InvokeStatic<int>("Init");

    public int Terminate() => InvokeStatic<int>("Terminate");

    public int GetDeviceCount() => InvokeStatic<int>("GetDeviceCount");

    public IntPtr OpenDevice(int index) => InvokeStatic<IntPtr>("OpenDevice", index);

    public int CloseDevice(IntPtr deviceHandle) => InvokeStatic<int>("CloseDevice", deviceHandle);

    public int AcquireFingerprint(IntPtr deviceHandle, byte[] imageBuffer, byte[] template, ref int size)
    {
        EnsureLoaded();

        object?[] args = [deviceHandle, imageBuffer, template, size];
        object? result = Invoke("AcquireFingerprint", args);
        size = args[3] is int updatedSize ? updatedSize : size;

        return Convert.ToInt32(result);
    }

    public bool TryGetParameter(IntPtr deviceHandle, int parameterCode, out int value) =>
        TryInvokeGetParameter(deviceHandle, parameterCode, out value);

    public IntPtr DBInit() => InvokeStatic<IntPtr>("DBInit");

    public int DBFree(IntPtr databaseHandle) => InvokeStatic<int>("DBFree", databaseHandle);

    public int DBMatch(IntPtr databaseHandle, byte[] template1, byte[] template2)
    {
        return InvokeStatic<int>("DBMatch", databaseHandle, template1, template2);
    }

    public int DBIdentify(IntPtr databaseHandle, byte[] template, ref int fingerId, ref int score)
    {
        EnsureLoaded();

        object?[] args = [databaseHandle, template, fingerId, score];
        object? result = Invoke("DBIdentify", args);
        fingerId = args[2] is int updatedFingerId ? updatedFingerId : fingerId;
        score = args[3] is int updatedScore ? updatedScore : score;

        return Convert.ToInt32(result);
    }

    public int DBMerge(IntPtr databaseHandle, byte[] template1, byte[] template2, byte[] template3, byte[] registeredTemplate, ref int registeredTemplateSize)
    {
        EnsureLoaded();

        object?[] args = [databaseHandle, template1, template2, template3, registeredTemplate, registeredTemplateSize];
        object? result = Invoke("DBMerge", args);
        registeredTemplateSize = args[5] is int updatedSize ? updatedSize : registeredTemplateSize;

        return Convert.ToInt32(result);
    }

    private static Type? LoadSdkType()
    {
        Type? loadedType = Type.GetType("libzkfpcsharp.zkfp2, libzkfpcsharp", throwOnError: false);
        if (loadedType is not null)
        {
            return loadedType;
        }

        string sdkPath = Path.Combine(AppContext.BaseDirectory, "Native", "x64", "libzkfpcsharp.dll");
        if (!File.Exists(sdkPath))
        {
            sdkPath = Path.Combine(Environment.CurrentDirectory, "Native", "x64", "libzkfpcsharp.dll");
        }

        if (!File.Exists(sdkPath))
        {
            return null;
        }

        Assembly sdkAssembly = AssemblyLoadContext.Default.LoadFromAssemblyPath(Path.GetFullPath(sdkPath));
        return sdkAssembly.GetType("libzkfpcsharp.zkfp2", throwOnError: false);
    }

    private T InvokeStatic<T>(string methodName, params object?[] args)
    {
        object? result = Invoke(methodName, args);
        if (result is null)
        {
            throw new InvalidOperationException($"El método {methodName} del SDK ZKTeco devolvió null.");
        }

        return result is T typed ? typed : (T)Convert.ChangeType(result, typeof(T))!;
    }

    private object? Invoke(string methodName, object?[] args)
    {
        EnsureLoaded();

        MethodInfo method = _zkfp2Type!.GetMethod(methodName, BindingFlags.Public | BindingFlags.Static)
            ?? throw new MissingMethodException(_zkfp2Type.FullName, methodName);

        return method.Invoke(null, args);
    }

    private bool TryInvokeGetParameter(IntPtr deviceHandle, int parameterCode, out int value)
    {
        value = 0;
        EnsureLoaded();

        MethodInfo? method = _zkfp2Type!.GetMethod("GetParameters", BindingFlags.Public | BindingFlags.Static);
        if (method is null)
        {
            return false;
        }

        byte[] parameterBuffer = new byte[4];
        int parameterSize = parameterBuffer.Length;
        object?[] args = [deviceHandle, parameterCode, parameterBuffer, parameterSize];
        object? result;

        try
        {
            result = method.Invoke(null, args);
        }
        catch (TargetParameterCountException)
        {
            return false;
        }

        if (Convert.ToInt32(result) != 0)
        {
            return false;
        }

        value = BitConverter.ToInt32(parameterBuffer, 0);
        return value > 0;
    }

    private void EnsureLoaded()
    {
        if (_zkfp2Type is null)
        {
            throw new InvalidOperationException(LoadError ?? "El SDK ZKTeco no está disponible.");
        }
    }
}
