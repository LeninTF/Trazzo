import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Incidencias } from './incidencias';

describe('Incidencias (admin-tenant)', () => {
  let component: Incidencias;
  let fixture: ComponentFixture<Incidencias>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Incidencias],
    }).compileComponents();

    fixture = TestBed.createComponent(Incidencias);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 3 initial solicitudes', () => {
    expect(component.solicitudes().length).toBe(3);
  });

  it('should have tipos disponibles', () => {
    expect(component.tiposDisponibles.length).toBe(3);
  });

  it('should compute filtradas by estado', () => {
    component.setFilterEstado('Pendiente');
    expect(component.filtradas().every(s => s.estado === 'Pendiente')).toBeTrue();
  });

  it('should compute filtradas by tipo', () => {
    component.setFilterTipo('Vacaciones');
    expect(component.filtradas().every(s => s.tipo === 'Vacaciones')).toBeTrue();
  });

  it('should show all with Todos and empty tipo', () => {
    component.setFilterEstado('Todos');
    component.setFilterTipo('');
    expect(component.filtradas().length).toBe(3);
  });

  it('should compute pendientes/aprobadas/rechazadas', () => {
    expect(component.pendientes().every(s => s.estado === 'Pendiente')).toBeTrue();
    expect(component.aprobadas().every(s => s.estado === 'Aprobado')).toBeTrue();
    expect(component.rechazadas().every(s => s.estado === 'Rechazado')).toBeTrue();
  });

  it('should compute metricas', () => {
    expect(component.metricas.length).toBe(3);
    expect(component.metricas[0].titulo).toBe('Pendientes');
  });

  it('should toggleFilter', () => {
    component.toggleFilter();
    expect(component.filterOpen).toBeTrue();
    component.toggleFilter();
    expect(component.filterOpen).toBeFalse();
  });

  it('should setFilterEstado', () => {
    component.setFilterEstado('Aprobado');
    expect(component.filterEstado()).toBe('Aprobado');
  });

  it('should setFilterTipo toggle', () => {
    component.setFilterTipo('Permiso Médico');
    expect(component.filterTipo()).toBe('Permiso Médico');
    component.setFilterTipo('Permiso Médico');
    expect(component.filterTipo()).toBe('');
  });

  it('should limpiarFiltros', () => {
    component.setFilterEstado('Pendiente');
    component.setFilterTipo('Vacaciones');
    component.limpiarFiltros();
    expect(component.filterEstado()).toBe('Todos');
    expect(component.filterTipo()).toBe('');
  });

  it('should detect hayFiltrosActivos', () => {
    expect(component.hayFiltrosActivos).toBeFalse();
    component.setFilterEstado('Pendiente');
    expect(component.hayFiltrosActivos).toBeTrue();
  });

  it('should openModal and closeModal', () => {
    component.openModal(component.solicitudes()[0]);
    expect(component.modalOpen).toBeTrue();
    expect(component.selectedSolicitud?.colaborador).toBe('Mariana Rodríguez');
    component.closeModal();
    expect(component.modalOpen).toBeFalse();
    expect(component.selectedSolicitud).toBeNull();
  });

  it('should aprobar solicitud', () => {
    const sol = component.solicitudes()[0];
    expect(sol.estado).toBe('Pendiente');
    component.aprobar(sol);
    expect(component.solicitudes()[0].estado).toBe('Aprobado');
    expect(component.modalOpen).toBeFalse();
  });

  it('should rechazar solicitud', () => {
    const sol = component.solicitudes()[0];
    component.rechazar(sol);
    expect(component.solicitudes()[0].estado).toBe('Rechazado');
    expect(component.modalOpen).toBeFalse();
  });

  it('should descargarArchivo', () => {
    spyOn(document, 'createElement').and.returnValue({ href: '', download: '', click: () => {} } as unknown as HTMLAnchorElement);
    component.descargarArchivo(component.solicitudes()[0]);
    expect(document.createElement).toHaveBeenCalledWith('a');
  });

  it('should exportarCSV', () => {
    spyOn(URL, 'createObjectURL').and.returnValue('blob:test');
    spyOn(URL, 'revokeObjectURL');
    component.exportarCSV();
    expect(URL.createObjectURL).toHaveBeenCalled();
  });
});
