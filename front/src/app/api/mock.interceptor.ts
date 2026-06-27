import type { HttpEvent, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable, of, delay, throwError } from 'rxjs';
import {
  mockTenantUsers, mockMasterUsers, mockUsuarioProfile, mockAuthResponse,
  mockIncidentTypes, mockIncidencias, mockShifts, mockSchedules,
  mockUserSchedules, mockDevices, mockBiometria, mockAttendance,
  mockNonWorkingDays, mockTenantContacts, mockUserDepartments,
  mockPublicKey, paginate,
} from './mock-data';
import type { AuthResponse, MessageResponse, SoftDeleteResponse } from './types';

const BASE = 'https://api.trazzo.pe/api/v1';
const MOCK_DELAY = 200;

function idFromUrl(url: string, pattern: RegExp): number | null {
  const m = url.match(pattern);
  return m ? parseInt(m[1], 10) : null;
}

function queryParams(req: HttpRequest<unknown>): Record<string, string> {
  const params: Record<string, string> = {};
  req.urlWithParams.replace(/[?&]+([^=&]+)=([^&]*)/gi, (_, k, v) => {
    params[k] = v;
    return '';
  });
  return params;
}

function ok<T>(body: T): Observable<HttpResponse<T>> {
  return of(new HttpResponse({ status: 200, body })).pipe(delay(MOCK_DELAY));
}

function created<T>(body: T): Observable<HttpResponse<T>> {
  return of(new HttpResponse({ status: 201, body })).pipe(delay(MOCK_DELAY));
}

function accepted<T>(body: T): Observable<HttpResponse<T>> {
  return of(new HttpResponse({ status: 202, body })).pipe(delay(MOCK_DELAY));
}

function noContent(): Observable<HttpResponse<object>> {
  return of(new HttpResponse<object>({ status: 204 })).pipe(delay(MOCK_DELAY));
}

function _error(status: number, message: string, details: unknown = null): Observable<never> {
  return throwError(() => new HttpErrorResponse({
    status,
    error: {
      timestamp: new Date().toISOString(),
      status,
      error: status === 403 ? 'Forbidden' : status === 404 ? 'Not Found' : 'Bad Request',
      message,
      details,
    },
  })).pipe(delay(MOCK_DELAY));
}

export function mockInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> {
  const { method } = req;
  let url = req.url;

  // Strip base URL if present
  if (url.startsWith(BASE)) {
    url = url.slice(BASE.length);
  }

  // Only handle /api/v1 paths
  if (!url.startsWith('/api/v1/') && !url.startsWith('/') || url.startsWith('/api/v1/')) {
    // Normalize
    if (url.startsWith('/api/v1')) {
      url = url.replace('/api/v1', '');
    }
  }

  // Skip non-API requests
  if (!url.startsWith('/auth/') && !url.startsWith('/usuarios') && !url.startsWith('/saas/')
    && !url.startsWith('/incidentes') && !url.startsWith('/corehr/')
    && !url.startsWith('/asistencia/') && !url.startsWith('/security/')
    && !url.startsWith('/ws/')) {
    return next(req);
  }

  const qp = queryParams(req);
  const page = parseInt(qp['page'] ?? '0', 10);
  const size = parseInt(qp['size'] ?? '20', 10);

  try {
    return handleRoute(method, url, req, page, size, qp) ?? next(req);
  } catch {
    return next(req);
  }
}

function handleRoute(
  method: string,
  url: string,
  req: HttpRequest<unknown>,
  page: number,
  size: number,
  qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  const u = url.split('?')[0];

  // ==========================================
  // AUTH
  // ==========================================
  if (u === '/auth/login' && method === 'POST') {
    return ok<AuthResponse>({
      ...mockAuthResponse,
      accessToken: `eyJhbGciOiJSUzI1NiIsImtpZCI6InB1YmtleS1hYmMxMjMifQ.${btoa(JSON.stringify({
        sub: '1',
        tenant_id: 'a1b2c3d4-0000-0000-0000-000000000001',
        context: 'TENANT',
        must_change_password: false,
        iat: Math.floor(Date.now() / 1000),
        exp: Math.floor(Date.now() / 1000) + 86400,
      }))}.mock-signature`,
      usuario: { ...mockUsuarioProfile, ultimo_acceso: new Date().toISOString() },
    });
  }

  // ==========================================
  // USUARIOS (TENANT)
  // ==========================================
  if (u === '/usuarios' && method === 'GET') {
    let filtered = [...mockTenantUsers];
    if (qp['status']) filtered = filtered.filter(u => u.estado === qp['status']);
    if (qp['search']) {
      const s = qp['search'].toLowerCase();
      filtered = filtered.filter(u =>
        u.persona.name.toLowerCase().includes(s) ||
        u.persona.father_surname.toLowerCase().includes(s) ||
        (u.email ?? '').toLowerCase().includes(s)
      );
    }
    if (qp['role_id']) filtered = filtered.filter(u => u.rol.id === parseInt(qp['role_id'], 10));
    // scope
    return ok(paginate(filtered, page, size));
  }

  if (u === '/usuarios' && method === 'POST') {
    const body = req.body as Record<string, unknown>;
    const newUser = {
      ...mockTenantUsers[0],
      id: mockTenantUsers.length + 1,
      persona: {
        ...mockTenantUsers[0].persona,
        name: body['name'] as string ?? '',
        father_surname: body['father_surname'] as string ?? '',
        mother_surname: body['mother_surname'] as string ?? '',
        document_type: body['document_type'] as 'DNI' ?? 'DNI',
        document_value: body['document_value'] as string ?? '',
        email: body['email'] as string ?? '',
      },
      email: body['email'] as string ?? '',
      must_change_password: true,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    };
    return created(newUser);
  }

  // /usuarios/me
  if (u === '/usuarios/me') {
    if (method === 'GET') return ok(mockTenantUsers[0]);
    if (method === 'PATCH') {
      return ok({ ...mockTenantUsers[0], ...(req.body as object) });
    }
  }

  // /usuarios/{id}
  const uidMatch = u.match(/^\/usuarios\/(\d+)$/);
  if (uidMatch) {
    const id = parseInt(uidMatch[1], 10);
    const user = mockTenantUsers.find(u => u.id === id);
    if (!user) return _error(404, 'Usuario no encontrado');

    if (method === 'GET') return ok(user);
    if (method === 'PUT') return ok({ ...user, ...(req.body as object), id });
    if (method === 'PATCH') {
      const body = req.body as Record<string, unknown>;
      if (body['estado'] && body['estado'] === 'INACTIVO') {
        return ok<SoftDeleteResponse>({
          id: user.id,
          status: 'INACTIVO',
          deleted_at: new Date().toISOString(),
          deleted_by: 1,
        });
      }
      return ok({ ...user, ...body, id });
    }
    if (method === 'DELETE') {
      return ok<SoftDeleteResponse>({
        id: user.id,
        status: 'INACTIVO',
        deleted_at: new Date().toISOString(),
        deleted_by: 1,
      });
    }
  }

  // /usuarios/{id}/rol
  const rolMatch = u.match(/^\/usuarios\/(\d+)\/rol$/);
  if (rolMatch && method === 'PUT') {
    const id = parseInt(rolMatch[1], 10);
    const user = mockTenantUsers.find(u => u.id === id);
    if (!user) return _error(404, 'Usuario no encontrado');
    const body = req.body as { role_id?: number };
    const rolesDisponibles = mockTenantUsers.map(u => u.rol);
    const newRole = rolesDisponibles.find(r => r.id === body.role_id);
    return ok({ ...user, rol: newRole ?? user.rol });
  }

  // /usuarios/{id}/password
  const passMatch = u.match(/^\/usuarios\/(\d+)\/password$/);
  if (passMatch && method === 'PATCH') {
    return noContent();
  }

  // ==========================================
  // SAAS / MASTER USERS
  // ==========================================
  if (u === '/saas/users' && method === 'GET') {
    let filtered = [...mockMasterUsers];
    if (qp['search']) {
      const s = qp['search'].toLowerCase();
      filtered = filtered.filter(u => (u.email ?? '').toLowerCase().includes(s));
    }
    if (qp['tenant_id']) filtered = filtered.filter(u => u.tenant_id === qp['tenant_id']);
    return ok(paginate(filtered, page, size));
  }

  if (u === '/saas/users' && method === 'POST') {
    const body = req.body as Record<string, unknown>;
    return created({
      ...mockMasterUsers[0],
      id: mockMasterUsers.length + 1,
      email: body['email'] as string ?? '',
      persona: {
        ...mockMasterUsers[0].persona,
        name: body['name'] as string ?? '',
        father_surname: body['father_surname'] as string ?? '',
        mother_surname: body['mother_surname'] as string ?? '',
      },
    });
  }

  // /saas/users/me
  if (u === '/saas/users/me' && method === 'GET') return ok(mockMasterUsers[0]);

  // /saas/users/{id}
  const mUidMatch = u.match(/^\/saas\/users\/(\d+)$/);
  if (mUidMatch) {
    const id = parseInt(mUidMatch[1], 10);
    const user = mockMasterUsers.find(u => u.id === id);
    if (!user) return _error(404, 'Usuario no encontrado');
    if (method === 'GET') return ok(user);
    if (method === 'PATCH') return ok({ ...user, ...(req.body as object), id });
    if (method === 'DELETE') {
      return ok<SoftDeleteResponse>({
        id: user.id,
        status: 'INACTIVO',
        deleted_at: new Date().toISOString(),
        deleted_by: 1,
      });
    }
  }

  // ==========================================
  // SECURITY
  // ==========================================
  if (u === '/security/public-key' && method === 'GET') {
    return ok(mockPublicKey);
  }

  // ==========================================
  // ASISTENCIA (biometric middleware)
  // ==========================================
  if (u === '/asistencia/marcar' && method === 'POST') {
    return ok(mockAttendance[0]);
  }
  if (u === '/asistencia/sync' && method === 'POST') {
    return accepted<MessageResponse>({ message: 'Lote aceptado para procesamiento en segundo plano.', status: 'queued' });
  }

  // ==========================================
  // INCIDENTES - TIPOS
  // ==========================================
  if (u === '/incidentes/tipos' && method === 'GET') {
    let filtered = [...mockIncidentTypes];
    if (qp['activo'] === 'true') filtered = filtered.filter(t => t.activo);
    if (qp['activo'] === 'false') filtered = filtered.filter(t => !t.activo);
    return ok(paginate(filtered, page, size));
  }
  if (u === '/incidentes/tipos' && method === 'POST') {
    const body = req.body as { nombre: string; descripcion?: string };
    const newType = {
      id: mockIncidentTypes.length + 1,
      nombre: body.nombre,
      descripcion: body.descripcion ?? null,
      activo: true,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    };
    return created(newType);
  }

  // /incidentes/tipos/{id}
  const itMatch = u.match(/^\/incidentes\/tipos\/(\d+)$/);
  if (itMatch) {
    const id = parseInt(itMatch[1], 10);
    const tipo = mockIncidentTypes.find(t => t.id === id);
    if (!tipo) return _error(404, 'Tipo de incidencia no encontrado');
    if (method === 'GET') return ok(tipo);
    if (method === 'PATCH') return ok({ ...tipo, ...(req.body as object), id });
  }

  // ==========================================
  // INCIDENTES
  // ==========================================
  if (u === '/incidentes' && method === 'GET') {
    let filtered = [...mockIncidencias];
    if (qp['state']) filtered = filtered.filter(i => i.state === qp['state']);
    if (qp['tipo_id']) filtered = filtered.filter(i => i.incidencia_type_id === parseInt(qp['tipo_id'], 10));
    if (qp['desde']) filtered = filtered.filter(i => i.created_at >= qp['desde']);
    if (qp['hasta']) filtered = filtered.filter(i => i.created_at <= qp['hasta'] + 'T23:59:59Z');
    if (qp['search']) {
      const s = qp['search'].toLowerCase();
      filtered = filtered.filter(i =>
        (i.comment ?? '').toLowerCase().includes(s) ||
        i.tenant_user.nombre.toLowerCase().includes(s)
      );
    }
    return ok(paginate(filtered, page, size));
  }

  if (u === '/incidentes' && method === 'POST') {
    const body = req.body as { incidencia_type_id: number; comment?: string; tenant_user_id?: number };
    const tipo = mockIncidentTypes.find(t => t.id === body.incidencia_type_id);
    const newIncident = {
      id: mockIncidencias.length + 1,
      tenant_user_id: body.tenant_user_id ?? 1,
      incidencia_type_id: body.incidencia_type_id,
      state: 'PENDIENTE' as const,
      comment: body.comment ?? null,
      tipo: tipo ?? mockIncidentTypes[0],
      permiso: null,
      evidencias: [],
      tenant_user: {
        id: 1,
        nombre: 'Josselin Anais',
        apellido_paterno: 'Rojas',
        apellido_materno: 'Luque',
        email: 'josselin.rojas@colegio.edu.pe',
      },
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    };
    return created(newIncident);
  }

  // /incidentes/{id}
  const incMatch = u.match(/^\/incidentes\/(\d+)$/);
  if (incMatch) {
    const id = parseInt(incMatch[1], 10);
    const incident = mockIncidencias.find(i => i.id === id);
    if (!incident) return _error(404, 'Incidencia no encontrada');

    if (method === 'GET') return ok(incident);
    if (method === 'PATCH') {
      return ok({ ...incident, ...(req.body as object), id });
    }
  }

  // /incidentes/{id}/estado
  const incStateMatch = u.match(/^\/incidentes\/(\d+)\/estado$/);
  if (incStateMatch && method === 'PATCH') {
    const id = parseInt(incStateMatch[1], 10);
    const incident = mockIncidencias.find(i => i.id === id);
    if (!incident) return _error(404, 'Incidencia no encontrada');
    const body = req.body as { state: 'APROBADO' | 'DENEGADO'; days_granted?: number; motivo_rechazo?: string };
    return ok({
      ...incident,
      state: body.state,
      permiso: body.state === 'APROBADO' && body.days_granted
        ? {
            id: (incident.permiso?.id ?? 0) + 1,
            incidencia_id: incident.id,
            start_date: new Date().toISOString().slice(0, 10),
            end_date: new Date(Date.now() + (body.days_granted * 86400000)).toISOString().slice(0, 10),
            days_granted: body.days_granted,
            created_at: new Date().toISOString(),
            updated_at: new Date().toISOString(),
          }
        : null,
    } as typeof incident);
  }

  // /incidentes/{id}/evidencias
  const evMatch = u.match(/^\/incidentes\/(\d+)\/evidencias$/);
  if (evMatch) {
    const id = parseInt(evMatch[1], 10);
    const incident = mockIncidencias.find(i => i.id === id);
    if (!incident) return _error(404, 'Incidencia no encontrada');

    if (method === 'GET') return ok(incident.evidencias);
    if (method === 'POST') {
      return created({
        id: Math.max(0, ...mockIncidencias.flatMap(i => i.evidencias.map(e => e.id))) + 1,
        incidencia_id: id,
        file_name: (req.body as { file_name: string }).file_name,
        file_url: (req.body as { file_url: string }).file_url,
        mime_type: (req.body as { mime_type: string }).mime_type,
        file_size: (req.body as { file_size: number }).file_size,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      });
    }
  }

  // /incidentes/{id}/evidencias/{evidenceId}
  const evDelMatch = u.match(/^\/incidentes\/(\d+)\/evidencias\/(\d+)$/);
  if (evDelMatch && method === 'DELETE') return noContent();

  // /incidentes/{id}/notificar
  if (u.match(/^\/incidentes\/(\d+)\/notificar$/) && method === 'POST') {
    return accepted<MessageResponse>({ message: 'Notificación encolada para envío.', status: 'queued' });
  }

  // /incidentes/{id}/justificar
  if (u.match(/^\/incidentes\/(\d+)\/justificar$/) && method === 'POST') {
    return accepted<MessageResponse>({ message: 'Proceso de justificación encolado.', status: 'queued' });
  }

  // ==========================================
  // COREHR - SHIFTS
  // ==========================================
  if (u === '/corehr/shifts' && method === 'GET') {
    let filtered = [...mockShifts];
    if (qp['search']) {
      const s = qp['search'].toLowerCase();
      filtered = filtered.filter(sh => sh.name.toLowerCase().includes(s));
    }
    return ok(paginate(filtered, page, size));
  }
  if (u === '/corehr/shifts' && method === 'POST') {
    const body = req.body as { name: string; description?: string };
    const newShift = {
      id: mockShifts.length + 1,
      name: body.name,
      description: body.description ?? null,
      schedules: [],
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    };
    return created(newShift);
  }

  // /corehr/shifts/{id}
  const shiftMatch = u.match(/^\/corehr\/shifts\/(\d+)$/);
  if (shiftMatch) {
    const id = parseInt(shiftMatch[1], 10);
    const shift = mockShifts.find(s => s.id === id);
    if (!shift) return _error(404, 'Turno no encontrado');
    if (method === 'GET') return ok(shift);
    if (method === 'PATCH') return ok({ ...shift, ...(req.body as object), id });
    if (method === 'DELETE') return noContent();
  }

  // ==========================================
  // COREHR - SCHEDULES
  // ==========================================
  if (u === '/corehr/schedules' && method === 'GET') {
    let filtered = [...mockSchedules];
    if (qp['shift_id']) filtered = filtered.filter(s => s.shift_id === parseInt(qp['shift_id'], 10));
    return ok(paginate(filtered, page, size));
  }
  if (u === '/corehr/schedules' && method === 'POST') {
    const body = req.body as { shift_id: number; name: string; entry_time: string; departure_time: string; description?: string };
    const shift = mockShifts.find(s => s.id === body.shift_id);
    const newSchedule = {
      id: mockSchedules.length + 1,
      shift_id: body.shift_id,
      shift: { id: body.shift_id, name: shift?.name ?? '' },
      name: body.name,
      description: body.description ?? null,
      entry_time: body.entry_time,
      departure_time: body.departure_time,
      tolerancias: [],
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    };
    return created(newSchedule);
  }

  // /corehr/schedules/{id}
  const schedMatch = u.match(/^\/corehr\/schedules\/(\d+)$/);
  if (schedMatch) {
    const id = parseInt(schedMatch[1], 10);
    const schedule = mockSchedules.find(s => s.id === id);
    if (!schedule) return _error(404, 'Schedule no encontrado');
    if (method === 'GET') return ok(schedule);
    if (method === 'PATCH') return ok({ ...schedule, ...(req.body as object), id });
    if (method === 'DELETE') return noContent();
  }

  // ==========================================
  // COREHR - TOLERANCIAS
  // ==========================================
  const tolMatch = u.match(/^\/corehr\/schedules\/(\d+)\/tolerancias$/);
  if (tolMatch) {
    const schedId = parseInt(tolMatch[1], 10);
    const tolerancias = mockSchedules.find(s => s.id === schedId)?.tolerancias ?? [];
    if (method === 'GET') return ok(paginate(tolerancias, page, size));
    if (method === 'POST') {
      const body = req.body as { type: string; minutes: number; name?: string; description?: string };
      const newTol = {
        id: Math.max(0, ...tolerancias.map(t => t.id), ...mockSchedules.flatMap(s => s.tolerancias.map(t => t.id))) + 1,
        schedule_id: schedId,
        name: body.name ?? null,
        type: body.type as 'ENTRADA' | 'SALIDA' | 'HORAS_EXTRA',
        minutes: body.minutes,
        description: body.description ?? null,
        activo: true,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      };
      return created(newTol);
    }
  }

  const tolItemMatch = u.match(/^\/corehr\/schedules\/(\d+)\/tolerancias\/(\d+)$/);
  if (tolItemMatch) {
    if (method === 'PATCH') {
      return ok({
        id: parseInt(tolItemMatch[2], 10),
        schedule_id: parseInt(tolItemMatch[1], 10),
        ...(req.body as object),
      });
    }
    if (method === 'DELETE') return noContent();
  }

  // ==========================================
  // COREHR - USER SCHEDULES
  // ==========================================
  if (u === '/corehr/user-schedules' && method === 'GET') {
    let filtered = [...mockUserSchedules];
    if (qp['tenant_user_id']) filtered = filtered.filter(us => us.tenant_user_id === parseInt(qp['tenant_user_id'], 10));
    if (qp['schedule_id']) filtered = filtered.filter(us => us.schedule_id === parseInt(qp['schedule_id'], 10));
    return ok(paginate(filtered, page, size));
  }
  if (u === '/corehr/user-schedules' && method === 'POST') {
    const body = req.body as { tenant_user_id: number; schedule_id: number; entry_time: string; departure_time: string };
    const schedule = mockSchedules.find(s => s.id === body.schedule_id);
    const newUs = {
      id: mockUserSchedules.length + 1,
      tenant_user_id: body.tenant_user_id,
      schedule_id: body.schedule_id,
      schedule: schedule ?? { id: body.schedule_id, name: '', entry_time: body.entry_time, departure_time: body.departure_time },
      description: (req.body as { description?: string }).description ?? null,
      entry_time: body.entry_time,
      departure_time: body.departure_time,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    };
    return created(newUs);
  }

  if (u.match(/^\/corehr\/user-schedules\/(\d+)$/) && method === 'DELETE') return noContent();

  // ==========================================
  // COREHR - DEVICES
  // ==========================================
  if (u === '/corehr/devices' && method === 'GET') {
    let filtered = [...mockDevices];
    if (qp['branch_id']) filtered = filtered.filter(d => d.branch_id === parseInt(qp['branch_id'], 10));
    if (qp['state'] === 'true') filtered = filtered.filter(d => d.state);
    if (qp['state'] === 'false') filtered = filtered.filter(d => !d.state);
    return ok(paginate(filtered, page, size));
  }
  if (u === '/corehr/devices' && method === 'POST') {
    const body = req.body as { code: string; branch_id: number; name?: string };
    const branch = ['', 'San Isidro', 'Miraflores', 'Surco'][body.branch_id] ?? '';
    return created({
      id: mockDevices.length + 1,
      code: body.code,
      name: body.name ?? null,
      branch_id: body.branch_id,
      branch_name: branch,
      ip: null,
      puerto: 4370,
      ubicacion: null,
      state: true,
      created_at: new Date().toISOString(),
    });
  }

  const devMatch = u.match(/^\/corehr\/devices\/(\d+)$/);
  if (devMatch) {
    const id = parseInt(devMatch[1], 10);
    const device = mockDevices.find(d => d.id === id);
    if (!device) return _error(404, 'Dispositivo no encontrado');
    if (method === 'GET') return ok(device);
    if (method === 'PATCH') return ok({ ...device, ...(req.body as object), id });
    if (method === 'DELETE') return noContent();
  }

  // ==========================================
  // COREHR - BIOMETRIA
  // ==========================================
  if (u === '/corehr/biometria' && method === 'GET') {
    let filtered = [...mockBiometria];
    if (qp['tenant_user_id']) filtered = filtered.filter(b => b.tenant_user_id === parseInt(qp['tenant_user_id'], 10));
    if (qp['device_id']) filtered = filtered.filter(b => b.device_id === parseInt(qp['device_id'], 10));
    if (qp['activo'] === 'true') filtered = filtered.filter(b => b.activo);
    if (qp['activo'] === 'false') filtered = filtered.filter(b => !b.activo);
    return ok(paginate(filtered, page, size));
  }

  if (u === '/corehr/biometria/enroll/iniciar' && method === 'POST') {
    return created({
      enroll_token: `enroll_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`,
      device_id: (req.body as { device_id: number }).device_id,
      tenant_user_id: (req.body as { tenant_user_id: number }).tenant_user_id,
      finger_index: (req.body as { finger_index: number }).finger_index,
      expires_at: new Date(Date.now() + 120000).toISOString(),
    });
  }

  if (u === '/corehr/biometria/enroll/completar' && method === 'POST') {
    return created({
      id: mockBiometria.length + 1,
      tenant_user_id: 1,
      device_id: 1,
      device_code: 'ZK-C2PRO-00123',
      finger_index: 1,
      activo: true,
      capturado_en: new Date().toISOString(),
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    });
  }

  const bioMatch = u.match(/^\/corehr\/biometria\/(\d+)$/);
  if (bioMatch && method === 'PATCH') {
    const id = parseInt(bioMatch[1], 10);
    const bio = mockBiometria.find(b => b.id === id);
    if (!bio) return _error(404, 'Registro biométrico no encontrado');
    return ok({ ...bio, ...(req.body as object), id });
  }

  // ==========================================
  // COREHR - ATTENDANCE
  // ==========================================
  if (u === '/corehr/attendance' && method === 'GET') {
    let filtered = [...mockAttendance];
    if (qp['tenant_user_id']) filtered = filtered.filter(a => a.tenant_user_id === parseInt(qp['tenant_user_id'], 10));
    if (qp['state']) filtered = filtered.filter(a => a.state === qp['state']);
    if (qp['date_from']) filtered = filtered.filter(a => a.attendance_date >= qp['date_from']);
    if (qp['date_to']) filtered = filtered.filter(a => a.attendance_date <= qp['date_to']);
    if (qp['branch_id']) filtered = filtered.filter(() => true);
    return ok(paginate(filtered, page, size));
  }

  const attMatch = u.match(/^\/corehr\/attendance\/(.+)$/);
  if (attMatch) {
    const id = attMatch[1];
    const record = mockAttendance.find(a => a.id === id);
    if (!record) return _error(404, 'Registro de asistencia no encontrado');
    if (method === 'GET') return ok(record);
    if (method === 'PATCH') return ok({ ...record, ...(req.body as object) });
  }

  // ==========================================
  // COREHR - NON-WORKING DAYS
  // ==========================================
  if (u === '/corehr/non-working-days' && method === 'GET') {
    let filtered = [...mockNonWorkingDays];
    if (qp['is_recurring'] === 'true') filtered = filtered.filter(d => d.is_recurring);
    if (qp['date_from']) filtered = filtered.filter(d => d.date >= qp['date_from']);
    if (qp['date_to']) filtered = filtered.filter(d => d.date <= qp['date_to']);
    return ok(paginate(filtered, page, size));
  }
  if (u === '/corehr/non-working-days' && method === 'POST') {
    const body = req.body as { date: string; description?: string; is_recurring?: boolean };
    return created({
      id: mockNonWorkingDays.length + 1,
      date: body.date,
      description: body.description ?? null,
      is_recurring: body.is_recurring ?? false,
      created_at: new Date().toISOString(),
    });
  }

  const nwdMatch = u.match(/^\/corehr\/non-working-days\/(\d+)$/);
  if (nwdMatch) {
    const id = parseInt(nwdMatch[1], 10);
    const day = mockNonWorkingDays.find(d => d.id === id);
    if (!day) return _error(404, 'Día no laborable no encontrado');
    if (method === 'PATCH') return ok({ ...day, ...(req.body as object), id });
    if (method === 'DELETE') return noContent();
  }

  // ==========================================
  // COREHR - TENANT CONTACTS
  // ==========================================
  if (u === '/corehr/tenant-contacts' && method === 'GET') {
    let filtered = [...mockTenantContacts];
    if (qp['type']) filtered = filtered.filter(c => c.type === qp['type']);
    return ok(paginate(filtered, page, size));
  }
  if (u === '/corehr/tenant-contacts' && method === 'POST') {
    const body = req.body as { tenant_user_id: number; type: string };
    const user = mockTenantUsers.find(u => u.id === body.tenant_user_id);
    return created({
      id: mockTenantContacts.length + 1,
      tenant_user_id: body.tenant_user_id,
      type: body.type,
      tenant_user: {
        id: body.tenant_user_id,
        nombre: user?.persona.name ?? '',
        apellido_paterno: user?.persona.father_surname ?? '',
        email: user?.email ?? '',
        phone: user?.phone ?? null,
      },
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
      deleted_at: null,
    });
  }

  const tcMatch = u.match(/^\/corehr\/tenant-contacts\/(\d+)$/);
  if (tcMatch) {
    const id = parseInt(tcMatch[1], 10);
    const contact = mockTenantContacts.find(c => c.id === id);
    if (!contact) return _error(404, 'Contacto no encontrado');
    if (method === 'PATCH') return ok({ ...contact, ...(req.body as object), id });
    if (method === 'DELETE') return noContent();
  }

  // ==========================================
  // COREHR - USUARIOS/{id}/DEPARTAMENTOS
  // ==========================================
  const deptMatch = u.match(/^\/corehr\/usuarios\/(\d+)\/departamentos$/);
  if (deptMatch) {
    const userId = parseInt(deptMatch[1], 10);
    const userDepts = mockUserDepartments.filter(d => d.tenant_user_id === userId);
    if (method === 'GET') {
      let filtered = [...userDepts];
      if (qp['activa'] === 'true') filtered = filtered.filter(d => d.end_date === null);
      return ok(paginate(filtered, page, size));
    }
    if (method === 'POST') {
      const body = req.body as { department_id: number; start_date: string; is_primary?: boolean };
      return created({
        id: mockUserDepartments.length + 1,
        tenant_user_id: userId,
        department_id: body.department_id,
        department_name: ['', 'Matemáticas', 'Comunicación', 'Ciencias', 'Humanidades', 'Idiomas', 'Educación Física', 'Arte y Cultura'][body.department_id] ?? null,
        is_primary: body.is_primary ?? false,
        start_date: body.start_date,
        end_date: null,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      });
    }
  }

  const deptItemMatch = u.match(/^\/corehr\/usuarios\/(\d+)\/departamentos\/(\d+)$/);
  if (deptItemMatch && method === 'PATCH') {
    return ok({
      id: parseInt(deptItemMatch[2], 10),
      tenant_user_id: parseInt(deptItemMatch[1], 10),
      ...(req.body as object),
    });
  }

  // ==========================================
  // WEBSOCKET INFO
  // ==========================================
  if (u.startsWith('/ws/') && method === 'GET') {
    return ok({
      endpoint: u,
      protocol: 'STOMP',
      auth_method: 'Authorization Bearer header + X-Tenant-ID',
      channels: [
        { channel: '/user/{userId}/queue/incidents', description: 'Notificaciones de incidencias' },
        { channel: '/topic/tenant/{tenantId}/incidents', description: 'Nuevas incidencias' },
      ],
    });
  }

  // Fall through to next handler
  return null;
}
