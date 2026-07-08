// ==========================================
// SECCIÓN: PERMISOS Y ROLES
// ==========================================

export interface PermissionProfile {
  id: number;
  name: string;
  descripcion: string;
  master_features_code: string | null;
  context: 'MASTER' | 'TENANT';
}

export interface TenantRoleProfile {
  id: number;
  name: string;
  descripcion: string | null;
  permissions: PermissionProfile[];
}

export interface MasterRoleProfile {
  id: number;
  name: string;
  descripcion: string | null;
}

// ==========================================
// SECCIÓN: PERSONAS Y USUARIOS
// ==========================================

export type DocumentType = 'DNI' | 'CARNET_EXTRANJERIA' | 'PASAPORTE' | 'OTRO';

export interface PersonaBase {
  id: number;
  img_url: string | null;
  document_type: DocumentType;
  document_value: string;
  name: string;
  father_surname: string;
  mother_surname: string;
  birth_date: string | null;
}

export interface MetodoRecuperacionEntry {
  method_type: 'EMAIL' | 'PHONE';
  method_value: string;
}

export interface TenantUserProfile {
  id: number;
  email: string | null;
  phone: string | null;
  estado: 'ACTIVO' | 'LICENCIA' | 'INACTIVO';
  must_change_password: boolean;
  created_at: string;
  updated_at: string;
  persona: PersonaBase;
  MetodoRecuperacion: MetodoRecuperacionEntry[];
  rol: TenantRoleProfile;
  sedes: { id: number; nombre: string }[];
  areas: { id: number; nombre: string }[];
  departamentos: { id: number; nombre: string }[];
}

export interface MasterUserProfile {
  id: number;
  email: string | null;
  phone: string | null;
  tenant_id: string | null;
  must_change_password: boolean;
  created_at: string;
  persona: PersonaBase;
  MetodoRecuperacion: MetodoRecuperacionEntry[];
  roles: MasterRoleProfile[];
  tenant_info: {
    id: string;
    nombre: string;
    slug: string;
  } | null;
}

// ── Listas paginadas ──

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type ScopeAplicado = 'ALL_TENANT' | 'MY_ASSIGNMENTS' | 'SEDE' | 'AREA' | 'DEPARTAMENTO' | 'SELF';

export interface TenantUserListResponse extends PageResponse<TenantUserProfile> {
  scope_aplicado?: ScopeAplicado;
}

export interface MasterUserListResponse extends PageResponse<MasterUserProfile> {}

// ── Request bodies ──

export interface CrearTenantUsuarioRequest {
  document_type: DocumentType;
  document_value: string;
  name: string;
  father_surname: string;
  mother_surname: string;
  birth_date?: string | null;
  img_url?: string | null;
  email: string;
  emails_adicionales?: string[] | null;
  phone?: string | null;
  phones_adicionales?: string[] | null;
  role_id: number;
  sede_ids?: number[] | null;
  area_ids?: number[] | null;
  departamento_ids?: number[] | null;
}

export interface ActualizarTenantUsuarioRequest extends CrearTenantUsuarioRequest {
  cargo?: string | null;
}

export interface PatchTenantUsuarioRequest {
  name?: string;
  father_surname?: string;
  mother_surname?: string;
  birth_date?: string | null;
  img_url?: string | null;
  email?: string;
  phone?: string | null;
  cargo?: string | null;
  estado?: 'ACTIVO' | 'LICENCIA' | 'INACTIVO';
  role_id?: number;
  sede_ids?: number[] | null;
  area_ids?: number[] | null;
  departamento_ids?: number[] | null;
}

export interface CrearMasterUsuarioRequest {
  document_type: DocumentType;
  document_value: string;
  name: string;
  father_surname: string;
  mother_surname: string;
  email: string;
  phone?: string | null;
  role_ids: number[];
}

export interface AsignarRolRequest {
  role_id: number;
}

export interface CambiarPasswordRequest {
  current_password?: string;
  new_password?: string;
}

export interface SoftDeleteResponse {
  id: number;
  status: string;
  deleted_at: string;
  deleted_by: number;
}

// ==========================================
// SECCIÓN: AUTENTICACIÓN
// ==========================================

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UsuarioProfile {
  id: number;
  nombre: string;
  apellido_paterno: string;
  apellido_materno: string;
  email: string;
  status: 'ACTIVO' | 'SUSPENDIDO' | 'INACTIVO';
  ultimo_acceso: string;
  rol: RoleProfile[];
}

export interface RoleProfile {
  id: number;
  name: string;
  permissions: Record<string, unknown>;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  usuario: UsuarioProfile;
}

export interface RegistroUsuarioRequest {
  document_type: DocumentType;
  document_value: string;
  name: string;
  father_surname: string;
  mother_surname: string;
  phone: string;
  img_url?: string | null;
  email: string;
  password: string;
  birth_date?: string | null;
  role_ids: number[];
}

// ==========================================
// SECCIÓN: INCIDENCIAS
// ==========================================

export interface IncidentTypeProfile {
  id: number;
  nombre: string;
  descripcion: string | null;
  activo: boolean;
  created_at: string;
  updated_at: string;
}

export interface IncidentTypeListResponse extends PageResponse<IncidentTypeProfile> {}

export interface CreateIncidentTypeRequest {
  nombre: string;
  descripcion?: string | null;
}

export interface PatchIncidentTypeRequest {
  nombre?: string;
  descripcion?: string | null;
  activo?: boolean;
}

export interface IncidentEvidenceProfile {
  id: number;
  incidencia_id: number;
  file_name: string;
  file_url: string;
  mime_type: string;
  file_size: number;
  created_at: string;
  updated_at: string;
}

export interface IncidentPermissionProfile {
  id: number;
  incidencia_id: number;
  start_date: string;
  end_date: string;
  days_granted: number;
  created_at: string;
  updated_at: string;
}

export interface IncidentProfile {
  id: number;
  tenant_user_id: number;
  incidencia_type_id: number;
  state: 'PENDIENTE' | 'APROBADO' | 'DENEGADO';
  comment: string | null;
  tipo: IncidentTypeProfile;
  permiso: IncidentPermissionProfile | null;
  evidencias: IncidentEvidenceProfile[];
  tenant_user: {
    id: number;
    nombre: string;
    apellido_paterno: string;
    apellido_materno: string;
    email: string;
  };
  created_at: string;
  updated_at: string;
}

export interface IncidentListResponse extends PageResponse<IncidentProfile> {
  scope_aplicado?: ScopeAplicado;
}

export interface CreateIncidentRequest {
  incidencia_type_id: number;
  comment?: string | null;
  tenant_user_id?: number | null;
}

export interface PatchIncidentRequest {
  comment?: string | null;
}

export interface IncidentStateChangeRequest {
  state: 'APROBADO' | 'DENEGADO';
  days_granted?: number | null;
  motivo_rechazo?: string | null;
}

export interface CreateEvidenceRequest {
  file_name: string;
  file_url: string;
  mime_type: string;
  file_size: number;
}

export interface NotifyIncidentRequest {
  tipo: 'EMAIL' | 'SISTEMA' | 'AMBOS';
}

// ==========================================
// SECCIÓN: COREEHR - SHIFTS & SCHEDULES
// ==========================================

export interface ShiftSummary {
  id: number;
  name: string;
}

export interface ToleranciaProfile {
  id: number;
  schedule_id: number;
  name: string | null;
  type: 'ENTRADA' | 'SALIDA' | 'HORAS_EXTRA';
  minutes: number;
  description: string | null;
  activo: boolean;
  created_at: string;
  updated_at: string;
}

export interface ScheduleSummary {
  id: number;
  name: string;
  entry_time: string;
  departure_time: string;
}

export interface ScheduleProfile {
  id: number;
  shift_id: number;
  shift: ShiftSummary;
  name: string;
  description: string | null;
  entry_time: string;
  departure_time: string;
  tolerancias: ToleranciaProfile[];
  created_at: string;
  updated_at: string;
}

export interface ShiftProfile {
  id: number;
  name: string;
  description: string | null;
  schedules: ScheduleSummary[];
  created_at: string;
  updated_at: string;
}

export interface ShiftListResponse extends PageResponse<ShiftProfile> {}

export interface CreateShiftRequest {
  name: string;
  description?: string | null;
}

export interface PatchShiftRequest {
  name?: string;
  description?: string | null;
}

export interface ScheduleListResponse extends PageResponse<ScheduleProfile> {}

export interface CreateScheduleRequest {
  shift_id: number;
  name: string;
  description?: string | null;
  entry_time: string;
  departure_time: string;
}

export interface PatchScheduleRequest {
  name?: string;
  description?: string | null;
  entry_time?: string;
  departure_time?: string;
}

// ── Tolerancia ──

export interface CreateToleranciaRequest {
  name?: string | null;
  type: 'ENTRADA' | 'SALIDA' | 'HORAS_EXTRA';
  minutes: number;
  description?: string | null;
}

export interface PatchToleranciaRequest {
  name?: string | null;
  minutes?: number;
  description?: string | null;
  activo?: boolean;
}

// ── User Schedule ──

export interface UserScheduleProfile {
  id: number;
  tenant_user_id: number;
  schedule_id: number;
  schedule: ScheduleSummary;
  description: string | null;
  entry_time: string;
  departure_time: string;
  created_at: string;
  updated_at: string;
}

export interface UserScheduleListResponse extends PageResponse<UserScheduleProfile> {}

export interface CreateUserScheduleRequest {
  tenant_user_id: number;
  schedule_id: number;
  description?: string | null;
  entry_time: string;
  departure_time: string;
}

// ── Device ──

export interface DeviceProfile {
  id: number;
  code: string;
  name: string | null;
  branch_id: number;
  branch_name: string | null;
  ip: string | null;
  puerto: number | null;
  ubicacion: string | null;
  state: boolean;
  created_at: string;
}

export interface DeviceListResponse extends PageResponse<DeviceProfile> {}

export interface CreateDeviceRequest {
  code: string;
  name?: string | null;
  branch_id: number;
  ip?: string | null;
  puerto?: number | null;
  ubicacion?: string | null;
}

export interface PatchDeviceRequest {
  name?: string | null;
  branch_id?: number;
  ip?: string | null;
  puerto?: number | null;
  ubicacion?: string | null;
  state?: boolean;
}

// ── Biometria ──

export interface UserBiometriaProfile {
  id: number;
  tenant_user_id: number;
  device_id: number | null;
  device_code: string | null;
  finger_index: number;
  activo: boolean;
  capturado_en: string | null;
  created_at: string;
  updated_at: string;
}

export interface UserBiometriaListResponse extends PageResponse<UserBiometriaProfile> {}

export interface InitEnrollRequest {
  tenant_user_id: number;
  device_id: number;
  finger_index: number;
}

export interface EnrollSessionResponse {
  enroll_token: string;
  device_id: number;
  tenant_user_id: number;
  finger_index: number;
  device_code: string;
  expires_at: string;
}

export interface CompleteEnrollHttpRequest {
  enroll_token: string;
  device_code: string;
  finger_index: number;
  encrypted_template_base64: string;
  encrypted_aes_key_base64: string;
  iv_base64?: string | null;
  tag_base64?: string | null;
  captured_at_utc?: string | null;
}

// ── Attendance ──

export interface AttendanceProfile {
  id: string;
  tenant_user_id: number;
  tenant_user: {
    id: number;
    nombre: string;
    apellido_paterno: string;
  };
  schedule_id: number | null;
  schedule: ScheduleSummary | null;
  device_id: number | null;
  device_code: string | null;
  check_in: string | null;
  check_out: string | null;
  attendance_date: string;
  minutes_late: number;
  state: 'PUNTUAL' | 'TARDANZA' | 'FALTA';
  created_at: string;
  updated_at: string;
}

export interface AttendanceListResponse extends PageResponse<AttendanceProfile> {
  scope_aplicado?: ScopeAplicado;
}

export interface PatchAttendanceRequest {
  check_in?: string | null;
  check_out?: string | null;
  state?: 'PUNTUAL' | 'TARDANZA' | 'FALTA';
  minutes_late?: number;
}

// ── Non-Working Days ──

export interface NonWorkingDayProfile {
  id: number;
  date: string;
  description: string | null;
  is_recurring: boolean;
  created_at: string;
}

export interface NonWorkingDayListResponse extends PageResponse<NonWorkingDayProfile> {}

export interface CreateNonWorkingDayRequest {
  date: string;
  description?: string | null;
  is_recurring?: boolean;
}

export interface PatchNonWorkingDayRequest {
  date?: string;
  description?: string | null;
  is_recurring?: boolean;
}

// ── Tenant Contacts ──

export interface TenantContactProfile {
  id: number;
  tenant_user_id: number;
  type: string;
  tenant_user: {
    id: number;
    nombre: string;
    apellido_paterno: string;
    email: string;
    phone: string | null;
  };
  created_at: string;
  updated_at: string;
  deleted_at: string | null;
}

export interface TenantContactListResponse extends PageResponse<TenantContactProfile> {}

export interface CreateTenantContactRequest {
  tenant_user_id: number;
  type: string;
}

export interface PatchTenantContactRequest {
  type: string;
}

// ── Tenant User Department ──

export interface TenantUserDepartmentProfile {
  id: number;
  tenant_user_id: number;
  department_id: number;
  department_name: string | null;
  is_primary: boolean;
  start_date: string;
  end_date: string | null;
  created_at: string;
  updated_at: string;
}

export interface TenantUserDepartmentListResponse extends PageResponse<TenantUserDepartmentProfile> {}

export interface CreateTenantUserDepartmentRequest {
  department_id: number;
  is_primary?: boolean;
  start_date: string;
  end_date?: string | null;
}

export interface PatchTenantUserDepartmentRequest {
  end_date?: string | null;
  is_primary?: boolean;
}

// ── Marcacion / Asistencia Biométrica ──

export interface MarcacionRequest {
  templateCifrado: string;
  llaveAesCifrada: string;
  timestampLocal: string;
  device_code: string;
  tenant_user_id: number;
}

export type MarcacionSyncRequest = MarcacionRequest[];

export interface PublicKeyResponse {
  publicKey: string;
  kid: string;
}

// ==========================================
// SECCIÓN: ERRORES Y RESPUESTAS COMUNES
// ==========================================

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  details: Array<{ field: string; message: string }> | null;
}

export interface MessageResponse {
  message: string;
  status: string;
}

// ==========================================
// SECCIÓN: ENDPOINTS MAP
// ==========================================

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

export interface EndpointMatch {
  method: HttpMethod;
  pattern: RegExp;
  paramNames: string[];
}
