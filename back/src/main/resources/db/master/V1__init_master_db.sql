-- ==============================================================================
-- 0. EXTENSIONES
-- ==============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ==============================================================================
-- 1. TIPOS ENUMERADOS
-- ==============================================================================

CREATE TYPE currency_enum AS ENUM ('SOLES', 'DOLAR', 'EURO');
CREATE TYPE tipo_dato_enum AS ENUM ('INT', 'CHAR', 'STRING', 'DOUBLE', 'FLOAT', 'LONG', 'BOOLEAN');
CREATE TYPE type_enum AS ENUM ('TRIAL', 'INFO');
CREATE TYPE status_enum AS ENUM ('PENDING', 'OBSERVADO', 'APPROVED', 'REJECTED');
CREATE TYPE status_payment_enum AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
CREATE TYPE estado_pago_comprobante_enum AS ENUM ('PENDIENTE', 'COMPLETADO', 'FALLIDO', 'REEMBOLSADO');
CREATE TYPE subscription_status_enum AS ENUM ('ACTIVE', 'SUSPENDED', 'CANCELED', 'TRIAL');
CREATE TYPE document_type_enum AS ENUM ('DNI', 'CARNET_DE_EXTRANJERIA', 'PASAPORTE', 'OTRO');
CREATE TYPE metodo_recuperacion_type_enum AS ENUM ('EMAIL', 'PHONE');
CREATE TYPE status_login_enum AS ENUM ('SUCCESS', 'FAILED_WRONG_PASSWORD', 'FAILED_USER_NOT_FOUND', 'FAILED_INACTIVE_USER', 'LOCKED_OUT', 'LOGOUT_EXPLICIT');
CREATE TYPE tipo_comprobante_enum AS ENUM ('01_FACTURA', '03_BOLETA', '07_NOTA_CREDITO', '08_NOTA_DEBITO');

-- ==============================================================================
-- 2. CATÁLOGOS GLOBALES Y PLANES
-- ==============================================================================

CREATE TABLE plans (
    id             SERIAL PRIMARY KEY,
    name           VARCHAR(100) UNIQUE NOT NULL,
    price          DECIMAL(10,2) NOT NULL,
    price_annual   DECIMAL(10,2),
    currency       currency_enum NOT NULL,
    billing_period VARCHAR(50),
    is_active      BOOLEAN DEFAULT TRUE,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP
);

CREATE TABLE features (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE plan_features (
    id         SERIAL PRIMARY KEY,
    plan_id    INT REFERENCES plans(id) ON DELETE CASCADE,
    feature_id INT REFERENCES features(id) ON DELETE CASCADE,
    tipo_dato  tipo_dato_enum NOT NULL,
    value      JSONB NOT NULL,
    date_start DATE,
    date_end   DATE,
    is_active  BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_plan_features_value CHECK (
        (tipo_dato IN ('INT', 'DOUBLE', 'FLOAT', 'LONG') AND jsonb_typeof(value) = 'number') OR
        (tipo_dato IN ('CHAR', 'STRING') AND jsonb_typeof(value) = 'string') OR
        (tipo_dato = 'BOOLEAN' AND jsonb_typeof(value) = 'boolean')
    ),
    CONSTRAINT chk_plan_features_date CHECK (
        (is_active = FALSE) OR (is_active = TRUE AND date_start IS NOT NULL)
    )
);

-- ==============================================================================
-- 3. NÚCLEO MULTI-TENANT
-- ==============================================================================

CREATE TABLE holding (
    id            SERIAL PRIMARY KEY,
    tax_id        VARCHAR(20) UNIQUE NOT NULL,
    reason_social VARCHAR(255) NOT NULL,
    state         BOOLEAN DEFAULT TRUE,
    type          VARCHAR(50) CHECK (type IN ('PUBLICO', 'PRIVADO')),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP
);

CREATE TABLE tenants (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    holding_id   INT REFERENCES holding(id) ON DELETE SET NULL,
    sub_domain   VARCHAR(100) UNIQUE NOT NULL,
    plan_id      INT REFERENCES plans(id),
    activated_at TIMESTAMP,
    suspended_at TIMESTAMP,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP
);

CREATE TABLE tenant_branding (
    tenant_id       UUID PRIMARY KEY REFERENCES tenants(id) ON DELETE CASCADE,
    logo_url        VARCHAR(255),
    slogan          TEXT,
    primary_color   VARCHAR(20),
    secondary_color VARCHAR(20),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tenant_settings (
    tenant_id    UUID PRIMARY KEY REFERENCES tenants(id) ON DELETE CASCADE,
    schema_name  VARCHAR(63) NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE subscriptions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id           INT REFERENCES plans(id),
    tenant_id         UUID REFERENCES tenants(id) ON DELETE RESTRICT,
    date_start        DATE NOT NULL,
    date_end          DATE,
    status            subscription_status_enum DEFAULT 'TRIAL',
    purchase_price    DECIMAL(10,2) NOT NULL,
    mp_preapproval_id VARCHAR(255),
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subscriptions_mp_preapproval_id ON subscriptions (mp_preapproval_id) WHERE mp_preapproval_id IS NOT NULL;

-- ==============================================================================
-- 4. PAGOS Y FACTURACIÓN
-- ==============================================================================

CREATE TABLE payment_transactions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    subscription_id  UUID NOT NULL REFERENCES subscriptions(id) ON DELETE RESTRICT,
    mp_preference_id VARCHAR(255),
    mp_payment_id    VARCHAR(255),
    amount           DECIMAL(10,2) NOT NULL,
    net_amount       DECIMAL(10,2) NOT NULL,
    status_payment   status_payment_enum DEFAULT 'PENDING',
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_webhooks_log (
    id          VARCHAR(255) PRIMARY KEY,
    mp_event_id VARCHAR(255),
    action      VARCHAR(100),
    raw_payload JSONB,
    processed   BOOLEAN DEFAULT FALSE,
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE invoices (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pdf_url                VARCHAR(255),
    tenant_id              UUID REFERENCES tenants(id),
    payment_transaction_id UUID REFERENCES payment_transactions(id),
    invoice_series         VARCHAR(50) UNIQUE NOT NULL,
    consecutive_number     VARCHAR(50) UNIQUE NOT NULL,
    type                   tipo_comprobante_enum,
    issuer_tax_id          VARCHAR(20) NOT NULL CHECK (issuer_tax_id ~ '^[0-9]{11}$'),
    issuer_name            VARCHAR(255) NOT NULL,
    issuer_tax_address     TEXT NOT NULL,
    client_tax_id          VARCHAR(20) NOT NULL CHECK (client_tax_id ~ '^[0-9]{8,15}$'),
    client_name            VARCHAR(255) NOT NULL,
    client_direccion       TEXT,
    currency_code          VARCHAR(10),
    exchange_rate          DECIMAL(10,4),
    sub_total              DECIMAL(10,2) NOT NULL,
    igv_amount             DECIMAL(10,2) NOT NULL,
    total                  DECIMAL(10,2) NOT NULL,
    descuento_total        DECIMAL(10,2),
    estado_pago            estado_pago_comprobante_enum DEFAULT 'PENDIENTE',
    observaciones          TEXT,
    created_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expiration_date        DATE,
    CONSTRAINT chk_invoice_total CHECK (total = sub_total + igv_amount),
    CONSTRAINT chk_invoice_igv CHECK (igv_amount = ROUND(sub_total * 0.18, 2))
);

CREATE TABLE invoice_details (
    id              SERIAL PRIMARY KEY,
    invoice_id      UUID REFERENCES invoices(id) ON DELETE CASCADE,
    subscription_id UUID REFERENCES subscriptions(id),
    description     TEXT NOT NULL,
    cantidad        INT NOT NULL,
    valor_unitario  DECIMAL(10,2) NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    unit_igv        DECIMAL(10,2) NOT NULL,
    subtotal        DECIMAL(10,2),
    igv             DECIMAL(10,2) NOT NULL,
    total           DECIMAL(10,2) NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 5. IDENTIDAD, USUARIOS Y ROLES
-- ==============================================================================

CREATE TABLE persons (
    id             SERIAL PRIMARY KEY,
    img_url        VARCHAR(255),
    document_type  document_type_enum NOT NULL,
    document_value VARCHAR(50) UNIQUE NOT NULL,
    name           VARCHAR(100) NOT NULL,
    father_surname VARCHAR(100) NOT NULL,
    mother_surname VARCHAR(100) NOT NULL,
    birth_date     DATE,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    person_id             INT REFERENCES persons(id) ON DELETE CASCADE,
    tenant_id             UUID REFERENCES tenants(id),
    email                 VARCHAR(150) UNIQUE NOT NULL,
    phone                 VARCHAR(20),
    password              VARCHAR(255) NOT NULL,
    must_change_password  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at            TIMESTAMP
);

CREATE TABLE roles_master (
    id           SERIAL PRIMARY KEY,
    name         VARCHAR(50) UNIQUE NOT NULL,
    display_name VARCHAR(100),
    description  TEXT
);

CREATE TABLE user_roles_master (
    user_id         UUID REFERENCES users(id) ON DELETE CASCADE,
    roles_master_id INT REFERENCES roles_master(id) ON DELETE CASCADE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, roles_master_id)
);

CREATE TABLE login_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    attempted_email VARCHAR(150),
    status          status_login_enum NOT NULL,
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE metodo_recuperacion (
    id          SERIAL PRIMARY KEY,
    user_id     UUID REFERENCES users(id) ON DELETE CASCADE,
    method_type metodo_recuperacion_type_enum NOT NULL,
    value       VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP
);

-- ==============================================================================
-- 6. SOLICITUDES Y LEADS
-- ==============================================================================

CREATE TABLE requests (
    id         SERIAL PRIMARY KEY,
    type       type_enum NOT NULL,
    title      VARCHAR(255) NOT NULL,
    message    TEXT NOT NULL,
    status     status_enum DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE request_contacts (
    request_id   INT PRIMARY KEY REFERENCES requests(id) ON DELETE CASCADE,
    name         VARCHAR(100) NOT NULL,
    last_name    VARCHAR(100) NOT NULL,
    email        VARCHAR(150) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    tax_id       VARCHAR(20) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE request_comments (
    id                 SERIAL PRIMARY KEY,
    request_id         INT REFERENCES requests(id) ON DELETE CASCADE,
    request_contact_id INT REFERENCES request_contacts(request_id),
    comment            TEXT NOT NULL,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_request_comment (
    id                 SERIAL PRIMARY KEY,
    user_id            UUID REFERENCES users(id) ON DELETE CASCADE,
    request_comment_id INT REFERENCES request_comments(id) ON DELETE CASCADE,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE requests_record (
    id            SERIAL PRIMARY KEY,
    request_id    INT REFERENCES requests(id) ON DELETE CASCADE,
    status        status_enum NOT NULL,
    user_id       UUID REFERENCES users(id) ON DELETE SET NULL,
    change_reason TEXT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 7. AUDITORÍA
-- ==============================================================================

CREATE TABLE tenant_settings_record (
    id                SERIAL PRIMARY KEY,
    tenant_setting_id UUID REFERENCES tenant_settings(tenant_id),
    db_name           VARCHAR(100),
    db_host           VARCHAR(255),
    db_port           VARCHAR(10),
    db_user           VARCHAR(100),
    db_password       VARCHAR(255),
    user_id           UUID REFERENCES users(id),
    change_reason     TEXT,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity     VARCHAR(100) NOT NULL,
    entity_id  VARCHAR(255) NOT NULL,
    action     VARCHAR(20) NOT NULL,
    user_id    UUID REFERENCES users(id) ON DELETE SET NULL,
    endpoint   VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    old_value  JSONB,
    new_value  JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 8. SPRING SESSION (gestionado por Spring Session JDBC)
-- ==============================================================================

CREATE TABLE SPRING_SESSION (
    PRIMARY_ID          VARCHAR(36) NOT NULL,
    SESSION_ID          VARCHAR(36) NOT NULL,
    CREATION_TIME       BIGINT NOT NULL,
    LAST_ACCESS_TIME    BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME         BIGINT NOT NULL,
    PRINCIPAL_NAME      VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID VARCHAR(36) NOT NULL,
    ATTRIBUTE_NAME     VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES    BYTEA NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
        REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE
);

-- ==============================================================================
-- 9. CATÁLOGO DE FEATURES
-- ==============================================================================

INSERT INTO features (name, description) VALUES
    ('max_trabajadores',      'Máximo de trabajadores permitidos'),
    ('max_sedes',              'Máximo de sedes permitidas'),
    ('almacenamiento_gb',      'Almacenamiento en GB'),
    ('reportes',               'Reportes Avanzados'),
    ('api-externa',            'API Externa'),
    ('facturacion-auto',       'Facturación Automática'),
    ('control-huella',         'Control por Huella'),
    ('escaneo-codigo',         'Escaneo de Código'),
    ('soporte-24-7',           'Soporte 24/7'),
    ('multi-sede',             'Multi-sede'),
    ('trial-gratuito',         'Trial Gratuito'),
    ('facturacion-publica',    'Facturación Pública')
ON CONFLICT (name) DO NOTHING;

-- ==============================================================================
-- 10. RBAC SaaS (permisos, roles y permisos maestros)
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

-- Permiso seed (15 permisos)
INSERT INTO permissions_master (code, module_id, action_id, description)
SELECT code, split_part(code, '.', 1), split_part(code, '.', 2), description
FROM (VALUES
    ('gestion-tenants.crear',                       'Crear tenants'),
    ('gestion-tenants.editar',                       'Editar tenants'),
    ('gestion-tenants.eliminar',                     'Eliminar tenants'),
    ('gestion-tenants.activar-suspender',            'Activar / suspender tenants'),
    ('gestion-tenants.configurar-identidad',         'Configurar identidad de tenant'),
    ('gestion-tenants.zonas-horarias',                'Configurar zonas horarias'),
    ('gestion-tenants.asignacion-planes',             'Asignar planes a tenants'),
    ('gestion-tenants.tipos-marcacion',                'Configurar tipos de marcación'),
    ('billing-suscripciones.gestionar-pagos',          'Gestionar pagos'),
    ('billing-suscripciones.historial-facturacion',    'Ver historial de facturación'),
    ('billing-suscripciones.bloqueo-impago',           'Bloquear tenants por impago'),
    ('configuracion-global.modulos-por-plan',          'Gestión de módulos por plan'),
    ('monitoreo-sistema.dashboard-global',             'Ver dashboard global'),
    ('monitoreo-sistema.logs-sistema',                 'Ver logs del sistema'),
    ('monitoreo-sistema.auditoria-acciones',           'Ver auditoría de acciones')
) AS seed(code, description)
ON CONFLICT (code) DO NOTHING;

-- Roles seed (6 roles)
INSERT INTO roles_master (name, display_name, description) VALUES
    ('admin_trazzo',          'Administrador Trazzo',         'Administrator with full access'),
    ('super-administrador',   'Super Administrador',          'Acceso total a la plataforma SASS: gestión completa de tenants, billing, configuración global y monitoreo del sistema.'),
    ('soporte',               'Administrador de Soporte',     'Gestión operativa de tenants y monitoreo del sistema. Sin acceso a billing ni configuración global de planes.'),
    ('operaciones',           'Administrador de Operaciones', 'Administración de tenants, configuración de identidad, zonas horarias y tipos de marcación. Acceso de solo lectura a monitoreo.'),
    ('financiero',            'Administrador Financiero',     'Gestión completa de billing y suscripciones: pagos, facturación y bloqueo por impago. Acceso de solo lectura a tenants.'),
    ('consultor',             'Consultor / Vista',            'Acceso de solo lectura a dashboards, logs y auditoría del sistema. Sin permisos de escritura en ningún módulo.')
ON CONFLICT (name) DO NOTHING;

-- admin_trazzo y super-administrador: los 15 permisos.
INSERT INTO role_permissions_master (role_id, permission_id)
SELECT r.id, p.id FROM roles_master r CROSS JOIN permissions_master p
WHERE r.name IN ('admin_trazzo', 'super-administrador')
ON CONFLICT DO NOTHING;

-- soporte (10 permisos)
INSERT INTO role_permissions_master (role_id, permission_id)
SELECT r.id, p.id FROM roles_master r CROSS JOIN permissions_master p
WHERE r.name = 'soporte' AND p.code IN (
    'gestion-tenants.crear', 'gestion-tenants.editar', 'gestion-tenants.activar-suspender',
    'gestion-tenants.configurar-identidad', 'gestion-tenants.zonas-horarias', 'gestion-tenants.tipos-marcacion',
    'billing-suscripciones.historial-facturacion',
    'monitoreo-sistema.dashboard-global', 'monitoreo-sistema.logs-sistema', 'monitoreo-sistema.auditoria-acciones'
) ON CONFLICT DO NOTHING;

-- operaciones (7 permisos)
INSERT INTO role_permissions_master (role_id, permission_id)
SELECT r.id, p.id FROM roles_master r CROSS JOIN permissions_master p
WHERE r.name = 'operaciones' AND p.code IN (
    'gestion-tenants.crear', 'gestion-tenants.editar', 'gestion-tenants.activar-suspender',
    'gestion-tenants.configurar-identidad', 'gestion-tenants.zonas-horarias', 'gestion-tenants.tipos-marcacion',
    'monitoreo-sistema.dashboard-global'
) ON CONFLICT DO NOTHING;

-- financiero (6 permisos)
INSERT INTO role_permissions_master (role_id, permission_id)
SELECT r.id, p.id FROM roles_master r CROSS JOIN permissions_master p
WHERE r.name = 'financiero' AND p.code IN (
    'gestion-tenants.asignacion-planes',
    'billing-suscripciones.gestionar-pagos', 'billing-suscripciones.historial-facturacion', 'billing-suscripciones.bloqueo-impago',
    'monitoreo-sistema.dashboard-global', 'monitoreo-sistema.auditoria-acciones'
) ON CONFLICT DO NOTHING;

-- consultor (3 permisos)
INSERT INTO role_permissions_master (role_id, permission_id)
SELECT r.id, p.id FROM roles_master r CROSS JOIN permissions_master p
WHERE r.name = 'consultor' AND p.code IN (
    'monitoreo-sistema.dashboard-global', 'monitoreo-sistema.logs-sistema', 'monitoreo-sistema.auditoria-acciones'
) ON CONFLICT DO NOTHING;
