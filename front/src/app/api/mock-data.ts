import type {
  TenantUserProfile, MasterUserProfile, PermissionProfile, TenantRoleProfile,
  UsuarioProfile, AuthResponse, IncidentTypeProfile, IncidentProfile,
  ShiftProfile,
  ScheduleProfile, ScheduleSummary, ToleranciaProfile, DeviceProfile,
  UserBiometriaProfile, AttendanceProfile, NonWorkingDayProfile,
  TenantContactProfile, TenantUserDepartmentProfile, UserScheduleProfile,
  MasterRoleProfile, PageResponse, PersonaBase, PublicKeyResponse,
} from './types';

const now = new Date().toISOString();
const today = new Date().toISOString().slice(0, 10);

function daysAgo(n: number): string {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return d.toISOString();
}

function paginate<T>(items: T[], page: number, size: number): PageResponse<T> {
  const start = page * size;
  const end = start + size;
  const content = items.slice(start, end);
  return {
    content,
    page,
    size,
    totalElements: items.length,
    totalPages: Math.ceil(items.length / size),
  };
}

function getEstado(i: number): 'ACTIVO' | 'LICENCIA' | 'INACTIVO' {
  if (i < 8) return 'ACTIVO';
  if (i === 8) return 'LICENCIA';
  return 'INACTIVO';
}

function getRolIndex(i: number): number {
  if (i < 2) return 0;
  if (i < 4) return 2;
  if (i < 6) return 3;
  return 4;
}

function getAttendanceState(isAbsent: boolean, isLate: boolean): 'PUNTUAL' | 'TARDANZA' | 'FALTA' {
  if (isAbsent) return 'FALTA';
  if (isLate) return 'TARDANZA';
  return 'PUNTUAL';
}

const MOCK_DEVICE_IPS = [
  '192.168.1.105', // NOSONAR
  '192.168.1.106', // NOSONAR
  '192.168.2.105', // NOSONAR
  '192.168.3.105', // NOSONAR
  '192.168.3.106', // NOSONAR
];

// ==========================================
// PERSONAS
// ==========================================

const personas: PersonaBase[] = [
  { id: 1, img_url: null, document_type: 'DNI', document_value: '76543210', name: 'Josselin Anais', father_surname: 'Rojas', mother_surname: 'Luque', birth_date: '1990-01-15' },
  { id: 2, img_url: null, document_type: 'DNI', document_value: '87654321', name: 'Carlos Alberto', father_surname: 'Mendoza', mother_surname: 'González', birth_date: '1985-06-22' },
  { id: 3, img_url: null, document_type: 'DNI', document_value: '98765432', name: 'María Fernanda', father_surname: 'López', mother_surname: 'Torres', birth_date: '1992-11-08' },
  { id: 4, img_url: null, document_type: 'DNI', document_value: '12345678', name: 'Roberto', father_surname: 'Castro', mother_surname: 'Díaz', birth_date: '1988-03-12' },
  { id: 5, img_url: null, document_type: 'DNI', document_value: '23456789', name: 'Ana Lucía', father_surname: 'Quispe', mother_surname: 'Huamán', birth_date: '1995-09-30' },
  { id: 6, img_url: null, document_type: 'CARNET_EXTRANJERIA', document_value: 'CE-001234', name: 'Luis Enrique', father_surname: 'García', mother_surname: 'Pérez', birth_date: '1982-07-14' },
  { id: 7, img_url: null, document_type: 'DNI', document_value: '34567890', name: 'Paola', father_surname: 'Ramírez', mother_surname: 'Vega', birth_date: '1991-04-25' },
  { id: 8, img_url: null, document_type: 'PASAPORTE', document_value: 'PP-987654', name: 'John', father_surname: 'Smith', mother_surname: 'Doe', birth_date: '1993-12-01' },
  { id: 9, img_url: null, document_type: 'DNI', document_value: '45678901', name: 'Diana', father_surname: 'Morales', mother_surname: 'Ríos', birth_date: '1994-08-19' },
  { id: 10, img_url: null, document_type: 'DNI', document_value: '56789012', name: 'Pedro', father_surname: 'Hernández', mother_surname: 'Silva', birth_date: '1987-05-05' },
  { id: 11, img_url: null, document_type: 'DNI', document_value: '67890123', name: 'Miguel Ángel', father_surname: 'Sánchez', mother_surname: 'Cruz', birth_date: '1986-10-10' },
  { id: 12, img_url: null, document_type: 'DNI', document_value: '78901234', name: 'Lucía', father_surname: 'Vargas', mother_surname: 'Prado', birth_date: '1996-02-28' },
];

// ==========================================
// PERMISOS
// ==========================================

const todosPermisos: PermissionProfile[] = [
  { id: 1, name: 'VIEW_USUARIOS', descripcion: 'Visualizar listado de usuarios del tenant', master_features_code: 'FEATURE_USER_MGMT', context: 'TENANT' },
  { id: 2, name: 'VIEW_ALL_TENANT_USERS', descripcion: 'Ver todos los usuarios del tenant', master_features_code: 'FEATURE_USER_MGMT', context: 'TENANT' },
  { id: 3, name: 'CREATE_USER', descripcion: 'Crear nuevos usuarios', master_features_code: 'FEATURE_USER_MGMT', context: 'TENANT' },
  { id: 4, name: 'EDIT_USER', descripcion: 'Editar usuarios existentes', master_features_code: 'FEATURE_USER_MGMT', context: 'TENANT' },
  { id: 5, name: 'DELETE_USER', descripcion: 'Eliminar usuarios (baja lógica)', master_features_code: 'FEATURE_USER_MGMT', context: 'TENANT' },
  { id: 6, name: 'ASSIGN_ROLES', descripcion: 'Asignar roles a usuarios', master_features_code: 'FEATURE_USER_MGMT', context: 'TENANT' },
  { id: 7, name: 'VIEW_OWN_INCIDENTS', descripcion: 'Ver sus propias incidencias', master_features_code: 'FEATURE_INCIDENTS', context: 'TENANT' },
  { id: 8, name: 'VIEW_AREA_INCIDENTS', descripcion: 'Ver incidencias de su área', master_features_code: 'FEATURE_INCIDENTS', context: 'TENANT' },
  { id: 9, name: 'VIEW_ALL_INCIDENTS', descripcion: 'Ver todas las incidencias', master_features_code: 'FEATURE_INCIDENTS', context: 'TENANT' },
  { id: 10, name: 'CREATE_INCIDENT', descripcion: 'Crear incidencias', master_features_code: 'FEATURE_INCIDENTS', context: 'TENANT' },
  { id: 11, name: 'APPROVE_INCIDENT', descripcion: 'Aprobar o denegar incidencias', master_features_code: 'FEATURE_INCIDENTS', context: 'TENANT' },
  { id: 12, name: 'MANAGE_INCIDENT_TYPES', descripcion: 'Gestionar tipos de incidencia', master_features_code: 'FEATURE_INCIDENTS', context: 'TENANT' },
  { id: 13, name: 'MANAGE_SCHEDULES', descripcion: 'Gestionar turnos y horarios', master_features_code: 'FEATURE_SCHEDULES', context: 'TENANT' },
  { id: 14, name: 'MANAGE_USER_SHIFTS', descripcion: 'Asignar horarios a trabajadores', master_features_code: 'FEATURE_SCHEDULES', context: 'TENANT' },
  { id: 15, name: 'MANAGE_DEVICES', descripcion: 'Gestionar dispositivos biométricos', master_features_code: 'FEATURE_BIOMETRICS', context: 'TENANT' },
  { id: 16, name: 'MANAGE_BIOMETRICS', descripcion: 'Gestionar huellas digitales', master_features_code: 'FEATURE_BIOMETRICS', context: 'TENANT' },
  { id: 17, name: 'VIEW_ATTENDANCE', descripcion: 'Ver asistencia de asignaciones', master_features_code: 'FEATURE_ATTENDANCE', context: 'TENANT' },
  { id: 18, name: 'VIEW_ALL_ATTENDANCE', descripcion: 'Ver toda la asistencia', master_features_code: 'FEATURE_ATTENDANCE', context: 'TENANT' },
  { id: 19, name: 'VIEW_OWN_ATTENDANCE', descripcion: 'Ver su propia asistencia', master_features_code: 'FEATURE_ATTENDANCE', context: 'TENANT' },
  { id: 20, name: 'EDIT_ATTENDANCE', descripcion: 'Corregir registros de asistencia', master_features_code: 'FEATURE_ATTENDANCE', context: 'TENANT' },
  { id: 21, name: 'MANAGE_DEPARTMENTS', descripcion: 'Gestionar departamentos', master_features_code: 'FEATURE_ORG', context: 'TENANT' },
  { id: 22, name: 'MANAGE_NON_WORKING_DAYS', descripcion: 'Gestionar días no laborables', master_features_code: 'FEATURE_SCHEDULES', context: 'TENANT' },
  { id: 23, name: 'VIEW_MASTER_USERS', descripcion: 'Ver usuarios del equipo Trazzo', master_features_code: 'FEATURE_SAAS', context: 'MASTER' },
  { id: 24, name: 'CREATE_MASTER_USER', descripcion: 'Crear usuarios Trazzo', master_features_code: 'FEATURE_SAAS', context: 'MASTER' },
  { id: 25, name: 'EDIT_MASTER_USER', descripcion: 'Editar usuarios Trazzo', master_features_code: 'FEATURE_SAAS', context: 'MASTER' },
  { id: 26, name: 'DELETE_MASTER_USER', descripcion: 'Eliminar usuarios Trazzo', master_features_code: 'FEATURE_SAAS', context: 'MASTER' },
  { id: 27, name: 'RESET_PASSWORD', descripcion: 'Resetear contraseñas de terceros', master_features_code: 'FEATURE_USER_MGMT', context: 'TENANT' },
  { id: 28, name: 'REACTIVATE_USER', descripcion: 'Reactivar usuarios dados de baja', master_features_code: 'FEATURE_USER_MGMT', context: 'TENANT' },
  { id: 29, name: 'VIEW_EVIDENCE', descripcion: 'Ver evidencias de incidencias', master_features_code: 'FEATURE_INCIDENTS', context: 'TENANT' },
  { id: 30, name: 'DELETE_EVIDENCE', descripcion: 'Eliminar evidencias', master_features_code: 'FEATURE_INCIDENTS', context: 'TENANT' },
];

// ==========================================
// ROLES
// ==========================================

const tenantRoles: TenantRoleProfile[] = [
  { id: 1, name: 'Super Administrador', descripcion: 'Acceso total al sistema', permissions: todosPermisos.filter(p => p.context === 'TENANT') },
  { id: 2, name: 'Director', descripcion: 'Visibilidad total del tenant', permissions: todosPermisos.filter(p => p.context === 'TENANT' && ![28].includes(p.id)) },
  { id: 3, name: 'Administrador', descripcion: 'Gestión administrativa general', permissions: todosPermisos.filter(p => p.context === 'TENANT' && [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 20, 21, 22, 29].includes(p.id)) },
  { id: 4, name: 'Supervisor', descripcion: 'Supervisión de área', permissions: todosPermisos.filter(p => p.context === 'TENANT' && [1, 7, 8, 10, 17, 19, 29].includes(p.id)) },
  { id: 5, name: 'Trabajador', descripcion: 'Acceso básico del empleado', permissions: todosPermisos.filter(p => p.context === 'TENANT' && [7, 10, 19].includes(p.id)) },
];

const masterRoles: MasterRoleProfile[] = [
  { id: 1, name: 'admin_trazzo', descripcion: 'Administrador global de la plataforma Trazzo' },
  { id: 2, name: 'soporte_trazzo', descripcion: 'Soporte técnico con acceso limitado' },
  { id: 3, name: 'viewer_trazzo', descripcion: 'Solo visualización de datos' },
];

// ==========================================
// SEDES, ÁREAS, DEPARTAMENTOS
// ==========================================

const sedes = [
  { id: 1, nombre: 'Sede San Isidro' },
  { id: 2, nombre: 'Sede Miraflores' },
  { id: 3, nombre: 'Sede Surco' },
];

const areas = [
  { id: 1, nombre: 'Dirección Académica' },
  { id: 2, nombre: 'Administración' },
  { id: 3, nombre: 'Docencia' },
  { id: 4, nombre: 'Mantenimiento' },
  { id: 5, nombre: 'Informática' },
  { id: 6, nombre: 'Psicología' },
  { id: 7, nombre: 'Secretaría Académica' },
];

const departamentosList = [
  { id: 1, nombre: 'Matemáticas' },
  { id: 2, nombre: 'Comunicación' },
  { id: 3, nombre: 'Ciencias' },
  { id: 4, nombre: 'Humanidades' },
  { id: 5, nombre: 'Idiomas' },
  { id: 6, nombre: 'Educación Física' },
  { id: 7, nombre: 'Arte y Cultura' },
];

// ==========================================
// USUARIOS TENANT
// ==========================================

export const mockTenantUsers: TenantUserProfile[] = personas.slice(0, 10).map((p, i) => ({
  id: i + 1,
  email: `${p.name.toLowerCase().replace(/\s+/g, '.')}.${p.father_surname.toLowerCase()}@colegio.edu.pe`,
  phone: `+519${String(87000000 + i * 1000).slice(0, 8)}`,
  estado: getEstado(i),
  must_change_password: i === 0,
  created_at: daysAgo(90 + i * 10),
  updated_at: i < 3 ? daysAgo(2) : daysAgo(15 + i * 3),
  persona: p,
  MetodoRecuperacion: [
    { method_type: 'EMAIL', method_value: `${p.name.toLowerCase()}.${p.father_surname.toLowerCase()}+backup@colegio.edu.pe` },
  ],
  rol: tenantRoles[getRolIndex(i)],
  sedes: [sedes[i % 3]],
  areas: [areas[i % 7]],
  departamentos: [departamentosList[i % 7]],
}));

// ==========================================
// USUARIOS MASTER (SAAS)
// ==========================================

export const mockMasterUsers: MasterUserProfile[] = [
  {
    id: 'a1a1a1a1-0000-0000-0000-000000000001',
    email: 'admin@trazzo.pe',
    phone: '+51987654321',
    tenant_id: null,
    must_change_password: false,
    created_at: daysAgo(365),
    persona: { id: 101, img_url: null, document_type: 'DNI', document_value: '11111111', name: 'Admin', father_surname: 'Trazzo', mother_surname: 'Principal', birth_date: null },
    MetodoRecuperacion: [{ method_type: 'EMAIL', method_value: 'admin+backup@trazzo.pe' }],
    roles: [masterRoles[0]],
    tenant_info: null,
  },
  {
    id: 'a1a1a1a1-0000-0000-0000-000000000002',
    email: 'soporte@trazzo.pe',
    phone: '+51976543210',
    tenant_id: null,
    must_change_password: false,
    created_at: daysAgo(200),
    persona: { id: 102, img_url: null, document_type: 'DNI', document_value: '22222222', name: 'Soporte', father_surname: 'Trazzo', mother_surname: 'Técnico', birth_date: null },
    MetodoRecuperacion: [{ method_type: 'EMAIL', method_value: 'soporte+backup@trazzo.pe' }],
    roles: [masterRoles[1]],
    tenant_info: null,
  },
  {
    id: 'a1a1a1a1-0000-0000-0000-000000000003',
    email: 'visor@trazzo.pe',
    phone: '+51965432109',
    tenant_id: 'a1b2c3d4-0000-0000-0000-000000000001',
    must_change_password: true,
    created_at: daysAgo(30),
    persona: { id: 103, img_url: null, document_type: 'DNI', document_value: '33333333', name: 'Visualizador', father_surname: 'Externo', mother_surname: 'Trazzo', birth_date: null },
    MetodoRecuperacion: [{ method_type: 'EMAIL', method_value: 'visor+backup@trazzo.pe' }],
    roles: [masterRoles[2]],
    tenant_info: { id: 'a1b2c3d4-0000-0000-0000-000000000001', nombre: 'Colegio Santa Rosa', slug: 'colegio-santa-rosa' },
  },
];

// ==========================================
// AUTH
// ==========================================

export const mockUsuarioProfile: UsuarioProfile = {
  id: 1,
  nombre: 'Josselin Anais',
  apellido_paterno: 'Rojas',
  apellido_materno: 'Luque',
  email: 'josselin.rojas@colegio.edu.pe',
  status: 'ACTIVO',
  ultimo_acceso: now,
  rol: [
    {
      id: 1,
      name: 'admin_trazzo',
      permissions: { VIEW_USUARIOS: true, VIEW_ALL_TENANT_USERS: true },
    },
  ],
  tenant_permissions: [],
};

export const mockAuthResponse: AuthResponse = {
  accessToken: 'mock-token-001',
  tokenType: 'Bearer',
  usuario: mockUsuarioProfile,
};

// ==========================================
// TIPOS DE INCIDENCIA
// ==========================================

export const mockIncidentTypes: IncidentTypeProfile[] = [
  { id: 1, nombre: 'Permiso por Salud', descripcion: 'Permiso por consulta médica o enfermedad', activo: true, created_at: daysAgo(200), updated_at: daysAgo(50) },
  { id: 2, nombre: 'Permiso Personal', descripcion: 'Permiso por asuntos personales', activo: true, created_at: daysAgo(200), updated_at: daysAgo(50) },
  { id: 3, nombre: 'Tardanza Justificada', descripcion: 'Tardanza con justificación válida', activo: true, created_at: daysAgo(200), updated_at: daysAgo(50) },
  { id: 4, nombre: 'Licencia por Maternidad', descripcion: 'Licencia oficial por maternidad', activo: true, created_at: daysAgo(200), updated_at: daysAgo(50) },
  { id: 5, nombre: 'Licencia por Paternidad', descripcion: 'Licencia oficial por paternidad', activo: true, created_at: daysAgo(200), updated_at: daysAgo(50) },
  { id: 6, nombre: 'Capacitación', descripcion: 'Asistencia a capacitación externa', activo: true, created_at: daysAgo(200), updated_at: daysAgo(50) },
  { id: 7, nombre: 'Comisión de Servicio', descripcion: 'Comisión fuera del centro laboral', activo: true, created_at: daysAgo(200), updated_at: daysAgo(50) },
  { id: 8, nombre: 'Permiso Sindical', descripcion: 'Ejercicio de actividad sindical', activo: false, created_at: daysAgo(200), updated_at: daysAgo(100) },
];

// ==========================================
// INCIDENCIAS
// ==========================================

export const mockIncidencias: IncidentProfile[] = [
  {
    id: 1,
    tenant_user_id: 3,
    incidencia_type_id: 1,
    state: 'PENDIENTE',
    comment: 'Adjunto certificado médico por inasistencia del 15 al 17 de junio',
    tipo: mockIncidentTypes[0],
    permiso: null,
    evidencias: [
      { id: 1, incidencia_id: 1, file_name: 'certificado_medico.pdf', file_key: 'evidences/42/1/uuid/certificado_medico.pdf', download_url: '/api/v1/incidentes/1/evidencias/1/descarga', mime_type: 'application/pdf', file_size: 1048576, created_at: daysAgo(3), updated_at: daysAgo(3) },
    ],
    tenant_user: { id: 3, nombre: 'María Fernanda', apellido_paterno: 'López', apellido_materno: 'Torres', email: 'maria.lopez@colegio.edu.pe' },
    created_at: daysAgo(3),
    updated_at: daysAgo(3),
  },
  {
    id: 2,
    tenant_user_id: 4,
    incidencia_type_id: 3,
    state: 'APROBADO',
    comment: 'Llegué tarde por problemas de tránsito en la Av. Javier Prado',
    tipo: mockIncidentTypes[2],
    permiso: { id: 1, incidencia_id: 2, start_date: today, end_date: today, days_granted: 0, created_at: daysAgo(1), updated_at: daysAgo(1) },
    evidencias: [],
    tenant_user: { id: 4, nombre: 'Roberto', apellido_paterno: 'Castro', apellido_materno: 'Díaz', email: 'roberto.castro@colegio.edu.pe' },
    created_at: daysAgo(1),
    updated_at: daysAgo(1),
  },
  {
    id: 3,
    tenant_user_id: 5,
    incidencia_type_id: 2,
    state: 'DENEGADO',
    comment: 'Solicito permiso para realizar trámite bancario',
    tipo: mockIncidentTypes[1],
    permiso: null,
    evidencias: [],
    tenant_user: { id: 5, nombre: 'Ana Lucía', apellido_paterno: 'Quispe', apellido_materno: 'Huamán', email: 'ana.quispe@colegio.edu.pe' },
    created_at: daysAgo(5),
    updated_at: daysAgo(4),
  },
  {
    id: 4,
    tenant_user_id: 6,
    incidencia_type_id: 6,
    state: 'PENDIENTE',
    comment: 'Capacitación en nuevas metodologías educativas',
    tipo: mockIncidentTypes[5],
    permiso: null,
    evidencias: [
      { id: 2, incidencia_id: 4, file_name: 'programa_capacitacion.pdf', file_key: 'evidences/42/4/uuid/programa_capacitacion.pdf', download_url: '/api/v1/incidentes/4/evidencias/2/descarga', mime_type: 'application/pdf', file_size: 524288, created_at: daysAgo(2), updated_at: daysAgo(2) },
    ],
    tenant_user: { id: 6, nombre: 'Luis Enrique', apellido_paterno: 'García', apellido_materno: 'Pérez', email: 'luis.garcia@colegio.edu.pe' },
    created_at: daysAgo(2),
    updated_at: daysAgo(2),
  },
  {
    id: 5,
    tenant_user_id: 7,
    incidencia_type_id: 5,
    state: 'APROBADO',
    comment: 'Nacimiento de mi primer hijo',
    tipo: mockIncidentTypes[4],
    permiso: { id: 2, incidencia_id: 5, start_date: daysAgo(10).slice(0, 10), end_date: daysAgo(3).slice(0, 10), days_granted: 7, created_at: daysAgo(10), updated_at: daysAgo(10) },
    evidencias: [],
    tenant_user: { id: 7, nombre: 'Paola', apellido_paterno: 'Ramírez', apellido_materno: 'Vega', email: 'paola.ramirez@colegio.edu.pe' },
    created_at: daysAgo(10),
    updated_at: daysAgo(10),
  },
  {
    id: 6,
    tenant_user_id: 8,
    incidencia_type_id: 7,
    state: 'PENDIENTE',
    comment: 'Comisión para supervisar evento deportivo intercolegial',
    tipo: mockIncidentTypes[6],
    permiso: null,
    evidencias: [],
    tenant_user: { id: 8, nombre: 'John', apellido_paterno: 'Smith', apellido_materno: 'Doe', email: 'john.smith@colegio.edu.pe' },
    created_at: daysAgo(1),
    updated_at: daysAgo(1),
  },
  {
    id: 7,
    tenant_user_id: 2,
    incidencia_type_id: 1,
    state: 'APROBADO',
    comment: 'Control médico anual',
    tipo: mockIncidentTypes[0],
    permiso: { id: 3, incidencia_id: 7, start_date: daysAgo(15).slice(0, 10), end_date: daysAgo(15).slice(0, 10), days_granted: 1, created_at: daysAgo(15), updated_at: daysAgo(15) },
    evidencias: [],
    tenant_user: { id: 2, nombre: 'Carlos Alberto', apellido_paterno: 'Mendoza', apellido_materno: 'González', email: 'carlos.mendoza@colegio.edu.pe' },
    created_at: daysAgo(15),
    updated_at: daysAgo(15),
  },
  {
    id: 8,
    tenant_user_id: 9,
    incidencia_type_id: 3,
    state: 'PENDIENTE',
    comment: 'Tardanza por accidente de tránsito',
    tipo: mockIncidentTypes[2],
    permiso: null,
    evidencias: [
      { id: 3, incidencia_id: 8, file_name: 'reporte_policial.jpg', file_key: 'evidences/42/8/uuid/reporte_policial.jpg', download_url: '/api/v1/incidentes/8/evidencias/3/descarga', mime_type: 'image/jpeg', file_size: 2097152, created_at: today + 'T08:00:00Z', updated_at: today + 'T08:00:00Z' },
    ],
    tenant_user: { id: 9, nombre: 'Diana', apellido_paterno: 'Morales', apellido_materno: 'Ríos', email: 'diana.morales@colegio.edu.pe' },
    created_at: today + 'T07:30:00Z',
    updated_at: today + 'T07:30:00Z',
  },
];

// ==========================================
// TURNOS (SHIFTS) Y HORARIOS (SCHEDULES)
// ==========================================

const mockScheduleSummaries: ScheduleSummary[] = [
  { id: 1, name: 'Horario 7:00 - 13:00', entry_time: '07:00:00', departure_time: '13:00:00' },
  { id: 2, name: 'Horario 8:00 - 14:00', entry_time: '08:00:00', departure_time: '14:00:00' },
  { id: 3, name: 'Horario 9:00 - 15:00', entry_time: '09:00:00', departure_time: '15:00:00' },
  { id: 4, name: 'Horario 13:00 - 19:00', entry_time: '13:00:00', departure_time: '19:00:00' },
  { id: 5, name: 'Horario 7:00 - 14:30', entry_time: '07:00:00', departure_time: '14:30:00' },
];

const mockTolerancias: ToleranciaProfile[] = [
  { id: 1, schedule_id: 1, name: 'Tol. entrada docentes', type: 'ENTRADA', minutes: 10, description: null, activo: true, created_at: daysAgo(100), updated_at: daysAgo(50) },
  { id: 2, schedule_id: 1, name: 'Tol. salida docentes', type: 'SALIDA', minutes: 5, description: null, activo: true, created_at: daysAgo(100), updated_at: daysAgo(50) },
  { id: 3, schedule_id: 2, name: 'Tol. entrada admin', type: 'ENTRADA', minutes: 15, description: null, activo: true, created_at: daysAgo(100), updated_at: daysAgo(50) },
  { id: 4, schedule_id: 3, name: 'Tol. entrada tarde', type: 'ENTRADA', minutes: 10, description: null, activo: true, created_at: daysAgo(100), updated_at: daysAgo(50) },
];

const mockSchedulesFull: ScheduleProfile[] = mockScheduleSummaries.map((s, i) => ({
  id: s.id,
  shift_id: i < 3 ? 1 : 2,
  shift: { id: i < 3 ? 1 : 2, name: i < 3 ? 'Turno Mañana' : 'Turno Tarde' },
  name: s.name,
  description: null,
  entry_time: s.entry_time,
  departure_time: s.departure_time,
  tolerancias: mockTolerancias.filter(t => t.schedule_id === s.id),
  created_at: daysAgo(100),
  updated_at: daysAgo(50),
}));

export const mockShifts: ShiftProfile[] = [
  {
    id: 1,
    name: 'Turno Mañana',
    description: 'Turno matutino para personal docente',
    schedules: mockScheduleSummaries.slice(0, 3),
    created_at: daysAgo(100),
    updated_at: daysAgo(50),
  },
  {
    id: 2,
    name: 'Turno Tarde',
    description: 'Turno vespertino para personal administrativo',
    schedules: mockScheduleSummaries.slice(3, 5),
    created_at: daysAgo(90),
    updated_at: daysAgo(40),
  },
  {
    id: 3,
    name: 'Turno Noche',
    description: 'Turno nocturno para personal de vigilancia',
    schedules: [],
    created_at: daysAgo(60),
    updated_at: daysAgo(20),
  },
];

export const mockSchedules: ScheduleProfile[] = mockSchedulesFull;

// ==========================================
// USER SCHEDULES
// ==========================================

export const mockUserSchedules: UserScheduleProfile[] = [
  { id: 1, tenant_user_id: 1, schedule_id: 1, schedule: mockScheduleSummaries[0], description: 'Horario regular año 2026', entry_time: '07:00:00', departure_time: '13:00:00', created_at: daysAgo(60), updated_at: daysAgo(30) },
  { id: 2, tenant_user_id: 2, schedule_id: 1, schedule: mockScheduleSummaries[0], description: 'Horario regular año 2026', entry_time: '07:00:00', departure_time: '13:00:00', created_at: daysAgo(60), updated_at: daysAgo(30) },
  { id: 3, tenant_user_id: 3, schedule_id: 2, schedule: mockScheduleSummaries[1], description: 'Horario administrativo', entry_time: '08:00:00', departure_time: '14:00:00', created_at: daysAgo(45), updated_at: daysAgo(15) },
  { id: 4, tenant_user_id: 4, schedule_id: 3, schedule: mockScheduleSummaries[2], description: null, entry_time: '09:00:00', departure_time: '15:00:00', created_at: daysAgo(30), updated_at: daysAgo(5) },
  { id: 5, tenant_user_id: 5, schedule_id: 1, schedule: mockScheduleSummaries[0], description: null, entry_time: '07:00:00', departure_time: '13:00:00', created_at: daysAgo(20), updated_at: daysAgo(20) },
];

// ==========================================
// DISPOSITIVOS
// ==========================================

export const mockDevices: DeviceProfile[] = [
  { id: 1, code: 'ZK-C2PRO-00123', name: 'Lector Principal - Entrada Sur', branch_id: 1, branch_name: 'Sede San Isidro', ip: MOCK_DEVICE_IPS[0], puerto: 4370, ubicacion: 'Puerta principal, primer piso', state: true, created_at: daysAgo(365) },
  { id: 2, code: 'ZK-C2PRO-00124', name: 'Lector Entrada Docentes', branch_id: 1, branch_name: 'Sede San Isidro', ip: MOCK_DEVICE_IPS[1], puerto: 4370, ubicacion: 'Entrada bloque B', state: true, created_at: daysAgo(300) },
  { id: 3, code: 'ZK-C2PRO-00125', name: 'Lector Principal - Miraflores', branch_id: 2, branch_name: 'Sede Miraflores', ip: MOCK_DEVICE_IPS[2], puerto: 4370, ubicacion: 'Hall principal', state: true, created_at: daysAgo(250) },
  { id: 4, code: 'ZK-C2PRO-00126', name: 'Lector Surco - Administrativos', branch_id: 3, branch_name: 'Sede Surco', ip: MOCK_DEVICE_IPS[3], puerto: 4370, ubicacion: 'Oficina de administración', state: false, created_at: daysAgo(180) },
  { id: 5, code: 'ZK-C2PRO-00127', name: 'Lector Surco - Docentes', branch_id: 3, branch_name: 'Sede Surco', ip: MOCK_DEVICE_IPS[4], puerto: 4370, ubicacion: 'Sala de profesores', state: true, created_at: daysAgo(150) },
];

// ==========================================
// BIOMETRÍA
// ==========================================

export const mockBiometria: UserBiometriaProfile[] = [
  { id: 1, tenant_user_id: 1, device_id: 1, device_code: 'ZK-C2PRO-00123', finger_index: 1, activo: true, capturado_en: daysAgo(200), created_at: daysAgo(200), updated_at: daysAgo(200) },
  { id: 2, tenant_user_id: 1, device_id: 1, device_code: 'ZK-C2PRO-00123', finger_index: 2, activo: true, capturado_en: daysAgo(200), created_at: daysAgo(200), updated_at: daysAgo(200) },
  { id: 3, tenant_user_id: 2, device_id: 1, device_code: 'ZK-C2PRO-00123', finger_index: 1, activo: true, capturado_en: daysAgo(180), created_at: daysAgo(180), updated_at: daysAgo(180) },
  { id: 4, tenant_user_id: 3, device_id: 3, device_code: 'ZK-C2PRO-00125', finger_index: 6, activo: true, capturado_en: daysAgo(150), created_at: daysAgo(150), updated_at: daysAgo(150) },
  { id: 5, tenant_user_id: 4, device_id: 2, device_code: 'ZK-C2PRO-00124', finger_index: 1, activo: false, capturado_en: daysAgo(300), created_at: daysAgo(300), updated_at: daysAgo(100) },
];

// ==========================================
// ASISTENCIA
// ==========================================

function generateAttendanceRecord(uid: number, dayOffset: number): AttendanceProfile | null {
  const date = new Date();
  date.setDate(date.getDate() - dayOffset);
  if (date.getDay() === 0 || date.getDay() === 6) return null;
  const dateStr = date.toISOString().slice(0, 10);
  const isLate = dayOffset === 3 || dayOffset === 7;
  const isAbsent = dayOffset === 5 || dayOffset === 12;
  const checkInTime = isLate ? '07:25:00' : '06:55:00';
  const checkOutTime = isLate ? '13:05:00' : '13:00:00';
  return {
    id: `att-${uid}-${dateStr}`,
    tenant_user_id: uid,
    tenant_user: {
      id: uid,
      nombre: mockTenantUsers.find(u => u.id === uid)?.persona.name ?? '',
      apellido_paterno: mockTenantUsers.find(u => u.id === uid)?.persona.father_surname ?? '',
    },
    schedule_id: 1,
    schedule: mockScheduleSummaries[0],
    device_id: 1,
    device_code: 'ZK-C2PRO-00123',
    check_in: isAbsent ? null : `${dateStr}T${checkInTime}Z`,
    check_out: isAbsent ? null : `${dateStr}T${checkOutTime}Z`,
    attendance_date: dateStr,
    minutes_late: isLate ? 15 : 0,
    state: getAttendanceState(isAbsent, isLate),
    created_at: `${dateStr}T06:30:00Z`,
    updated_at: `${dateStr}T13:10:00Z`,
  };
}

function generateAttendance(): AttendanceProfile[] {
  const result: AttendanceProfile[] = [];
  const users = [1, 2, 3, 4, 5, 6, 7];
  for (const uid of users) {
    for (let d = 0; d < 15; d++) {
      const record = generateAttendanceRecord(uid, d);
      if (record) result.push(record);
    }
  }
  return result;
}

export const mockAttendance: AttendanceProfile[] = generateAttendance();

// ==========================================
// DÍAS NO LABORABLES
// ==========================================

export const mockNonWorkingDays: NonWorkingDayProfile[] = [
  { id: 1, date: `${today.slice(0, 4)}-01-01`, description: 'Año Nuevo', is_recurring: true, created_at: daysAgo(365) },
  { id: 2, date: `${today.slice(0, 4)}-05-01`, description: 'Día del Trabajo', is_recurring: true, created_at: daysAgo(365) },
  { id: 3, date: `${today.slice(0, 4)}-07-28`, description: 'Fiestas Patrias', is_recurring: true, created_at: daysAgo(365) },
  { id: 4, date: `${today.slice(0, 4)}-07-29`, description: 'Fiestas Patrias', is_recurring: true, created_at: daysAgo(365) },
  { id: 5, date: `${today.slice(0, 4)}-08-30`, description: 'Santa Rosa de Lima', is_recurring: true, created_at: daysAgo(365) },
  { id: 6, date: `${today.slice(0, 4)}-10-08`, description: 'Combate de Angamos', is_recurring: true, created_at: daysAgo(365) },
  { id: 7, date: `${today.slice(0, 4)}-11-01`, description: 'Día de Todos los Santos', is_recurring: true, created_at: daysAgo(365) },
  { id: 8, date: `${today.slice(0, 4)}-12-08`, description: 'Inmaculada Concepción', is_recurring: true, created_at: daysAgo(365) },
  { id: 9, date: `${today.slice(0, 4)}-12-25`, description: 'Navidad', is_recurring: true, created_at: daysAgo(365) },
  { id: 10, date: '2026-07-06', description: 'Día del Maestro (feriado institucional)', is_recurring: false, created_at: daysAgo(30) },
];

// ==========================================
// CONTACTOS DEL TENANT
// ==========================================

export const mockTenantContacts: TenantContactProfile[] = [
  {
    id: 1,
    tenant_user_id: 2,
    type: 'RRHH',
    tenant_user: {
      id: 2,
      nombre: 'Carlos Alberto',
      apellido_paterno: 'Mendoza',
      email: 'carlos.mendoza@colegio.edu.pe',
      phone: '+51987654321',
    },
    created_at: daysAgo(200),
    updated_at: daysAgo(50),
    deleted_at: null,
  },
  {
    id: 2,
    tenant_user_id: 6,
    type: 'Soporte Técnico',
    tenant_user: {
      id: 6,
      nombre: 'Luis Enrique',
      apellido_paterno: 'García',
      email: 'luis.garcia@colegio.edu.pe',
      phone: '+51965432109',
    },
    created_at: daysAgo(150),
    updated_at: daysAgo(30),
    deleted_at: null,
  },
  {
    id: 3,
    tenant_user_id: 1,
    type: 'Director',
    tenant_user: {
      id: 1,
      nombre: 'Josselin Anais',
      apellido_paterno: 'Rojas',
      email: 'josselin.rojas@colegio.edu.pe',
      phone: '+51954321098',
    },
    created_at: daysAgo(100),
    updated_at: daysAgo(10),
    deleted_at: null,
  },
];

// ==========================================
// DEPARTAMENTOS DE USUARIO
// ==========================================

export const mockUserDepartments: TenantUserDepartmentProfile[] = [
  { id: 1, tenant_user_id: 1, department_id: 1, department_name: 'Matemáticas', is_primary: true, start_date: '2026-01-01', end_date: null, created_at: daysAgo(150), updated_at: daysAgo(50) },
  { id: 2, tenant_user_id: 2, department_id: 2, department_name: 'Comunicación', is_primary: true, start_date: '2026-01-01', end_date: null, created_at: daysAgo(150), updated_at: daysAgo(50) },
  { id: 3, tenant_user_id: 3, department_id: 7, department_name: 'Arte y Cultura', is_primary: true, start_date: '2026-03-01', end_date: null, created_at: daysAgo(100), updated_at: daysAgo(50) },
  { id: 4, tenant_user_id: 4, department_id: 3, department_name: 'Ciencias', is_primary: true, start_date: '2026-01-01', end_date: null, created_at: daysAgo(150), updated_at: daysAgo(50) },
  { id: 5, tenant_user_id: 5, department_id: 2, department_name: 'Comunicación', is_primary: true, start_date: '2026-02-01', end_date: '2026-06-30', created_at: daysAgo(120), updated_at: daysAgo(30) },
  { id: 6, tenant_user_id: 5, department_id: 5, department_name: 'Idiomas', is_primary: true, start_date: '2026-07-01', end_date: null, created_at: daysAgo(30), updated_at: daysAgo(30) },
];

// ==========================================
// LLAVE PÚBLICA
// ==========================================

export const mockPublicKey: PublicKeyResponse = {
  publicKey: `-----BEGIN RSA PUBLIC KEY-----
MIIBCgKCAQEA0sYgGqR3PToDzMqCgk1IX3A0V+X0JhTBHRtMHLxXH8oFqQkG
mock-mock-mock-mock-mock-mock-mock-mock-mock-mock-mock-mock-mock
mock-mock-mock-mock-mock-mock-mock-mock-mock-mock-mock-mock-mock
-----END RSA PUBLIC KEY-----`,
  kid: 'pubkey-abc123',
};

// ==========================================
// HELPER: PAGINAR RESPUESTAS
// ==========================================

export { paginate };
