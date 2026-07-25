-- ==============================================================================
-- V5: Days-of-week support for schedules (pivot table).
--
-- Each schedule now has an associated list of weekdays (Mon..Sun). Modeled as a
-- simple pivot table `schedule_day` with the schedule PK as FK and a `day_of_week`
-- VARCHAR(10) holding the Java time DayOfWeek enum string (MONDAY, TUESDAY,
-- WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY) so the OpenAPI JSON contract
-- (array of strings) is stored verbatim.
--
-- TenantSchemaMigrator re-runs every script in db/tenant/migration/ on every app
-- startup for every already-provisioned tenant, so every statement is idempotent.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS schedule_day (
    id              BIGSERIAL PRIMARY KEY,
    schedule_id     BIGINT NOT NULL,
    day_of_week     VARCHAR(10) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT schedule_day_schedule_fk FOREIGN KEY (schedule_id)
        REFERENCES schedule (id) ON DELETE CASCADE,
    CONSTRAINT schedule_day_day_ck CHECK (day_of_week IN
        ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'))
);

-- Idempotent patch: if the table was created by an earlier version of this script
-- with VARCHAR(3) + CHECK('MON',...) the column could not store the full Java
-- DayOfWeek enum names used by the mapper. Re-widen the column and replace the
-- check constraint. ALTER TYPE and DROP/ADD CONSTRAINT are safe to run repeatedly:
-- failures are ignored by TenantSchemaMigrator's per-script error handler, but we
-- also wrap them in DO blocks to keep logs clean.
DO $$
BEGIN
    ALTER TABLE schedule_day ALTER COLUMN day_of_week TYPE VARCHAR(10);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

DO $$
BEGIN
    ALTER TABLE schedule_day DROP CONSTRAINT IF EXISTS schedule_day_day_ck;
    ALTER TABLE schedule_day ADD CONSTRAINT schedule_day_day_ck
        CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'));
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_schedule_day_schedule_day
    ON schedule_day (schedule_id, day_of_week);

CREATE INDEX IF NOT EXISTS ix_schedule_day_schedule_id
    ON schedule_day (schedule_id);

-- Add a unique index on (tenant_user_id, schedule_id) so the "ON CONFLICT DO
-- NOTHING" idempotency rule for user_schedule bulk inserts is enforced at the
-- database level (currently no constraint covers this; the V5 column adds the
-- idempotency guarantee the frontend depends on).
CREATE UNIQUE INDEX IF NOT EXISTS ux_user_schedule_tenant_schedule
    ON user_schedule (tenant_user_id, schedule_id);



