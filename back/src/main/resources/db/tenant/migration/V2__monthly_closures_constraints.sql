-- Idempotent migration: enforce NOT NULL and UNIQUE constraints on monthly_closures
-- tables that may have been created without them in early tenant provisions.
-- V4 re-applies these same guards; both scripts are safe to re-run.

-- 1. Backfill created_by_user_id with a sentinel UUID where NULL, then enforce NOT NULL
UPDATE monthly_closures SET created_by_user_id = '00000000-0000-0000-0000-000000000000'
WHERE created_by_user_id IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'monthly_closures' AND column_name = 'created_by_user_id' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE monthly_closures ALTER COLUMN created_by_user_id SET NOT NULL;
    END IF;
END $$;

-- 2. Delete orphan detail rows with NULL tenant_user_id, then enforce NOT NULL
DELETE FROM monthly_closures_details WHERE tenant_user_id IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'monthly_closures_details' AND column_name = 'tenant_user_id' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE monthly_closures_details ALTER COLUMN tenant_user_id SET NOT NULL;
    END IF;
END $$;

-- 3. Add UNIQUE constraint on (year, month) if it does not exist yet
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_monthly_closures_period'
    ) THEN
        ALTER TABLE monthly_closures ADD CONSTRAINT uq_monthly_closures_period UNIQUE (year, month);
    END IF;
END $$;
