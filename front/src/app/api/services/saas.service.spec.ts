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

  it('should list public plans', () => {
    service.listPublicPlans().subscribe();
    const req = httpMock.expectOne(`${apiBase}/public/plans`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should checkout', () => {
    const body = { planId: 1, subdomain: 'acme', email: 'a@b.com' } as any;
    service.checkout(body).subscribe();
    const req = httpMock.expectOne(`${apiBase}/shop/checkout`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ id: 't-1' });
  });

  it('should list invoices with params', () => {
    service.listInvoices({ paymentStatus: 'PAID', tenantId: 't-1', dateFrom: '2024-01-01', dateTo: '2024-12-31', page: 0, size: 10 }).subscribe();
    const req = httpMock.expectOne(r => r.url === `${apiBase}/saas/invoices`);
    expect(req.request.params.get('paymentStatus')).toBe('PAID');
    expect(req.request.params.get('tenantId')).toBe('t-1');
    req.flush({ content: [], totalElements: 0 });
  });

  it('should list invoices with no params', () => {
    service.listInvoices().subscribe();
    const req = httpMock.expectOne(r => r.url === `${apiBase}/saas/invoices`);
    req.flush({ content: [], totalElements: 0 });
  });

  it('should get invoice by id', () => {
    service.getInvoice('inv-1').subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/invoices/inv-1`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 'inv-1' });
  });

  it('should export invoices excel', () => {
    service.exportInvoicesExcel({ paymentStatus: 'PAID' }).subscribe();
    const req = httpMock.expectOne(r => r.url === `${apiBase}/saas/invoices/export/excel`);
    expect(req.request.params.get('paymentStatus')).toBe('PAID');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob());
  });

  it('should export invoices pdf', () => {
    service.exportInvoicesPdf({ tenantId: 't-1' }).subscribe();
    const req = httpMock.expectOne(r => r.url === `${apiBase}/saas/invoices/export/pdf`);
    expect(req.request.params.get('tenantId')).toBe('t-1');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob());
  });

  it('should list subscriptions', () => {
    service.listSubscriptions({ page: 0, size: 20 }).subscribe();
    const req = httpMock.expectOne(r => r.url === `${apiBase}/saas/subscriptions`);
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    req.flush({ content: [], totalElements: 0 });
  });

  it('should list subscriptions with no params', () => {
    service.listSubscriptions().subscribe();
    const req = httpMock.expectOne(r => r.url === `${apiBase}/saas/subscriptions`);
    req.flush({ content: [], totalElements: 0 });
  });

  it('should list holdings', () => {
    service.listHoldings().subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/holdings`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
