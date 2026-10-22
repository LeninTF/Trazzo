import numpy as np
import pytest

from domain.errors import BiometricError, ErrorCode
from domain.models import EnrolledFace


def _enroll(repo, person_id, tenant, seed=0):
    rng = np.random.default_rng(seed)
    emb = rng.standard_normal(512).astype(np.float32)
    emb /= np.linalg.norm(emb)
    repo.replace_person(EnrolledFace(person_id, tenant, emb))
    return emb


def test_insert_y_load_round_trip(repo, emb):
    repo.replace_person(EnrolledFace("juan", "trazzo", emb))
    loaded = repo.load_person("juan", "trazzo")
    assert len(loaded) == 1
    assert np.allclose(loaded[0], emb)


def test_replace_reemplaza_no_agrega(repo):
    _enroll(repo, "juan", "trazzo", seed=1)
    _enroll(repo, "juan", "trazzo", seed=2)  # segundo enroll
    assert len(repo.load_person("juan", "trazzo")) == 1


def test_load_persona_inexistente_devuelve_vacio(repo):
    assert repo.load_person("nadie", "trazzo") == []


def test_delete_devuelve_count(repo, emb):
    repo.replace_person(EnrolledFace("juan", "trazzo", emb))
    assert repo.delete_person("juan", "trazzo") == 1
    assert repo.delete_person("juan", "trazzo") == 0


def test_delete_no_afecta_otras_personas(repo):
    _enroll(repo, "juan", "trazzo", 1)
    _enroll(repo, "pedro", "trazzo", 2)
    repo.delete_person("juan", "trazzo")
    assert len(repo.load_person("pedro", "trazzo")) == 1


def test_tenant_isolation(repo):
    _enroll(repo, "juan", "tenant_a", 1)
    _enroll(repo, "juan", "tenant_b", 2)
    a = repo.load_person("juan", "tenant_a")
    b = repo.load_person("juan", "tenant_b")
    assert len(a) == 1
    assert len(b) == 1
    assert not np.allclose(a[0], b[0])


def test_count_for_tenant(repo):
    _enroll(repo, "juan", "trazzo", 1)
    _enroll(repo, "pedro", "trazzo", 2)
    _enroll(repo, "otro", "otra_org", 3)
    assert repo.count_for_tenant("trazzo") == 2
    assert repo.count_for_tenant("otra_org") == 1
    assert repo.count_for_tenant("vacio") == 0


def test_iter_gallery_streaming(repo):
    for i in range(5):
        _enroll(repo, f"person_{i}", "trazzo", seed=i)
    ids = [pid for pid, _ in repo.iter_gallery("trazzo")]
    assert set(ids) == {f"person_{i}" for i in range(5)}


def test_gallery_full_bloquea_insert(repo, override_setting):
    override_setting("max_gallery_per_tenant", 2)
    _enroll(repo, "juan", "trazzo", 1)
    _enroll(repo, "pedro", "trazzo", 2)
    with pytest.raises(BiometricError) as exc:
        _enroll(repo, "carlos", "trazzo", 3)
    assert exc.value.code == ErrorCode.GALLERY_FULL


def test_embedding_no_se_guarda_en_claro(repo, emb):
    """El blob en la DB no debe contener el embedding sin cifrar."""
    repo.replace_person(EnrolledFace("juan", "trazzo", emb))
    with repo._tx() as cur:
        cur.execute("SELECT ciphertext FROM face_embeddings LIMIT 1")
        blob = bytes(cur.fetchone()["ciphertext"])
    assert emb.tobytes() not in blob


def test_close_es_idempotente(repo):
    repo.close()
    repo.close()  # no debe crashear


def test_operacion_tras_close_falla(repo, emb):
    repo.close()
    with pytest.raises(BiometricError):
        repo.replace_person(EnrolledFace("juan", "trazzo", emb))


def test_embedding_vacio_rechazado(repo):
    with pytest.raises(BiometricError):
        repo.replace_person(EnrolledFace("juan", "trazzo", np.array([], np.float32)))


def test_iter_gallery_no_bloquea_escrituras_mientras_se_consume(repo):
    """Leer la galería y escribir antes de agotar el iterador no debe fallar."""
    _enroll(repo, "juan", "trazzo", 1)
    _enroll(repo, "pedro", "trazzo", 2)

    gallery = repo.iter_gallery("trazzo")
    next(gallery)
    _enroll(repo, "carlos", "trazzo", 3)  # antes: "cannot start a transaction within a transaction"

    assert repo.count_for_tenant("trazzo") == 3


def test_solo_se_comparan_templates_del_modelo_activo(repo, override_setting):
    override_setting("model_pack", "buffalo_sc")
    _enroll(repo, "juan", "trazzo", 1)

    override_setting("model_pack", "buffalo_l")
    _enroll(repo, "pedro", "trazzo", 2)

    assert [pid for pid, _ in repo.iter_gallery("trazzo")] == ["pedro"]
    assert repo.load_person("juan", "trazzo") == []
    assert repo.count_other_models("trazzo") == 1


def test_migra_base_sin_columna_model(tmp_path, override_setting):
    """Bases creadas antes de la columna `model` se migran y conservan sus registros."""
    import sqlite3
    from infrastructure.crypto import CryptoService
    from infrastructure.db import FaceRepository

    db_path = tmp_path / "legacy.db"
    override_setting("db_path", str(db_path))
    CryptoService._instance = None
    emb = np.random.default_rng(9).standard_normal(512).astype(np.float32)
    emb /= np.linalg.norm(emb)
    nonce, ct = CryptoService.instance().encrypt(emb.tobytes(), "ana", "trazzo")

    con = sqlite3.connect(db_path)
    con.executescript("""
        CREATE TABLE face_embeddings (
            id INTEGER PRIMARY KEY AUTOINCREMENT, person_id TEXT NOT NULL, tenant_id TEXT NOT NULL,
            nonce BLOB NOT NULL, ciphertext BLOB NOT NULL, key_version INTEGER NOT NULL DEFAULT 1,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP);
    """)
    con.execute("INSERT INTO face_embeddings (person_id, tenant_id, nonce, ciphertext) VALUES (?,?,?,?)",
                ("ana", "trazzo", nonce, ct))
    con.commit()
    con.close()

    override_setting("model_pack", "buffalo_sc")
    migrated = FaceRepository()
    try:
        loaded = migrated.load_person("ana", "trazzo")
        assert len(loaded) == 1 and np.allclose(loaded[0], emb)
    finally:
        migrated.close()
