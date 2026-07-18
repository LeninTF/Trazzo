import { HttpRequest, HttpResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { mockInterceptor, _setTestApiBase } from './mock.interceptor';
import { mockAttendance, mockMasterUsers } from './mock-data';

_setTestApiBase('https://api.trazzo.pe/api/v1');

describe('mockInterceptor', () => {
  function next() {
    return of(new HttpResponse({ status: 404 }));
  }

  it('should mock auth login POST', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/auth/login', {});
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body).toBeDefined();
      expect(r.body.accessToken).toBeTruthy();
      expect(r.body.usuario).toBeTruthy();
      done();
    });
  });

  it('should mock GET /usuarios', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/usuarios');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content).toBeDefined();
      expect(Array.isArray(r.body.content)).toBeTrue();
      done();
    });
  });

  it('should mock POST /usuarios', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/usuarios', { name: 'Test' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(201);
      expect(r.body.id).toBeDefined();
      done();
    });
  });

  it('should mock GET /usuarios/me', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/usuarios/me');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.id).toBeDefined();
      done();
    });
  });

  it('should mock PATCH /usuarios/me', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/usuarios/me', { name: 'X' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock GET /usuarios/{id}', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/usuarios/1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock DELETE /usuarios/{id}', (done) => {
    const req = new HttpRequest('DELETE', 'https://api.trazzo.pe/api/v1/usuarios/1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.status).toBe('INACTIVO');
      done();
    });
  });

  it('should mock PUT /usuarios/{id}/rol', (done) => {
    const req = new HttpRequest('PUT', 'https://api.trazzo.pe/api/v1/usuarios/1/rol', { role_id: 2 });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock PATCH /usuarios/{id}/password', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/usuarios/1/password', {});
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(204);
      done();
    });
  });

  it('should mock GET /saas/users', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/saas/users');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content).toBeDefined();
      done();
    });
  });

  it('should mock POST /saas/users', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/saas/users', { email: 'a@b.com' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(201);
      done();
    });
  });

  it('should mock GET /saas/users/me', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/saas/users/me');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock GET /security/public-key', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/security/public-key');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.publicKey).toBeTruthy();
      done();
    });
  });

  it('should mock GET /incidentes/tipos', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/incidentes/tipos');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content).toBeDefined();
      done();
    });
  });

  it('should mock POST /incidentes/tipos', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/incidentes/tipos', { nombre: 'Test' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(201);
      done();
    });
  });

  it('should mock GET /incidentes', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/incidentes');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content).toBeDefined();
      done();
    });
  });

  it('should mock POST /incidentes', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/incidentes', { incidencia_type_id: 1 });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(201);
      done();
    });
  });

  it('should mock GET /corehr/shifts', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/shifts');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content).toBeDefined();
      done();
    });
  });

  it('should mock POST /corehr/shifts', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/corehr/shifts', { name: 'Test' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(201);
      done();
    });
  });

  it('should mock GET /corehr/devices', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/devices');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content).toBeDefined();
      done();
    });
  });

  it('should mock POST /corehr/devices', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/corehr/devices', { code: 'D1', branch_id: 1 });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(201);
      done();
    });
  });

  it('should mock GET /corehr/attendance', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/attendance');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content).toBeDefined();
      done();
    });
  });

  it('should mock GET /corehr/non-working-days', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/non-working-days');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content).toBeDefined();
      done();
    });
  });

  it('should mock POST /corehr/non-working-days', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/corehr/non-working-days', { date: '2025-01-01', description: 'Año Nuevo' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(201);
      done();
    });
  });

  it('should mock GET /corehr/tenant-contacts', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/tenant-contacts');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content).toBeDefined();
      done();
    });
  });

  it('should handle GET /usuarios/{id} not found', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/usuarios/999');
    mockInterceptor(req, next).subscribe({
      error: err => {
        expect(err.status).toBe(404);
        done();
      },
    });
  });

  it('should handle PATCH /usuarios/{id}/estado', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/usuarios/1', { estado: 'INACTIVO' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock GET /incidentes with query params', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/incidentes?state=PENDIENTE');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content).toBeDefined();
      done();
    });
  });

  it('should mock POST /asistencia/marcar', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/asistencia/marcar', {});
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock POST /asistencia/sync', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/asistencia/sync', []);
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(202);
      done();
    });
  });

  it('should mock PATCH /incidentes/{id}/estado approve', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/incidentes/1/estado', { state: 'APROBADO', days_granted: 1 });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock GET /corehr/schedules with shift_id', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/schedules?shift_id=1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content).toBeDefined();
      done();
    });
  });

  it('should mock POST /corehr/schedules', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/corehr/schedules', { shift_id: 1, name: 'Test', entry_time: '08:00', departure_time: '16:00' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(201);
      done();
    });
  });

  it('should mock GET /corehr/user-schedules', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/user-schedules');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content).toBeDefined();
      done();
    });
  });

  it('should mock POST /corehr/user-schedules', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/corehr/user-schedules', { tenant_user_id: 1, schedule_id: 1, entry_time: '08:00', departure_time: '16:00' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(201);
      done();
    });
  });

  it('should mock DELETE /corehr/user-schedules/{id}', (done) => {
    const req = new HttpRequest('DELETE', 'https://api.trazzo.pe/api/v1/corehr/user-schedules/1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(204);
      done();
    });
  });

  it('should mock GET /corehr/biometria', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/biometria');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content).toBeDefined();
      done();
    });
  });

  it('should mock GET /incidentes/{id}/evidencias', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/incidentes/1/evidencias');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should handle GET /corehr/shifts/{id} not found', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/shifts/999');
    mockInterceptor(req, next).subscribe({
      error: err => {
        expect(err.status).toBe(404);
        done();
      },
    });
  });

  it('should mock PUT /usuarios/{id}', (done) => {
    const req = new HttpRequest('PUT', 'https://api.trazzo.pe/api/v1/usuarios/1', { nombre: 'Updated' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body).toBeDefined();
      done();
    });
  });

  it('should mock GET /saas/users/{id}', (done) => {
    const req = new HttpRequest('GET', `https://api.trazzo.pe/api/v1/saas/users/${mockMasterUsers[0].id}`);
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body).toBeDefined();
      done();
    });
  });

  it('should mock GET /saas/users/{id} not found', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/saas/users/999');
    mockInterceptor(req, next).subscribe({
      error: err => {
        expect(err.status).toBe(404);
        done();
      },
    });
  });

  it('should mock PATCH /saas/users/{id}', (done) => {
    const req = new HttpRequest('PATCH', `https://api.trazzo.pe/api/v1/saas/users/${mockMasterUsers[0].id}`, { nombre: 'Updated' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock DELETE /saas/users/{id}', (done) => {
    const req = new HttpRequest('DELETE', `https://api.trazzo.pe/api/v1/saas/users/${mockMasterUsers[0].id}`);
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock GET /incidentes/tipos/{id}', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/incidentes/tipos/1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body).toBeDefined();
      done();
    });
  });

  it('should mock GET /incidentes/tipos/{id} not found', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/incidentes/tipos/999');
    mockInterceptor(req, next).subscribe({
      error: err => {
        expect(err.status).toBe(404);
        done();
      },
    });
  });

  it('should mock PATCH /incidentes/tipos/{id}', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/incidentes/tipos/1', { name: 'Updated' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock GET /incidentes/{id}', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/incidentes/1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body).toBeDefined();
      done();
    });
  });

  it('should mock GET /incidentes/{id} not found', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/incidentes/999');
    mockInterceptor(req, next).subscribe({
      error: err => {
        expect(err.status).toBe(404);
        done();
      },
    });
  });

  it('should mock PATCH /incidentes/{id}', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/incidentes/1', { comment: 'Updated' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock POST /incidentes/{id}/evidencias', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/incidentes/1/evidencias', { file: 'data' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(201);
      done();
    });
  });

  it('should mock DELETE /incidentes/{id}/evidencias/{evidenceId}', (done) => {
    const req = new HttpRequest('DELETE', 'https://api.trazzo.pe/api/v1/incidentes/1/evidencias/1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(204);
      done();
    });
  });

  it('should mock POST /incidentes/{id}/notificar', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/incidentes/1/notificar', {});
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(202);
      done();
    });
  });

  it('should mock POST /incidentes/{id}/justificar', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/incidentes/1/justificar', {});
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(202);
      done();
    });
  });

  it('should mock PATCH /corehr/shifts/{id}', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/corehr/shifts/1', { name: 'Updated' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock DELETE /corehr/shifts/{id}', (done) => {
    const req = new HttpRequest('DELETE', 'https://api.trazzo.pe/api/v1/corehr/shifts/1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(204);
      done();
    });
  });

  it('should mock GET /corehr/schedules/{id}', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/schedules/1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body).toBeDefined();
      done();
    });
  });

  it('should mock GET /corehr/schedules/{id} not found', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/schedules/999');
    mockInterceptor(req, next).subscribe({
      error: err => {
        expect(err.status).toBe(404);
        done();
      },
    });
  });

  it('should mock PATCH /corehr/schedules/{id}', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/corehr/schedules/1', { name: 'Updated' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock DELETE /corehr/schedules/{id}', (done) => {
    const req = new HttpRequest('DELETE', 'https://api.trazzo.pe/api/v1/corehr/schedules/1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(204);
      done();
    });
  });

  it('should mock GET /corehr/schedules/{id}/tolerancias', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/schedules/1/tolerancias');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content).toBeDefined();
      done();
    });
  });

  it('should mock POST /corehr/schedules/{id}/tolerancias', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/corehr/schedules/1/tolerancias', { minutes: 15, type: 'entry' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(201);
      done();
    });
  });

  it('should mock PATCH /corehr/schedules/{id}/tolerancias/{tolId}', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/corehr/schedules/1/tolerancias/1', { minutes: 10 });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock DELETE /corehr/schedules/{id}/tolerancias/{tolId}', (done) => {
    const req = new HttpRequest('DELETE', 'https://api.trazzo.pe/api/v1/corehr/schedules/1/tolerancias/1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(204);
      done();
    });
  });

  it('should mock GET /corehr/devices/{id}', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/devices/1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body).toBeDefined();
      done();
    });
  });

  it('should mock GET /corehr/devices/{id} not found', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/devices/999');
    mockInterceptor(req, next).subscribe({
      error: err => {
        expect(err.status).toBe(404);
        done();
      },
    });
  });

  it('should mock PATCH /corehr/devices/{id}', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/corehr/devices/1', { name: 'Updated' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock DELETE /corehr/devices/{id}', (done) => {
    const req = new HttpRequest('DELETE', 'https://api.trazzo.pe/api/v1/corehr/devices/1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(204);
      done();
    });
  });

  it('should mock POST /corehr/biometria/enroll/iniciar', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/corehr/biometria/enroll/iniciar', { tenant_user_id: 1 });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(201);
      done();
    });
  });

  it('should mock POST /corehr/biometria/enroll/completar', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/corehr/biometria/enroll/completar', { enrollment_id: 'e1' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(201);
      done();
    });
  });

  it('should mock PATCH /corehr/biometria/{id}', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/corehr/biometria/1', { activo: false });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock PATCH /corehr/biometria/{id} not found', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/corehr/biometria/999', {});
    mockInterceptor(req, next).subscribe({
      error: err => {
        expect(err.status).toBe(404);
        done();
      },
    });
  });

  it('should mock GET /corehr/attendance/{id}', (done) => {
    const validId = mockAttendance[0].id;
    const req = new HttpRequest('GET', `https://api.trazzo.pe/api/v1/corehr/attendance/${validId}`);
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body).toBeDefined();
      done();
    });
  });

  it('should mock GET /corehr/attendance/{id} not found', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/attendance/nonexistent');
    mockInterceptor(req, next).subscribe({
      error: err => {
        expect(err.status).toBe(404);
        done();
      },
    });
  });

  it('should mock PATCH /corehr/attendance/{id}', (done) => {
    const validId = mockAttendance[0].id;
    const req = new HttpRequest('PATCH', `https://api.trazzo.pe/api/v1/corehr/attendance/${validId}`, { state: 'PRESENTE' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock PATCH /corehr/non-working-days/{id}', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/corehr/non-working-days/1', { description: 'Updated' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock PATCH /corehr/non-working-days/{id} not found', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/corehr/non-working-days/999', {});
    mockInterceptor(req, next).subscribe({
      error: err => {
        expect(err.status).toBe(404);
        done();
      },
    });
  });

  it('should mock DELETE /corehr/non-working-days/{id}', (done) => {
    const req = new HttpRequest('DELETE', 'https://api.trazzo.pe/api/v1/corehr/non-working-days/1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(204);
      done();
    });
  });

  it('should mock POST /corehr/tenant-contacts', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/corehr/tenant-contacts', { type: 'PHONE', value: '999888777' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(201);
      done();
    });
  });

  it('should mock PATCH /corehr/tenant-contacts/{id}', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/corehr/tenant-contacts/1', { value: 'Updated' });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock DELETE /corehr/tenant-contacts/{id}', (done) => {
    const req = new HttpRequest('DELETE', 'https://api.trazzo.pe/api/v1/corehr/tenant-contacts/1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(204);
      done();
    });
  });

  it('should mock DELETE /corehr/tenant-contacts/{id} not found', (done) => {
    const req = new HttpRequest('DELETE', 'https://api.trazzo.pe/api/v1/corehr/tenant-contacts/999');
    mockInterceptor(req, next).subscribe({
      error: err => {
        expect(err.status).toBe(404);
        done();
      },
    });
  });

  it('should mock GET /corehr/usuarios/{id}/departamentos', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/usuarios/1/departamentos');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content).toBeDefined();
      done();
    });
  });

  it('should mock POST /corehr/usuarios/{id}/departamentos', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/corehr/usuarios/1/departamentos', { departamento_id: 1, activa: true });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(201);
      done();
    });
  });

  it('should mock PATCH /corehr/usuarios/{id}/departamentos/{deptId}', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/corehr/usuarios/1/departamentos/1', { activa: false });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should mock GET /ws/info', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/ws/info');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.endpoint).toBeDefined();
      done();
    });
  });

  it('should pass through non-API requests', (done) => {
    const req = new HttpRequest('GET', '/some/other/url');
    const nextSpy = jasmine.createSpy('next').and.returnValue(of(new HttpResponse({ status: 404 })));
    mockInterceptor(req, nextSpy).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(404);
      expect(nextSpy).toHaveBeenCalled();
      done();
    });
  });
});
