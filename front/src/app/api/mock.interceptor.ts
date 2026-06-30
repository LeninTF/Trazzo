import type { HttpEvent, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { isDevMode, inject } from '@angular/core';
import { Observable, of, delay, throwError } from 'rxjs';
import {
  mockTenantUsers, mockMasterUsers, mockUsuarioProfile, mockAuthResponse,
  mockIncidentTypes, mockIncidencias, mockShifts, mockSchedules,
  mockUserSchedules, mockDevices, mockBiometria, mockAttendance,
  mockNonWorkingDays, mockTenantContacts, mockUserDepartments,
  mockPublicKey, paginate,
} from './mock-data';
import type { AuthResponse, MessageResponse, SoftDeleteResponse } from './types';
import { API_BASE_URL } from './services/helpers';

const MOCK_DELAY = 200;

/** @internal used in tests to provide apiBase without injection context */
let _testApiBase: string | undefined;
export function _setTestApiBase(base: string | undefined): void {
  _testApiBase = base;
}

function idFromUrl(url: string, pattern: RegExp): number | null {
  const m = pattern.exec(url);
  return m ? Number.parseInt(m[1], 10) : null;
}

function queryParams(req: HttpRequest<unknown>): Record<string, string> {
  const params: Record<string, string> = {};
  const idx = req.urlWithParams.indexOf('?');
  if (idx >= 0) {
    new URLSearchParams(req.urlWithParams.slice(idx)).forEach((value, key) => {
      params[key] = value;
    });
  }
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
  let errorLabel: string;
  if (status === 403) errorLabel = 'Forbidden';
  else if (status === 404) errorLabel = 'Not Found';
  else errorLabel = 'Bad Request';
  return throwError(() => new HttpErrorResponse({
    status,
    error: {
      timestamp: new Date().toISOString(),
      status,
      error: errorLabel,
      message,
      details,
    },
  })).pipe(delay(MOCK_DELAY));
}

export function mockInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> {
  if (!isDevMode()) {
    return next(req);
  }

  const apiBase = _testApiBase ?? inject(API_BASE_URL);
  const { method } = req;
  let url = req.url;

  // Strip base URL if present
  if (url.startsWith(apiBase)) {
    url = url.slice(apiBase.length);
  }

  // Strip /api/v1 prefix if base URL didn't include it
  if (url.startsWith('/api/v1')) {
    url = url.slice('/api/v1'.length);
  }

  // Skip non-API requests
  if (!url.startsWith('/auth/') && !url.startsWith('/usuarios') && !url.startsWith('/saas/')
    && !url.startsWith('/incidentes') && !url.startsWith('/corehr/')
    && !url.startsWith('/asistencia/') && !url.startsWith('/security/')
    && !url.startsWith('/ws/')) {
    return next(req);
  }

  const qp = queryParams(req);
  const page = Number.parseInt(qp['page'] ?? '0', 10);
  const size = Number.parseInt(qp['size'] ?? '20', 10);

  try {
    return handleRoute(method, url, req, page, size, qp) ?? next(req);
  } catch {
    return next(req);
  }
}

type RouteHandler = (
  method: string,
  u: string,
  req: HttpRequest<unknown>,
  page: number,
  size: number,
  qp: Record<string, string>,
) => Observable<HttpEvent<unknown>> | null;

function handleRoute(
  method: string,
  url: string,
  req: HttpRequest<unknown>,
  page: number,
  size: number,
  qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  const u = url.split('?')[0];
  return (
    handleAuth(method, u, req, page, size, qp) ??
    handleTenantUserCollection(method, u, req, page, size, qp) ??
    handleTenantUserItem(method, u, req, page, size, qp) ??
    handleMasterUsers(method, u, req, page, size, qp) ??
    handleSecurity(method, u, req, page, size, qp) ??
    handleAsistencia(method, u, req, page, size, qp) ??
    handleIncidenteTipos(method, u, req, page, size, qp) ??
    handleIncidenteListCreate(method, u, req, page, size, qp) ??
    handleIncidenteById(method, u, req, page, size, qp) ??
    handleIncidenteEstadoActions(method, u, req, page, size, qp) ??
    handleIncidenteEvidencias(method, u, req, page, size, qp) ??
    handleCorehrShifts(method, u, req, page, size, qp) ??
    handleCorehrSchedules(method, u, req, page, size, qp) ??
    handleCorehrTolerancias(method, u, req, page, size, qp) ??
    handleCorehrUserSchedules(method, u, req, page, size, qp) ??
    handleCorehrDevices(method, u, req, page, size, qp) ??
    handleCorehrBiometria(method, u, req, page, size, qp) ??
    handleCorehrAttendance(method, u, req, page, size, qp) ??
    handleCorehrNonWorkingDays(method, u, req, page, size, qp) ??
    handleCorehrTenantContacts(method, u, req, page, size, qp) ??
    handleCorehrUserDepartments(method, u, req, page, size, qp) ??
    handleWebsocket(method, u, req, page, size, qp) ??
    null
  );
}

function handleAuth(
  _method: string, u: string, _req: HttpRequest<unknown>,
  _page: number, _size: number, _qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (u === '/auth/login' && _method === 'POST') {
    return ok<AuthResponse>({
      ...mockAuthResponse,
      accessToken: 'mock-token-002',
      usuario: { ...mockUsuarioProfile, ultimo_acceso: new Date().toISOString() },
    });
  }
  return null;
}

function handleTenantUserList(
  method: string, u: string, _req: HttpRequest<unknown>,
  page: number, size: number, qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (!(u === '/usuarios' && method === 'GET')) return null;

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
  if (qp['role_id']) filtered = filtered.filter(u => u.rol.id === Number.parseInt(qp['role_id'], 10));
  return ok(paginate(filtered, page, size));
}

function handleTenantUserCreate(
  method: string, u: string, req: HttpRequest<unknown>,
  _page: number, _size: number, _qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (!(u === '/usuarios' && method === 'POST')) return null;

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

function handleTenantUserMe(
  method: string, u: string, req: HttpRequest<unknown>,
  _page: number, _size: number, _qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (u !== '/usuarios/me') return null;

  if (method === 'GET') return ok(mockTenantUsers[0]);
  if (method === 'PATCH') {
    return ok({ ...mockTenantUsers[0], ...(req.body as object) });
  }
  return null;
}

function handleTenantUserCollection(
  method: string, u: string, req: HttpRequest<unknown>,
  page: number, size: number, qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (!u.startsWith('/usuarios')) return null;
  return (
    handleTenantUserList(method, u, req, page, size, qp) ??
    handleTenantUserCreate(method, u, req, page, size, qp) ??
    handleTenantUserMe(method, u, req, page, size, qp) ??
    null
  );
}

function handleTenantUserItem(
  method: string, u: string, req: HttpRequest<unknown>,
  _page: number, _size: number, _qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (!u.startsWith('/usuarios')) return null;

  const uidMatch = /^\/usuarios\/(\d+)$/.exec(u);
  if (uidMatch) {
    const id = Number.parseInt(uidMatch[1], 10);
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

  const rolMatch = /^\/usuarios\/(\d+)\/rol$/.exec(u);
  if (rolMatch && method === 'PUT') {
    const id = Number.parseInt(rolMatch[1], 10);
    const user = mockTenantUsers.find(u => u.id === id);
    if (!user) return _error(404, 'Usuario no encontrado');
    const body = req.body as { role_id?: number };
    const rolesDisponibles = mockTenantUsers.map(u => u.rol);
    const newRole = rolesDisponibles.find(r => r.id === body.role_id);
    return ok({ ...user, rol: newRole ?? user.rol });
  }

  const passMatch = /^\/usuarios\/(\d+)\/password$/.exec(u);
  if (passMatch && method === 'PATCH') {
    return noContent();
  }

  return null;
}

function handleMasterUsers(
  method: string, u: string, req: HttpRequest<unknown>,
  page: number, size: number, qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (!u.startsWith('/saas/')) return null;

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

  if (u === '/saas/users/me' && method === 'GET') return ok(mockMasterUsers[0]);

  const mUidMatch = /^\/saas\/users\/(\d+)$/.exec(u);
  if (mUidMatch) {
    const id = Number.parseInt(mUidMatch[1], 10);
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

  return null;
}

function handleSecurity(
  _method: string, u: string, _req: HttpRequest<unknown>,
  _page: number, _size: number, _qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (u === '/security/public-key' && _method === 'GET') {
    return ok(mockPublicKey);
  }
  return null;
}

function handleAsistencia(
  method: string, u: string, _req: HttpRequest<unknown>,
  _page: number, _size: number, _qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (u === '/asistencia/marcar' && method === 'POST') {
    return ok(mockAttendance[0]);
  }
  if (u === '/asistencia/sync' && method === 'POST') {
    return accepted<MessageResponse>({ message: 'Lote aceptado para procesamiento en segundo plano.', status: 'queued' });
  }
  return null;
}

function handleIncidenteTipos(
  method: string, u: string, req: HttpRequest<unknown>,
  page: number, size: number, qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (!u.startsWith('/incidentes/tipos')) return null;

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

  const itMatch = /^\/incidentes\/tipos\/(\d+)$/.exec(u);
  if (itMatch) {
    const id = Number.parseInt(itMatch[1], 10);
    const tipo = mockIncidentTypes.find(t => t.id === id);
    if (!tipo) return _error(404, 'Tipo de incidencia no encontrado');
    if (method === 'GET') return ok(tipo);
    if (method === 'PATCH') return ok({ ...tipo, ...(req.body as object), id });
  }

  return null;
}

function handleIncidenteListCreate(
  method: string, u: string, req: HttpRequest<unknown>,
  page: number, size: number, qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (!u.startsWith('/incidentes')) return null;

  if (u === '/incidentes' && method === 'GET') {
    let filtered = [...mockIncidencias];
    if (qp['state']) filtered = filtered.filter(i => i.state === qp['state']);
    if (qp['tipo_id']) filtered = filtered.filter(i => i.incidencia_type_id === Number.parseInt(qp['tipo_id'], 10));
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

  return null;
}

function handleIncidenteById(
  method: string, u: string, req: HttpRequest<unknown>,
  _page: number, _size: number, _qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  const incMatch = /^\/incidentes\/(\d+)$/.exec(u);
  if (incMatch) {
    const id = Number.parseInt(incMatch[1], 10);
    const incident = mockIncidencias.find(i => i.id === id);
    if (!incident) return _error(404, 'Incidencia no encontrada');

    if (method === 'GET') return ok(incident);
    if (method === 'PATCH') {
      return ok({ ...incident, ...(req.body as object), id });
    }
  }
  return null;
}

function handleIncidenteEstadoActions(
  method: string, u: string, req: HttpRequest<unknown>,
  _page: number, _size: number, _qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  const incStateMatch = /^\/incidentes\/(\d+)\/estado$/.exec(u);
  if (incStateMatch && method === 'PATCH') {
    const id = Number.parseInt(incStateMatch[1], 10);
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

  if (/^\/incidentes\/(\d+)\/notificar$/.test(u) && method === 'POST') {
    return accepted<MessageResponse>({ message: 'Notificación encolada para envío.', status: 'queued' });
  }

  if (/^\/incidentes\/(\d+)\/justificar$/.test(u) && method === 'POST') {
    return accepted<MessageResponse>({ message: 'Proceso de justificación encolado.', status: 'queued' });
  }

  return null;
}

function handleIncidenteEvidencias(
  method: string, u: string, req: HttpRequest<unknown>,
  _page: number, _size: number, _qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  const evMatch = /^\/incidentes\/(\d+)\/evidencias$/.exec(u);
  if (evMatch) {
    const id = Number.parseInt(evMatch[1], 10);
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

  const evDelMatch = /^\/incidentes\/(\d+)\/evidencias\/(\d+)$/.exec(u);
  if (evDelMatch && method === 'DELETE') return noContent();

  return null;
}

function handleCorehrShifts(
  method: string, u: string, req: HttpRequest<unknown>,
  page: number, size: number, qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (!u.startsWith('/corehr/shifts')) return null;

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

  const shiftMatch = /^\/corehr\/shifts\/(\d+)$/.exec(u);
  if (shiftMatch) {
    const id = Number.parseInt(shiftMatch[1], 10);
    const shift = mockShifts.find(s => s.id === id);
    if (!shift) return _error(404, 'Turno no encontrado');
    if (method === 'GET') return ok(shift);
    if (method === 'PATCH') return ok({ ...shift, ...(req.body as object), id });
    if (method === 'DELETE') return noContent();
  }

  return null;
}

function handleCorehrSchedules(
  method: string, u: string, req: HttpRequest<unknown>,
  page: number, size: number, qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (!u.startsWith('/corehr/schedules')) return null;

  if (u === '/corehr/schedules' && method === 'GET') {
    let filtered = [...mockSchedules];
    if (qp['shift_id']) filtered = filtered.filter(s => s.shift_id === Number.parseInt(qp['shift_id'], 10));
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

  const schedMatch = /^\/corehr\/schedules\/(\d+)$/.exec(u);
  if (schedMatch) {
    const id = Number.parseInt(schedMatch[1], 10);
    const schedule = mockSchedules.find(s => s.id === id);
    if (!schedule) return _error(404, 'Schedule no encontrado');
    if (method === 'GET') return ok(schedule);
    if (method === 'PATCH') return ok({ ...schedule, ...(req.body as object), id });
    if (method === 'DELETE') return noContent();
  }

  return null;
}

function handleCorehrTolerancias(
  method: string, u: string, req: HttpRequest<unknown>,
  page: number, size: number, _qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  const tolMatch = /^\/corehr\/schedules\/(\d+)\/tolerancias$/.exec(u);
  if (tolMatch) {
    const schedId = Number.parseInt(tolMatch[1], 10);
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

  const tolItemMatch = /^\/corehr\/schedules\/(\d+)\/tolerancias\/(\d+)$/.exec(u);
  if (tolItemMatch) {
    if (method === 'PATCH') {
      return ok({
        id: Number.parseInt(tolItemMatch[2], 10),
        schedule_id: Number.parseInt(tolItemMatch[1], 10),
        ...(req.body as object),
      });
    }
    if (method === 'DELETE') return noContent();
  }

  return null;
}

function handleCorehrUserSchedules(
  method: string, u: string, req: HttpRequest<unknown>,
  page: number, size: number, qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (!u.startsWith('/corehr/user-schedules')) return null;

  if (u === '/corehr/user-schedules' && method === 'GET') {
    let filtered = [...mockUserSchedules];
    if (qp['tenant_user_id']) filtered = filtered.filter(us => us.tenant_user_id === Number.parseInt(qp['tenant_user_id'], 10));
    if (qp['schedule_id']) filtered = filtered.filter(us => us.schedule_id === Number.parseInt(qp['schedule_id'], 10));
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

  if (/^\/corehr\/user-schedules\/(\d+)$/.test(u) && method === 'DELETE') return noContent();

  return null;
}

function handleCorehrDevices(
  method: string, u: string, req: HttpRequest<unknown>,
  page: number, size: number, qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (!u.startsWith('/corehr/devices')) return null;

  if (u === '/corehr/devices' && method === 'GET') {
    let filtered = [...mockDevices];
    if (qp['branch_id']) filtered = filtered.filter(d => d.branch_id === Number.parseInt(qp['branch_id'], 10));
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

  const devMatch = /^\/corehr\/devices\/(\d+)$/.exec(u);
  if (devMatch) {
    const id = Number.parseInt(devMatch[1], 10);
    const device = mockDevices.find(d => d.id === id);
    if (!device) return _error(404, 'Dispositivo no encontrado');
    if (method === 'GET') return ok(device);
    if (method === 'PATCH') return ok({ ...device, ...(req.body as object), id });
    if (method === 'DELETE') return noContent();
  }

  return null;
}

function handleCorehrBiometria(
  method: string, u: string, req: HttpRequest<unknown>,
  page: number, size: number, qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (!u.startsWith('/corehr/biometria')) return null;

  if (u === '/corehr/biometria' && method === 'GET') {
    let filtered = [...mockBiometria];
    if (qp['tenant_user_id']) filtered = filtered.filter(b => b.tenant_user_id === Number.parseInt(qp['tenant_user_id'], 10));
    if (qp['device_id']) filtered = filtered.filter(b => b.device_id === Number.parseInt(qp['device_id'], 10));
    if (qp['activo'] === 'true') filtered = filtered.filter(b => b.activo);
    if (qp['activo'] === 'false') filtered = filtered.filter(b => !b.activo);
    return ok(paginate(filtered, page, size));
  }

  if (u === '/corehr/biometria/enroll/iniciar' && method === 'POST') {
    return created({
      enroll_token: 'mock-enroll-token-001',
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

  const bioMatch = /^\/corehr\/biometria\/(\d+)$/.exec(u);
  if (bioMatch && method === 'PATCH') {
    const id = Number.parseInt(bioMatch[1], 10);
    const bio = mockBiometria.find(b => b.id === id);
    if (!bio) return _error(404, 'Registro biométrico no encontrado');
    return ok({ ...bio, ...(req.body as object), id });
  }

  return null;
}

function handleCorehrAttendance(
  method: string, u: string, req: HttpRequest<unknown>,
  page: number, size: number, qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (!u.startsWith('/corehr/attendance')) return null;

  if (u === '/corehr/attendance' && method === 'GET') {
    let filtered = [...mockAttendance];
    if (qp['tenant_user_id']) filtered = filtered.filter(a => a.tenant_user_id === Number.parseInt(qp['tenant_user_id'], 10));
    if (qp['state']) filtered = filtered.filter(a => a.state === qp['state']);
    if (qp['date_from']) filtered = filtered.filter(a => a.attendance_date >= qp['date_from']);
    if (qp['date_to']) filtered = filtered.filter(a => a.attendance_date <= qp['date_to']);
    if (qp['branch_id']) filtered = filtered.filter(() => true);
    return ok(paginate(filtered, page, size));
  }

  const attMatch = /^\/corehr\/attendance\/(.+)$/.exec(u);
  if (attMatch) {
    const id = attMatch[1];
    const record = mockAttendance.find(a => a.id === id);
    if (!record) return _error(404, 'Registro de asistencia no encontrado');
    if (method === 'GET') return ok(record);
    if (method === 'PATCH') return ok({ ...record, ...(req.body as object) });
  }

  return null;
}

function handleCorehrNonWorkingDays(
  method: string, u: string, req: HttpRequest<unknown>,
  page: number, size: number, qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (!u.startsWith('/corehr/non-working-days')) return null;

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

  const nwdMatch = /^\/corehr\/non-working-days\/(\d+)$/.exec(u);
  if (nwdMatch) {
    const id = Number.parseInt(nwdMatch[1], 10);
    const day = mockNonWorkingDays.find(d => d.id === id);
    if (!day) return _error(404, 'Día no laborable no encontrado');
    if (method === 'PATCH') return ok({ ...day, ...(req.body as object), id });
    if (method === 'DELETE') return noContent();
  }

  return null;
}

function handleCorehrTenantContacts(
  method: string, u: string, req: HttpRequest<unknown>,
  page: number, size: number, qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (!u.startsWith('/corehr/tenant-contacts')) return null;

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

  const tcMatch = /^\/corehr\/tenant-contacts\/(\d+)$/.exec(u);
  if (tcMatch) {
    const id = Number.parseInt(tcMatch[1], 10);
    const contact = mockTenantContacts.find(c => c.id === id);
    if (!contact) return _error(404, 'Contacto no encontrado');
    if (method === 'PATCH') return ok({ ...contact, ...(req.body as object), id });
    if (method === 'DELETE') return noContent();
  }

  return null;
}

function handleCorehrUserDepartments(
  method: string, u: string, req: HttpRequest<unknown>,
  page: number, size: number, qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  const deptMatch = /^\/corehr\/usuarios\/(\d+)\/departamentos$/.exec(u);
  if (deptMatch) {
    const userId = Number.parseInt(deptMatch[1], 10);
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

  const deptItemMatch = /^\/corehr\/usuarios\/(\d+)\/departamentos\/(\d+)$/.exec(u);
  if (deptItemMatch && method === 'PATCH') {
    return ok({
      id: Number.parseInt(deptItemMatch[2], 10),
      tenant_user_id: Number.parseInt(deptItemMatch[1], 10),
      ...(req.body as object),
    });
  }

  return null;
}

function handleWebsocket(
  _method: string, u: string, _req: HttpRequest<unknown>,
  _page: number, _size: number, _qp: Record<string, string>,
): Observable<HttpEvent<unknown>> | null {
  if (u.startsWith('/ws/') && _method === 'GET') {
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
  return null;
}
