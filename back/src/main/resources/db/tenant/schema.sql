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
    finger_index INT,
    template_cifrado TEXT NOT NULL,
    llave_cifrado VARCHAR(255),
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
    attendance_date DATE NOT NULL,
    minutes_late INT DEFAULT 0,
    state attendance_status_enum NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 6. GESTIÓN DE INCIDENCIAS Y PERMISOS
-- ==============================================================================

CREATE TABLE incidencia_types (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE incidencias (
    id SERIAL PRIMARY KEY,
    tenant_user_id INT REFERENCES tenant_user(id) ON DELETE CASCADE,
    incidencia_type_id INT REFERENCES incidencia_types(id) ON DELETE CASCADE,
    state state_incidencias_enum DEFAULT 'PENDIENTE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE incidencia_evidencia (
    id SERIAL PRIMARY KEY,
    incidencia_id INT REFERENCES incidencias(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    mime_type VARCHAR(50),
    file_size INT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permisos_incidencia (
    id SERIAL PRIMARY KEY,
    incidencia_id INT REFERENCES incidencias(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    finish_date DATE NOT NULL,
    days_granted INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
    created_by_user_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE monthly_closures_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    monthly_closures_id UUID REFERENCES monthly_closures(id) ON DELETE CASCADE,
    tenant_user_id INT REFERENCES tenant_user(id),
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
