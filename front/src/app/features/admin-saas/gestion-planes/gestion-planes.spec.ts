import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { GestionPlanes } from './gestion-planes';
import { SaasService } from '../../../api/services/saas.service';
import { ToastService } from '../../../services/toast.service';
import { ModalService } from '../../../services/modal.service';
import { ExportService } from '../../../services/export.service';

describe('GestionPlanes', () => {
  let component: GestionPlanes;
  let fixture: ComponentFixture<GestionPlanes>;

  const mockPlans = [
    { id: 1, name: 'Plan Básico', price: 50, priceAnnual: 500, currency: 'SOLES', billingPeriod: 'MONTHLY', active: true, createdAt: '2025-01-01T00:00:00', features: { max_trabajadores: 100, max_sedes: 1, almacenamiento_gb: 5 } },
    { id: 2, name: 'Plan Pro', price: 100, priceAnnual: 1000, currency: 'SOLES', billingPeriod: 'MONTHLY', active: true, createdAt: '2025-01-01T00:00:00', features: { max_trabajadores: 200, max_sedes: 2, almacenamiento_gb: 10 } },
    { id: 3, name: 'Plan Enterprise', price: 1000, priceAnnual: 10000, currency: 'SOLES', billingPeriod: 'ANNUAL', active: false, createdAt: '2025-01-01T00:00:00', features: { max_trabajadores: 500, max_sedes: 5, almacenamiento_gb: 50 } },
  ];

  const mockSubscriptions = {
    content: [
      { id: 'sub-1', tenantId: 'tenant-1', tenantName: 'demo', planId: 1, planName: 'Plan Demo',
        dateStart: '2026-01-01', dateEnd: '2026-12-31', status: 'ACTIVE', purchasePrice: 29.99, createdAt: '2026-01-01T00:00:00' },
    ],
    page: 0, size: 100, totalElements: 1, totalPages: 1,
  };

  const mockSaas = {
    listPlans: jasmine.createSpy('listPlans').and.returnValue(of(mockPlans)),
    listActivePlans: jasmine.createSpy('listActivePlans').and.returnValue(of(mockPlans)),
    getPlan: jasmine.createSpy('getPlan').and.returnValue(of(mockPlans[0])),
    createPlan: jasmine.createSpy('createPlan').and.returnValue(of(mockPlans[0])),
    updatePlan: jasmine.createSpy('updatePlan').and.returnValue(of(mockPlans[0])),
    activatePlan: jasmine.createSpy('activatePlan').and.returnValue(of(mockPlans[0])),
    deactivatePlan: jasmine.createSpy('deactivatePlan').and.returnValue(of(mockPlans[0])),
    deletePlan: jasmine.createSpy('deletePlan').and.returnValue(of(undefined)),
    listSubscriptions: jasmine.createSpy('listSubscriptions').and.returnValue(of(mockSubscriptions)),
  };

  const mockToast = jasmine.createSpyObj('ToastService', ['show', 'error']);
  const mockModal = jasmine.createSpyObj('ModalService', ['show', 'hide']);

  beforeEach(async () => {
    Object.values(mockSaas).forEach(spy => spy.calls.reset());
    mockSaas.listPlans.and.returnValue(of(mockPlans));
    mockSaas.listSubscriptions.and.returnValue(of(mockSubscriptions));

    await TestBed.configureTestingModule({
      imports: [GestionPlanes],
      providers: [
        { provide: SaasService, useValue: mockSaas },
        { provide: ToastService, useValue: mockToast },
        { provide: ModalService, useValue: mockModal },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GestionPlanes);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load planes on init', () => {
    expect(mockSaas.listPlans).toHaveBeenCalled();
    expect(component.planes().length).toBe(3);
  });

  it('should have 6 modulos disponibles', () => {
    expect(component.modulosDisponibles.length).toBe(6);
  });

  it('should have 3 reglas disponibles', () => {
    expect(component.reglasDisponibles.length).toBe(3);
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
      moneda: 'SOLES', periodo: 'MONTHLY',
      maxTrabajadores: 100, maxSedes: 5, almacenamiento: 30,
    });
    component.guardarPlan();
    expect(mockSaas.createPlan).toHaveBeenCalledWith(jasmine.objectContaining({
      name: 'Plan Test', price: 50, priceAnnual: 500, currency: 'SOLES', billingPeriod: 'MONTHLY',
      features: jasmine.objectContaining({
        max_trabajadores: 100, max_sedes: 5, almacenamiento_gb: 30,
      }),
    }));
  });

  it('should guardarPlan update existing plan', () => {
    const plan = component.planes()[0];
    component.seleccionarPlan(plan);
    component.planForm.patchValue({ nombre: 'Plan Actualizado' });
    component.guardarPlan();
    expect(mockSaas.updatePlan).toHaveBeenCalled();
  });

  it('should not guardarPlan with invalid form', () => {
    component.nuevoPlan();
    component.planForm.patchValue({ nombre: '' });
    component.guardarPlan();
    expect(mockSaas.createPlan).not.toHaveBeenCalled();
    expect(mockToast.show).toHaveBeenCalled();
  });

  it('should handle guardarPlan error', () => {
    mockSaas.createPlan.and.returnValue(throwError(() => new Error('fail')));
    component.nuevoPlan();
    component.planForm.patchValue({ nombre: 'Plan Test', sku: 'X', precioMensual: 10, precioAnual: 100 });
    component.guardarPlan();
    expect(mockToast.show).toHaveBeenCalledWith('Error al guardar el plan.', 'error');
  });

  it('should eliminarPlan', () => {
    component.editPlanId.set(1);
    component.eliminarPlan();
    expect(mockSaas.deletePlan).toHaveBeenCalledWith(1);
  });

  it('should toggleActivo deactivate active plan', () => {
    const plan = component.planes()[0];
    component.toggleActivo(plan);
    expect(mockSaas.deactivatePlan).toHaveBeenCalledWith(plan.id);
  });

  it('should toggleActivo activate inactive plan', () => {
    const plan = component.planes()[2];
    component.toggleActivo(plan);
    expect(mockSaas.activatePlan).toHaveBeenCalledWith(plan.id);
  });

  it('should compute modulosActivos', () => {
    component.planForm.patchValue({ modulos: { reportes: true, 'api-externa': true } });
    expect(component.modulosActivos().length).toBe(2);
  });

  it('should compute reglasActivas', () => {
    component.planForm.patchValue({ reglas: { 'multi-sede': true } });
    expect(component.reglasActivas().length).toBe(1);
  });

  it('should compute precioFinal', () => {
    component.planForm.patchValue({ precioMensual: 100, precioAnual: 1000 });
    expect(component.precioFinal().mensual).toBe(100);
    expect(component.precioFinal().anual).toBe(1000);
  });

  it('should load suscripciones on init', () => {
    expect(mockSaas.listSubscriptions).toHaveBeenCalledWith({ page: 0, size: 100 });
    expect(component.suscripciones().length).toBe(1);
    expect(component.suscripciones()[0].tenant).toBe('demo');
    expect(component.suscripciones()[0].plan).toBe('Plan Demo');
    expect(component.suscripciones()[0].estado).toBe('activo');
  });

  it('should handle cargarSuscripciones error', () => {
    mockSaas.listSubscriptions.and.returnValue(throwError(() => new Error('fail')));
    component.cargarSuscripciones();
    expect(mockToast.error).toHaveBeenCalledWith('No se pudieron cargar las suscripciones.');
  });

  it('should paginate suscripciones', () => {
    expect(component.suscripcionesPaginadas().length).toBe(1);
  });

  it('should compute totalSuscripcionPaginas', () => {
    expect(component.totalSuscripcionPaginas()).toBe(1);
  });

  it('should go to suscripcion page', () => {
    component.irPaginaSuscripcion(2);
    expect(component.suscripcionPagina()).toBe(2);
  });

  it('should not change page beyond limits', () => {
    component.cambiarPaginaSuscripcion(-1);
    expect(component.suscripcionPagina()).toBe(1);
    component.cambiarPaginaSuscripcion(10);
    expect(component.suscripcionPagina()).toBe(1);
  });

  it('should exportarCSV using suscripciones, not planes', () => {
    const exportService = TestBed.inject(ExportService);
    const exportSpy = spyOn(exportService, 'exportCSV');

    component.exportarCSV();

    expect(exportSpy).toHaveBeenCalled();
    const [, headers, rows] = exportSpy.calls.mostRecent().args;
    expect(headers).toEqual(['Tenant', 'Plan', 'Fecha Inicio', 'Fecha Fin', 'Monto', 'Estado']);
    expect(rows).toEqual([['demo', 'Plan Demo', '2026-01-01', '2026-12-31', '29.99', 'activo']]);
  });

  it('should exportarCSV with no rows when there are no suscripciones', () => {
    mockSaas.listSubscriptions.and.returnValue(of({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }));
    component.cargarSuscripciones();
    const exportService = TestBed.inject(ExportService);
    const exportSpy = spyOn(exportService, 'exportCSV');

    component.exportarCSV();

    const [, , rows] = exportSpy.calls.mostRecent().args;
    expect(rows).toEqual([]);
  });

  it('should handle cargarPlanes error', () => {
    mockSaas.listPlans.and.returnValue(throwError(() => new Error('fail')));
    component.cargarPlanes();
    expect(component.error()).toBe('Error al cargar los planes');
  });

  it('should toggleActivo and reload plans on success', () => {
    const plan = component.planes()[0];
    mockSaas.deactivatePlan.calls.reset();
    component.toggleActivo(plan);
    expect(mockSaas.deactivatePlan).toHaveBeenCalledWith(plan.id);
    expect(mockSaas.listPlans).toHaveBeenCalled();
  });

  it('should toggleActivo and show toast on error', () => {
    mockSaas.deactivatePlan.and.returnValue(throwError(() => new Error('fail')));
    const plan = component.planes()[0];
    component.toggleActivo(plan);
    expect(mockToast.show).toHaveBeenCalledWith('Error al cambiar estado del plan.', 'error');
  });

  it('should toggleActivo activate inactive plan and reload', () => {
    const plan = component.planes()[2];
    mockSaas.activatePlan.calls.reset();
    mockSaas.listPlans.calls.reset();
    component.toggleActivo(plan);
    expect(mockSaas.activatePlan).toHaveBeenCalledWith(plan.id);
    expect(mockSaas.listPlans).toHaveBeenCalled();
  });

  it('should toggleActivo activate error and show toast', () => {
    mockSaas.activatePlan.and.returnValue(throwError(() => new Error('fail')));
    const plan = component.planes()[2];
    component.toggleActivo(plan);
    expect(mockToast.show).toHaveBeenCalledWith('Error al cambiar estado del plan.', 'error');
  });

  it('should confirmarEliminar open modal when editPlanId is set', () => {
    component.editPlanId.set(1);
    component.confirmarEliminar();
    expect(mockModal.show).toHaveBeenCalledWith('modalEliminarPlan');
  });

  it('should confirmarEliminar do nothing when editPlanId is null', () => {
    mockModal.show.calls.reset();
    component.editPlanId.set(null);
    component.confirmarEliminar();
    expect(mockModal.show).not.toHaveBeenCalled();
  });

  it('should eliminarPlan do nothing when no editPlanId', () => {
    component.editPlanId.set(null);
    component.eliminarPlan();
    expect(mockSaas.deletePlan).not.toHaveBeenCalled();
  });

  it('should eliminarPlan success and reload', () => {
    component.editPlanId.set(1);
    mockSaas.deletePlan.calls.reset();
    mockSaas.listPlans.calls.reset();
    component.eliminarPlan();
    expect(mockSaas.deletePlan).toHaveBeenCalledWith(1);
    expect(mockModal.hide).toHaveBeenCalledWith('modalEliminarPlan');
    expect(mockToast.show).toHaveBeenCalledWith('Plan eliminado.', 'success');
    expect(mockSaas.listPlans).toHaveBeenCalled();
  });

  it('should eliminarPlan and show toast on error', () => {
    mockSaas.deletePlan.and.returnValue(throwError(() => new Error('fail')));
    component.editPlanId.set(1);
    component.eliminarPlan();
    expect(mockToast.show).toHaveBeenCalledWith('Error al eliminar el plan.', 'error');
  });

  it('should guardarPlan with ANNUAL period using precioAnual as price', () => {
    component.nuevoPlan();
    component.planForm.patchValue({
      nombre: 'Plan Anual', sku: 'PLAN-ANNUAL',
      precioMensual: 100, precioAnual: 1200,
      moneda: 'SOLES', periodo: 'ANNUAL',
    });
    component.guardarPlan();
    expect(mockSaas.createPlan).toHaveBeenCalledWith(jasmine.objectContaining({
      name: 'Plan Anual', price: 1200, billingPeriod: 'ANNUAL',
    }));
  });

  it('should guardarPlan call markAllAsTouched on invalid form', () => {
    component.nuevoPlan();
    component.planForm.patchValue({ nombre: '' });
    spyOn(component.planForm, 'markAllAsTouched');
    component.guardarPlan();
    expect(component.planForm.markAllAsTouched).toHaveBeenCalled();
  });

  it('should precioFinal return 0 when form values are null', () => {
    component.nuevoPlan();
    component.planForm.get('precioMensual')?.setValue(null);
    component.planForm.get('precioAnual')?.setValue(null);
    const result = component.precioFinal();
    expect(result.mensual).toBe(0);
    expect(result.anual).toBe(0);
  });

  it('should modulosActivos return empty when no modulos selected', () => {
    component.nuevoPlan();
    expect(component.modulosActivos()).toEqual([]);
  });

  it('should modulosActivos return all when all modulos selected', () => {
    component.nuevoPlan();
    component.planForm.patchValue({
      modulos: { reportes: true, 'api-externa': true, 'facturacion-auto': true, 'control-huella': true, 'escaneo-codigo': true, 'soporte-24-7': true },
    });
    expect(component.modulosActivos().length).toBe(6);
  });

  it('should reglasActivas return empty when no reglas selected', () => {
    component.nuevoPlan();
    expect(component.reglasActivas()).toEqual([]);
  });

  it('should reglasActivas return all when all reglas selected', () => {
    component.nuevoPlan();
    component.planForm.patchValue({
      reglas: { 'multi-sede': true, 'trial-gratuito': true, 'facturacion-publica': true },
    });
    expect(component.reglasActivas().length).toBe(3);
  });

  it('should verDetalleSuscripcion set selected and open modal', () => {
    const sub = component.suscripciones()[0];
    component.verDetalleSuscripcion(sub);
    expect(component.suscripcionSeleccionada()).toBe(sub);
    expect(mockModal.show).toHaveBeenCalledWith('modalDetalleSuscripcion');
  });

  it('should cerrarDetalleSuscripcion hide modal', () => {
    component.cerrarDetalleSuscripcion();
    expect(mockModal.hide).toHaveBeenCalledWith('modalDetalleSuscripcion');
  });

  it('should irPaginaSuscripcion set page', () => {
    component.irPaginaSuscripcion(3);
    expect(component.suscripcionPagina()).toBe(3);
  });

  it('should SUBSCRIPTION_STATUS_MAP map TRIAL to pendiente', () => {
    mockSaas.listSubscriptions.and.returnValue(of({
      content: [{ id: 'sub-t', tenantId: 't', tenantName: 'trial', planId: 1, planName: 'P',
        dateStart: '2026-01-01', dateEnd: '2026-06-01', status: 'TRIAL', purchasePrice: 0, createdAt: '2026-01-01' }],
      page: 0, size: 100, totalElements: 1, totalPages: 1,
    }));
    component.cargarSuscripciones();
    expect(component.suscripciones()[0].estado).toBe('pendiente');
  });

  it('should SUBSCRIPTION_STATUS_MAP map SUSPENDED to vencido', () => {
    mockSaas.listSubscriptions.and.returnValue(of({
      content: [{ id: 'sub-s', tenantId: 't', tenantName: 'sus', planId: 1, planName: 'P',
        dateStart: '2026-01-01', dateEnd: '2026-06-01', status: 'SUSPENDED', purchasePrice: 0, createdAt: '2026-01-01' }],
      page: 0, size: 100, totalElements: 1, totalPages: 1,
    }));
    component.cargarSuscripciones();
    expect(component.suscripciones()[0].estado).toBe('vencido');
  });

  it('should SUBSCRIPTION_STATUS_MAP map CANCELED to vencido', () => {
    mockSaas.listSubscriptions.and.returnValue(of({
      content: [{ id: 'sub-c', tenantId: 't', tenantName: 'can', planId: 1, planName: 'P',
        dateStart: '2026-01-01', dateEnd: '2026-06-01', status: 'CANCELED', purchasePrice: 0, createdAt: '2026-01-01' }],
      page: 0, size: 100, totalElements: 1, totalPages: 1,
    }));
    component.cargarSuscripciones();
    expect(component.suscripciones()[0].estado).toBe('vencido');
  });

  it('should SUBSCRIPTION_STATUS_MAP map unknown status to pendiente', () => {
    mockSaas.listSubscriptions.and.returnValue(of({
      content: [{ id: 'sub-u', tenantId: 't', tenantName: 'unk', planId: 1, planName: 'P',
        dateStart: '2026-01-01', dateEnd: '2026-06-01', status: 'UNKNOWN_STATUS', purchasePrice: 0, createdAt: '2026-01-01' }],
      page: 0, size: 100, totalElements: 1, totalPages: 1,
    }));
    component.cargarSuscripciones();
    expect(component.suscripciones()[0].estado).toBe('pendiente');
  });

  it('should SUBSCRIPTION_STATUS_MAP handle null planName', () => {
    mockSaas.listSubscriptions.and.returnValue(of({
      content: [{ id: 'sub-n', tenantId: 't', tenantName: 'np', planId: 1, planName: null,
        dateStart: '2026-01-01', dateEnd: null, status: 'ACTIVE', purchasePrice: 0, createdAt: '2026-01-01' }],
      page: 0, size: 100, totalElements: 1, totalPages: 1,
    }));
    component.cargarSuscripciones();
    expect(component.suscripciones()[0].plan).toBe('—');
    expect(component.suscripciones()[0].fechaFin).toBe('—');
  });

  it('should cambiarPaginaSuscripcion not go below 1 with multiple pages', () => {
    mockSaas.listSubscriptions.and.returnValue(of({
      content: Array.from({ length: 12 }, (_, i) => ({
        id: `sub-${i}`, tenantId: `t-${i}`, tenantName: `tenant-${i}`, planId: 1, planName: 'P',
        dateStart: '2026-01-01', dateEnd: '2026-12-31', status: 'ACTIVE', purchasePrice: 10, createdAt: '2026-01-01',
      })),
      page: 0, size: 100, totalElements: 12, totalPages: 1,
    }));
    component.cargarSuscripciones();
    expect(component.totalSuscripcionPaginas()).toBe(3);

    component.suscripcionPagina.set(1);
    component.cambiarPaginaSuscripcion(-1);
    expect(component.suscripcionPagina()).toBe(1);

    component.suscripcionPagina.set(3);
    component.cambiarPaginaSuscripcion(1);
    expect(component.suscripcionPagina()).toBe(3);

    component.suscripcionPagina.set(2);
    component.cambiarPaginaSuscripcion(1);
    expect(component.suscripcionPagina()).toBe(3);

    component.cambiarPaginaSuscripcion(-1);
    expect(component.suscripcionPagina()).toBe(2);
  });

  it('should suscripcionesPaginadas slice correctly across pages', () => {
    mockSaas.listSubscriptions.and.returnValue(of({
      content: Array.from({ length: 12 }, (_, i) => ({
        id: `sub-${i}`, tenantId: `t-${i}`, tenantName: `tenant-${i}`, planId: 1, planName: 'P',
        dateStart: '2026-01-01', dateEnd: '2026-12-31', status: 'ACTIVE', purchasePrice: 10, createdAt: '2026-01-01',
      })),
      page: 0, size: 100, totalElements: 12, totalPages: 1,
    }));
    component.cargarSuscripciones();

    component.suscripcionPagina.set(1);
    expect(component.suscripcionesPaginadas().length).toBe(5);
    expect(component.suscripcionesPaginadas()[0].id).toBe('sub-0');

    component.suscripcionPagina.set(3);
    expect(component.suscripcionesPaginadas().length).toBe(2);
    expect(component.suscripcionesPaginadas()[0].id).toBe('sub-10');
  });

  it('should suscripcionPaginasNum compute correct array', () => {
    mockSaas.listSubscriptions.and.returnValue(of({
      content: Array.from({ length: 12 }, (_, i) => ({
        id: `sub-${i}`, tenantId: `t-${i}`, tenantName: `tenant-${i}`, planId: 1, planName: 'P',
        dateStart: '2026-01-01', dateEnd: '2026-12-31', status: 'ACTIVE', purchasePrice: 10, createdAt: '2026-01-01',
      })),
      page: 0, size: 100, totalElements: 12, totalPages: 1,
    }));
    component.cargarSuscripciones();
    expect(component.suscripcionPaginasNum()).toEqual([1, 2, 3]);
  });

  it('should totalSuscripcionPaginas return 1 when no suscripciones', () => {
    mockSaas.listSubscriptions.and.returnValue(of({
      content: [], page: 0, size: 100, totalElements: 0, totalPages: 0,
    }));
    component.cargarSuscripciones();
    expect(component.totalSuscripcionPaginas()).toBe(1);
    expect(component.suscripcionesPaginadas().length).toBe(0);
  });

  it('should cancelarEliminar close the delete modal', () => {
    component.cancelarEliminar();
    expect(mockModal.hide).toHaveBeenCalledWith('modalEliminarPlan');
  });

  it('should cargarPlanes set loading true then false', () => {
    component.loading.set(false);
    component.cargarPlanes();
    expect(component.loading()).toBeFalse();
  });

  it('should guardarPlan ANNUAL period build features correctly', () => {
    component.nuevoPlan();
    component.planForm.patchValue({
      nombre: 'Plan Full', sku: 'PF', precioMensual: 100, precioAnual: 1000,
      moneda: 'DOLARES', periodo: 'ANNUAL',
      maxTrabajadores: 500, maxSedes: 10, almacenamiento: 100,
      modulos: { reportes: true, 'soporte-24-7': true },
      reglas: { 'multi-sede': true },
    });
    component.guardarPlan();
    expect(mockSaas.createPlan).toHaveBeenCalledWith(jasmine.objectContaining({
      name: 'Plan Full', price: 1000, currency: 'DOLARES', billingPeriod: 'ANNUAL',
      features: jasmine.objectContaining({
        max_trabajadores: 500, max_sedes: 10, almacenamiento_gb: 100,
        reportes: true, 'soporte-24-7': true, 'multi-sede': true,
      }),
    }));
  });

  describe('deep branch coverage - 13 targeted branches', () => {
    it('should handle cargarPlanes with null features on plan', () => {
      mockSaas.listPlans.and.returnValue(of([
        { id: 10, name: 'No Features', price: 10, priceAnnual: 100, currency: 'SOLES', billingPeriod: 'MONTHLY', active: true, createdAt: '2025-01-01', features: null },
      ]));
      component.cargarPlanes();
      expect(component.planes().length).toBe(1);
      expect(component.planes()[0].maxTrabajadores).toBe(100);
      expect(component.planes()[0].maxSedes).toBe(1);
      expect(component.planes()[0].almacenamiento).toBe(5);
    });

    it('should handle cargarPlanes with plan missing feature keys', () => {
      mockSaas.listPlans.and.returnValue(of([
        { id: 11, name: 'Partial', price: 20, priceAnnual: 200, currency: 'SOLES', billingPeriod: 'MONTHLY', active: true, createdAt: '2025-01-01', features: {} },
      ]));
      component.cargarPlanes();
      expect(component.planes()[0].maxTrabajadores).toBe(100);
      expect(component.planes()[0].maxSedes).toBe(1);
      expect(component.planes()[0].almacenamiento).toBe(5);
    });

    it('should handle cargarPlanes with null priceAnnual', () => {
      mockSaas.listPlans.and.returnValue(of([
        { id: 12, name: 'No Annual', price: 30, priceAnnual: null, currency: 'SOLES', billingPeriod: 'MONTHLY', active: true, createdAt: '2025-01-01', features: {} },
      ]));
      component.cargarPlanes();
      expect(component.planes()[0].precioAnual).toBe(0);
    });

    it('should handle guardarPlan with MONTHLY period using precioMensual as price', () => {
      component.nuevoPlan();
      component.planForm.patchValue({
        nombre: 'Monthly Plan', sku: 'MP',
        precioMensual: 75, precioAnual: 900,
        moneda: 'SOLES', periodo: 'MONTHLY',
      });
      component.guardarPlan();
      expect(mockSaas.createPlan).toHaveBeenCalledWith(jasmine.objectContaining({
        name: 'Monthly Plan', price: 75, billingPeriod: 'MONTHLY',
      }));
    });

    it('should handle guardarPlan error on update', () => {
      mockSaas.updatePlan.and.returnValue(throwError(() => new Error('fail')));
      const plan = component.planes()[0];
      component.seleccionarPlan(plan);
      component.guardarPlan();
      expect(mockToast.show).toHaveBeenCalledWith('Error al guardar el plan.', 'error');
    });

    it('should handle guardarPlan with invalid form showing toast', () => {
      component.nuevoPlan();
      component.planForm.patchValue({ nombre: '', sku: '' });
      component.guardarPlan();
      expect(mockToast.show).toHaveBeenCalledWith('Corrige los errores antes de guardar.', 'error');
    });

    it('should handle guardarPlan create success toast', () => {
      component.nuevoPlan();
      component.planForm.patchValue({
        nombre: 'New Plan', sku: 'NP',
        precioMensual: 50, precioAnual: 500,
        moneda: 'SOLES', periodo: 'MONTHLY',
      });
      component.guardarPlan();
      expect(mockToast.show).toHaveBeenCalledWith('Plan creado.', 'success');
    });

    it('should handle guardarPlan update success toast', () => {
      const plan = component.planes()[0];
      component.seleccionarPlan(plan);
      component.guardarPlan();
      expect(mockToast.show).toHaveBeenCalledWith('Plan actualizado.', 'success');
    });

    it('should handle features construction with undefined modulos', () => {
      component.nuevoPlan();
      component.planForm.patchValue({
        nombre: 'Test', sku: 'T',
        precioMensual: 10, precioAnual: 100,
        modulos: undefined,
        reglas: undefined,
      });
      component.guardarPlan();
      expect(mockSaas.createPlan).toHaveBeenCalledWith(jasmine.objectContaining({
        features: jasmine.objectContaining({
          reportes: false, 'api-externa': false,
        }),
      }));
    });

    it('should handle precioFinal with null form values', () => {
      component.nuevoPlan();
      component.planForm.get('precioMensual')?.setValue(null);
      component.planForm.get('precioAnual')?.setValue(null);
      expect(component.precioFinal()).toEqual({ mensual: 0, anual: 0 });
    });

    it('should handle guardarPlan with maxTrabajadores/maxSedes/almacenamiento null', () => {
      component.nuevoPlan();
      component.planForm.patchValue({
        nombre: 'Edge Plan', sku: 'EP',
        precioMensual: 10, precioAnual: 100,
        moneda: 'SOLES', periodo: 'MONTHLY',
      });
      component.planForm.get('maxTrabajadores')?.setValue(null);
      component.planForm.get('maxSedes')?.setValue(null);
      component.planForm.get('almacenamiento')?.setValue(null);
      component.guardarPlan();
      expect(mockSaas.createPlan).toHaveBeenCalledWith(jasmine.objectContaining({
        features: jasmine.objectContaining({
          max_trabajadores: 0, max_sedes: 0, almacenamiento_gb: 0,
        }),
      }));
    });

    it('should handle cargarPlanes sets loading false on success', () => {
      component.loading.set(true);
      component.cargarPlanes();
      expect(component.loading()).toBeFalse();
    });

    it('should handle cargarPlanes sets loading false on error', () => {
      mockSaas.listPlans.and.returnValue(throwError(() => new Error('fail')));
      component.loading.set(true);
      component.cargarPlanes();
      expect(component.loading()).toBeFalse();
      expect(component.error()).toBe('Error al cargar los planes');
    });
  });
});
