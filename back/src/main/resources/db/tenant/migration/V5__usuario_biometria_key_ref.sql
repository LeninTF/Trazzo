-- Al enrolar, el template se vuelve a sellar con la clave de almacenamiento (app.biometric.storage-key):
-- key_ref la identifica. key_ref NULL = fila legada, el payload de transporte tal como llego del agente,
-- que se sigue abriendo con las claves RSA.
ALTER TABLE usuario_biometria ADD COLUMN IF NOT EXISTS key_ref VARCHAR(32);

-- Una fila sellada no lleva llave AES envuelta en RSA.
ALTER TABLE usuario_biometria ALTER COLUMN encrypted_aes_key_base64 DROP NOT NULL;
