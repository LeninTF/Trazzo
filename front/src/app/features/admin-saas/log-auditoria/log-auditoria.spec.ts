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

  const emptyLogs = {
    content: [],
    page: 0, size: 5, totalElements: 0, totalPages: 0,
  };

  const errorTipoLogs = {
    content: [
      {
        id: '3', eventId: 'evt-3', fecha: '2026-06-08T10:00:00', tenant: 'Tenant A', tenantId: 't-2',
        userName: '', userEmail: 'test@test.com', accion: 'Delete', tipo: 'error',
        entidad: 'roles', entidadId: '3', ipAddress: '10.0.0.1', userAgent: 'Safari',
        oldValue: { name: 'old' }, newValue: { name: 'new' },
      },
    ],
    page: 0, size: 5, totalElements: 1, totalPages: 1,
  };

  const unknownTipoLogs = {
    content: [
      {
        id: '4', eventId: 'evt-4', fecha: '2026-06-09T10:00:00', tenant: 'Tenant B', tenantId: 't-3',
        userName: 'Carlos Perez', userEmail: 'carlos@test.com', accion: 'Create', tipo: 'unknown_type',
        entidad: 'users', entidadId: '4', ipAddress: '10.0.0.2', userAgent: 'Edge',
        oldValue: null, newValue: null,
      },
    ],
    page: 0, size: 5, totalElements: 1, totalPages: 1,
  };

  const nullFieldsLogs = {
    content: [
      {
        id: '7', eventId: null, fecha: '2026-06-12T10:00:00', tenant: null, tenantId: null,
        userName: null, userEmail: null, accion: null, tipo: null,
        entidad: null, entidadId: null, ipAddress: null, userAgent: null,
        oldValue: null, newValue: null,
      },
    ],
    page: 0, size: 5, totalElements: 1, totalPages: 1,
  };

  const doubleSpaceNameLogs = {
    content: [
      {
        id: '8', eventId: 'evt-8', fecha: '2026-06-13T10:00:00', tenant: 'Tenant E', tenantId: 't-6',
        userName: 'a  b', userEmail: 'ab@test.com', accion: 'Login', tipo: 'exito',
        entidad: 'users', entidadId: '8', ipAddress: '10.0.0.5', userAgent: 'Chrome',
        oldValue: null, newValue: null,
      },
    ],
    page: 0, size: 5, totalElements: 1, totalPages: 1,
  };

  const singleWordNameLogs = {
    content: [
      {
        id: '5', eventId: 'evt-5', fecha: '2026-06-10T10:00:00', tenant: 'Tenant C', tenantId: 't-4',
        userName: 'Maria', userEmail: 'maria@test.com', accion: 'Read', tipo: 'success',
        entidad: 'plans', entidadId: '5', ipAddress: '10.0.0.3', userAgent: 'Chrome',
        oldValue: null, newValue: null,
      },
    ],
    page: 0, size: 5, totalElements: 1, totalPages: 1,
  };

  const multiPageLogs = {
    content: [
      {
        id: '6', eventId: 'evt-6', fecha: '2026-06-11T10:00:00', tenant: 'Tenant D', tenantId: 't-5',
        userName: 'Pedro Garcia Lopez', userEmail: 'pedro@test.com', accion: 'Login', tipo: 'success',
        entidad: 'sessions', entidadId: '6', ipAddress: '10.0.0.4', userAgent: 'Chrome',
        oldValue: null, newValue: null,
      },
    ],
    page: 2, size: 5, totalElements: 30, totalPages: 6,
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
    expect(component.metricas.sesionesActivas).toBe(12);
    expect(component.metricas.crecimiento).toBe(5);
    expect(component.metricas.porcentajeSesiones).toBe(40);
  });

  it('should map tipo success to exito', () => {
    expect(component.logs[0].tipo).toBe('exito');
  });

  it('should map tipo warning to advertencia', () => {
    expect(component.logs[1].tipo).toBe('advertencia');
  });

  it('should map tipo error to error', async () => {
    mockAudit.listLogs.and.returnValue(of(errorTipoLogs));
    component.cargarLogs();
    expect(component.logs[0].tipo).toBe('error');
  });

  it('should fallback to exito for unknown tipo', async () => {
    mockAudit.listLogs.and.returnValue(of(unknownTipoLogs));
    component.cargarLogs();
    expect(component.logs[0].tipo).toBe('exito');
  });

  it('should handle empty userName with single word name', async () => {
    mockAudit.listLogs.and.returnValue(of(singleWordNameLogs));
    component.cargarLogs();
    expect(component.logs[0].userInitials).toBe('M');
  });

    it('should handle multi-word name initials limited to 2', async () => {
      mockAudit.listLogs.and.returnValue(of(multiPageLogs));
      component.cargarLogs();
      expect(component.logs[0].userInitials).toBe('PG');
    });

    it('should handle name with consecutive spaces covering p[0] fallback', async () => {
      mockAudit.listLogs.and.returnValue(of(doubleSpaceNameLogs));
      component.cargarLogs();
      expect(component.logs[0].userInitials).toBe('A');
    });

  it('should handle empty userName returning question mark', async () => {
    mockAudit.listLogs.and.returnValue(of(errorTipoLogs));
    component.cargarLogs();
    expect(component.logs[0].userInitials).toBe('?');
  });

  it('should have logs loaded', () => {
    expect(component.logs.length).toBe(2);
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

  it('should compute finRegistro capped at totalElementos', async () => {
    mockAudit.listLogs.and.returnValue(of(multiPageLogs));
    component.cargarLogs();
    expect(component.finRegistro).toBe(5);
  });

  it('should clamp paginaActual when totalPages shrinks', async () => {
    mockAudit.listLogs.and.returnValue(of(multiPageLogs));
    component.paginaActual = 7;
    component.cargarLogs();
    expect(component.paginaActual).toBeLessThanOrEqual(component.totalPaginas);
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
    mockAudit.listLogs.and.returnValue(of({ ...mockLogs, totalPages: 3 }));
    component.paginaActual = 1;
    mockAudit.listLogs.calls.reset();
    component.cambiarPagina(2);
    expect(component.paginaActual).toBe(2);
    expect(component.logSeleccionado).toBeNull();
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

  it('should seleccionarLog switch to different log', () => {
    component.seleccionarLog(component.logs[0]);
    expect(component.logSeleccionado?.id).toBe('1');
    component.seleccionarLog(component.logs[1]);
    expect(component.logSeleccionado?.id).toBe('2');
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
    expect(component.error()).toBe('Error al cargar los logs de auditoría');
  });

  it('should not fail when cargarMetricas errors', () => {
    mockAudit.getMetrics.and.returnValue(throwError(() => new Error('fail')));
    expect(() => component.cargarMetricas()).not.toThrow();
  });

  it('should show error state in template when error signal is set', () => {
    component.error.set('Test error');
    fixture.detectChanges();
    const el = fixture.nativeElement;
    expect(el.querySelector('.text-danger')).toBeTruthy();
  });

  it('should show empty state when no logs', async () => {
    mockAudit.listLogs.and.returnValue(of(emptyLogs));
    component.cargarLogs();
    fixture.detectChanges();
    expect(component.logs.length).toBe(0);
  });

  it('should show detail panel when log is selected', () => {
    component.seleccionarLog(component.logs[0]);
    fixture.detectChanges();
    const panel = fixture.nativeElement.querySelector('.detail-panel');
    expect(panel).toBeTruthy();
  });

  it('should hide detail panel when cerrarDetalle is called', () => {
    component.seleccionarLog(component.logs[0]);
    component.cerrarDetalle();
    expect(component.logSeleccionado).toBeNull();
  });

  it('should pass searchTerm to listLogs', () => {
    component.searchTerm = 'test search';
    mockAudit.listLogs.calls.reset();
    component.filtrarLogs();
    expect(mockAudit.listLogs).toHaveBeenCalledWith(
      jasmine.objectContaining({ searchTerm: 'test search' })
    );
  });

  it('should pass date filters to listLogs', () => {
    component.filtroFechaDesde = '2026-01-01';
    component.filtroFechaHasta = '2026-12-31';
    mockAudit.listLogs.calls.reset();
    component.filtrarLogs();
    expect(mockAudit.listLogs).toHaveBeenCalledWith(
      jasmine.objectContaining({ fecha_desde: '2026-01-01', fecha_hasta: '2026-12-31' })
    );
  });

  it('should pass undefined for empty search and date filters', () => {
    component.searchTerm = '';
    component.filtroFechaDesde = '';
    component.filtroFechaHasta = '';
    mockAudit.listLogs.calls.reset();
    component.filtrarLogs();
    expect(mockAudit.listLogs).toHaveBeenCalledWith(
      jasmine.objectContaining({ searchTerm: undefined, fecha_desde: undefined, fecha_hasta: undefined })
    );
  });

  it('should handle log with oldValue and newValue', async () => {
    mockAudit.listLogs.and.returnValue(of(errorTipoLogs));
    component.cargarLogs();
    expect(component.logs[0].oldValue).toEqual({ name: 'old' });
    expect(component.logs[0].newValue).toEqual({ name: 'new' });
  });

  it('should handle log with null oldValue and newValue', () => {
    expect(component.logs[0].oldValue).toBeNull();
    expect(component.logs[0].newValue).toBeNull();
  });

  it('should default all null optional fields to empty strings', async () => {
    mockAudit.listLogs.and.returnValue(of(nullFieldsLogs));
    component.cargarLogs();
    const log = component.logs[0];
    expect(log.tenant).toBe('');
    expect(log.tenantId).toBe('');
    expect(log.userName).toBe('');
    expect(log.userEmail).toBe('');
    expect(log.accion).toBe('');
    expect(log.entidad).toBe('');
    expect(log.entidadId).toBe('');
    expect(log.eventId).toBe('');
    expect(log.ipAddress).toBe('');
    expect(log.userAgent).toBe('');
  });

  it('should fallback to exito when tipo is null via optional chaining', async () => {
    mockAudit.listLogs.and.returnValue(of(nullFieldsLogs));
    component.cargarLogs();
    expect(component.logs[0].tipo).toBe('exito');
  });

  it('should change to page 1 (lower boundary)', () => {
    component.totalPaginasServidor = 3;
    component.paginaActual = 2;
    mockAudit.listLogs.calls.reset();
    component.cambiarPagina(1);
    expect(component.paginaActual).toBe(1);
    expect(mockAudit.listLogs).toHaveBeenCalled();
  });

  it('should change to last page (upper boundary)', () => {
    component.totalPaginasServidor = 3;
    component.paginaActual = 1;
    mockAudit.listLogs.and.returnValue(of({ ...mockLogs, totalPages: 3 }));
    mockAudit.listLogs.calls.reset();
    component.cambiarPagina(3);
    expect(component.paginaActual).toBe(3);
    expect(mockAudit.listLogs).toHaveBeenCalled();
  });

  it('should show loading state in template', () => {
    component.loading.set(true);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.loading-state')).toBeTruthy();
  });

  it('should hide pagination when totalElementos is 0', async () => {
    mockAudit.listLogs.and.returnValue(of(emptyLogs));
    component.cargarLogs();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.table-card__footer')).toBeFalsy();
  });

  it('should assign consistent color for same email', () => {
    const c1 = component.logs[0].userColor;
    component.cargarLogs();
    const c2 = component.logs[0].userColor;
    expect(c1).toBe(c2);
  });

  it('should set loading false after cargarLogs error', () => {
    mockAudit.listLogs.and.returnValue(throwError(() => new Error('fail')));
    component.cargarLogs();
    expect(component.loading()).toBeFalse();
  });
});
