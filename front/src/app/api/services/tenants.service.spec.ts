import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TenantsService } from './tenants.service';
import { API_BASE_URL } from './helpers';

describe('TenantsService', () => {
  let service: TenantsService;
  let httpMock: HttpTestingController;
  let apiBase: string;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TenantsService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'https://api.trazzo.pe/api/v1' },
      ],
    });
    service = TestBed.inject(TenantsService);
    httpMock = TestBed.inject(HttpTestingController);
    apiBase = TestBed.inject(API_BASE_URL);
  });

  afterEach(() => httpMock.verify());

  it('should create trial tenant', () => {
    const body = { subdomain: 'acme' } as any;
    service.createTrial(body).subscribe();
    const req = httpMock.expectOne(`${apiBase}/tenants/trial`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ id: 't-1' });
  });

  it('should list tenants with no params', () => {
    service.list().subscribe();
    const req = httpMock.expectOne(r => r.url === `${apiBase}/saas/tenants`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0 });
  });

  it('should list tenants with all params', () => {
    service.list({ search: 'acme', planId: 3, status: 'ACTIVE', page: 1, size: 20 }).subscribe();
    const req = httpMock.expectOne(r => r.url === `${apiBase}/saas/tenants`);
    expect(req.request.params.get('search')).toBe('acme');
    expect(req.request.params.get('planId')).toBe('3');
    expect(req.request.params.get('status')).toBe('ACTIVE');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('20');
    req.flush({ content: [], totalElements: 0 });
  });

  it('should get tenant by id', () => {
    service.getById('t-1').subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/tenants/t-1`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 't-1' });
  });

  it('should get tenant metrics', () => {
    service.getMetrics().subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/tenants/metrics`);
    expect(req.request.method).toBe('GET');
    req.flush({ totalTenants: 10 });
  });

  it('should suspend tenant', () => {
    service.suspend('t-1').subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/tenants/t-1/suspend`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({});
    req.flush({ id: 't-1' });
  });

  it('should reactivate tenant', () => {
    service.reactivate('t-1').subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/tenants/t-1/reactivate`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({});
    req.flush({ id: 't-1' });
  });

  it('should update branding', () => {
    const body = { logoUrl: 'https://example.com/logo.png' } as any;
    service.updateBranding('t-1', body).subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/tenants/t-1/branding`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(body);
    req.flush({ id: 't-1' });
  });

  it('should delete tenant', () => {
    service.deleteById('t-1').subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/tenants/t-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
