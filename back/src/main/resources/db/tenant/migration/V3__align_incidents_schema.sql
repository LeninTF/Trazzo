-- Align incidents tables with JPA entities (add missing columns)

-- V1 already defines comment and rejection_reason as TEXT; these are no-ops if present.
ALTER TABLE incidencias ADD COLUMN IF NOT EXISTS comment TEXT;
ALTER TABLE incidencias ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

-- incidencia_evidencia: add missing columns
ALTER TABLE incidencia_evidencia ADD COLUMN IF NOT EXISTS file_key VARCHAR(255);
ALTER TABLE incidencia_evidencia ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE incidencia_evidencia ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE incidencia_evidencia ADD COLUMN IF NOT EXISTS uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE incidencia_evidencia ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Backfill uploaded_at from updated_at for existing rows
UPDATE incidencia_evidencia SET uploaded_at = updated_at WHERE uploaded_at IS NULL;
UPDATE incidencia_evidencia SET created_at = updated_at WHERE created_at IS NULL;
