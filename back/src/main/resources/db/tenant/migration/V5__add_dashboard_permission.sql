-- ==============================================================================
-- Add dashboard.ver permission for the admin-tenant dashboard endpoints.
-- TenantSchemaMigrator re-runs on every startup, so all statements must be
-- idempotent.
-- ==============================================================================

INSERT INTO permissions (id, name, description)
SELECT gen_random_uuid(), seed.name, seed.description
FROM (VALUES
    ('dashboard.ver', 'Ver dashboard administrativo del tenant')
) AS seed(name, description)
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.name = seed.name);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM (VALUES
    ('administrador', 'dashboard.ver'),
    ('director', 'dashboard.ver'),
    ('coordinador', 'dashboard.ver')
) AS grants(role_name, permission_name)
JOIN role r ON r.name = grants.role_name
JOIN permissions p ON p.name = grants.permission_name
ON CONFLICT DO NOTHING;
