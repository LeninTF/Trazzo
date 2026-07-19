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
  const mockExport = jasmine.createSpyObj('ExportService', ['exportCSV', 'escCSV', 'dateSuffix']);

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
        { provide: ExportService, useValue: mockExport },
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
});
