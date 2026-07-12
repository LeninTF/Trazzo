import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CierresMensuales } from './cierres-mensuales';
import { ReportsService } from '../../../api/services/reports.service';
import { ToastService } from '../../../services/toast.service';
import { ExportService } from '../../../services/export.service';
import { API_BASE_URL } from '../../../api/services/helpers';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import type { MonthlyClosure } from '../../../api/types';

describe('CierresMensuales', () => {
  let component: CierresMensuales;
  let fixture: ComponentFixture<CierresMensuales>;
  let routerSpy: jasmine.SpyObj<Router>;
  let toastSpy: jasmine.SpyObj<ToastService>;
  let exportSpy: jasmine.SpyObj<ExportService>;
  let reportsSpy: jasmine.SpyObj<ReportsService>;

  const mockClosures: MonthlyClosure[] = [
    { id: 'c1', month: 6, year: 2026, totalEmployees: 45, excelReportUrl: '/r/excel/c1.xlsx', pdfReportUrl: '/r/pdf/c1.pdf', createdAt: '2026-06-27T10:00:00' },
    { id: 'c2', month: 5, year: 2026, totalEmployees: 42, excelReportUrl: '/r/excel/c2.xlsx', pdfReportUrl: '/r/pdf/c2.pdf', createdAt: '2026-05-28T09:00:00' },
  ];

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);
    toastSpy = jasmine.createSpyObj<ToastService>('ToastService', ['info', 'error', 'success']);
    exportSpy = jasmine.createSpyObj<ExportService>('ExportService', ['exportCSV']);
    reportsSpy = jasmine.createSpyObj<ReportsService>('ReportsService', ['listClosures', 'createClosure']);

    reportsSpy.listClosures.and.returnValue(of(mockClosures));
    reportsSpy.createClosure.and.returnValue(of(mockClosures[0]));

    await TestBed.configureTestingModule({
      imports: [CierresMensuales],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: ExportService, useValue: exportSpy },
        { provide: ReportsService, useValue: reportsSpy },
        { provide: API_BASE_URL, useValue: 'https://api.test.com/api/v1' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CierresMensuales);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load closures on init', () => {
    expect(reportsSpy.listClosures).toHaveBeenCalled();
    expect(component.closures.length).toBe(2);
    expect(component.loading()).toBeFalse();
  });

  it('should handle error loading closures', () => {
    reportsSpy.listClosures.and.returnValue(throwError(() => new Error('fail')));
    component.cargarCierres();
    expect(component.error()).toBe('Error al cargar los cierres mensuales');
    expect(component.loading()).toBeFalse();
  });

  it('should filter and reload closures', () => {
    component.filtroYear = '2026';
    component.filtroMonth = '6';
    component.filtrar();
    expect(component.paginaActual).toBe(1);
    expect(reportsSpy.listClosures).toHaveBeenCalledWith({ year: 2026, month: 6 });
  });

  it('should clear filters and reload', () => {
    component.filtroYear = '2026';
    component.filtroMonth = '6';
    component.limpiarFiltros();
    expect(component.filtroYear).toBe('');
    expect(component.filtroMonth).toBe('');
    expect(reportsSpy.listClosures).toHaveBeenCalledWith({});
  });

  it('should paginate closures', () => {
    expect(component.closuresPaginado.length).toBe(2);
    expect(component.totalPaginas).toBe(1);
    expect(component.inicioRegistro).toBe(1);
    expect(component.finRegistro).toBe(2);
  });

  it('should not change page if out of range', () => {
    component.cambiarPagina(0);
    expect(component.paginaActual).toBe(1);
    component.cambiarPagina(999);
    expect(component.paginaActual).toBe(1);
  });

  it('should change page within range', () => {
    const manyClosures: MonthlyClosure[] = Array.from({ length: 15 }, (_, i) => ({
      id: `c${i}`, month: (i % 12) + 1, year: 2026, totalEmployees: 10,
      excelReportUrl: null, pdfReportUrl: null, createdAt: '2026-06-01T00:00:00',
    }));
    reportsSpy.listClosures.and.returnValue(of(manyClosures));
    component.cargarCierres();
    expect(component.totalPaginas).toBe(2);
    component.cambiarPagina(2);
    expect(component.paginaActual).toBe(2);
    expect(component.closuresPaginado.length).toBe(5);
  });

  it('should get month name', () => {
    expect(component.getMonthName(1)).toBe('Enero');
    expect(component.getMonthName(6)).toBe('Junio');
    expect(component.getMonthName(12)).toBe('Diciembre');
    expect(component.getMonthName(0)).toBe('');
  });

  it('should open and close create modal', () => {
    component.abrirCrear();
    expect(component.showCreateModal()).toBeTrue();
    expect(component.nuevoMes).toBe(new Date().getMonth() + 1);
    expect(component.nuevoAnio).toBe(new Date().getFullYear());

    component.cerrarCrear();
    expect(component.showCreateModal()).toBeFalse();
  });

  it('should create a closure', () => {
    component.abrirCrear();
    component.nuevoMes = 7;
    component.nuevoAnio = 2026;
    component.crearCierre();
    expect(reportsSpy.createClosure).toHaveBeenCalledWith({ month: 7, year: 2026 });
    expect(toastSpy.info).toHaveBeenCalledWith('Cierre mensual creado exitosamente');
    expect(component.showCreateModal()).toBeFalse();
  });

  it('should handle create closure error', () => {
    reportsSpy.createClosure.and.returnValue(throwError(() => ({ error: { message: 'Ya existe' } })));
    component.abrirCrear();
    component.crearCierre();
    expect(toastSpy.error).toHaveBeenCalledWith('Ya existe');
    expect(component.creando).toBeFalse();
  });

  it('should not create if already creating', () => {
    component.creando = true;
    component.crearCierre();
    expect(reportsSpy.createClosure).not.toHaveBeenCalledTimes(2);
  });

  it('should navigate to detail', () => {
    component.verDetalle('c1');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/tenant/cierres-mensuales', 'c1']);
  });

  it('should export CSV', () => {
    component.exportarCSV();
    expect(exportSpy.exportCSV).toHaveBeenCalled();
    expect(toastSpy.info).toHaveBeenCalledWith('Exportando CSV...');
  });

  it('should return month options', () => {
    expect(component.monthOptions).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]);
  });

  it('should compute inicioRegistro as 0 when no closures', () => {
    reportsSpy.listClosures.and.returnValue(of([]));
    component.cargarCierres();
    expect(component.inicioRegistro).toBe(0);
    expect(component.finRegistro).toBe(0);
  });
});
