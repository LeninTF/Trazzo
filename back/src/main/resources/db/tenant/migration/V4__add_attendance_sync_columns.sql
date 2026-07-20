ALTER TABLE attendances ADD COLUMN offline_event_id INT;
ALTER TABLE attendances ADD COLUMN device_code VARCHAR(100);

CREATE UNIQUE INDEX idx_attendances_offline_event_device
    ON attendances (offline_event_id, device_code)
    WHERE offline_event_id IS NOT NULL AND device_code IS NOT NULL;
