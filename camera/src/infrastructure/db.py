import os
import logging
import sqlite3
import threading
from contextlib import contextmanager
from typing import Iterator

import numpy as np

from config import SETTINGS
from domain.errors import BiometricError, ErrorCode
from domain.models import EnrolledFace
from infrastructure.crypto import CryptoService, CryptoError

log = logging.getLogger(__name__)

_SCHEMA = """
CREATE TABLE IF NOT EXISTS face_embeddings (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    person_id   TEXT NOT NULL,
    tenant_id   TEXT NOT NULL,
    nonce       BLOB NOT NULL,
    ciphertext  BLOB NOT NULL,
    key_version INTEGER NOT NULL DEFAULT 1,
    model       TEXT NOT NULL DEFAULT 'buffalo_sc',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_tenant ON face_embeddings(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_person ON face_embeddings(tenant_id, person_id);
"""

_EMBEDDING_DIM_MAX = 2048

# Todos los registros anteriores a la columna `model` se crearon con buffalo_sc (único
# modelo usado hasta entonces), así que la migración los marca con ese valor.
_LEGACY_MODEL = "buffalo_sc"


class FaceRepository:
    def __init__(self) -> None:
        db_dir = os.path.dirname(SETTINGS.db_path)
        os.makedirs(db_dir, exist_ok=True)

        self._crypto = CryptoService.instance()
        self._lock = threading.RLock()
        self._conn = sqlite3.connect(
            SETTINGS.db_path,
            check_same_thread=False,
            timeout=5.0,
            isolation_level=None,
        )
        self._conn.row_factory = sqlite3.Row
        self._conn.execute("PRAGMA journal_mode=WAL")
        self._conn.execute("PRAGMA synchronous=NORMAL")
        self._conn.execute("PRAGMA foreign_keys=ON")
        self._conn.executescript(_SCHEMA)
        self._migrate()
        self._closed = False

    def _migrate(self) -> None:
        """Agrega la columna `model` a bases creadas antes de existir.

        Cada embedding solo es comparable con embeddings del MISMO modelo: dos modelos
        distintos producen vectores de igual dimensión (512) pero en espacios diferentes, y
        compararlos da similitudes sin sentido (falsos positivos silenciosos).
        """
        columns = {row["name"] for row in self._conn.execute("PRAGMA table_info(face_embeddings)")}
        if "model" not in columns:
            self._conn.execute(
                f"ALTER TABLE face_embeddings ADD COLUMN model TEXT NOT NULL DEFAULT '{_LEGACY_MODEL}'"
            )
            log.info("migracion: columna 'model' agregada (registros existentes = %s)", _LEGACY_MODEL)
        self._conn.execute(
            "CREATE INDEX IF NOT EXISTS idx_tenant_model ON face_embeddings(tenant_id, model)"
        )

    @contextmanager
    def _tx(self) -> Iterator[sqlite3.Cursor]:
        with self._lock:
            if self._closed:
                raise BiometricError(ErrorCode.INTERNAL_ERROR, "db cerrada")
            cur = self._conn.cursor()
            try:
                cur.execute("BEGIN IMMEDIATE")
                yield cur
                cur.execute("COMMIT")
            except Exception:
                try:
                    cur.execute("ROLLBACK")
                except sqlite3.Error:
                    pass
                raise
            finally:
                cur.close()

    def count_for_tenant(self, tenant_id: str) -> int:
        with self._tx() as cur:
            cur.execute(
                "SELECT COUNT(*) AS n FROM face_embeddings WHERE tenant_id = ?",
                (tenant_id,),
            )
            row = cur.fetchone()
            return int(row["n"]) if row else 0

    def replace_person(self, face: EnrolledFace) -> None:
        if face.embedding.size == 0 or face.embedding.size > _EMBEDDING_DIM_MAX:
            raise BiometricError(ErrorCode.INTERNAL_ERROR, "embedding size fuera de rango")

        plaintext = face.embedding.astype(np.float32, copy=False).tobytes()
        nonce, ciphertext = self._crypto.encrypt(plaintext, face.person_id, face.tenant_id)

        with self._tx() as cur:
            cur.execute(
                "SELECT COUNT(*) AS n FROM face_embeddings WHERE tenant_id = ? AND person_id != ?",
                (face.tenant_id, face.person_id),
            )
            row = cur.fetchone()
            other = int(row["n"]) if row else 0
            if other >= SETTINGS.max_gallery_per_tenant:
                raise BiometricError(ErrorCode.GALLERY_FULL)

            cur.execute(
                "DELETE FROM face_embeddings WHERE person_id = ? AND tenant_id = ?",
                (face.person_id, face.tenant_id),
            )
            cur.execute(
                "INSERT INTO face_embeddings (person_id, tenant_id, nonce, ciphertext, key_version, model) "
                "VALUES (?, ?, ?, ?, ?, ?)",
                (face.person_id, face.tenant_id, nonce, ciphertext, self._crypto.key_version,
                 SETTINGS.model_pack),
            )

    def delete_person(self, person_id: str, tenant_id: str) -> int:
        with self._tx() as cur:
            cur.execute(
                "DELETE FROM face_embeddings WHERE person_id = ? AND tenant_id = ?",
                (person_id, tenant_id),
            )
            return cur.rowcount

    def count_other_models(self, tenant_id: str) -> int:
        """Registros del tenant creados con un modelo distinto al actual (requieren re-enrolar)."""
        with self._tx() as cur:
            cur.execute(
                "SELECT COUNT(*) AS n FROM face_embeddings WHERE tenant_id = ? AND model != ?",
                (tenant_id, SETTINGS.model_pack),
            )
            row = cur.fetchone()
            return int(row["n"]) if row else 0

    def load_person(self, person_id: str, tenant_id: str) -> list[np.ndarray]:
        # solo templates del modelo activo: los de otro modelo no son comparables
        with self._tx() as cur:
            cur.execute(
                "SELECT nonce, ciphertext FROM face_embeddings "
                "WHERE tenant_id = ? AND person_id = ? AND model = ?",
                (tenant_id, person_id, SETTINGS.model_pack),
            )
            rows = cur.fetchall()

        embeddings: list[np.ndarray] = []
        for row in rows:
            try:
                plain = self._crypto.decrypt(row["nonce"], row["ciphertext"], person_id, tenant_id)
            except CryptoError:
                log.error("registro corrupto o alterado para %s/%s — se ignora",
                          tenant_id, person_id)
                continue
            embeddings.append(np.frombuffer(plain, dtype=np.float32))
        return embeddings

    def iter_gallery(self, tenant_id: str) -> Iterator[tuple[str, np.ndarray]]:
        # Se leen las filas cifradas DENTRO de la transacción y se descifran/entregan FUERA.
        # Antes el generador mantenía abierta una transacción de escritura (BEGIN IMMEDIATE)
        # entre cada yield: bloqueaba otras escrituras y, si el consumidor escribía antes de
        # agotarlo (p. ej. validar duplicados y luego guardar), fallaba con transacción anidada.
        with self._tx() as cur:
            cur.execute(
                "SELECT person_id, nonce, ciphertext FROM face_embeddings "
                "WHERE tenant_id = ? AND model = ?",
                (tenant_id, SETTINGS.model_pack),
            )
            rows = [(row["person_id"], bytes(row["nonce"]), bytes(row["ciphertext"]))
                    for row in cur.fetchall()]

        for pid, nonce, ciphertext in rows:
            try:
                plain = self._crypto.decrypt(nonce, ciphertext, pid, tenant_id)
            except CryptoError:
                log.error("registro corrupto o alterado para %s/%s — se ignora", tenant_id, pid)
                continue
            yield pid, np.frombuffer(plain, dtype=np.float32)

    def close(self) -> None:
        with self._lock:
            if self._closed:
                return
            self._closed = True
            try:
                self._conn.close()
            except sqlite3.Error:
                pass
