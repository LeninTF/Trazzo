import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LogAuditoria } from './log-auditoria';

describe('LogAuditoria', () => {
  let component: LogAuditoria;
  let fixture: ComponentFixture<LogAuditoria>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LogAuditoria],
    }).compileComponents();

    fixture = TestBed.createComponent(LogAuditoria);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 5 initial logs', () => {
    expect(component.logs.length).toBe(5);
  });

  it('should filter by searchTerm in accion', () => {
    component.searchTerm = 'login';
    expect(component.logsFiltrado.length).toBe(1);
    expect(component.logsFiltrado[0].accion).toBe('Login');
  });

  it('should filter by searchTerm in entidad', () => {
    component.searchTerm = 'users';
    expect(component.logsFiltrado.length).toBe(1);
  });

  it('should filter by searchTerm in tenant', () => {
    component.searchTerm = 'trazzo';
    const count = component.logs.filter(l => l.tenant.toLowerCase().includes('trazzo')).length;
    expect(component.logsFiltrado.length).toBe(count);
  });

  it('should filter by fecha', () => {
    const fechaStr = '2026-06-06';
    component.filtroFecha = fechaStr;
    const fechaFiltro = new Date(fechaStr);
    const esperados = component.logs.filter(l => l.fecha.toDateString() === fechaFiltro.toDateString());
    expect(component.logsFiltrado.length).toBe(esperados.length);
    expect(component.logsFiltrado).toEqual(esperados);
  });

  it('should show all without filters', () => {
    expect(component.logsFiltrado.length).toBe(5);
  });

  it('should compute logsPaginado', () => {
    expect(component.logsPaginado.length).toBe(5);
    component.itemsPerPage = 2;
    expect(component.logsPaginado.length).toBe(2);
  });

  it('should compute totalPaginas', () => {
    expect(component.totalPaginas).toBe(1);
    component.itemsPerPage = 2;
    expect(component.totalPaginas).toBe(3);
  });

  it('should compute inicioRegistro', () => {
    expect(component.inicioRegistro).toBe(1);
  });

  it('should compute inicioRegistro as 0 when empty', () => {
    component.searchTerm = 'zzzzznoexist';
    expect(component.inicioRegistro).toBe(0);
  });

  it('should compute finRegistro', () => {
    expect(component.finRegistro).toBe(5);
  });

  it('should filtrarLogs reset page and selection', () => {
    component.paginaActual = 3;
    component.logSeleccionado = component.logs[0];
    component.filtrarLogs();
    expect(component.paginaActual).toBe(1);
    expect(component.logSeleccionado).toBeNull();
  });

  it('should aplicarFiltros', () => {
    component.aplicarFiltros();
    expect(component.paginaActual).toBe(1);
  });

  it('should limpiarFiltros', () => {
    component.searchTerm = 'test';
    component.filtroFecha = '2026-01-01';
    component.limpiarFiltros();
    expect(component.searchTerm).toBe('');
    expect(component.filtroFecha).toBe('');
    expect(component.paginaActual).toBe(1);
  });

  it('should cambiarPagina within range', () => {
    component.itemsPerPage = 2;
    component.cambiarPagina(2);
    expect(component.paginaActual).toBe(2);
  });

  it('should not cambiarPagina out of range', () => {
    component.cambiarPagina(0);
    expect(component.paginaActual).toBe(1);
    component.cambiarPagina(999);
    expect(component.paginaActual).toBe(1);
  });

  it('should seleccionarLog toggle', () => {
    component.seleccionarLog(component.logs[0]);
    expect(component.logSeleccionado?.id).toBe(1);
    component.seleccionarLog(component.logs[0]);
    expect(component.logSeleccionado).toBeNull();
  });

  it('should cerrarDetalle', () => {
    component.seleccionarLog(component.logs[0]);
    component.cerrarDetalle();
    expect(component.logSeleccionado).toBeNull();
  });

  it('should exportarCSV', () => {
    spyOn(URL, 'createObjectURL').and.returnValue('blob:test');
    spyOn(URL, 'revokeObjectURL');
    component.exportarCSV();
    expect(URL.createObjectURL).toHaveBeenCalled();
  });
});
