"""Cifrado en reposo de templates biometricos.

Los embeddings faciales son PII bajo la Ley 29733 peruana. Los guardamos
cifrados con AES-256-GCM (AEAD): cada registro tiene nonce unico y su AAD
bindea el ciphertext a (person_id, tenant_id), de modo que mover una fila
a otra persona hace fallar la autenticacion.

Master key:
  - 32 bytes aleatorios (AES-256)
  - Persistida en %PROGRAMDATA%\\TrazzoAgent\\keys\\master.key
  - Permisos restrictivos (0o600 / ACL solo dueno) al crearla
  - Generada al primer arranque si no existe
"""
import os
import logging
import secrets
import stat
import threading
from typing import Final

from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.exceptions import InvalidTag

from config import SETTINGS

log = logging.getLogger(__name__)

_NONCE_BYTES: Final[int] = 12
_KEY_BYTES: Final[int] = 32       # AES-256
_KEY_VERSION_CURRENT: Final[int] = 1
_AAD_SEP: Final[bytes] = b"\x00"


class CryptoError(Exception):
    pass


class CryptoService:
    _instance: "CryptoService | None" = None
    _lock = threading.Lock()

    def __init__(self) -> None:
        key_dir = os.path.join(os.path.dirname(SETTINGS.db_path), "keys")
        self._key_path = os.path.join(key_dir, "master.key")
        os.makedirs(key_dir, exist_ok=True)

        master = self._load_or_create_key()
        self._aesgcm = AESGCM(master)
        self._key_version = _KEY_VERSION_CURRENT

    @classmethod
    def instance(cls) -> "CryptoService":
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = cls()
        return cls._instance

    def _load_or_create_key(self) -> bytes:
        if os.path.exists(self._key_path):
            with open(self._key_path, "rb") as fh:
                key = fh.read()
            if len(key) != _KEY_BYTES:
                raise CryptoError(f"master key corrupta (bytes={len(key)})")
            log.info("master key cargada desde %s", self._key_path)
            return key

        # generacion nueva: bytes criptograficamente aleatorios
        key = secrets.token_bytes(_KEY_BYTES)
        # escritura atomica: escribimos a temp y renombramos, evita clave parcial en crash
        tmp_path = self._key_path + ".tmp"
        flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL
        if hasattr(os, "O_BINARY"):
            flags |= os.O_BINARY
        fd = os.open(tmp_path, flags, 0o600)
        try:
            with os.fdopen(fd, "wb") as fh:
                fh.write(key)
        except Exception:
            if os.path.exists(tmp_path):
                os.remove(tmp_path)
            raise
        os.replace(tmp_path, self._key_path)

        # en windows chmod es limitado; el 0o600 en O_CREAT ya dio permisos restrictivos
        try:
            os.chmod(self._key_path, stat.S_IRUSR | stat.S_IWUSR)
        except OSError:
            pass

        log.info("master key generada en %s", self._key_path)
        return key

    @property
    def key_version(self) -> int:
        return self._key_version

    @staticmethod
    def _aad(person_id: str, tenant_id: str) -> bytes:
        return person_id.encode("utf-8") + _AAD_SEP + tenant_id.encode("utf-8")

    def encrypt(self, plaintext: bytes, person_id: str, tenant_id: str) -> tuple[bytes, bytes]:
        """Devuelve (nonce, ciphertext). El tag de 16 bytes va al final del ciphertext."""
        nonce = secrets.token_bytes(_NONCE_BYTES)
        ct = self._aesgcm.encrypt(nonce, plaintext, self._aad(person_id, tenant_id))
        return nonce, ct

    def decrypt(self, nonce: bytes, ciphertext: bytes, person_id: str, tenant_id: str) -> bytes:
        try:
            return self._aesgcm.decrypt(nonce, ciphertext, self._aad(person_id, tenant_id))
        except InvalidTag as e:
            # NO exponemos detalle: podria ser tampering o AAD incorrecto
            raise CryptoError("ciphertext no autentica (posible tampering)") from e
