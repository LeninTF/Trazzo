import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { LogAuditoria } from './log-auditoria';
import { AuditService } from '../../../api/services/audit.service';
import { ToastService } from '../../../services/toast.service';
import { ExportService } from '../../../services/export.service';

describe('LogAuditoria', () => {
  let component: LogAuditoria;
  let fixture: ComponentFixture<LogAuditoria>;

  const mockLogs = {
    content: [
      {
        id: '1', eventId: 'evt-1', fecha: '2026-06-06T10:00:00', tenant: 'Colegio Trazzo', tenantId: 't-1',
        userName: 'Ana Torres', userEmail: 'ana@trazzo.pe', accion: 'Login', tipo: 'success',
        entidad: 'users', entidadId: '1', ipAddress: '127.0.0.1', userAgent: 'Chrome',
        oldValue: null, newValue: null,
      },
      {
        id: '2', eventId: 'evt-2', fecha: '2026-06-07T10:00:00', tenant: 'Colegio Trazzo', tenantId: 't-1',
        userName: 'Luis Paredes', userEmail: 'luis@trazzo.pe', accion: 'Update', tipo: 'warning',
        entidad: 'plans', entidadId: '2', ipAddress: '127.0.0.1', userAgent: 'Firefox',
        oldValue: null, newValue: null,
      },
    ],
    page: 0, size: 5, totalElements: 2, totalPages: 1,
  };

  const mockMetrics = {
    total_eventos: 100, errores: 3, sesiones_activas: 12, crecimiento: 5, porcentaje_sesiones: 40,
  };

  const mockAudit = {
    listLogs: jasmine.createSpy('listLogs').and.returnValue(of(mockLogs)),
    getLog: jasmine.createSpy('getLog').and.returnValue(of(mockLogs.content[0])),
    getMetrics: jasmine.createSpy('getMetrics').and.returnValue(of(mockMetrics)),
  };

  const mockToast = jasmine.createSpyObj('ToastService', ['info']);
  const mockExport = jasmine.createSpyObj('ExportService', ['exportCSV']);

  beforeEach(async () => {
    mockAudit.listLogs.calls.reset();
    mockAudit.getMetrics.calls.reset();
    mockAudit.listLogs.and.returnValue(of(mockLogs));
    mockAudit.getMetrics.and.returnValue(of(mockMetrics));

    await TestBed.configureTestingModule({
      imports: [LogAuditoria],
      providers: [
        { provide: AuditService, useValue: mockAudit },
        { provide: ToastService, useValue: mockToast },
        { provide: ExportService, useValue: mockExport },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LogAuditoria);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load logs on init', () => {
    expect(mockAudit.listLogs).toHaveBeenCalled();
    expect(component.logs.length).toBe(2);
  });

  it('should load metricas on init', () => {
    expect(mockAudit.getMetrics).toHaveBeenCalled();
    expect(component.metricas.totalEventos).toBe(100);
    expect(component.metricas.errores).toBe(3);
  });

  it('should map tipo from backend to UI value', () => {
    expect(component.logs[0].tipo).toBe('exito');
    expect(component.logs[1].tipo).toBe('advertencia');
  });

  it('should compute logsFiltrado', () => {
    expect(component.logsFiltrado.length).toBe(2);
  });

  it('should compute logsPaginado', () => {
    expect(component.logsPaginado.length).toBe(2);
  });

  it('should compute totalPaginas from server', () => {
    expect(component.totalPaginas).toBe(1);
  });

  it('should compute inicioRegistro', () => {
    expect(component.inicioRegistro).toBe(1);
  });

  it('should compute inicioRegistro as 0 when empty', () => {
    component.totalElementos = 0;
    expect(component.inicioRegistro).toBe(0);
  });

  it('should compute finRegistro', () => {
    expect(component.finRegistro).toBe(2);
  });

  it('should filtrarLogs reset page, selection and refetch', () => {
    component.paginaActual = 3;
    component.logSeleccionado = component.logs[0];
    mockAudit.listLogs.calls.reset();
    component.filtrarLogs();
    expect(component.paginaActual).toBe(1);
    expect(component.logSeleccionado).toBeNull();
    expect(mockAudit.listLogs).toHaveBeenCalled();
  });

  it('should aplicarFiltros', () => {
    component.aplicarFiltros();
    expect(component.paginaActual).toBe(1);
    expect(mockToast.info).toHaveBeenCalled();
  });

  it('should limpiarFiltros', () => {
    component.searchTerm = 'test';
    component.filtroFechaDesde = '2026-01-01';
    component.filtroFechaHasta = '2026-01-31';
    component.limpiarFiltros();
    expect(component.searchTerm).toBe('');
    expect(component.filtroFechaDesde).toBe('');
    expect(component.filtroFechaHasta).toBe('');
    expect(component.paginaActual).toBe(1);
  });

  it('should cambiarPagina within range and refetch', () => {
    component.totalPaginasServidor = 3;
    mockAudit.listLogs.calls.reset();
    component.cambiarPagina(2);
    expect(component.paginaActual).toBe(2);
    expect(mockAudit.listLogs).toHaveBeenCalled();
  });

  it('should not cambiarPagina out of range', () => {
    component.cambiarPagina(0);
    expect(component.paginaActual).toBe(1);
    component.cambiarPagina(999);
    expect(component.paginaActual).toBe(1);
  });

  it('should seleccionarLog toggle', () => {
    component.seleccionarLog(component.logs[0]);
    expect(component.logSeleccionado?.id).toBe('1');
    component.seleccionarLog(component.logs[0]);
    expect(component.logSeleccionado).toBeNull();
  });

  it('should cerrarDetalle', () => {
    component.seleccionarLog(component.logs[0]);
    component.cerrarDetalle();
    expect(component.logSeleccionado).toBeNull();
  });

  it('should exportarCSV', () => {
    component.exportarCSV();
    expect(mockExport.exportCSV).toHaveBeenCalled();
  });

  it('should handle cargarLogs error', () => {
    mockAudit.listLogs.and.returnValue(throwError(() => new Error('fail')));
    component.cargarLogs();
    expect(component.error()).toBe('Error al cargar los logs');
  });

  it('should not fail when cargarMetricas errors', () => {
    mockAudit.getMetrics.and.returnValue(throwError(() => new Error('fail')));
    expect(() => component.cargarMetricas()).not.toThrow();
  });
});
