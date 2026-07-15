import os
import pytest


def test_round_trip(crypto):
    data = b"embedding secreto"
    nonce, ct = crypto.encrypt(data, "juan", "trazzo")
    assert crypto.decrypt(nonce, ct, "juan", "trazzo") == data


def test_nonce_es_unico_por_llamada(crypto):
    _, ct1 = crypto.encrypt(b"x", "a", "t")
    _, ct2 = crypto.encrypt(b"x", "a", "t")
    assert ct1 != ct2  # nonce distinto => ciphertext distinto


def test_tampering_bit_flip_detectado(crypto):
    from infrastructure.crypto import CryptoError
    nonce, ct = crypto.encrypt(b"payload", "juan", "trazzo")
    tampered = bytearray(ct)
    tampered[0] ^= 0x01
    with pytest.raises(CryptoError):
        crypto.decrypt(nonce, bytes(tampered), "juan", "trazzo")


def test_swap_persona_detectado(crypto):
    from infrastructure.crypto import CryptoError
    nonce, ct = crypto.encrypt(b"embedding de juan", "juan", "trazzo")
    with pytest.raises(CryptoError):
        crypto.decrypt(nonce, ct, "pedro", "trazzo")


def test_swap_tenant_detectado(crypto):
    from infrastructure.crypto import CryptoError
    nonce, ct = crypto.encrypt(b"embedding", "juan", "tenant_a")
    with pytest.raises(CryptoError):
        crypto.decrypt(nonce, ct, "juan", "tenant_b")


def test_master_key_se_reusa_entre_instancias(crypto):
    # forzar nueva instancia y verificar que aun descifra
    from infrastructure.crypto import CryptoService
    nonce, ct = crypto.encrypt(b"payload", "juan", "trazzo")

    CryptoService._instance = None  # reset singleton
    fresh = CryptoService.instance()
    assert fresh.decrypt(nonce, ct, "juan", "trazzo") == b"payload"


def test_master_key_persistente_en_disco(crypto):
    assert os.path.exists(crypto._key_path)
    assert os.path.getsize(crypto._key_path) == 32


def test_master_key_corrupta_es_rechazada(crypto):
    from infrastructure.crypto import CryptoService, CryptoError
    with open(crypto._key_path, "wb") as fh:
        fh.write(b"corrupta")  # tamano incorrecto

    CryptoService._instance = None
    with pytest.raises(CryptoError):
        CryptoService.instance()


def test_key_version_expuesta(crypto):
    assert crypto.key_version >= 1
