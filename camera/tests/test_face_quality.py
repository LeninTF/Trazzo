import numpy as np
import pytest

from domain.models import DetectedFace
from services import face_quality as fq


def _face(bbox=(200, 150, 400, 350), kps=None, score=0.95):
    if kps is None:
        # frontal: nariz centrada entre ojos y a mitad entre ojos y boca
        kps = [[250, 220], [350, 220], [300, 260], [260, 300], [340, 300]]
    return DetectedFace(
        bbox=np.array(bbox, np.float32),
        keypoints=np.array(kps, np.float32),
        embedding=np.zeros(512, np.float32),
        detection_score=score,
    )


def _textured_frame():
    return np.random.default_rng(0).integers(0, 255, (480, 640, 3), np.uint8)


def test_cara_frontal_nitida_es_aceptada():
    q = fq.assess(_textured_frame(), _face())
    assert q.ok, q.issue


def test_rechaza_deteccion_poco_confiable():
    q = fq.assess(_textured_frame(), _face(score=0.3))
    assert q.issue == fq.QualityIssue.LOW_DETECTION


def test_rechaza_cara_pequena(override_setting):
    override_setting("min_face_size", 300)
    q = fq.assess(_textured_frame(), _face())
    assert q.issue == fq.QualityIssue.TOO_SMALL


def test_rechaza_cara_cortada_por_el_borde():
    q = fq.assess(_textured_frame(), _face(bbox=(0, 150, 200, 350)))
    assert q.issue == fq.QualityIssue.CUT_OFF


def test_rechaza_cara_de_perfil():
    girada = [[250, 220], [350, 220], [345, 260], [260, 300], [340, 300]]  # nariz muy desplazada
    q = fq.assess(_textured_frame(), _face(kps=girada))
    assert q.issue == fq.QualityIssue.NOT_FRONTAL
    assert abs(q.yaw) > 0.15


def test_rechaza_cabeza_muy_inclinada():
    mirando_abajo = [[250, 220], [350, 220], [300, 296], [260, 300], [340, 300]]  # nariz casi en la boca
    q = fq.assess(_textured_frame(), _face(kps=mirando_abajo))
    assert q.issue == fq.QualityIssue.NOT_FRONTAL


def test_rechaza_imagen_borrosa():
    import cv2
    borroso = cv2.GaussianBlur(_textured_frame(), (0, 0), 6.0)
    q = fq.assess(borroso, _face())
    assert q.issue == fq.QualityIssue.BLURRY


def test_nitidez_no_depende_de_la_resolucion():
    import cv2
    frame = _textured_frame()
    grande = cv2.resize(frame, (1280, 960), interpolation=cv2.INTER_NEAREST)
    s_chico = fq.sharpness(frame, _face())
    s_grande = fq.sharpness(grande, _face(bbox=(400, 300, 800, 700)))
    assert s_grande == pytest.approx(s_chico, rel=0.5)


def test_luz_buena_no_se_marca_como_pobre():
    frame = np.zeros((480, 640, 3), np.uint8)
    rng = np.random.default_rng(1)
    # piel con color (BGR) y sombreado lateral, como un rostro iluminado de frente
    shade = np.linspace(0.25, 1.0, 200)[None, :, None]
    skin = np.array((110, 140, 200))[None, None, :] * shade
    frame[150:350, 200:400] = np.clip(skin + rng.normal(0, 8, (200, 200, 3)), 0, 255)
    light = fq.lighting(frame, _face())
    assert not light.poor


def test_imagen_gris_y_plana_se_marca_como_poca_luz():
    frame = np.zeros((480, 640, 3), np.uint8)
    rng = np.random.default_rng(2)
    gray = np.clip(rng.normal(125, 15, (200, 200)), 0, 255).astype(np.uint8)
    frame[150:350, 200:400] = np.stack([gray, gray, gray], axis=2)
    light = fq.lighting(frame, _face())
    assert light.poor
    assert light.saturation < 45


def test_imagen_oscura_se_marca_como_poca_luz():
    frame = np.full((480, 640, 3), 30, np.uint8)
    assert fq.lighting(frame, _face()).poor
