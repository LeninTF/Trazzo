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
from infrastructure.process_qos import disable_execution_speed_throttling
from services.liveness_service import LivenessService
from services.anti_spoof_service import AntiSpoofingService
from services.capture_service import CaptureService
from services.enrollment_service import EnrollmentService
from services.recognition_service import RecognitionService
from services import face_quality

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")

TENANT = "test"

# Presupuesto de CPU del preview (el reconocimiento completo solo corre al pulsar un botón)
PREVIEW_DETECT_FPS = 5        # detecciones por segundo con rostro presente
PREVIEW_IDLE_S = 0.5          # espera entre detecciones cuando no hay nadie
PREVIEW_SPOOF_EVERY_S = 0.33  # anti-spoof del indicador REAL/FALSO (~3 Hz, ~2 ms por lectura)
PREVIEW_SPOOF_WINDOW = 5      # el indicador usa la mediana de las últimas lecturas
PREVIEW_SPOOF_MIN_SAMPLES = 3
DISPLAY_FPS = 15
MIN_FRAME_INTERVAL = 1.0 / DISPLAY_FPS

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

disable_execution_speed_throttling()
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
    "FACE_ALREADY_ENROLLED": "este rostro ya esta registrado con otro ID",
    "FACE_LOW_QUALITY": "la imagen no tiene calidad suficiente",
    "IDENTITY_INCONSISTENT": "se detectaron personas distintas durante la captura",
    "MISSING_FIELD": "falta un campo obligatorio",
}


def _traducir(msg):
    for code, es in _MENSAJES.items():
        if code in msg:
            if face_quality.POOR_LIGHTING_HINT in msg:
                return f"{es} ({face_quality.POOR_LIGHTING_HINT})"
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

# botones repartidos a lo ancho, en el area del panel (y = CAM_H + margen)
_BTN_TOP = CAM_H + 10
_BTN_BOT = CAM_H + BTN_H - 10
_BTN_LABELS = [
    ("REGISTRAR (E)",   (0, 160, 0),   "enroll"),
    ("VERIFICAR (V)",   (0, 120, 200), "verify"),
    ("IDENTIFICAR (I)", (200, 100, 0), "identify"),
    ("ELIMINAR (D)",    (0, 0, 180),   "delete"),
    ("CERRAR (Q)",      (70, 70, 70),  "quit"),
]
_BTN_GAP = 8
_BTN_W = (WIN_W - _BTN_GAP * (len(_BTN_LABELS) + 1)) // len(_BTN_LABELS)
BUTTONS = []
for i, (label, color, action) in enumerate(_BTN_LABELS):
    x1 = _BTN_GAP + i * (_BTN_W + _BTN_GAP)
    x2 = x1 + _BTN_W
    BUTTONS.append((label, (x1, _BTN_TOP, x2, _BTN_BOT), color, action))

# se activa con el botón CERRAR, la tecla Q o la X de la ventana
_quit_requested = threading.Event()


def start_action(action):
    if action == "quit":
        # permitido aun durante una captura: el finally libera cámara y base de datos
        _quit_requested.set()
        return
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
        (tw, th), _ = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.4, 1)
        tx = x1 + ((x2 - x1) - tw) // 2
        ty = y1 + ((y2 - y1) + th) // 2
        cv2.putText(canvas, label, (tx, ty), cv2.FONT_HERSHEY_SIMPLEX, 0.4, (255, 255, 255), 1)

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


def fit_to_view(frame):
    """Escala el frame (que puede ser 1080p o más) al área de preview SIN deformarlo
    (letterbox). Devuelve la vista y la transformación para ubicar coordenadas del
    frame original (bbox) sobre la vista."""
    h, w = frame.shape[:2]
    scale = min(CAM_W / w, CAM_H / h)
    new_w, new_h = max(1, int(w * scale)), max(1, int(h * scale))
    off_x, off_y = (CAM_W - new_w) // 2, (CAM_H - new_h) // 2
    view = np.zeros((CAM_H, CAM_W, 3), np.uint8)
    # INTER_LINEAR: suficiente para un preview y mucho más barato que INTER_AREA sobre 1080p
    view[off_y:off_y + new_h, off_x:off_x + new_w] = cv2.resize(
        frame, (new_w, new_h), interpolation=cv2.INTER_LINEAR)
    return view, scale, off_x, off_y


next_frame_at = 0.0

# Análisis en vivo en un HILO APARTE. Detección facial, CNN anti-spoof y heurísticas son
# pesadas en CPU: si corren en el hilo de la ventana, cv2.waitKey casi no se ejecuta, la
# ventana deja de atender clics/teclas y parece colgada.
_analysis_lock = threading.Lock()
_stop_analysis = threading.Event()
_latest_frame = None
cached_face = None
cached_spoof = None
cached_lighting = None


def _analysis_worker():
    """Preview con presupuesto de CPU acotado (estándar para equipos compartidos):
    - solo DETECCIÓN (sin embedding de reconocimiento) a PREVIEW_DETECT_FPS;
    - anti-spoof a 1 Hz, solo si hay rostro;
    - sin rostro, reposo más largo (idle backoff).
    El pipeline completo (reconocimiento + liveness) corre únicamente al pulsar un botón.
    """
    global cached_face, cached_spoof, cached_lighting
    last_spoof = 0.0
    # Un frame aislado (parpadeo, desenfoque, reflejo) no debe pintar FALSO: estándar PAD en
    # video es fusionar varias lecturas. Se usa la mediana, robusta ante valores extremos.
    spoof_window = deque(maxlen=PREVIEW_SPOOF_WINDOW)
    while not _stop_analysis.is_set():
        if ui.busy:
            # durante una captura, la cámara y el motor los usa la operación en curso
            time.sleep(0.2)
            continue

        with _analysis_lock:
            frame = _latest_frame
        if frame is None:
            time.sleep(0.1)
            continue

        face = None
        try:
            faces = engine.detect_only(frame)
            face = faces[0] if faces else None
            now = time.monotonic()
            if face is None:
                spoof_window.clear()
            elif now - last_spoof >= PREVIEW_SPOOF_EVERY_S:
                spoof_window.append(anti_spoof.score(frame, face))
                last_spoof = now
            spoof = (float(np.median(spoof_window))
                     if len(spoof_window) >= PREVIEW_SPOOF_MIN_SAMPLES else None)
            light = face_quality.lighting(frame, face) if face is not None else None
            with _analysis_lock:
                cached_face = face
                cached_spoof = spoof
                cached_lighting = light
        except Exception as e:  # el preview nunca debe tumbar la ventana
            logging.warning("analisis en vivo fallo: %s", e)

        time.sleep(1.0 / PREVIEW_DETECT_FPS if face is not None else PREVIEW_IDLE_S)

canvas = np.zeros((WIN_H, WIN_W, 3), np.uint8)
busy_placeholder = np.full((CAM_H, CAM_W, 3), 40, np.uint8)
cv2.putText(busy_placeholder, "PROCESANDO CAPTURA...", (100, CAM_H // 2),
            cv2.FONT_HERSHEY_SIMPLEX, 1.0, (0, 200, 200), 2)
cv2.putText(busy_placeholder, "no te muevas por favor", (150, CAM_H // 2 + 40),
            cv2.FONT_HERSHEY_SIMPLEX, 0.6, (180, 180, 180), 1)

threading.Thread(target=_analysis_worker, daemon=True).start()

try:
    while not _quit_requested.is_set():
        # la X de la ventana también cierra la aplicación
        if cv2.getWindowProperty("Trazzo Camera Test", cv2.WND_PROP_VISIBLE) < 1:
            break
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

        # el hilo de análisis toma este frame; aquí solo se dibuja lo último que calculó
        with _analysis_lock:
            _latest_frame = frame
            face_view, spoof_view, light_view = cached_face, cached_spoof, cached_lighting

        view, scale, off_x, off_y = fit_to_view(frame)
        frame_h, frame_w = frame.shape[:2]
        cv2.putText(view, f"{frame_w}x{frame_h}", (CAM_W - 110, CAM_H - 10),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, (200, 200, 200), 1)

        if face_view is not None:
            # el bbox está en coordenadas del frame original: llevarlo a la vista escalada
            x1, y1, x2, y2 = face_view.bbox
            p1 = (int(x1 * scale + off_x), int(y1 * scale + off_y))
            p2 = (int(x2 * scale + off_x), int(y2 * scale + off_y))
            cv2.rectangle(view, p1, p2, (0, 255, 0), 2)
            cv2.putText(view, f"deteccion={face_view.detection_score:.2f} tamano={face_view.size}",
                        (p1[0], max(20, p1[1] - 8)), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 1)

        # indicador del CNN (mediana de varias lecturas); el liveness completo corre al capturar
        if spoof_view is not None:
            final = spoof_view >= SETTINGS.anti_spoof_min_real
            poor_light = light_view is not None and light_view.poor
            if final:
                color, verdict = (0, 255, 0), "REAL"
            elif poor_light:
                # con luz pobre el CNN no es concluyente: se pide corregir la luz, no se acusa
                color, verdict = (0, 220, 255), "POCA LUZ"
            else:
                color, verdict = (0, 0, 255), "POSIBLE FALSO"
            cv2.putText(view, f"{verdict}  IA={spoof_view:.2f}",
                        (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 2)
            if poor_light and not final:
                cv2.putText(view, "ilumina tu rostro de frente", (10, 55),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.55, (0, 220, 255), 1)
        elif face_view is not None:
            cv2.putText(view, "VERIFICANDO...", (10, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 220, 255), 2)
        if light_view is not None:
            cv2.putText(view, f"luz={light_view.brightness:.0f} color={light_view.saturation:.0f} "
                              f"contraste={light_view.contrast:.0f}",
                        (10, CAM_H - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (200, 200, 200), 1)

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
    _stop_analysis.set()
    cam.release()
    repo.close()
    cv2.destroyAllWindows()
    loop.call_soon_threadsafe(loop.stop)
