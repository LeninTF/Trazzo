UPDATE monthly_closures SET created_by_user_id = '00000000-0000-0000-0000-000000000000' WHERE created_by_user_id IS NULL;
ALTER TABLE monthly_closures ALTER COLUMN created_by_user_id SET NOT NULL;

DELETE FROM monthly_closures_details WHERE tenant_user_id IS NULL;
ALTER TABLE monthly_closures_details ALTER COLUMN tenant_user_id SET NOT NULL;
