import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ReportsService } from './reports.service';
import { API_BASE_URL } from './helpers';

describe('ReportsService', () => {
  let service: ReportsService;
  let httpMock: HttpTestingController;
  let apiBase: string;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ReportsService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'https://api.trazzo.pe/api/v1' },
      ],
    });
    service = TestBed.inject(ReportsService);
    httpMock = TestBed.inject(HttpTestingController);
    apiBase = TestBed.inject(API_BASE_URL);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should list closures without filters', () => {
    service.listClosures().subscribe();
    const req = httpMock.expectOne(r => r.url === `${apiBase}/reports/monthly-closures`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should list closures with year and month filters', () => {
    service.listClosures({ year: 2026, month: 6 }).subscribe();
    const req = httpMock.expectOne(r => r.url === `${apiBase}/reports/monthly-closures`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('year')).toBe('2026');
    expect(req.request.params.get('month')).toBe('6');
    req.flush([]);
  });

  it('should get a single closure by id', () => {
    service.getClosure('abc-123').subscribe(result => {
      expect(result.id).toBe('abc-123');
    });
    const req = httpMock.expectOne(`${apiBase}/reports/monthly-closures/abc-123`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 'abc-123', month: 6, year: 2026, totalEmployees: 45, excelReportUrl: null, pdfReportUrl: null, createdAt: '2026-06-27T10:00:00' });
  });

  it('should create a closure', () => {
    service.createClosure({ month: 7, year: 2026 }).subscribe(result => {
      expect(result.month).toBe(7);
    });
    const req = httpMock.expectOne(`${apiBase}/reports/monthly-closures`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ month: 7, year: 2026 });
    req.flush({ id: 'new-id', month: 7, year: 2026, totalEmployees: 45, excelReportUrl: null, pdfReportUrl: null, createdAt: '2026-07-01T00:00:00' });
  });

  it('should get closure detail by id', () => {
    service.getClosureDetail('detail-1').subscribe(result => {
      expect(result.id).toBe('detail-1');
    });
    const req = httpMock.expectOne(`${apiBase}/reports/monthly-closure-details/detail-1`);
    expect(req.request.method).toBe('GET');
    req.flush({
      id: 'detail-1', monthClosureId: 'abc-123', tenantUserId: 1,
      tenantUserFullName: 'Test User', tenantUserDocument: '12345',
      departmentName: 'IT', roleName: 'Admin', totalWorkedHours: 160,
      totalTardinessMinutes: 0, totalAbsences: 0, totalOvertimeHours: 0,
      createdAt: '2026-06-27T10:00:00',
    });
  });

  it('should get full report by id', () => {
    service.getFullReport('abc-123').subscribe(result => {
      expect(result.id).toBe('abc-123');
      expect(result.details.length).toBe(1);
    });
    const req = httpMock.expectOne(`${apiBase}/reports/monthly-reports/abc-123`);
    expect(req.request.method).toBe('GET');
    req.flush({
      id: 'abc-123', month: 6, year: 2026, totalEmployees: 45,
      excelReportUrl: null, pdfReportUrl: null, createdAt: '2026-06-27T10:00:00',
      details: [{
        id: 'd1', monthClosureId: 'abc-123', tenantUserId: 1,
        tenantUserFullName: 'Test', tenantUserDocument: '12345',
        departmentName: 'IT', roleName: 'Admin', totalWorkedHours: 160,
        totalTardinessMinutes: 0, totalAbsences: 0, totalOvertimeHours: 0,
        createdAt: '2026-06-27T10:00:00',
      }],
    });
  });
});
