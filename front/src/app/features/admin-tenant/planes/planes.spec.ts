import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Planes } from './planes';

describe('Planes', () => {
  let component: Planes;
  let fixture: ComponentFixture<Planes>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Planes],
    }).compileComponents();

    fixture = TestBed.createComponent(Planes);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 4 initial facturas', () => {
    expect(component.facturas().length).toBe(4);
  });

  it('should have 3 initial planes', () => {
    expect(component.planes().length).toBe(3);
  });

  it('should have default plan actual', () => {
    expect(component.planActualId).toBe('professional');
  });

  it('should get planActual', () => {
    expect(component.planActual?.nombre).toBe('Professional');
  });

  it('should get planSeleccionado', () => {
    component.planSeleccionadoId = 'starter';
    expect(component.planSeleccionado?.nombre).toBe('Starter');
  });

  it('should return undefined for non-existent plan', () => {
    component.planSeleccionadoId = 'non-existent';
    expect(component.planSeleccionado).toBeUndefined();
  });

  it('should get facturaSeleccionada', () => {
    component.facturaSeleccionadaId = 1;
    expect(component.facturaSeleccionada?.codigo).toBe('INV 2023-006');
  });

  it('should compute porcentajeUsuarios', () => {
    const pct = Math.round((component.metricas.usuariosActivos / component.metricas.limiteUsuarios) * 100);
    expect(component.porcentajeUsuarios).toBe(pct);
  });

  it('should compute porcentajeAlmacenamiento', () => {
    const pct = Math.round((component.metricas.almacenamientoUsado / component.metricas.limiteAlmacenamiento) * 100);
    expect(component.porcentajeAlmacenamiento).toBe(pct);
  });

  it('should seleccionarPlan', () => {
    component.seleccionarPlan(component.planes()[0]);
    expect(component.planSeleccionadoId).toBe('starter');
  });

  it('should seleccionarFactura', () => {
    component.seleccionarFactura(component.facturas()[0]);
    expect(component.facturaSeleccionadaId).toBe(1);
    expect(component.modalFacturaOpen).toBeTrue();
  });

  it('should abrirModalActualizarPlan', () => {
    component.abrirModalActualizarPlan();
    expect(component.modalActualizarOpen).toBeTrue();
  });

  it('should cerrarModalActualizar', () => {
    component.abrirModalActualizarPlan();
    component.cerrarModalActualizar();
    expect(component.modalActualizarOpen).toBeFalse();
  });

  it('should confirmarActualizarPlan with selected plan', () => {
    component.seleccionarPlan(component.planes()[0]);
    component.abrirModalActualizarPlan();
    component.confirmarActualizarPlan();
    expect(component.planActualId).toBe('starter');
    expect(component.facturas().length).toBe(5);
  });

  it('should confirmarActualizarPlan without selection', () => {
    component.planSeleccionadoId = null;
    component.abrirModalActualizarPlan();
    expect(component.modalActualizarOpen).toBeTrue();
  });

  it('should cerrarModalFactura', () => {
    component.seleccionarFactura(component.facturas()[0]);
    component.cerrarModalFactura();
    expect(component.modalFacturaOpen).toBeFalse();
    expect(component.facturaSeleccionadaId).toBeNull();
  });

  it('should exportarFacturas', () => {
    component.exportarFacturas();
    expect(component.facturas().length).toBeGreaterThan(0);
  });

  it('should descargarFactura', () => {
    component.descargarFactura();
    expect(component).toBeTruthy();
  });
});
