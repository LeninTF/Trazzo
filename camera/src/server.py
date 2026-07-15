import asyncio
import json
import logging
import signal

import websockets
from websockets import ConnectionClosed

from config import SETTINGS
from domain.contracts import error_response
from domain.errors import ErrorCode
from infrastructure.camera import CameraCapture
from infrastructure.face_engine import FaceEngine
from infrastructure.db import FaceRepository
from services.liveness_service import LivenessService
from services.anti_spoof_service import AntiSpoofingService
from services.capture_service import CaptureService
from services.enrollment_service import EnrollmentService
from services.recognition_service import RecognitionService
from handlers.router import MessageRouter

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
log = logging.getLogger("camera")


class App:
    def __init__(self) -> None:
        self.camera = CameraCapture()
        self.engine = FaceEngine.instance()
        self.repo = FaceRepository()

        liveness = LivenessService()
        anti_spoof = AntiSpoofingService.instance()
        capture = CaptureService(self.camera, self.engine, liveness, anti_spoof)
        enrollment = EnrollmentService(capture, self.repo)
        recognition = RecognitionService(capture, self.repo)
        self.router = MessageRouter(self.camera, enrollment, recognition)

        self._client_sem = asyncio.Semaphore(SETTINGS.max_concurrent_clients)
        self._active_clients = 0

    async def handle_client(self, ws) -> None:
        # limite duro de conexiones concurrentes; extras se cierran con 1013 (try again later)
        if self._client_sem.locked():
            await ws.close(code=1013, reason="server busy")
            return

        async with self._client_sem:
            self._active_clients += 1
            peer = _safe_peer(ws)
            log.info("cliente conectado: %s (activos=%d)", peer, self._active_clients)
            try:
                await self._read_loop(ws)
            except ConnectionClosed:
                pass
            except asyncio.CancelledError:
                raise
            except Exception:
                # Nunca propagar stack traces al cliente
                log.exception("error inesperado en cliente %s", peer)
            finally:
                self._active_clients -= 1
                log.info("cliente desconectado: %s (activos=%d)", peer, self._active_clients)

    async def _read_loop(self, ws) -> None:
        async for raw in ws:
            if not isinstance(raw, str):
                # binarios rechazados: solo JSON textual
                await ws.send(json.dumps(error_response(ErrorCode.INVALID_JSON.value)))
                continue
            response = await self.router.dispatch(raw)
            await ws.send(json.dumps(response))

    def shutdown(self) -> None:
        log.info("cerrando...")
        try:
            self.camera.release()
        finally:
            self.repo.close()


def _safe_peer(ws) -> str:
    try:
        return str(ws.remote_address)
    except (AttributeError, OSError):
        return "unknown"


async def main() -> None:
    app = App()
    loop = asyncio.get_running_loop()
    stop = loop.create_future()

    def _stop(*_: object) -> None:
        if not stop.done():
            stop.set_result(None)

    try:
        loop.add_signal_handler(signal.SIGTERM, _stop)
        loop.add_signal_handler(signal.SIGINT, _stop)
    except NotImplementedError:
        pass  # windows no lo soporta

    log.info("ws://%s:%d (max_msg=%dB, max_clients=%d)",
             SETTINGS.websocket_host, SETTINGS.websocket_port,
             SETTINGS.max_message_bytes, SETTINGS.max_concurrent_clients)

    async with websockets.serve(
        app.handle_client,
        SETTINGS.websocket_host,
        SETTINGS.websocket_port,
        max_size=SETTINGS.max_message_bytes,
        max_queue=8,
        ping_interval=20,
        ping_timeout=20,
        close_timeout=5,
    ):
        try:
            await stop
        finally:
            app.shutdown()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        pass
