"""Calidad de servicio (QoS) del proceso en Windows.

Windows 11 aplica EcoQoS / "modo eficiencia" a procesos que considera de fondo (servicios,
procesos sin ventana en primer plano) y en CPUs híbridas (núcleos P + E, p. ej. Intel 12ª-14ª
gen) los agenda en los núcleos de eficiencia. Medido en este proyecto: la misma inferencia
tardaba 116 ms en una ejecución y 2.2 s en otra, según dónde lo ubicara el planificador.

Para un servicio biométrico que debe responder en tiempo real, la práctica documentada es
excluir el proceso de la limitación de velocidad de ejecución con SetProcessInformation
(ProcessPowerThrottling). No cambia la prioridad ni la cantidad de hilos: solo evita que
Windows degrade su rendimiento. En otros sistemas operativos no hace nada.
"""
import ctypes
import logging
import sys

log = logging.getLogger(__name__)

_PROCESS_POWER_THROTTLING = 4                       # PROCESS_INFORMATION_CLASS.ProcessPowerThrottling
_PROCESS_POWER_THROTTLING_CURRENT_VERSION = 1
_PROCESS_POWER_THROTTLING_EXECUTION_SPEED = 0x1


class _ProcessPowerThrottlingState(ctypes.Structure):
    _fields_ = [
        ("Version", ctypes.c_ulong),
        ("ControlMask", ctypes.c_ulong),
        ("StateMask", ctypes.c_ulong),
    ]


def disable_execution_speed_throttling() -> bool:
    """Pide a Windows no aplicar EcoQoS al proceso actual. Devuelve True si se aplicó."""
    if sys.platform != "win32":
        return False
    try:
        kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)
        state = _ProcessPowerThrottlingState(
            Version=_PROCESS_POWER_THROTTLING_CURRENT_VERSION,
            ControlMask=_PROCESS_POWER_THROTTLING_EXECUTION_SPEED,
            StateMask=0,  # 0 = sin throttling de velocidad de ejecución
        )
        kernel32.GetCurrentProcess.restype = ctypes.c_void_p
        kernel32.SetProcessInformation.argtypes = [
            ctypes.c_void_p, ctypes.c_int, ctypes.c_void_p, ctypes.c_ulong,
        ]
        ok = kernel32.SetProcessInformation(
            kernel32.GetCurrentProcess(),
            _PROCESS_POWER_THROTTLING,
            ctypes.byref(state),
            ctypes.sizeof(state),
        )
        if not ok:
            log.warning("no se pudo desactivar EcoQoS (error %d)", ctypes.get_last_error())
            return False
        log.info("EcoQoS desactivado para el proceso (sin limitacion de velocidad de ejecucion)")
        return True
    except (OSError, AttributeError) as exc:  # Windows sin la API (anterior a Windows 8)
        log.warning("EcoQoS no disponible: %s", exc)
        return False
