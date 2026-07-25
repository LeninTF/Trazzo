-- ==============================================================================
-- Multi-tenancy: una sola base física, tenants separados por PostgreSQL schema.
-- Reemplaza las credenciales de conexión por-tenant (db_host/db_port/db_user/
-- db_password) por el nombre del schema del tenant dentro de la misma base.
-- No hay tenants reales provisionados todavía en ningún ambiente, así que no
-- se requiere backfill de datos existentes.
-- ==============================================================================

ALTER TABLE tenant_settings
    ADD COLUMN schema_name VARCHAR(63) NOT NULL DEFAULT 'public';

ALTER TABLE tenant_settings
    ALTER COLUMN schema_name DROP DEFAULT;

ALTER TABLE tenant_settings
    DROP COLUMN db_name,
    DROP COLUMN db_host,
    DROP COLUMN db_port,
    DROP COLUMN db_user,
    DROP COLUMN db_password;
