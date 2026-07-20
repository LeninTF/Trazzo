-- ==============================================================================
-- 1. TIPOS ENUMERADOS
-- ==============================================================================

CREATE TYPE tenant_user_state_enum AS ENUM ('ACTIVO', 'LICENCIA', 'INACTIVO');
CREATE TYPE tolerancia_type_enum AS ENUM ('ENTRADA', 'SALIDA', 'HORAS EXTRA');
CREATE TYPE attendance_status_enum AS ENUM ('PUNTUAL', 'TARDANZA', 'FALTA');
CREATE TYPE state_incidencias_enum AS ENUM ('DENEGADO', 'APROBADO', 'PENDIENTE');
CREATE TYPE accion_sistema_enum AS ENUM ('GET', 'PUT', 'POST', 'DELETE', 'READ');

-- ==============================================================================
-- 2. ORGANIZACIÓN Y JERARQUÍA
-- ==============================================================================

CREATE TABLE branch (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    state BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE area (
    id SERIAL PRIMARY KEY,
    branch_id INT REFERENCES branch(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    state BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT uq_area_branch UNIQUE (branch_id, name)
);

CREATE TABLE department (
    id SERIAL PRIMARY KEY,
    area_id INT REFERENCES area(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    state BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT uq_department_area UNIQUE (area_id, name)
);

-- ==============================================================================
-- 3. USUARIOS DEL TENANT Y CONTROL DE ACCESOS (RBAC)
-- ==============================================================================

CREATE TABLE tenant_user (
    id SERIAL PRIMARY KEY,
    master_user_id UUID NOT NULL,
    state tenant_user_state_enum DEFAULT 'ACTIVO',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE tenant_contact (
    id SERIAL PRIMARY KEY,
    tenant_user_id INT REFERENCES tenant_user(id) ON DELETE CASCADE,
    type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE tenant_user_department (
    id SERIAL PRIMARY KEY,
    tenant_user_id INT REFERENCES tenant_user(id) ON DELETE CASCADE,
    department_id INT REFERENCES department(id) ON DELETE CASCADE,
    is_primary BOOLEAN DEFAULT FALSE,
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    master_features_code VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role_permissions (
    role_id UUID REFERENCES role(id) ON DELETE CASCADE,
    permission_id UUID REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE tenant_user_role (
    id SERIAL PRIMARY KEY,
    tenant_user_id INT REFERENCES tenant_user(id) ON DELETE CASCADE,
    role_id UUID REFERENCES role(id) ON DELETE CASCADE,
    department_id INT REFERENCES department(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 4. HORARIOS Y JORNADAS LABORALES
-- ==============================================================================

CREATE TABLE non_working_days (
    id SERIAL PRIMARY KEY,
    date DATE NOT NULL,
    description VARCHAR(255),
    is_recurring BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE shift (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE schedule (
    id SERIAL PRIMARY KEY,
    shift_id INT REFERENCES shift(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    entry_time TIME NOT NULL,
    departure_time TIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_schedule (
    id SERIAL PRIMARY KEY,
    tenant_user_id INT REFERENCES tenant_user(id) ON DELETE CASCADE,
    schedule_id INT REFERENCES schedule(id) ON DELETE CASCADE,
    description TEXT,
    entry_time TIME NOT NULL,
    departure_time TIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tolerancia (
    id SERIAL PRIMARY KEY,
    schedule_id INT REFERENCES schedule(id) ON DELETE CASCADE,
    name VARCHAR(100),
    type tolerancia_type_enum NOT NULL,
    minutes INTEGER NOT NULL,
    description TEXT,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 5. BIOMETRÍA Y MARCACIONES
-- ==============================================================================

CREATE TABLE device (
    id SERIAL PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(150),
    ip VARCHAR(45),
    puerto INT,
    ubicacion VARCHAR(255),
    branch_id INT REFERENCES branch(id) ON DELETE SET NULL,
    state BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuario_biometria (
    id SERIAL PRIMARY KEY,
    tenant_user_id INT REFERENCES tenant_user(id) ON DELETE CASCADE,
    device_id INT REFERENCES device(id) ON DELETE SET NULL,
    device_code VARCHAR(100),
    finger_index INT,
    encrypted_template_base64 TEXT NOT NULL,
    encrypted_aes_key_base64 TEXT NOT NULL,
    iv_base64 TEXT,
    tag_base64 TEXT,
    capturado_en TIMESTAMP,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE attendances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_user_id INT REFERENCES tenant_user(id) ON DELETE CASCADE,
    schedule_id INT REFERENCES schedule(id) ON DELETE SET NULL,
    device_id INT REFERENCES device(id) ON DELETE SET NULL,
    check_in TIMESTAMP,
    check_out TIMESTAMP,
    offline_event_id INT,
    device_code VARCHAR(100),
    attendance_date DATE NOT NULL,
    minutes_late INT DEFAULT 0,
    state attendance_status_enum NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_attendances_offline_event_device
    ON attendances (offline_event_id, device_code)
    WHERE offline_event_id IS NOT NULL AND device_code IS NOT NULL;

-- ==============================================================================
-- 6. GESTIÓN DE INCIDENCIAS Y PERMISOS
-- ==============================================================================

-- Nombres de tabla en inglés para coincidir con las entidades JPA del módulo
-- incidents (IncidentEntity, IncidentTypeEntity, IncidentEvidenceEntity,
-- IncidentPermissionEntity), que son la fuente de verdad hoy. Los IDs son
-- VARCHAR(36) (UUID generado en Java), no SERIAL. "state" es VARCHAR porque
-- Hibernate mapea @Enumerated(STRING) a texto plano, no a un enum nativo de
-- Postgres. tenant_user_id no lleva FK porque las entidades tampoco declaran
-- esa relación (es un @Column simple, no @ManyToOne).
CREATE TABLE incident_types (
    id VARCHAR(36) PRIMARY KEY,
    nombre VARCHAR(100) UNIQUE NOT NULL,
    descripcion TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE incidents (
    id VARCHAR(36) PRIMARY KEY,
    tenant_user_id VARCHAR(36) NOT NULL,
    incident_type_id VARCHAR(36) NOT NULL REFERENCES incident_types(id) ON DELETE CASCADE,
    state VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    comment TEXT,
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE incident_evidences (
    id VARCHAR(36) PRIMARY KEY,
    incident_id VARCHAR(36) NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size INT NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    uploaded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE incident_permissions (
    id VARCHAR(36) PRIMARY KEY,
    incident_id VARCHAR(36) NOT NULL UNIQUE REFERENCES incidents(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    days_granted INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 7. CIERRES MENSUALES Y REPORTES
-- ==============================================================================

CREATE TABLE monthly_closures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    month INT NOT NULL,
    year INT NOT NULL,
    total_employees INT NOT NULL,
    excel_report_url VARCHAR(255),
    pdf_report_url VARCHAR(255),
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_monthly_closures_period UNIQUE (year, month)
);

CREATE TABLE monthly_closures_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    monthly_closures_id UUID REFERENCES monthly_closures(id) ON DELETE CASCADE,
    tenant_user_id INT NOT NULL REFERENCES tenant_user(id),
    tenant_user_full_name VARCHAR(255),
    tenant_user_document VARCHAR(50),
    department_name VARCHAR(255),
    role_name VARCHAR(100),
    total_worked_hours DECIMAL(8,2) DEFAULT 0,
    total_tardiness_minutes INT DEFAULT 0,
    total_absences INT DEFAULT 0,
    total_overtime_hours DECIMAL(8,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 8. SEGURIDAD Y AUDITORÍA
-- ==============================================================================

CREATE TABLE sesion (
    id SERIAL PRIMARY KEY,
    tenant_user_id INT REFERENCES tenant_user(id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    device_fingerprint VARCHAR(255),
    login_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP,
    expires_at TIMESTAMP,
    logout_at TIMESTAMP,
    state BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE auditoria_sistema (
    id SERIAL PRIMARY KEY,
    tenant_user_id INT REFERENCES tenant_user(id) ON DELETE SET NULL,
    sesion_id INT REFERENCES sesion(id) ON DELETE SET NULL,
    accion_sistema accion_sistema_enum NOT NULL,
    modulo VARCHAR(100) NOT NULL,
    entidad_id VARCHAR(100),
    valores_prev JSONB,
    endpoint VARCHAR(255),
    descripcion TEXT,
    valor_anterior JSONB,
    valor_nuevo JSONB,
    ip_address VARCHAR(45),
    resultado VARCHAR(100),
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 9. PERMISOS Y ROLES DEL TENANT
-- ==============================================================================

INSERT INTO permissions (id, name, description)
SELECT gen_random_uuid(), seed.name, seed.description
FROM (VALUES
    ('gestion-trabajadores.crear', 'Crear trabajadores'),
    ('gestion-trabajadores.editar', 'Editar trabajadores'),
    ('gestion-trabajadores.eliminar', 'Eliminar trabajadores'),
    ('gestion-trabajadores.asignar-roles', 'Asignar roles a trabajadores'),
    ('estructura-organizacional.crear-editar-areas', 'Crear/editar áreas'),
    ('estructura-organizacional.crear-editar-sedes', 'Crear/editar sedes'),
    ('estructura-organizacional.asignar-personal', 'Asignar personal'),
    ('gestion-horarios.configurar-turnos', 'Configurar turnos'),
    ('gestion-horarios.configurar-tolerancias', 'Configurar tolerancias'),
    ('gestion-horarios.asignacion-masiva', 'Asignación masiva de horarios'),
    ('control-asistencia.corregir-marcaciones', 'Corregir marcaciones'),
    ('control-asistencia.justificar-marcaciones', 'Justificar marcaciones'),
    ('control-asistencia.operador-asistencia', 'Operador de asistencia'),
    ('reportes.exportar-excel', 'Exportar reportes a Excel'),
    ('reportes.exportar-pdf', 'Exportar reportes a PDF'),
    ('permisos-licencias.aprobar-incidencias', 'Aprobar incidencias'),
    ('permisos-licencias.rechazar-incidencias', 'Rechazar incidencias'),
    ('notificaciones.enviar-avisos', 'Enviar avisos'),
    ('notificaciones.configurar-reportes', 'Configurar reportes automáticos'),
    ('configuracion-tenant.gestionar-metodos', 'Gestionar métodos de marcación'),
    ('auditoria-seguridad.acceso-logs', 'Acceso a logs'),
    ('auditoria-seguridad.historial-cambios', 'Historial de cambios'),
    ('asistencia-docente.registrar-entrada-salida', 'Registrar entrada/salida con huella'),
    ('asistencia-docente.ver-historial', 'Ver historial de asistencia'),
    ('asistencia-docente.ver-tardanzas-faltas', 'Ver tardanzas/faltas'),
    ('horarios-docente.ver-turnos', 'Ver turnos asignados'),
    ('horarios-docente.ver-calendario', 'Ver calendario laboral'),
    ('solicitudes.solicitar-correccion', 'Solicitar corrección de marcación'),
    ('solicitudes.ver-estado', 'Ver estado de solicitudes'),
    ('notificaciones-docente.recibir-alertas', 'Recibir alertas'),
    ('notificaciones-docente.ver-cambios-horario', 'Ver cambios de horario'),
    ('perfil.ver-actualizar-datos', 'Ver/actualizar datos personales'),
    ('perfil.cambiar-contrasena', 'Cambiar contraseña')
) AS seed(name, description)
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.name = seed.name);

INSERT INTO role (id, name, description)
SELECT gen_random_uuid(), seed.name, seed.description
FROM (VALUES
    ('administrador', 'El Administrador tiene acceso total a la configuración y seguridad del sistema, incluyendo gestión de trabajadores, horarios, reportes y auditoría.'),
    ('director', 'El Director puede supervisar la estructura organizacional, revisar reportes y aprobar incidencias, con acceso a la mayoría de módulos operativos.'),
    ('coordinador', 'El Coordinador gestiona horarios, control de asistencia y trabajadores a su cargo, con permisos limitados a módulos operativos.'),
    ('recursos-humanos', 'RRHH administra trabajadores, corrige marcaciones, gestiona permisos y licencias, y puede exportar reportes.'),
    ('docente', 'El Docente puede registrar su asistencia, consultar horarios, realizar solicitudes de corrección y gestionar su perfil personal.')
) AS seed(name, description)
WHERE NOT EXISTS (SELECT 1 FROM role r WHERE r.name = seed.name);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM (VALUES
    ('administrador', 'gestion-trabajadores.crear'),
    ('administrador', 'gestion-trabajadores.editar'),
    ('administrador', 'gestion-trabajadores.eliminar'),
    ('administrador', 'gestion-trabajadores.asignar-roles'),
    ('administrador', 'estructura-organizacional.crear-editar-areas'),
    ('administrador', 'estructura-organizacional.crear-editar-sedes'),
    ('administrador', 'estructura-organizacional.asignar-personal'),
    ('administrador', 'gestion-horarios.configurar-turnos'),
    ('administrador', 'gestion-horarios.configurar-tolerancias'),
    ('administrador', 'gestion-horarios.asignacion-masiva'),
    ('administrador', 'control-asistencia.corregir-marcaciones'),
    ('administrador', 'control-asistencia.justificar-marcaciones'),
    ('administrador', 'control-asistencia.operador-asistencia'),
    ('administrador', 'reportes.exportar-excel'),
    ('administrador', 'reportes.exportar-pdf'),
    ('administrador', 'permisos-licencias.aprobar-incidencias'),
    ('administrador', 'permisos-licencias.rechazar-incidencias'),
    ('administrador', 'notificaciones.enviar-avisos'),
    ('administrador', 'notificaciones.configurar-reportes'),
    ('administrador', 'configuracion-tenant.gestionar-metodos'),
    ('administrador', 'auditoria-seguridad.acceso-logs'),
    ('administrador', 'auditoria-seguridad.historial-cambios'),

    ('director', 'gestion-trabajadores.crear'),
    ('director', 'gestion-trabajadores.editar'),
    ('director', 'estructura-organizacional.crear-editar-areas'),
    ('director', 'estructura-organizacional.crear-editar-sedes'),
    ('director', 'estructura-organizacional.asignar-personal'),
    ('director', 'gestion-horarios.configurar-turnos'),
    ('director', 'gestion-horarios.configurar-tolerancias'),
    ('director', 'control-asistencia.corregir-marcaciones'),
    ('director', 'control-asistencia.justificar-marcaciones'),
    ('director', 'reportes.exportar-excel'),
    ('director', 'reportes.exportar-pdf'),
    ('director', 'permisos-licencias.aprobar-incidencias'),
    ('director', 'permisos-licencias.rechazar-incidencias'),
    ('director', 'notificaciones.enviar-avisos'),
    ('director', 'auditoria-seguridad.acceso-logs'),

    ('coordinador', 'gestion-trabajadores.crear'),
    ('coordinador', 'gestion-trabajadores.editar'),
    ('coordinador', 'gestion-horarios.configurar-turnos'),
    ('coordinador', 'gestion-horarios.asignacion-masiva'),
    ('coordinador', 'control-asistencia.corregir-marcaciones'),
    ('coordinador', 'reportes.exportar-excel'),
    ('coordinador', 'notificaciones.enviar-avisos'),

    ('recursos-humanos', 'gestion-trabajadores.crear'),
    ('recursos-humanos', 'gestion-trabajadores.editar'),
    ('recursos-humanos', 'gestion-trabajadores.eliminar'),
    ('recursos-humanos', 'estructura-organizacional.asignar-personal'),
    ('recursos-humanos', 'control-asistencia.corregir-marcaciones'),
    ('recursos-humanos', 'control-asistencia.justificar-marcaciones'),
    ('recursos-humanos', 'reportes.exportar-excel'),
    ('recursos-humanos', 'reportes.exportar-pdf'),
    ('recursos-humanos', 'permisos-licencias.aprobar-incidencias'),
    ('recursos-humanos', 'permisos-licencias.rechazar-incidencias'),
    ('recursos-humanos', 'notificaciones.enviar-avisos'),

    ('docente', 'asistencia-docente.registrar-entrada-salida'),
    ('docente', 'asistencia-docente.ver-historial'),
    ('docente', 'asistencia-docente.ver-tardanzas-faltas'),
    ('docente', 'horarios-docente.ver-turnos'),
    ('docente', 'horarios-docente.ver-calendario'),
    ('docente', 'solicitudes.solicitar-correccion'),
    ('docente', 'solicitudes.ver-estado'),
    ('docente', 'notificaciones-docente.recibir-alertas'),
    ('docente', 'notificaciones-docente.ver-cambios-horario'),
    ('docente', 'perfil.ver-actualizar-datos'),
    ('docente', 'perfil.cambiar-contrasena')
) AS grants(role_name, permission_name)
JOIN role r ON r.name = grants.role_name
JOIN permissions p ON p.name = grants.permission_name
ON CONFLICT DO NOTHING;
