-- attendances.state era el enum de PostgreSQL attendance_status_enum, pero la entidad lo envia como texto
-- (@Enumerated STRING) y el INSERT de una marcacion fallaba. Pasa a VARCHAR con los mismos valores permitidos.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'attendances'
                 AND column_name = 'state' AND udt_name = 'attendance_status_enum') THEN
        ALTER TABLE attendances ALTER COLUMN state TYPE VARCHAR(20) USING state::text;
    END IF;
END $$;

ALTER TABLE attendances DROP CONSTRAINT IF EXISTS chk_attendances_state;
ALTER TABLE attendances ADD CONSTRAINT chk_attendances_state CHECK (state IN ('PUNTUAL', 'TARDANZA', 'FALTA'));
