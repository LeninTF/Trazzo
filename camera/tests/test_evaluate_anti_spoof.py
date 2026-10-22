import importlib.util
import os

_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                     "scripts", "evaluate_anti_spoof.py")
_spec = importlib.util.spec_from_file_location("evaluate_anti_spoof", _PATH)
eva = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(eva)


def test_captura_pasa_con_frames_reales_consistentes():
    assert eva.capture_passes([0.9] * 20, 0.6) == 1.0


def test_captura_no_pasa_si_todos_los_frames_son_ataque():
    assert eva.capture_passes([0.1] * 40, 0.6) == 0.0


def test_ataque_intermitente_no_pasa_por_la_fusion_temporal():
    # 1 frame aceptado y 2 rechazados: nunca llega a 30 rechazos seguidos, pero la mediana es baja
    scores = [0.9, 0.1, 0.1] * 20
    assert eva.capture_passes(scores, 0.6) == 0.0


def test_racha_de_rechazos_aborta_la_captura():
    scores = [0.9] * 3 + [0.1] * 30 + [0.9] * 10
    # desde el inicio: 3 aceptados y luego 30 rechazos seguidos -> aborta
    assert eva.capture_passes(scores, 0.6) < 1.0


def test_reporte_recomienda_umbral_que_bloquea_ataques():
    recordings = [
        {"kind": "real", "scores": [0.8, 0.85, 0.9, 0.7, 0.75] * 4, "poor_light_ratio": 0.0},
        {"kind": "celular_foto", "scores": [0.05, 0.2, 0.4, 0.1] * 5, "poor_light_ratio": 0.0},
    ]
    rep = eva.report(recordings)
    assert rep["recommended_threshold"] is not None
    assert 0.4 < rep["recommended_threshold"] <= 0.7
