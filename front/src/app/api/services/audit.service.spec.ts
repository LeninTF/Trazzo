import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuditService } from './audit.service';
import { API_BASE_URL } from './helpers';

describe('AuditService', () => {
  let service: AuditService;
  let httpMock: HttpTestingController;
  let apiBase: string;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuditService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'https://api.trazzo.pe/api/v1' },
      ],
    });
    service = TestBed.inject(AuditService);
    httpMock = TestBed.inject(HttpTestingController);
    apiBase = TestBed.inject(API_BASE_URL);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should list logs with filters', () => {
    service.listLogs({ searchTerm: 'login', page: 0, size: 5 }).subscribe();
    const req = httpMock.expectOne(r => r.url === `${apiBase}/audit/logs`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('searchTerm')).toBe('login');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('5');
    req.flush({ content: [], page: 0, size: 5, totalElements: 0, totalPages: 0 });
  });

  it('should list logs without filters', () => {
    service.listLogs().subscribe();
    const req = httpMock.expectOne(r => r.url === `${apiBase}/audit/logs`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('should get a single log', () => {
    service.getLog('evt-1').subscribe();
    const req = httpMock.expectOne(`${apiBase}/audit/logs/evt-1`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 'evt-1' });
  });

  it('should get metrics', () => {
    service.getMetrics().subscribe();
    const req = httpMock.expectOne(`${apiBase}/audit/metrics`);
    expect(req.request.method).toBe('GET');
    req.flush({ total_eventos: 0, errores: 0, sesiones_activas: 0, crecimiento: 0, porcentaje_sesiones: 0 });
  });
});
