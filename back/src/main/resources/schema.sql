CREATE TABLE IF NOT EXISTS monthly_closures (
    id UUID PRIMARY KEY,
    month INTEGER NOT NULL,
    year INTEGER NOT NULL,
    total_employees INTEGER NOT NULL,
    excel_report_url VARCHAR(500),
    pdf_report_url VARCHAR(500),
    created_by_user_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS monthly_closure_details (
    id UUID PRIMARY KEY,
    month_closure_id UUID NOT NULL REFERENCES monthly_closures(id),
    tenant_user_id VARCHAR(100) NOT NULL,
    tenant_user_full_name VARCHAR(200) NOT NULL,
    tenant_user_document VARCHAR(50) NOT NULL,
    department_name VARCHAR(200),
    role_name VARCHAR(200),
    total_worked_hours DOUBLE PRECISION NOT NULL,
    total_tardiness_minutes DOUBLE PRECISION NOT NULL,
    total_absences INTEGER NOT NULL,
    total_overtime_hours DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL
);
