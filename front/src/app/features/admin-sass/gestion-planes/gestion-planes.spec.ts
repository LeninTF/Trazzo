import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GestionPlanes } from './gestion-planes';

describe('GestionPlanes', () => {
  let component: GestionPlanes;
  let fixture: ComponentFixture<GestionPlanes>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GestionPlanes],
    }).compileComponents();

    const mockModalInstance = { show: () => {}, hide: () => {} };
    (window as any).bootstrap = {
      Modal: Object.assign(
        function () { return mockModalInstance; },
        { getInstance: () => mockModalInstance }
      ),
    };

    fixture = TestBed.createComponent(GestionPlanes);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  afterEach(() => {
    delete (window as any).bootstrap;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 3 initial planes', () => {
    expect(component.planes().length).toBe(3);
  });

  it('should have 6 modulos disponibles', () => {
    expect(component.modulosDisponibles.length).toBe(6);
  });

  it('should have 3 reglas disponibles', () => {
    expect(component.reglasDisponibles.length).toBe(3);
  });

  it('should have 8 suscripciones', () => {
    expect(component.suscripciones.length).toBe(8);
  });

  it('should seleccionarPlan and populate form', () => {
    const plan = component.planes()[0];
    component.seleccionarPlan(plan);
    expect(component.editPlanId()).toBe(plan.id);
    expect(component.editando()).toBeTrue();
    expect(component.planForm.get('nombre')?.value).toBe(plan.nombre);
  });

  it('should nuevoPlan reset form', () => {
    component.nuevoPlan();
    expect(component.editPlanId()).toBeNull();
    expect(component.mostrarFormNuevo()).toBeTrue();
    expect(component.editando()).toBeTrue();
    expect(component.planForm.get('nombre')?.value).toBe('');
  });

  it('should cancelarEdicion reset state', () => {
    component.nuevoPlan();
    component.cancelarEdicion();
    expect(component.editando()).toBeFalse();
    expect(component.mostrarFormNuevo()).toBeFalse();
    expect(component.editPlanId()).toBeNull();
  });

  it('should guardarPlan create new plan', () => {
    component.nuevoPlan();
    component.planForm.patchValue({
      nombre: 'Plan Test', sku: 'PLAN-TEST',
      precioMensual: 50, precioAnual: 500,
      maxTrabajadores: 100, maxSedes: 5, almacenamiento: 30,
    });
    component.guardarPlan();
    expect(component.planes().length).toBe(4);
    expect(component.planes().find(p => p.nombre === 'Plan Test')).toBeTruthy();
  });

  it('should guardarPlan update existing plan', () => {
    const plan = component.planes()[0];
    component.seleccionarPlan(plan);
    component.planForm.patchValue({ nombre: 'Plan Actualizado' });
    component.guardarPlan();
    expect(component.planes()[0].nombre).toBe('Plan Actualizado');
  });

  it('should not guardarPlan with invalid form', () => {
    component.nuevoPlan();
    component.guardarPlan();
    expect(component.planes().length).toBe(3);
  });

  it('should eliminarPlan', () => {
    component.editPlanId.set(1);
    component.eliminarPlan();
    expect(component.planes().length).toBe(2);
  });

  it('should compute modulosActivos', () => {
    const plan = component.planes()[2];
    component.seleccionarPlan(plan);
    const activos = component.modulosActivos();
    expect(activos.length).toBe(plan.modulos.length);
  });

  it('should compute reglasActivas', () => {
    const plan = component.planes()[2];
    component.seleccionarPlan(plan);
    const activas = component.reglasActivas();
    expect(activas.length).toBe(plan.reglas.length);
  });

  it('should compute precioFinal', () => {
    component.planForm.patchValue({ precioMensual: 100, precioAnual: 1000 });
    expect(component.precioFinal().mensual).toBe(100);
    expect(component.precioFinal().anual).toBe(1000);
  });

  it('should paginate suscripciones', () => {
    expect(component.suscripcionesPaginadas().length).toBe(5);
    component.suscripcionPagina.set(2);
    expect(component.suscripcionesPaginadas().length).toBe(3);
  });

  it('should compute totalSuscripcionPaginas', () => {
    expect(component.totalSuscripcionPaginas()).toBe(2);
  });

  it('should go to suscripcion page', () => {
    component.irPaginaSuscripcion(2);
    expect(component.suscripcionPagina()).toBe(2);
  });

  it('should cambiarPaginaSuscripcion forward', () => {
    component.cambiarPaginaSuscripcion(1);
    expect(component.suscripcionPagina()).toBe(2);
  });

  it('should cambiarPaginaSuscripcion backward', () => {
    component.suscripcionPagina.set(2);
    component.cambiarPaginaSuscripcion(-1);
    expect(component.suscripcionPagina()).toBe(1);
  });

  it('should not change page beyond limits', () => {
    component.cambiarPaginaSuscripcion(-1);
    expect(component.suscripcionPagina()).toBe(1);
    component.cambiarPaginaSuscripcion(10);
    expect(component.suscripcionPagina()).toBe(1);
  });

  it('should verDetalleSuscripcion set selected', () => {
    component.verDetalleSuscripcion(component.suscripciones[0]);
    expect(component.suscripcionSeleccionada()).toEqual(component.suscripciones[0]);
  });

  it('should exportarCSV', () => {
    spyOn(URL, 'createObjectURL').and.returnValue('blob:test');
    spyOn(URL, 'revokeObjectURL');
    component.exportarCSV();
    expect(URL.createObjectURL).toHaveBeenCalled();
  });

  it('should show and clear toast', () => {
    (component as any).mostrarToast('Test', 'success');
    expect(component.toast()).toEqual({ message: 'Test', type: 'success' });
  });

  it('should compute suscripcionPaginasNum', () => {
    expect(component.suscripcionPaginasNum()).toEqual([1, 2]);
  });
});
