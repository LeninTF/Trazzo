import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { OrgService } from './org.service';
import { API_BASE_URL } from './helpers';

describe('OrgService', () => {
  let service: OrgService;
  let httpMock: HttpTestingController;
  let apiBase: string;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        OrgService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'https://api.trazzo.pe/api/v1' },
      ],
    });
    service = TestBed.inject(OrgService);
    httpMock = TestBed.inject(HttpTestingController);
    apiBase = TestBed.inject(API_BASE_URL);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('branches', () => {
    it('should list branches', () => {
      service.listBranches({ search: 'sede' }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/org/branches`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('search')).toBe('sede');
      req.flush({ content: [], page: 0, size: 20, total: 0, totalPages: 0 });
    });

    it('should create branch', () => {
      const body = { name: 'Sede Norte', description: 'Principal' };
      service.createBranch(body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/branches`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 1 });
    });

    it('should update branch', () => {
      service.updateBranch(1, { name: 'Sede Editada' }).subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/branches/1`);
      expect(req.request.method).toBe('PUT');
      req.flush({ id: 1 });
    });

    it('should delete branch', () => {
      service.deleteBranch(1).subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/branches/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('areas', () => {
    it('should list areas', () => {
      service.listAreas({ branchId: 1 }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/org/areas`);
      expect(req.request.params.get('branchId')).toBe('1');
      req.flush({ content: [], page: 0, size: 20, total: 0, totalPages: 0 });
    });

    it('should create area', () => {
      const body = { branchId: 1, name: 'Area Nueva' };
      service.createArea(body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/areas`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 1 });
    });

    it('should update area', () => {
      service.updateArea(1, { name: 'Area Editada' }).subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/areas/1`);
      expect(req.request.method).toBe('PUT');
      req.flush({ id: 1 });
    });

    it('should delete area', () => {
      service.deleteArea(1).subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/areas/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('departments', () => {
    it('should list departments', () => {
      service.listDepartments({ areaId: 1 }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/org/departments`);
      expect(req.request.params.get('areaId')).toBe('1');
      req.flush({ content: [], page: 0, size: 20, total: 0, totalPages: 0 });
    });

    it('should create department', () => {
      const body = { areaId: 1, name: 'Depto Nuevo' };
      service.createDepartment(body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/departments`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 1 });
    });

    it('should update department', () => {
      service.updateDepartment(1, { name: 'Depto Editado' }).subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/departments/1`);
      expect(req.request.method).toBe('PUT');
      req.flush({ id: 1 });
    });

    it('should delete department', () => {
      service.deleteDepartment(1).subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/departments/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('roles', () => {
    it('should list roles', () => {
      service.listRoles({ search: 'admin' }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/org/roles`);
      expect(req.request.params.get('search')).toBe('admin');
      req.flush({ content: [], page: 0, size: 20, total: 0, totalPages: 0 });
    });

    it('should create role', () => {
      const body = { name: 'Supervisor' };
      service.createRole(body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/roles`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 'r1' });
    });

    it('should update role', () => {
      service.updateRole('r1', { name: 'Supervisor Senior' }).subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/roles/r1`);
      expect(req.request.method).toBe('PUT');
      req.flush({ id: 'r1' });
    });

    it('should delete role', () => {
      service.deleteRole('r1').subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/roles/r1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('should list role permissions', () => {
      service.listRolePermissions('r1').subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/roles/r1/permissions`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });

    it('should assign permission to role', () => {
      service.assignPermissionToRole('r1', 'p1').subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/roles/r1/permissions`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ permissionId: 'p1' });
      req.flush({ roleId: 'r1', permissionId: 'p1', createdAt: '2026-01-01' });
    });

    it('should remove permission from role', () => {
      service.removePermissionFromRole('r1', 'p1').subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/roles/r1/permissions/p1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('permissions', () => {
    it('should list permissions', () => {
      service.listPermissions({ search: 'read' }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/org/permissions`);
      expect(req.request.params.get('search')).toBe('read');
      req.flush({ content: [], page: 0, size: 20, total: 0, totalPages: 0 });
    });

    it('should create permission', () => {
      const body = { name: 'read:users' };
      service.createPermission(body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/permissions`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 'p1' });
    });

    it('should update permission', () => {
      service.updatePermission('p1', { name: 'write:users' }).subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/permissions/p1`);
      expect(req.request.method).toBe('PUT');
      req.flush({ id: 'p1' });
    });

    it('should delete permission', () => {
      service.deletePermission('p1').subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/permissions/p1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('user roles', () => {
    it('should list user roles', () => {
      service.listUserRoles(5).subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/users/5/roles`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });

    it('should assign role to user', () => {
      const body = { roleId: 'r1', departmentId: 2 };
      service.assignRoleToUser(5, body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/users/5/roles`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 1 });
    });

    it('should remove user role', () => {
      service.removeUserRole(5, 1).subscribe();
      const req = httpMock.expectOne(`${apiBase}/org/users/5/roles/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
