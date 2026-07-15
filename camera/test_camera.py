import sys
import time
import asyncio
import logging
import threading
from collections import deque

import numpy as np
import cv2

sys.path.insert(0, "src")

from config import SETTINGS
from infrastructure.camera import CameraCapture
from infrastructure.face_engine import FaceEngine
from infrastructure.db import FaceRepository
from services.liveness_service import LivenessService, _lap_var, _crop
from services.anti_spoof_service import AntiSpoofingService
from services.capture_service import CaptureService
from services.enrollment_service import EnrollmentService
from services.recognition_service import RecognitionService

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")

TENANT = "test"

DETECT_EVERY_S = 0.20
LIVENESS_EVERY_S = 0.50
DISPLAY_FPS = 20
MIN_FRAME_INTERVAL = 1.0 / DISPLAY_FPS
RECENT_MAX = 5

# tamano fijo del area de la camara + panel abajo
CAM_W, CAM_H = 640, 480
BTN_H = 60
STATUS_H = 60
WIN_W = CAM_W
WIN_H = CAM_H + BTN_H + STATUS_H

def _detectar_camara_disponible():
    """Prueba desde el indice configurado hacia abajo hasta encontrar una que abra."""
    for i in [SETTINGS.camera_index, 0, 1, 2]:
        cap = cv2.VideoCapture(i, cv2.CAP_DSHOW)
        if cap.isOpened():
            ok, frame = cap.read()
            cap.release()
            if ok and frame is not None and frame.size > 0:
                return i
        else:
            cap.release()
    return None


print(f"cam={SETTINGS.camera_index} backend={SETTINGS.camera_backend}")
detected = _detectar_camara_disponible()
if detected is None:
    print("ERROR: no encuentro ninguna camara disponible. Conecta una y vuelve a correr.")
    sys.exit(1)
if detected != SETTINGS.camera_index:
    print(f"AVISO: camara indice {SETTINGS.camera_index} no disponible; usando indice {detected}")
    import os as _os
    _os.environ["CAMERA_INDEX"] = str(detected)
    # forzar recarga del SETTINGS
    object.__setattr__(SETTINGS, "camera_index", detected)

engine = FaceEngine.instance()
cam = CameraCapture()
repo = FaceRepository()
liveness = LivenessService()
anti_spoof = AntiSpoofingService.instance()
capture = CaptureService(cam, engine, liveness, anti_spoof)
enrollment = EnrollmentService(capture, repo)
recognition = RecognitionService(capture, repo)


loop = asyncio.new_event_loop()
threading.Thread(target=loop.run_forever, daemon=True).start()


_MENSAJES = {
    "NO_FACE_DETECTED": "no se detecto ningun rostro",
    "MULTIPLE_FACES": "hay mas de una persona en la camara",
    "FACE_TOO_SMALL": "acercate mas a la camara",
    "CAMERA_UNAVAILABLE": "no se pudo abrir la camara",
    "CAMERA_FRAME_INVALID": "la camara no devolvio imagen",
    "SPOOF_DETECTED": "posible foto o pantalla detectada",
    "PERSON_NOT_ENROLLED": "esa persona no esta registrada",
    "NO_ENROLLED_FACES": "no hay nadie registrado todavia",
    "NO_MATCH": "no se encontro coincidencia",
    "GALLERY_FULL": "la galeria esta llena",
    "MISSING_FIELD": "falta un campo obligatorio",
}


def _traducir(msg):
    for code, es in _MENSAJES.items():
        if code in msg:
            return es
    return msg


class UI:
    def __init__(self):
        self.busy = False
        self.banner = "listo"
        self.color = (200, 200, 200)
        self.prompt = None
        self.text = ""
        self.challenge_text = ""    # texto grande del challenge activo

    def run(self, coro_factory, on_ok):
        self.busy = True
        self.banner = "procesando..."
        self.color = (0, 200, 200)

        def done(fut):
            self.busy = False
            self.challenge_text = ""
            try:
                on_ok(fut.result())
            except Exception as e:
                self.result(f"ERROR: {_traducir(str(e))}", ok=False)

        coro = coro_factory(self._on_prompt)
        asyncio.run_coroutine_threadsafe(coro, loop).add_done_callback(done)

    def _on_prompt(self, p):
        # ejecutado desde el thread del asyncio loop; solo asignamos strings
        self.challenge_text = p.text.upper()

    def result(self, msg, ok=True):
        self.banner = msg
        self.color = (0, 200, 0) if ok else (0, 0, 220)


ui = UI()

# 4 botones repartidos a lo ancho, en el area del panel (y = CAM_H + margen)
_BTN_TOP = CAM_H + 10
_BTN_BOT = CAM_H + BTN_H - 10
_BTN_W = (WIN_W - 50) // 4  # 50 = 5 gaps de 10px
_BTN_LABELS = [
    ("REGISTRAR (E)",   (0, 160, 0),   "enroll"),
    ("VERIFICAR (V)",   (0, 120, 200), "verify"),
    ("IDENTIFICAR (I)", (200, 100, 0), "identify"),
    ("ELIMINAR (D)",    (0, 0, 180),   "delete"),
]
BUTTONS = []
for i, (label, color, action) in enumerate(_BTN_LABELS):
    x1 = 10 + i * (_BTN_W + 10)
    x2 = x1 + _BTN_W
    BUTTONS.append((label, (x1, _BTN_TOP, x2, _BTN_BOT), color, action))


def start_action(action):
    if ui.busy:
        return
    if action == "identify":
        ui.run(
            lambda cb: recognition.identify(TENANT, on_prompt=cb),
            lambda r: ui.result(f"IDENTIFICADO -> {r[0].person_id}  puntaje={r[0].score}"),
        )
        return
    ui.prompt = action
    ui.text = ""
    accion_es = {"enroll": "REGISTRAR", "verify": "VERIFICAR", "delete": "ELIMINAR"}[action]
    ui.banner = f"escribe el ID de la persona para {accion_es} (Enter confirma, Esc cancela)"
    ui.color = (255, 255, 0)


def execute_prompt():
    action = ui.prompt
    person = ui.text.strip()
    ui.prompt = None
    ui.text = ""
    if not person:
        ui.result("cancelado (ID vacio)", ok=False)
        return
    if action == "enroll":
        ui.run(
            lambda cb: enrollment.enroll(person, TENANT, on_prompt=cb),
            lambda r: ui.result(f"REGISTRADO {r['personId']} ({r['samples']} muestras)"),
        )
    elif action == "verify":
        ui.run(
            lambda cb: recognition.verify(person, TENANT, on_prompt=cb),
            lambda r: ui.result(f"COINCIDE {r[0].person_id}  puntaje={r[0].score}"),
        )
    elif action == "delete":
        removed = enrollment.delete(person, TENANT)
        ui.result(f"ELIMINADO {person} ({removed} registros)", ok=removed > 0)


def on_click(event, x, y, flags, param):
    if event != cv2.EVENT_LBUTTONDOWN:
        return
    for _l, (x1, y1, x2, y2), _c, action in BUTTONS:
        if x1 <= x <= x2 and y1 <= y <= y2:
            start_action(action)
            return


cv2.namedWindow("Trazzo Camera Test")
cv2.setMouseCallback("Trazzo Camera Test", on_click)


def draw_panel(canvas):
    # fondo oscuro para el panel de controles y estado
    cv2.rectangle(canvas, (0, CAM_H), (WIN_W, WIN_H), (25, 25, 30), -1)
    # linea divisoria camara/panel
    cv2.line(canvas, (0, CAM_H), (WIN_W, CAM_H), (80, 80, 80), 1)
    # divisoria botones/status
    cv2.line(canvas, (0, CAM_H + BTN_H), (WIN_W, CAM_H + BTN_H), (55, 55, 60), 1)

    for label, (x1, y1, x2, y2), color, _a in BUTTONS:
        cv2.rectangle(canvas, (x1, y1), (x2, y2), color, -1)
        cv2.rectangle(canvas, (x1, y1), (x2, y2), (255, 255, 255), 1)
        # centrado del texto
        (tw, th), _ = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.5, 1)
        tx = x1 + ((x2 - x1) - tw) // 2
        ty = y1 + ((y2 - y1) + th) // 2
        cv2.putText(canvas, label, (tx, ty), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1)

    y_status = CAM_H + BTN_H
    cv2.putText(canvas, ui.banner, (10, y_status + 25),
                cv2.FONT_HERSHEY_SIMPLEX, 0.55, ui.color, 1)
    if ui.prompt:
        cv2.putText(canvas, f"ID persona: {ui.text}_", (10, y_status + 50),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 1)


def blit_frame(canvas, frame):
    # ajustar el frame de la camara al area exacta (por si la cam da otra resolucion)
    if frame.shape[1] != CAM_W or frame.shape[0] != CAM_H:
        frame = cv2.resize(frame, (CAM_W, CAM_H))
    canvas[:CAM_H, :CAM_W] = frame


recent_frames = deque(maxlen=RECENT_MAX)
recent_faces = deque(maxlen=RECENT_MAX)
last_detect = 0.0
last_live_eval = 0.0
last_spoof_eval = 0.0
cached_face = None
cached_liveness = None
cached_spoof = None
next_frame_at = 0.0

canvas = np.zeros((WIN_H, WIN_W, 3), np.uint8)
busy_placeholder = np.full((CAM_H, CAM_W, 3), 40, np.uint8)
cv2.putText(busy_placeholder, "PROCESANDO CAPTURA...", (100, CAM_H // 2),
            cv2.FONT_HERSHEY_SIMPLEX, 1.0, (0, 200, 200), 2)
cv2.putText(busy_placeholder, "no te muevas por favor", (150, CAM_H // 2 + 40),
            cv2.FONT_HERSHEY_SIMPLEX, 0.6, (180, 180, 180), 1)

try:
    while True:
        now = time.monotonic()

        if ui.busy:
            # mostrar el prompt del challenge activo en grande
            busy_view = np.full((CAM_H, CAM_W, 3), 40, np.uint8)
            if ui.challenge_text:
                # sombra para legibilidad
                (tw, _), _ = cv2.getTextSize(ui.challenge_text, cv2.FONT_HERSHEY_SIMPLEX, 1.4, 3)
                tx = max(10, (CAM_W - tw) // 2)
                cv2.putText(busy_view, ui.challenge_text, (tx, CAM_H // 2),
                            cv2.FONT_HERSHEY_SIMPLEX, 1.4, (0, 220, 255), 3)
                cv2.putText(busy_view, "sigue la instruccion", (180, CAM_H // 2 + 40),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.6, (180, 180, 180), 1)
            else:
                cv2.putText(busy_view, "PREPARANDO...", (170, CAM_H // 2),
                            cv2.FONT_HERSHEY_SIMPLEX, 1.0, (0, 200, 200), 2)
            blit_frame(canvas, busy_view)
            draw_panel(canvas)
            cv2.imshow("Trazzo Camera Test", canvas)
            if (cv2.waitKey(50) & 0xFF) == ord("q"):
                break
            continue

        wait = next_frame_at - now
        if wait > 0:
            time.sleep(wait)
        next_frame_at = time.monotonic() + MIN_FRAME_INTERVAL

        try:
            frame = cam.read()
        except Exception as e:
            print("ERROR camara:", e)
            print("cerrando (la camara se desconecto o no responde)")
            break

        if now - last_detect >= DETECT_EVERY_S:
            faces = engine.detect(frame)
            last_detect = now
            if faces:
                cached_face = faces[0]
                recent_frames.append(frame)
                recent_faces.append(cached_face)
            else:
                cached_face = None

        if cached_face is not None and now - last_spoof_eval >= LIVENESS_EVERY_S:
            cached_spoof = anti_spoof.score(frame, cached_face)
            last_spoof_eval = now

        if len(recent_faces) >= 3 and now - last_live_eval >= LIVENESS_EVERY_S:
            cached_liveness = liveness.evaluate(list(recent_frames), list(recent_faces))
            last_live_eval = now

        view = frame if frame.shape[:2] == (CAM_H, CAM_W) else cv2.resize(frame, (CAM_W, CAM_H))
        view = view.copy()

        if cached_face is not None:
            b = cached_face.bbox.astype(int)
            cv2.rectangle(view, (b[0], b[1]), (b[2], b[3]), (0, 255, 0), 2)
            cv2.putText(view, f"deteccion={cached_face.detection_score:.2f} tamano={cached_face.size}",
                        (b[0], max(20, b[1] - 8)), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 1)

        # veredicto combinado: el CNN manda; las heuristicas son secundarias
        if cached_spoof is not None:
            from config import SETTINGS as _S
            cnn_real = cached_spoof >= _S.anti_spoof_min_real
            heur_real = cached_liveness.is_live if cached_liveness else True
            final = cnn_real and heur_real
            color = (0, 255, 0) if final else (0, 0, 255)
            verdict = "REAL" if final else "FALSO"
            cv2.putText(view, f"{verdict}  IA={cached_spoof:.2f}",
                        (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 2)
            if cached_liveness is not None:
                r = cached_liveness
                cv2.putText(view,
                            f"heur={r.score:.2f} tex={r.signals.texture_score:.0f} "
                            f"fft={r.signals.fft_score:.2f} piel={r.signals.skin_score:.2f}",
                            (10, 55), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (200, 200, 200), 1)
                cv2.putText(view,
                            f"motion={r.signals.motion_variance:.2f} "
                            f"lm_indep={r.signals.landmark_independence:.2f}",
                            (10, 78), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (200, 200, 200), 1)
                if r.reason:
                    cv2.putText(view, r.reason[:50], (10, 100),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.4, (0, 100, 255), 1)

        blit_frame(canvas, view)
        draw_panel(canvas)
        cv2.imshow("Trazzo Camera Test", canvas)

        key = cv2.waitKey(1) & 0xFF
        if key == 255:
            continue

        if ui.prompt:
            if key == 13:
                execute_prompt()
            elif key == 27:
                ui.prompt = None
                ui.text = ""
                ui.result("cancelado", ok=False)
            elif key == 8:
                ui.text = ui.text[:-1]
            elif 32 <= key <= 126:
                ui.text += chr(key)
            continue

        if key == ord("q"):
            break
        if key == ord("e"): start_action("enroll")
        elif key == ord("v"): start_action("verify")
        elif key == ord("i"): start_action("identify")
        elif key == ord("d"): start_action("delete")

finally:
    cam.release()
    repo.close()
    cv2.destroyAllWindows()
    loop.call_soon_threadsafe(loop.stop)
