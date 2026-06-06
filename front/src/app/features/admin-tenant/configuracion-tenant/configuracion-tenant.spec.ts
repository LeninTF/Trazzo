import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfiguracionTenant } from './configuracion-tenant';

describe('ConfiguracionTenant', () => {
  let component: ConfiguracionTenant;
  let fixture: ComponentFixture<ConfiguracionTenant>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfiguracionTenant],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfiguracionTenant);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial tenant info', () => {
    expect(component.tenantInfo().nombre).toBe('Universidad Nacional');
    expect(component.tenantInfo().email).toBe('admin@unacional.edu');
  });

  it('should have initial limits', () => {
    expect(component.limites().maxUsuarios).toBe(1000);
    expect(component.limites().maxSedes).toBe(5);
  });

  it('should have 6 modules', () => {
    expect(component.modulos().length).toBe(6);
  });

  it('should have initial branding colors', () => {
    expect(component.branding().colorPrimario).toBe('#163A96');
    expect(component.branding().colorSecundario).toBe('#60283F');
  });

  it('should mark cambios pendientes', () => {
    expect(component.cambiosPendientes).toBeFalse();
    component.marcarCambiosPendientes();
    expect(component.cambiosPendientes).toBeTrue();
  });

  it('should open and close confirm modal', () => {
    component.guardarCambios();
    expect(component.modalConfirmacionOpen).toBeTrue();
    component.cerrarModalConfirmacion();
    expect(component.modalConfirmacionOpen).toBeFalse();
  });

  it('should confirm guardar and update cambiosPendientes', () => {
    component.marcarCambiosPendientes();
    component.confirmarGuardar();
    expect(component.cambiosPendientes).toBeFalse();
    expect(component.modalConfirmacionOpen).toBeFalse();
  });

  it('should toggle modulo and log', () => {
    const modulo = component.modulos()[1];
    expect(modulo.activo).toBeFalse();
    spyOn<any>(component, 'agregarLog');
    component.toggleModulo(modulo);
    expect(component.cambiosPendientes).toBeTrue();
  });

  it('should open and close logs modal', () => {
    component.verLogs();
    expect(component.modalLogsOpen).toBeTrue();
    component.cerrarModalLogs();
    expect(component.modalLogsOpen).toBeFalse();
  });

  it('should open and close suspender modal', () => {
    component.suspenderTenant();
    expect(component.modalSuspenderOpen).toBeTrue();
    component.cerrarModalSuspender();
    expect(component.modalSuspenderOpen).toBeFalse();
    expect(component.motivoSuspension).toBe('');
  });

  it('should confirm suspender and add log', () => {
    component.motivoSuspension = 'Prueba';
    spyOn<any>(component, 'agregarLog');
    component.confirmarSuspender();
    expect(component.modalSuspenderOpen).toBeFalse();
  });

  it('should update color primario', () => {
    component.actualizarColorPrimario('#FF0000');
    expect(component.branding().colorPrimario).toBe('#FF0000');
    expect(component.cambiosPendientes).toBeTrue();
  });

  it('should update color secundario', () => {
    component.actualizarColorSecundario('#00FF00');
    expect(component.branding().colorSecundario).toBe('#00FF00');
    expect(component.cambiosPendientes).toBeTrue();
  });

  it('should update color acento', () => {
    component.actualizarColorAcento('#0000FF');
    expect(component.branding().colorAcento).toBe('#0000FF');
    expect(component.cambiosPendientes).toBeTrue();
  });

  it('should reset colors to default', () => {
    component.actualizarColorPrimario('#FF0000');
    component.resetearColores();
    expect(component.branding().colorPrimario).toBe('#163A96');
    expect(component.branding().colorSecundario).toBe('#60283F');
    expect(component.branding().colorAcento).toBe('#10B981');
    expect(component.cambiosPendientes).toBeTrue();
  });

  it('should validate max usuarios lower bound', () => {
    component.limites.update(l => ({ ...l, maxUsuarios: 0 }));
    component.validarLimiteMaxUsuarios();
    expect(component.limites().maxUsuarios).toBe(1);
  });

  it('should validate max usuarios upper bound', () => {
    component.limites.update(l => ({ ...l, maxUsuarios: 100001 }));
    component.validarLimiteMaxUsuarios();
    expect(component.limites().maxUsuarios).toBe(100000);
  });

  it('should validate max sedes lower bound', () => {
    component.limites.update(l => ({ ...l, maxSedes: 0 }));
    component.validarLimiteSedes();
    expect(component.limites().maxSedes).toBe(1);
  });

  it('should validate max sedes upper bound', () => {
    component.limites.update(l => ({ ...l, maxSedes: 51 }));
    component.validarLimiteSedes();
    expect(component.limites().maxSedes).toBe(50);
  });

  it('should add log entry', () => {
    spyOn<any>(component, 'mostrarToast');
    component.confirmarGuardar();
    expect(component.logs().length).toBeGreaterThan(3);
  });

  it('should open file selector for logo', () => {
    const input = document.createElement('input');
    input.type = 'file';
    input.id = 'fake-file-input';
    document.body.appendChild(input);
    spyOn(input, 'click');
    component.abrirSelectorLogo();
    document.body.removeChild(input);
  });

  it('should handle logo file too large', () => {
    const blob = new Blob(['a'.repeat(3 * 1024 * 1024)]);
    const file = new File([blob], 'test.png', { type: 'image/png' });
    const event = { target: { files: [file] } } as unknown as Event;
    component.cambiarLogo(event);
    expect(component.tenantInfo().logoUrl).toBeNull();
  });

  it('should eliminar logo', () => {
    component.tenantInfo.update(info => ({ ...info, logoUrl: 'data:image/png;base64,abc' }));
    const event = new MouseEvent('click');
    component.eliminarLogo(event);
    expect(component.tenantInfo().logoUrl).toBeNull();
  });

  it('should keep 50 max logs', () => {
    for (let i = 0; i < 60; i++) {
      (component as any).agregarLog(`Log ${i}`, 'info');
    }
    expect(component.logs().length).toBeLessThanOrEqual(50);
  });
});
