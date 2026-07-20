import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { RolesService } from './roles.service';
import { API_BASE_URL } from './helpers';

describe('RolesService', () => {
  let service: RolesService;
  let httpMock: HttpTestingController;
  let apiBase: string;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        RolesService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'https://api.trazzo.pe/api/v1' },
      ],
    });
    service = TestBed.inject(RolesService);
    httpMock = TestBed.inject(HttpTestingController);
    apiBase = TestBed.inject(API_BASE_URL);
  });

  afterEach(() => httpMock.verify());

  it('should list roles', () => {
    service.list().subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/roles`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should get role by id', () => {
    service.getById(3).subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/roles/3`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 3 });
  });

  it('should create role', () => {
    const body = { name: 'Admin', permissions: ['read'] } as any;
    service.create(body).subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/roles`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ id: 1 });
  });

  it('should update role', () => {
    const body = { name: 'Updated' } as any;
    service.update(5, body).subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/roles/5`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(body);
    req.flush({ id: 5 });
  });

  it('should delete role', () => {
    service.delete(7).subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/roles/7`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should update permissions', () => {
    const body = { permissions: ['read', 'write'] } as any;
    service.updatePermissions(2, body).subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/roles/2/permissions`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(body);
    req.flush({ id: 2 });
  });
});
