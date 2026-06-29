import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { UsersService } from './users.service';
import { API } from './helpers';
import type { TenantUserProfile, MasterUserProfile } from '../types';

describe('UsersService', () => {
  let service: UsersService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [UsersService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UsersService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  const mockTenantUser: TenantUserProfile = {
    id: 1, email: 'user@test.com', phone: '999888777',
    estado: 'ACTIVO', must_change_password: false,
    created_at: '2024-01-01T00:00:00Z', updated_at: '2024-01-01T00:00:00Z',
    persona: { id: 1, name: 'Test', father_surname: 'User', mother_surname: '', document_type: 'DNI', document_value: '12345678', birth_date: null, img_url: null },
    MetodoRecuperacion: [], rol: { id: 1, name: 'Admin', descripcion: null, permissions: [] }, sedes: [], areas: [], departamentos: [],
  };

  describe('tenant users', () => {
    it('should list users with params', () => {
      service.list({ page: 1, size: 10, search: 'test' }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${API}/usuarios`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('1');
      expect(req.request.params.get('search')).toBe('test');
      req.flush({ data: [], meta: { total: 0 } });
    });

    it('should get user by id', () => {
      service.get(1).subscribe(user => {
        expect(user.id).toBe(1);
      });
      const req = httpMock.expectOne(`${API}/usuarios/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockTenantUser);
    });

    it('should create user', () => {
      const body = { email: 'new@test.com', password: '123456', persona: { name: 'New' } } as any;
      service.create(body).subscribe(user => {
        expect(user.email).toBe('new@test.com');
      });
      const req = httpMock.expectOne(`${API}/usuarios`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ ...mockTenantUser, email: 'new@test.com' });
    });

    it('should update user', () => {
      const body = { email: 'updated@test.com', persona: { name: 'Updated' } } as any;
      service.update(1, body).subscribe();
      const req = httpMock.expectOne(`${API}/usuarios/1`);
      expect(req.request.method).toBe('PUT');
      req.flush(mockTenantUser);
    });

    it('should patch user', () => {
      const body = { phone: '111222333' } as any;
      service.patch(1, body).subscribe();
      const req = httpMock.expectOne(`${API}/usuarios/1`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual(body);
      req.flush(mockTenantUser);
    });

    it('should delete user', () => {
      service.delete(1).subscribe();
      const req = httpMock.expectOne(`${API}/usuarios/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush({ message: 'Deleted' });
    });

    it('should assign role', () => {
      const body = { role_id: 2 } as any;
      service.assignRole(1, body).subscribe();
      const req = httpMock.expectOne(`${API}/usuarios/1/rol`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(body);
      req.flush(mockTenantUser);
    });

    it('should change password', () => {
      const body = { current_password: 'old', new_password: 'new' } as any;
      service.changePassword(1, body).subscribe();
      const req = httpMock.expectOne(`${API}/usuarios/1/password`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual(body);
      req.flush(null);
    });

    it('should getMe', () => {
      service.getMe().subscribe(user => {
        expect(user.id).toBe(1);
      });
      const req = httpMock.expectOne(`${API}/usuarios/me`);
      expect(req.request.method).toBe('GET');
      req.flush(mockTenantUser);
    });

    it('should patchMe', () => {
      service.patchMe({ phone: '999000111' }).subscribe();
      const req = httpMock.expectOne(`${API}/usuarios/me`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ phone: '999000111' });
      req.flush(mockTenantUser);
    });
  });

  describe('master users', () => {
    const mockMasterUser: MasterUserProfile = {
      id: 1, email: 'master@trazzo.com', phone: '999888777',
      tenant_id: null, must_change_password: false,
      created_at: '2024-01-01T00:00:00Z',
      persona: { id: 1, name: 'Master', father_surname: 'User', mother_surname: '', document_type: 'DNI', document_value: '87654321', birth_date: null, img_url: null },
      MetodoRecuperacion: [], roles: [{ id: 1, name: 'Super Admin', descripcion: null }], tenant_info: null,
    };

    it('should list masters', () => {
      service.listMasters({ page: 1, size: 20 }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${API}/saas/users`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('1');
      req.flush({ data: [], meta: { total: 0 } });
    });

    it('should get master by id', () => {
      service.getMaster(1).subscribe(user => {
        expect(user.id).toBe(1);
      });
      const req = httpMock.expectOne(`${API}/saas/users/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockMasterUser);
    });

    it('should getMasterMe', () => {
      service.getMasterMe().subscribe(user => {
        expect(user.id).toBe(1);
      });
      const req = httpMock.expectOne(`${API}/saas/users/me`);
      expect(req.request.method).toBe('GET');
      req.flush(mockMasterUser);
    });

    it('should patchMasterMe', () => {
      service.patchMasterMe({ img_url: 'https://example.com/avatar.jpg' }).subscribe();
      const req = httpMock.expectOne(`${API}/saas/users/me`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ img_url: 'https://example.com/avatar.jpg' });
      req.flush(mockMasterUser);
    });
  });
});
