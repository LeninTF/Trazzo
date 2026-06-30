# Mock Data — `mock-data.ts`

Datos simulados para el interceptor HTTP de desarrollo (`mock.interceptor.ts`) y las pruebas unitarias.

## Mocks exportados

| Export | Tipo | Cant. | Descripción |
|---|---|---|---|
| `mockTenantUsers` | `TenantUserProfile[]` | 10 | Usuarios del tenant (colegio). Estados: ACTIVO, LICENCIA, INACTIVO. Roles: Super Admin, Director, Admin, Supervisor, Trabajador. |
| `mockMasterUsers` | `MasterUserProfile[]` | 3 | Usuarios del equipo Trazzo (admin, soporte, viewer). |
| `mockUsuarioProfile` | `UsuarioProfile` | 1 | Perfil de la sesión activa (Josselin Rojas, admin_trazzo). |
| `mockAuthResponse` | `AuthResponse` | 1 | Respuesta de login: `accessToken` + `usuario`. |
| `mockIncidentTypes` | `IncidentTypeProfile[]` | 8 | Tipos de incidencia (Permiso Salud, Personal, Tardanza, Licencias, Capacitación, etc.). |
| `mockIncidencias` | `IncidentProfile[]` | 8 | Incidencias en distintos estados (PENDIENTE, APROBADO, DENEGADO). Incluyen evidencias y permisos anidados. |
| `mockShifts` | `ShiftProfile[]` | 3 | Turnos: Mañana, Tarde, Noche. Cada uno con sus horarios anidados. |
| `mockSchedules` | `ScheduleProfile[]` | 5 | Horarios detallados (entry/departure time + tolerancias). |
| `mockUserSchedules` | `UserScheduleProfile[]` | 5 | Asignaciones de horario a usuarios del tenant. |
| `mockDevices` | `DeviceProfile[]` | 5 | Dispositivos biométricos en 3 sedes (San Isidro, Miraflores, Surco). |
| `mockBiometria` | `UserBiometriaProfile[]` | 5 | Registros de huellas digitales por usuario/dispositivo. |
| `mockAttendance` | `AttendanceProfile[]` | ~95 | Registros de asistencia (7 usuarios × 15 días, sin fines de semana). Estados: PUNTUAL, TARDANZA, FALTA. |
| `mockNonWorkingDays` | `NonWorkingDayProfile[]` | 10 | Días no laborables (9 feriados recurrentes + 1 institucional). |
| `mockTenantContacts` | `TenantContactProfile[]` | 3 | Contactos del tenant (RRHH, Soporte Técnico, Director). |
| `mockUserDepartments` | `TenantUserDepartmentProfile[]` | 6 | Asignaciones departamento-usuario. |
| `mockPublicKey` | `PublicKeyResponse` | 1 | Llave RSA pública mock para `/security/public-key`. |
| `paginate` | `fn` | — | Helper: `paginate(items, page, size)` → `PageResponse<T>`. |

## Datos internos (no exportados)

| Variable | Tipo | Cant. | Uso |
|---|---|---|---|
| `personas` | `PersonaBase[]` | 12 | Datos personales base para construir `mockTenantUsers`. |
| `todosPermisos` | `PermissionProfile[]` | 30 | Catálogo completo de permisos (VIEW_USUARIOS, CREATE_USER, APPROVE_INCIDENT, etc.). |
| `tenantRoles` | `TenantRoleProfile[]` | 5 | Roles del tenant con sus permisos asignados. |
| `masterRoles` | `MasterRoleProfile[]` | 3 | Roles del equipo Trazzo (admin, soporte, viewer). |
| `sedes` | — | 3 | Sedes del colegio (San Isidro, Miraflores, Surco). |
| `areas` | — | 7 | Áreas funcionales (Dirección Académica, Administración, Docencia, etc.). |
| `departamentosList` | — | 7 | Departamentos académicos (Matemáticas, Comunicación, Ciencias, etc.). |

## Convenciones

- Fechas generadas con `daysAgo(n)` para datos relativos al día actual.
- `today` se usa para referencias al día actual.
- Los usuarios 1–7 son ACTIVOS, 8 es LICENCIA, 9–10 son INACTIVOS.
- Las incidencias 1, 4, 6, 8 están PENDIENTES; 2, 5, 7 están APROBADAS; 3 está DENEGADA.
- `getEstado()` y `getRolIndex()` asignan estado/rol según índice numérico.
