import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { SaasService } from './saas.service';
import { API_BASE_URL } from './helpers';

describe('SaasService', () => {
  let service: SaasService;
  let httpMock: HttpTestingController;
  let apiBase: string;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SaasService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'https://api.trazzo.pe/api/v1' },
      ],
    });
    service = TestBed.inject(SaasService);
    httpMock = TestBed.inject(HttpTestingController);
    apiBase = TestBed.inject(API_BASE_URL);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should list plans', () => {
    service.listPlans().subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/plans`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should list active plans', () => {
    service.listActivePlans().subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/plans/active`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should get plan', () => {
    service.getPlan(1).subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/plans/1`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 1 });
  });

  it('should create plan', () => {
    const body = { name: 'Plan Pro', price: 100, priceAnnual: 1000, currency: 'SOLES', billingPeriod: 'MONTHLY', features: {} };
    service.createPlan(body).subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/plans`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ id: 1 });
  });

  it('should update plan', () => {
    const body = { id: 1, name: 'Plan Pro+', price: 120, priceAnnual: 1200, currency: 'SOLES', billingPeriod: 'MONTHLY', features: {} };
    service.updatePlan(1, body).subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/plans/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(body);
    req.flush({ id: 1 });
  });

  it('should activate plan', () => {
    service.activatePlan(1).subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/plans/1/activate`);
    expect(req.request.method).toBe('PUT');
    req.flush({ id: 1 });
  });

  it('should deactivate plan', () => {
    service.deactivatePlan(1).subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/plans/1/deactivate`);
    expect(req.request.method).toBe('PUT');
    req.flush({ id: 1 });
  });

  it('should delete plan', () => {
    service.deletePlan(1).subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/plans/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
