import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { DetalleCierre } from './detalle-cierre';
import { ReportsService } from '../../../../api/services/reports.service';
import { API_BASE_URL } from '../../../../api/services/helpers';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import type { MonthlyClosureWithDetails } from '../../../../api/types';

describe('DetalleCierre', () => {
  let component: DetalleCierre;
  let fixture: ComponentFixture<DetalleCierre>;
  let routerSpy: jasmine.SpyObj<Router>;
  let reportsSpy: jasmine.SpyObj<ReportsService>;
  let paramMap: Map<string, string>;

  const mockReport: MonthlyClosureWithDetails = {
    id: 'c1',
    month: 6,
    year: 2026,
    totalEmployees: 2,
    excelReportUrl: '/r/excel/c1.xlsx',
    pdfReportUrl: '/r/pdf/c1.pdf',
    createdAt: '2026-06-27T10:00:00',
    details: [
      {
        id: 'd1', monthClosureId: 'c1', tenantUserId: 1, tenantUserFullName: 'Juan Pérez',
        tenantUserDocument: '12345', departmentName: 'IT', roleName: 'Admin',
        totalWorkedHours: 160, totalTardinessMinutes: 15, totalAbsences: 1, totalOvertimeHours: 8,
        createdAt: '2026-06-27T10:00:00',
      },
      {
        id: 'd2', monthClosureId: 'c1', tenantUserId: 2, tenantUserFullName: 'Ana López',
        tenantUserDocument: '67890', departmentName: null, roleName: null,
        totalWorkedHours: 140, totalTardinessMinutes: 0, totalAbsences: 0, totalOvertimeHours: 0,
        createdAt: '2026-06-27T10:00:00',
      },
    ],
  };

  function setup(idParam: string | null) {
    paramMap = new Map();
    if (idParam !== null) paramMap.set('id', idParam);

    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);
    reportsSpy = jasmine.createSpyObj<ReportsService>('ReportsService', ['getFullReport']);
    reportsSpy.getFullReport.and.returnValue(of(mockReport));

    TestBed.configureTestingModule({
      imports: [DetalleCierre],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy },
        { provide: ReportsService, useValue: reportsSpy },
        { provide: API_BASE_URL, useValue: 'https://api.test.com/api/v1' },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap } },
        },
      ],
    });

    fixture = TestBed.createComponent(DetalleCierre);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  describe('with valid id', () => {
    beforeEach(() => {
      setup('c1');
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should load report on init', () => {
      expect(reportsSpy.getFullReport).toHaveBeenCalledWith('c1');
      expect(component.report).toEqual(mockReport);
      expect(component.loading()).toBeFalse();
    });

    it('should compute total hours worked', () => {
      expect(component.totalHorasTrabajadas).toBe(300);
    });

    it('should compute total tardiness', () => {
      expect(component.totalTardanzas).toBe(15);
    });

    it('should compute total absences', () => {
      expect(component.totalAusencias).toBe(1);
    });

    it('should compute total overtime', () => {
      expect(component.totalHorasExtras).toBe(8);
    });

    it('should get month name', () => {
      expect(component.getMonthName(6)).toBe('Junio');
      expect(component.getMonthName(1)).toBe('Enero');
    });

    it('should navigate back', () => {
      component.volver();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/tenant/cierres-mensuales']);
    });

    it('should handle error loading report', () => {
      reportsSpy.getFullReport.and.returnValue(throwError(() => new Error('fail')));
      component.cargarReporte('c1');
      expect(component.error()).toBe('Error al cargar el reporte');
      expect(component.loading()).toBeFalse();
    });
  });

  describe('with no id', () => {
    beforeEach(() => {
      setup(null);
    });

    it('should set error when no id provided', () => {
      expect(component.error()).toBe('ID de cierre no proporcionado');
      expect(component.loading()).toBeFalse();
      expect(reportsSpy.getFullReport).not.toHaveBeenCalled();
    });
  });

  describe('with null report', () => {
    beforeEach(() => {
      setup('c1');
      component.report = null;
    });

    it('should return 0 for computed totals when report is null', () => {
      expect(component.totalHorasTrabajadas).toBe(0);
      expect(component.totalTardanzas).toBe(0);
      expect(component.totalAusencias).toBe(0);
      expect(component.totalHorasExtras).toBe(0);
    });
  });
});
