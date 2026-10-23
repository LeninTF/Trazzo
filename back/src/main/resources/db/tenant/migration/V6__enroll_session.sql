-- Sesiones de enrolamiento: vivian en memoria, se perdian al reiniciar y no servian con varias instancias.
-- Una fila por token; se consume con DELETE ... RETURNING y caduca por expires_at.
CREATE TABLE IF NOT EXISTS enroll_session (
    enroll_token   VARCHAR(64) PRIMARY KEY,
    tenant_user_id INT NOT NULL REFERENCES tenant_user(id) ON DELETE CASCADE,
    device_id      INT NOT NULL REFERENCES device(id) ON DELETE CASCADE,
    finger_index   INT NOT NULL,
    device_code    VARCHAR(100) NOT NULL,
    expires_at     TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_enroll_session_device_code ON enroll_session (device_code, expires_at);
CREATE INDEX IF NOT EXISTS idx_enroll_session_user_device ON enroll_session (tenant_user_id, device_id);
