import { HttpRequest, HttpResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { mockInterceptor } from './mock.interceptor';

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
