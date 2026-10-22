from infrastructure import face_engine
from infrastructure.process_qos import disable_execution_speed_throttling
from config import SETTINGS


def test_missing_pack_falls_back_to_repo_pack(tmp_path, override_setting):
    (tmp_path / "models" / "buffalo_sc").mkdir(parents=True)
    (tmp_path / "models" / "buffalo_sc" / "det_500m.onnx").write_bytes(b"x")
    override_setting("models_dir", str(tmp_path))
    override_setting("model_pack", "trazzo_r50")

    face_engine._ensure_model_pack()

    assert SETTINGS.model_pack == "buffalo_sc"


def test_installed_pack_is_kept(tmp_path, override_setting):
    (tmp_path / "models" / "trazzo_r50").mkdir(parents=True)
    (tmp_path / "models" / "trazzo_r50" / "w600k_r50.onnx").write_bytes(b"x")
    override_setting("models_dir", str(tmp_path))
    override_setting("model_pack", "trazzo_r50")

    face_engine._ensure_model_pack()

    assert SETTINGS.model_pack == "trazzo_r50"


def test_qos_call_never_raises():
    assert disable_execution_speed_throttling() in (True, False)
