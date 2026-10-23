-- Credencial de maquina de cada lector (agente biometrico): reemplaza al JWT de usuario que caducaba a las 24 h.
-- Solo se guarda el SHA-256 del token; el valor se muestra una vez al emitirlo. Un dispositivo tiene a lo sumo
-- un token vigente: emitir otro revoca el anterior.
CREATE TABLE IF NOT EXISTS agent_device_tokens (
    token_hash   CHAR(64) PRIMARY KEY,
    tenant_id    UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    schema_name  VARCHAR(63) NOT NULL,
    device_id    INT NOT NULL,
    device_code  VARCHAR(100) NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    revoked_at   TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_agent_device_tokens_active
    ON agent_device_tokens (schema_name, device_id) WHERE revoked_at IS NULL;
