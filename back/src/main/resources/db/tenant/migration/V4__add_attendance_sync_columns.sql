ALTER TABLE attendances ADD COLUMN IF NOT EXISTS offline_event_id INT;
ALTER TABLE attendances ADD COLUMN IF NOT EXISTS device_code VARCHAR(100);

CREATE UNIQUE INDEX IF NOT EXISTS idx_attendances_offline_event_device
    ON attendances (offline_event_id, device_code)
    WHERE offline_event_id IS NOT NULL AND device_code IS NOT NULL;
