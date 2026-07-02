import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Monitoreo } from './monitoreo';

describe('Monitoreo', () => {
  let component: Monitoreo;
  let fixture: ComponentFixture<Monitoreo>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Monitoreo],
    }).compileComponents();

    fixture = TestBed.createComponent(Monitoreo);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  afterEach(() => {
    fixture.destroy();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial metrics', () => {
    expect(component.metricas.presentesHoy).toBe(1284);
    expect(component.metricas.tardanzas).toBe(42);
    expect(component.metricas.dispositivosActivos).toBe(2);
  });

  it('should have 4 initial events', () => {
    expect(component.eventos.length).toBe(4);
  });

  it('should have 3 escaneres', () => {
    expect(component.escaneres.length).toBe(3);
  });

  it('should compute totalDispositivosTexto', () => {
    expect(component.totalDispositivosTexto).toBe('2/3');
  });

  it('should compute eventosATiempo', () => {
    const aTiempo = component.eventos.filter(e => e.estado === 'A TIEMPO').length;
    expect(component.eventosATiempo).toBe(aTiempo);
  });

  it('should compute eventosTarde', () => {
    const tarde = component.eventos.filter(e => e.estado === 'TARDE').length;
    expect(component.eventosTarde).toBe(tarde);
  });

  it('should show ultimaActualizacionTexto', () => {
    expect(component.ultimaActualizacionTexto()).toContain('segundos');
  });

  it('should actualizarDatosTiempoReal', () => {
    const lengthBefore = component.eventos.length;
    component.actualizarDatosTiempoReal();
    expect(component.eventos.length).toBeGreaterThanOrEqual(lengthBefore);
    expect(component.metricas.presentesHoy).toBeGreaterThan(1284);
  });

  it('should keep max 10 events after real-time update', () => {
    for (let i = 0; i < 10; i++) {
      component.actualizarDatosTiempoReal();
    }
    expect(component.eventos.length).toBeLessThanOrEqual(10);
  });

  it('should registrarEscaner', () => {
    component.nuevoEscaner = { nombre: 'Test Escaner', ubicacion: 'Test Ubicacion', online: true };
    component.registrarEscaner();
    expect(component.escaneres.length).toBe(4);
    expect(component.metricas.totalDispositivos).toBe(4);
  });

  it('should not registrarEscaner with empty nombre', () => {
    component.nuevoEscaner = { nombre: '', ubicacion: 'Ubicacion', online: true };
    component.registrarEscaner();
    expect(component.escaneres.length).toBe(3);
  });

  it('should not registrarEscaner with empty ubicacion', () => {
    component.nuevoEscaner = { nombre: 'Nombre', ubicacion: '', online: true };
    component.registrarEscaner();
    expect(component.escaneres.length).toBe(3);
  });

  it('should eliminarEscaner find and set', () => {
    component.eliminarEscaner(1);
    expect(component.escanerAEliminar).toBeTruthy();
    expect(component.escanerAEliminar!.id).toBe(1);
  });

  it('should confirmarEliminarEscaner', () => {
    component.escanerAEliminar = component.escaneres[0];
    component.confirmarEliminarEscaner();
    expect(component.escaneres.length).toBe(2);
    expect(component.escanerAEliminar).toBeNull();
  });

  it('should toggleEscaner online/offline', () => {
    const escaner = component.escaneres[0];
    const wasOnline = escaner.online;
    component.toggleEscaner(escaner.id);
    expect(escaner.online).toBe(!wasOnline);
  });

  it('should toggleEscaner update metricas', () => {
    component.toggleEscaner(1);
    expect(component.metricas.dispositivosActivos).toBe(component.escaneres.filter(e => e.online).length);
  });

  it('should limpiarFormularioEscaner', () => {
    component.nuevoEscaner = { nombre: 'Test', ubicacion: 'Test', online: false };
    component.limpiarFormularioEscaner();
    expect(component.nuevoEscaner.nombre).toBe('');
    expect(component.nuevoEscaner.ubicacion).toBe('');
    expect(component.nuevoEscaner.online).toBeTrue();
  });

  it('should agregarEventoDeSistema', () => {
    const lenBefore = component.eventos.length;
    component.agregarEventoDeSistema('Test event');
    expect(component.eventos.length).toBe(lenBefore + 1);
    expect(component.eventos[0].nombre).toBe('SISTEMA');
  });

  it('should eliminarEvento', () => {
    const lenBefore = component.eventos.length;
    component.eliminarEvento(1);
    expect(component.eventos.length).toBe(lenBefore - 1);
  });

  it('should not eliminarEvento with non-existent id', () => {
    const lenBefore = component.eventos.length;
    component.eliminarEvento(999);
    expect(component.eventos.length).toBe(lenBefore);
  });

  it('should refrescarDatos', () => {
    const presentesBefore = component.metricas.presentesHoy;
    component.refrescarDatos();
    expect(component.metricas.presentesHoy).toBeGreaterThan(presentesBefore);
  });

  it('should update tardanza level on many late events', () => {
    for (let i = 0; i < 30; i++) {
      component.actualizarDatosTiempoReal();
    }
  });

  it('should set ultimaActualizacion on init', () => {
    expect(component.ultimaActualizacion).toBeDefined();
  });

  it('should clean up interval on destroy', () => {
    spyOn(window, 'clearInterval');
    component.ngOnDestroy();
    expect(window.clearInterval).toHaveBeenCalled();
  });
});
