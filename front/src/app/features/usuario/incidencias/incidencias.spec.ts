import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { Incidencias } from './incidencias';

describe('Incidencias (usuario)', () => {
  let component: Incidencias;
  let fixture: ComponentFixture<Incidencias>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Incidencias, FormsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(Incidencias);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 6 initial incidencias', () => {
    expect(component.incidencias().length).toBe(6);
  });

  it('should have tipos disponibles', () => {
    expect(component.tiposDisponibles.length).toBe(4);
  });

  it('should filter by estado', () => {
    component.setFilterEstado('Aprobado');
    expect(component.filtradas.every(i => i.estado === 'Aprobado')).toBeTrue();
  });

  it('should show all with Todos filter', () => {
    component.setFilterEstado('Todos');
    expect(component.filtradas.length).toBe(6);
  });

  it('should compute pendientes count', () => {
    const count = component.incidencias().filter(i => i.estado === 'Pendiente').length;
    expect(component.pendientes).toBe(count);
  });

  it('should compute aprobados count', () => {
    const count = component.incidencias().filter(i => i.estado === 'Aprobado').length;
    expect(component.aprobados).toBe(count);
  });

  it('should compute rechazados count', () => {
    const count = component.incidencias().filter(i => i.estado === 'Rechazado').length;
    expect(component.rechazados).toBe(count);
  });

  it('should compute resumen', () => {
    expect(component.resumen.length).toBe(3);
    expect(component.resumen[0].label).toBe('Pendientes');
  });

  it('should abrirModalCrear', () => {
    component.abrirModalCrear();
    expect(component.mostrarModalCrear).toBeTrue();
    expect(component.nuevaIncidencia.tipo).toBe('');
  });

  it('should cerrarModalCrear', () => {
    component.abrirModalCrear();
    component.cerrarModalCrear();
    expect(component.mostrarModalCrear).toBeFalse();
  });

  it('should abrirDetalle', () => {
    component.abrirDetalle(component.incidencias()[0]);
    expect(component.mostrarModalDetalle).toBeTrue();
    expect(component.selectedIncidencia?.id).toBe('#INC-006');
  });

  it('should cerrarDetalle', () => {
    component.abrirDetalle(component.incidencias()[0]);
    component.cerrarDetalle();
    expect(component.mostrarModalDetalle).toBeFalse();
    expect(component.selectedIncidencia).toBeNull();
  });

  it('should enviar new incidencia', () => {
    component.abrirModalCrear();
    component.nuevaIncidencia.tipo = 'Permiso Personal';
    component.nuevaIncidencia.descripcion = 'Test descripcion';
    component.nuevaIncidencia.fecha = '10/06/2026';
    component.enviar();
    expect(component.incidencias().length).toBe(7);
    expect(component.mostrarModalCrear).toBeFalse();
  });

  it('should not enviar without tipo', () => {
    component.abrirModalCrear();
    component.enviar();
    expect(component.incidencias().length).toBe(6);
  });

  it('should not enviar without descripcion', () => {
    component.abrirModalCrear();
    component.nuevaIncidencia.tipo = 'Permiso Personal';
    component.enviar();
    expect(component.incidencias().length).toBe(6);
  });

  it('should descargarArchivo', () => {
    spyOn(document, 'createElement').and.returnValue({ href: '', download: '', click: () => {} } as any);
    const inc = component.incidencias().find(i => i.archivo);
    if (inc) {
      component.descargarArchivo(inc);
      expect(document.createElement).toHaveBeenCalledWith('a');
    }
  });
});
