-- ==============================================================================
-- Granular SaaS-admin RBAC: a fixed 15-permission catalog (module.action) that
-- must stay in sync with the frontend's `modulos` array
-- (front/src/app/features/admin-saas/gestion-roles/gestion-roles.ts) and the
-- backend's PermissionCatalog.ALL_CODES constant. Permission codes use the
-- frontend's existing "{moduloId}.{accionId}" key format verbatim.
-- ==============================================================================

CREATE TABLE permissions_master (
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(80) UNIQUE NOT NULL,
    module_id   VARCHAR(50) NOT NULL,
    action_id   VARCHAR(50) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role_permissions_master (
    role_id       INT REFERENCES roles_master(id) ON DELETE CASCADE,
    permission_id INT REFERENCES permissions_master(id) ON DELETE CASCADE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);

-- Machine slug (roles_master.name) vs human label, since the frontend distinguishes
-- Rol.id (slug, e.g. 'super-administrador') from Rol.nombre (display label).
ALTER TABLE roles_master ADD COLUMN display_name VARCHAR(100);

-- Backs the frontend's MasterUserProfile.must_change_password, set when a SaaS
-- admin user is created with an auto-generated temporary password.
ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- ==============================================================================
-- Permission catalog seed (15 rows)
-- ==============================================================================

INSERT INTO permissions_master (code, module_id, action_id, description) VALUES
    ('gestion-tenants.crear',                       'gestion-tenants',        'crear',                 'Crear tenants'),
    ('gestion-tenants.editar',                       'gestion-tenants',        'editar',                'Editar tenants'),
    ('gestion-tenants.eliminar',                     'gestion-tenants',        'eliminar',               'Eliminar tenants'),
    ('gestion-tenants.activar-suspender',            'gestion-tenants',        'activar-suspender',      'Activar / suspender tenants'),
    ('gestion-tenants.configurar-identidad',         'gestion-tenants',        'configurar-identidad',   'Configurar identidad de tenant'),
    ('gestion-tenants.zonas-horarias',                'gestion-tenants',        'zonas-horarias',         'Configurar zonas horarias'),
    ('gestion-tenants.asignacion-planes',             'gestion-tenants',        'asignacion-planes',      'Asignar planes a tenants'),
    ('gestion-tenants.tipos-marcacion',                'gestion-tenants',        'tipos-marcacion',        'Configurar tipos de marcación'),
    ('billing-suscripciones.gestionar-pagos',          'billing-suscripciones', 'gestionar-pagos',        'Gestionar pagos'),
    ('billing-suscripciones.historial-facturacion',    'billing-suscripciones', 'historial-facturacion',  'Ver historial de facturación'),
    ('billing-suscripciones.bloqueo-impago',           'billing-suscripciones', 'bloqueo-impago',         'Bloquear tenants por impago'),
    ('configuracion-global.modulos-por-plan',          'configuracion-global',  'modulos-por-plan',       'Gestión de módulos por plan'),
    ('monitoreo-sistema.dashboard-global',             'monitoreo-sistema',     'dashboard-global',       'Ver dashboard global'),
    ('monitoreo-sistema.logs-sistema',                 'monitoreo-sistema',     'logs-sistema',           'Ver logs del sistema'),
    ('monitoreo-sistema.auditoria-acciones',           'monitoreo-sistema',     'auditoria-acciones',     'Ver auditoría de acciones')
ON CONFLICT (code) DO NOTHING;

-- ==============================================================================
-- Role seed (6 rows). admin_trazzo is seeded here too (idempotently) rather than
-- assumed to already exist: DataSeeder is @Profile("local") only and never runs
-- outside local dev, so a non-local environment would otherwise have no
-- admin_trazzo row for the grant below to attach to.
-- ==============================================================================

INSERT INTO roles_master (name, display_name, description) VALUES
    ('admin_trazzo',          'Administrador Trazzo',         'Administrator with full access'),
    ('super-administrador',   'Super Administrador',          'Acceso total a la plataforma SASS: gestión completa de tenants, billing, configuración global y monitoreo del sistema.'),
    ('soporte',               'Administrador de Soporte',     'Gestión operativa de tenants y monitoreo del sistema. Sin acceso a billing ni configuración global de planes.'),
    ('operaciones',           'Administrador de Operaciones', 'Administración de tenants, configuración de identidad, zonas horarias y tipos de marcación. Acceso de solo lectura a monitoreo.'),
    ('financiero',            'Administrador Financiero',     'Gestión completa de billing y suscripciones: pagos, facturación y bloqueo por impago. Acceso de solo lectura a tenants.'),
    ('consultor',             'Consultor / Vista',            'Acceso de solo lectura a dashboards, logs y auditoría del sistema. Sin permisos de escritura en ningún módulo.')
ON CONFLICT (name) DO NOTHING;

-- ==============================================================================
-- Role -> permission grants, translated 1:1 from PERMISOS_DEFAULT in
-- gestion-roles.ts (cross-checked line by line against that file).
-- ==============================================================================

-- admin_trazzo and super-administrador: all 15 permissions.
INSERT INTO role_permissions_master (role_id, permission_id)
SELECT r.id, p.id FROM roles_master r CROSS JOIN permissions_master p
WHERE r.name IN ('admin_trazzo', 'super-administrador')
ON CONFLICT DO NOTHING;

-- soporte (10 permissions)
INSERT INTO role_permissions_master (role_id, permission_id)
SELECT r.id, p.id FROM roles_master r CROSS JOIN permissions_master p
WHERE r.name = 'soporte' AND p.code IN (
    'gestion-tenants.crear', 'gestion-tenants.editar', 'gestion-tenants.activar-suspender',
    'gestion-tenants.configurar-identidad', 'gestion-tenants.zonas-horarias', 'gestion-tenants.tipos-marcacion',
    'billing-suscripciones.historial-facturacion',
    'monitoreo-sistema.dashboard-global', 'monitoreo-sistema.logs-sistema', 'monitoreo-sistema.auditoria-acciones'
) ON CONFLICT DO NOTHING;

-- operaciones (7 permissions)
INSERT INTO role_permissions_master (role_id, permission_id)
SELECT r.id, p.id FROM roles_master r CROSS JOIN permissions_master p
WHERE r.name = 'operaciones' AND p.code IN (
    'gestion-tenants.crear', 'gestion-tenants.editar', 'gestion-tenants.activar-suspender',
    'gestion-tenants.configurar-identidad', 'gestion-tenants.zonas-horarias', 'gestion-tenants.tipos-marcacion',
    'monitoreo-sistema.dashboard-global'
) ON CONFLICT DO NOTHING;

-- financiero (6 permissions)
INSERT INTO role_permissions_master (role_id, permission_id)
SELECT r.id, p.id FROM roles_master r CROSS JOIN permissions_master p
WHERE r.name = 'financiero' AND p.code IN (
    'gestion-tenants.asignacion-planes',
    'billing-suscripciones.gestionar-pagos', 'billing-suscripciones.historial-facturacion', 'billing-suscripciones.bloqueo-impago',
    'monitoreo-sistema.dashboard-global', 'monitoreo-sistema.auditoria-acciones'
) ON CONFLICT DO NOTHING;

-- consultor (3 permissions)
INSERT INTO role_permissions_master (role_id, permission_id)
SELECT r.id, p.id FROM roles_master r CROSS JOIN permissions_master p
WHERE r.name = 'consultor' AND p.code IN (
    'monitoreo-sistema.dashboard-global', 'monitoreo-sistema.logs-sistema', 'monitoreo-sistema.auditoria-acciones'
) ON CONFLICT DO NOTHING;
