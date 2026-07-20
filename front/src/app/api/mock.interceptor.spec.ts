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

  // =====================================================================
  // Branch coverage: _error() status 403 "Forbidden" & default "Bad Request"
  // _error is internal-only and every call site passes 404, so these
  // branches are unreachable through mockInterceptor. Documented here
  // for completeness — no test can exercise them via the public API.
  // =====================================================================

  // Branch coverage: queryParams() idx < 0 (URL without '?')
  it('should handle queryParams idx < 0 when URL has no question mark', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/usuarios');
    const nextSpy = jasmine.createSpy('next').and.returnValue(of(new HttpResponse({ status: 404 })));
    mockInterceptor(req, nextSpy).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(nextSpy).not.toHaveBeenCalled();
      done();
    });
  });

  // Branch coverage: handleTenantUserList – filter by status
  it('should filter GET /usuarios by status=ACTIVO', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/usuarios?status=ACTIVO');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((u: any) => expect(u.estado).toBe('ACTIVO'));
      done();
    });
  });

  it('should filter GET /usuarios by status=INACTIVO', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/usuarios?status=INACTIVO');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((u: any) => expect(u.estado).toBe('INACTIVO'));
      done();
    });
  });

  // Branch coverage: handleTenantUserList – filter by search
  it('should filter GET /usuarios by search query param', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/usuarios?search=josselin');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content.length).toBeGreaterThan(0);
      r.body.content.forEach((u: any) => {
        const match =
          u.persona.name.toLowerCase().includes('josselin') ||
          u.persona.father_surname.toLowerCase().includes('josselin') ||
          (u.email ?? '').toLowerCase().includes('josselin');
        expect(match).toBeTrue();
      });
      done();
    });
  });

  // Branch coverage: handleTenantUserList – filter by role_id
  it('should filter GET /usuarios by role_id', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/usuarios?role_id=1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((u: any) => expect(u.rol.id).toBe(1));
      done();
    });
  });

  it('should filter GET /usuarios by role_id=5', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/usuarios?role_id=5');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((u: any) => expect(u.rol.id).toBe(5));
      done();
    });
  });

  // Branch coverage: handleIncidenteListCreate – filter by tipo_id
  it('should filter GET /incidentes by tipo_id', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/incidentes?tipo_id=1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((i: any) => expect(i.incidencia_type_id).toBe(1));
      done();
    });
  });

  // Branch coverage: handleIncidenteListCreate – filter by desde
  it('should filter GET /incidentes by desde', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/incidentes?desde=2026-07-01');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((i: any) => expect(i.created_at >= '2026-07-01').toBe(true));
      done();
    });
  });

  // Branch coverage: handleIncidenteListCreate – filter by hasta
  it('should filter GET /incidentes by hasta', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/incidentes?hasta=2026-07-01');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((i: any) => expect(i.created_at <= '2026-07-01T23:59:59Z').toBe(true));
      done();
    });
  });

  // Branch coverage: handleIncidenteListCreate – filter by search (matches tenant_user.nombre)
  it('should filter GET /incidentes by search matching tenant_user nombre', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/incidentes?search=maría');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content.length).toBeGreaterThan(0);
      r.body.content.forEach((i: any) => {
        const match =
          (i.comment ?? '').toLowerCase().includes('maría') ||
          i.tenant_user.nombre.toLowerCase().includes('maría');
        expect(match).toBeTrue();
      });
      done();
    });
  });

  // Branch coverage: handleIncidenteTipoListCreate – activo=false filter
  it('should filter GET /incidentes/tipos by activo=false', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/incidentes/tipos?activo=false');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content.length).toBeGreaterThan(0);
      r.body.content.forEach((t: any) => expect(t.activo).toBe(false));
      done();
    });
  });

  // Branch coverage: handleIncidenteTipoListCreate – activo=true filter
  it('should filter GET /incidentes/tipos by activo=true', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/incidentes/tipos?activo=true');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((t: any) => expect(t.activo).toBe(true));
      done();
    });
  });

  // Branch coverage: handleIncidenteEstadoActions – DENEGADO (permiso=null path)
  it('should mock PATCH /incidentes/{id}/estado with DENEGADO', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/incidentes/1/estado', {
      state: 'DENEGADO',
      motivo_rechazo: 'Documentación insuficiente',
    });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.state).toBe('DENEGADO');
      expect(r.body.permiso).toBeNull();
      done();
    });
  });

  // Branch coverage: handleIncidenteEstadoActions – APROBADO without days_granted
  it('should mock PATCH /incidentes/{id}/estado APROBADO without days_granted', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/incidentes/4/estado', {
      state: 'APROBADO',
    });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.state).toBe('APROBADO');
      expect(r.body.permiso).toBeNull();
      done();
    });
  });

  // Branch coverage: handleIncidenteEstadoActions – incident not found
  it('should return 404 for PATCH /incidentes/{id}/estado when incident not found', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/incidentes/999/estado', {
      state: 'APROBADO',
      days_granted: 1,
    });
    mockInterceptor(req, next).subscribe({
      error: err => {
        expect(err.status).toBe(404);
        done();
      },
    });
  });

  // Branch coverage: handleIncidenteById – incident not found for PATCH
  it('should return 404 for PATCH /incidentes/{id} when incident not found', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/incidentes/999', { comment: 'test' });
    mockInterceptor(req, next).subscribe({
      error: err => {
        expect(err.status).toBe(404);
        done();
      },
    });
  });

  // Branch coverage: handleIncidenteEvidencias – incident not found
  it('should return 404 for GET /incidentes/{id}/evidencias when incident not found', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/incidentes/999/evidencias');
    mockInterceptor(req, next).subscribe({
      error: err => {
        expect(err.status).toBe(404);
        done();
      },
    });
  });

  it('should return 404 for POST /incidentes/{id}/evidencias when incident not found', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/incidentes/999/evidencias', { file_name: 'f.pdf', file_url: 'url', mime_type: 'application/pdf', file_size: 100 });
    mockInterceptor(req, next).subscribe({
      error: err => {
        expect(err.status).toBe(404);
        done();
      },
    });
  });

  // Branch coverage: handleIncidenteTipoById – not found for PATCH
  it('should return 404 for PATCH /incidentes/tipos/{id} when tipo not found', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/incidentes/tipos/999', { name: 'Updated' });
    mockInterceptor(req, next).subscribe({
      error: err => {
        expect(err.status).toBe(404);
        done();
      },
    });
  });

  // Branch coverage: handleCorehrShiftListCreate – search filter
  it('should filter GET /corehr/shifts by search', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/shifts?search=mañana');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content.length).toBeGreaterThan(0);
      r.body.content.forEach((s: any) => expect(s.name.toLowerCase()).toContain('mañana'));
      done();
    });
  });

  // Branch coverage: handleCorehrUserSchedules – filter by tenant_user_id
  it('should filter GET /corehr/user-schedules by tenant_user_id', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/user-schedules?tenant_user_id=1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((us: any) => expect(us.tenant_user_id).toBe(1));
      done();
    });
  });

  // Branch coverage: handleCorehrUserSchedules – filter by schedule_id
  it('should filter GET /corehr/user-schedules by schedule_id', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/user-schedules?schedule_id=2');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((us: any) => expect(us.schedule_id).toBe(2));
      done();
    });
  });

  // Branch coverage: handleCorehrDeviceListCreate – filter by branch_id
  it('should filter GET /corehr/devices by branch_id', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/devices?branch_id=1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((d: any) => expect(d.branch_id).toBe(1));
      done();
    });
  });

  // Branch coverage: handleCorehrDeviceListCreate – filter by state=true
  it('should filter GET /corehr/devices by state=true', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/devices?state=true');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content.length).toBeGreaterThan(0);
      r.body.content.forEach((d: any) => expect(d.state).toBe(true));
      done();
    });
  });

  // Branch coverage: handleCorehrDeviceListCreate – filter by state=false
  it('should filter GET /corehr/devices by state=false', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/devices?state=false');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content.length).toBeGreaterThan(0);
      r.body.content.forEach((d: any) => expect(d.state).toBe(false));
      done();
    });
  });

  // Branch coverage: handleCorehrBiometriaList – filter by tenant_user_id
  it('should filter GET /corehr/biometria by tenant_user_id', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/biometria?tenant_user_id=1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((b: any) => expect(b.tenant_user_id).toBe(1));
      done();
    });
  });

  // Branch coverage: handleCorehrBiometriaList – filter by device_id
  it('should filter GET /corehr/biometria by device_id', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/biometria?device_id=1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((b: any) => expect(b.device_id).toBe(1));
      done();
    });
  });

  // Branch coverage: handleCorehrBiometriaList – filter by activo=true
  it('should filter GET /corehr/biometria by activo=true', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/biometria?activo=true');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content.length).toBeGreaterThan(0);
      r.body.content.forEach((b: any) => expect(b.activo).toBe(true));
      done();
    });
  });

  // Branch coverage: handleCorehrBiometriaList – filter by activo=false
  it('should filter GET /corehr/biometria by activo=false', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/biometria?activo=false');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content.length).toBeGreaterThan(0);
      r.body.content.forEach((b: any) => expect(b.activo).toBe(false));
      done();
    });
  });

  // Branch coverage: handleCorehrAttendanceList – filter by tenant_user_id
  it('should filter GET /corehr/attendance by tenant_user_id', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/attendance?tenant_user_id=1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((a: any) => expect(a.tenant_user_id).toBe(1));
      done();
    });
  });

  // Branch coverage: handleCorehrAttendanceList – filter by state
  it('should filter GET /corehr/attendance by state=PUNTUAL', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/attendance?state=PUNTUAL');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((a: any) => expect(a.state).toBe('PUNTUAL'));
      done();
    });
  });

  it('should filter GET /corehr/attendance by state=TARDANZA', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/attendance?state=TARDANZA');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((a: any) => expect(a.state).toBe('TARDANZA'));
      done();
    });
  });

  // Branch coverage: handleCorehrAttendanceList – filter by date_from
  it('should filter GET /corehr/attendance by date_from', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/attendance?date_from=2026-07-15');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((a: any) => expect(a.attendance_date >= '2026-07-15').toBe(true));
      done();
    });
  });

  // Branch coverage: handleCorehrAttendanceList – filter by date_to
  it('should filter GET /corehr/attendance by date_to', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/attendance?date_to=2026-07-10');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((a: any) => expect(a.attendance_date <= '2026-07-10').toBe(true));
      done();
    });
  });

  // Branch coverage: handleCorehrAttendanceList – filter by branch_id
  it('should filter GET /corehr/attendance by branch_id', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/attendance?branch_id=1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content).toBeDefined();
      done();
    });
  });

  // Branch coverage: handleCorehrNonWorkingDayListCreate – filter by is_recurring=true
  it('should filter GET /corehr/non-working-days by is_recurring=true', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/non-working-days?is_recurring=true');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content.length).toBeGreaterThan(0);
      r.body.content.forEach((d: any) => expect(d.is_recurring).toBe(true));
      done();
    });
  });

  // Branch coverage: handleCorehrNonWorkingDayListCreate – filter by date_from
  it('should filter GET /corehr/non-working-days by date_from', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/non-working-days?date_from=2026-07-01');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((d: any) => expect(d.date >= '2026-07-01').toBe(true));
      done();
    });
  });

  // Branch coverage: handleCorehrNonWorkingDayListCreate – filter by date_to
  it('should filter GET /corehr/non-working-days by date_to', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/non-working-days?date_to=2026-06-30');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((d: any) => expect(d.date <= '2026-06-30').toBe(true));
      done();
    });
  });

  // Branch coverage: handleCorehrTenantContacts – filter by type
  it('should filter GET /corehr/tenant-contacts by type', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/tenant-contacts?type=RRHH');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((c: any) => expect(c.type).toBe('RRHH'));
      done();
    });
  });

  // Branch coverage: handleCorehrTenantContacts – PATCH not found
  it('should return 404 for PATCH /corehr/tenant-contacts/{id} when not found', (done) => {
    const req = new HttpRequest('PATCH', 'https://api.trazzo.pe/api/v1/corehr/tenant-contacts/999', { value: 'Updated' });
    mockInterceptor(req, next).subscribe({
      error: err => {
        expect(err.status).toBe(404);
        done();
      },
    });
  });

  // Branch coverage: handleCorehrTenantContacts – POST with tenant_user_id
  it('should mock POST /corehr/tenant-contacts with tenant_user_id lookup', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/corehr/tenant-contacts', {
      tenant_user_id: 1,
      type: 'EMAIL',
      value: 'test@example.com',
    });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(201);
      expect(r.body.tenant_user_id).toBe(1);
      expect(r.body.tenant_user).toBeDefined();
      expect(r.body.tenant_user.nombre).toBeTruthy();
      done();
    });
  });

  // Branch coverage: handleCorehrUserDepartments – filter by activa=true
  it('should filter GET /corehr/usuarios/{id}/departamentos by activa=true', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/usuarios/5/departamentos?activa=true');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((d: any) => expect(d.end_date).toBeNull());
      done();
    });
  });

  // Branch coverage: handleCorehrUserDepartments – POST creation
  it('should mock POST /corehr/usuarios/{id}/departamentos with full body', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/corehr/usuarios/1/departamentos', {
      department_id: 3,
      start_date: '2026-08-01',
      is_primary: true,
    });
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(201);
      expect(r.body.department_name).toBe('Ciencias');
      expect(r.body.is_primary).toBe(true);
      expect(r.body.start_date).toBe('2026-08-01');
      done();
    });
  });

  // Branch coverage: handleSaasUsers – filter by search
  it('should filter GET /saas/users by search query param', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/saas/users?search=admin');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(r.body.content.length).toBeGreaterThan(0);
      r.body.content.forEach((u: any) =>
        expect(u.email.toLowerCase()).toContain('admin')
      );
      done();
    });
  });

  // Branch coverage: handleSaasUsers – filter by tenant_id
  it('should filter GET /saas/users by tenant_id', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/saas/users?tenant_id=a1b2c3d4-0000-0000-0000-000000000001');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((u: any) =>
        expect(u.tenant_id).toBe('a1b2c3d4-0000-0000-0000-000000000001')
      );
      done();
    });
  });

  // Branch coverage: handleWebsocket – non-GET method
  it('should pass through non-GET /ws/ requests to next()', (done) => {
    const req = new HttpRequest('POST', 'https://api.trazzo.pe/api/v1/ws/connect', {});
    const nextSpy = jasmine.createSpy('next').and.returnValue(of(new HttpResponse({ status: 200 })));
    mockInterceptor(req, nextSpy).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(nextSpy).toHaveBeenCalled();
      done();
    });
  });

  it('should pass through DELETE /ws/info to next()', (done) => {
    const req = new HttpRequest('DELETE', 'https://api.trazzo.pe/api/v1/ws/info');
    const nextSpy = jasmine.createSpy('next').and.returnValue(of(new HttpResponse({ status: 200 })));
    mockInterceptor(req, nextSpy).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      expect(nextSpy).toHaveBeenCalled();
      done();
    });
  });

  // Branch coverage: combined filters for more thorough branch hits
  it('should handle GET /incidentes with combined state + tipo_id + search filters', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/incidentes?state=PENDIENTE&tipo_id=1&search=maría');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should handle GET /corehr/attendance with combined filters', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/attendance?tenant_user_id=1&state=PUNTUAL&branch_id=1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should handle GET /corehr/biometria with combined filters', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/biometria?tenant_user_id=1&device_id=1&activo=true');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((b: any) => {
        expect(b.tenant_user_id).toBe(1);
        expect(b.device_id).toBe(1);
        expect(b.activo).toBe(true);
      });
      done();
    });
  });

  it('should handle GET /usuarios with combined status + role_id + search', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/usuarios?status=ACTIVO&role_id=5&search=john');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });

  it('should handle GET /corehr/non-working-days with combined is_recurring + date_from + date_to', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/non-working-days?is_recurring=true&date_from=2026-01-01&date_to=2026-12-31');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((d: any) => {
        expect(d.is_recurring).toBe(true);
        expect(d.date >= '2026-01-01').toBe(true);
        expect(d.date <= '2026-12-31').toBe(true);
      });
      done();
    });
  });

  it('should handle GET /corehr/user-schedules with combined tenant_user_id + schedule_id', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/user-schedules?tenant_user_id=1&schedule_id=1');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((us: any) => {
        expect(us.tenant_user_id).toBe(1);
        expect(us.schedule_id).toBe(1);
      });
      done();
    });
  });

  it('should handle GET /corehr/devices with combined branch_id + state', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/corehr/devices?branch_id=3&state=true');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      r.body.content.forEach((d: any) => {
        expect(d.branch_id).toBe(3);
        expect(d.state).toBe(true);
      });
      done();
    });
  });

  it('should handle GET /saas/users with combined search + tenant_id', (done) => {
    const req = new HttpRequest('GET', 'https://api.trazzo.pe/api/v1/saas/users?search=visor&tenant_id=a1b2c3d4-0000-0000-0000-000000000001');
    mockInterceptor(req, next).subscribe(res => {
      const r = res as HttpResponse<any>;
      expect(r.status).toBe(200);
      done();
    });
  });
});
