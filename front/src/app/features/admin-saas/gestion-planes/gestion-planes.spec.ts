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
    { id: 1, name: 'Plan Básico', price: 50, currency: 'SOLES', billingPeriod: 'MONTHLY', active: true, createdAt: '2025-01-01T00:00:00' },
    { id: 2, name: 'Plan Pro', price: 100, currency: 'SOLES', billingPeriod: 'MONTHLY', active: true, createdAt: '2025-01-01T00:00:00' },
    { id: 3, name: 'Plan Enterprise', price: 1000, currency: 'SOLES', billingPeriod: 'ANNUAL', active: false, createdAt: '2025-01-01T00:00:00' },
  ];

  const mockSaas = {
    listPlans: jasmine.createSpy('listPlans').and.returnValue(of(mockPlans)),
    listActivePlans: jasmine.createSpy('listActivePlans').and.returnValue(of(mockPlans)),
    getPlan: jasmine.createSpy('getPlan').and.returnValue(of(mockPlans[0])),
    createPlan: jasmine.createSpy('createPlan').and.returnValue(of(mockPlans[0])),
    updatePlan: jasmine.createSpy('updatePlan').and.returnValue(of(mockPlans[0])),
    activatePlan: jasmine.createSpy('activatePlan').and.returnValue(of(mockPlans[0])),
    deactivatePlan: jasmine.createSpy('deactivatePlan').and.returnValue(of(mockPlans[0])),
    deletePlan: jasmine.createSpy('deletePlan').and.returnValue(of(undefined)),
  };

  const mockToast = jasmine.createSpyObj('ToastService', ['show']);
  const mockModal = jasmine.createSpyObj('ModalService', ['show', 'hide']);
  const mockExport = jasmine.createSpyObj('ExportService', ['exportCSV', 'escCSV', 'dateSuffix']);

  beforeEach(async () => {
    Object.values(mockSaas).forEach(spy => spy.calls.reset());
    mockSaas.listPlans.and.returnValue(of(mockPlans));

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
    expect(mockSaas.createPlan).toHaveBeenCalledWith({
      name: 'Plan Test', price: 50, currency: 'SOLES', billingPeriod: 'MONTHLY',
    });
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

  it('should paginate suscripciones (empty)', () => {
    expect(component.suscripcionesPaginadas().length).toBe(0);
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

  it('should exportarCSV', () => {
    component.exportarCSV();
    expect(mockExport.exportCSV).toHaveBeenCalled();
  });

  it('should handle cargarPlanes error', () => {
    mockSaas.listPlans.and.returnValue(throwError(() => new Error('fail')));
    component.cargarPlanes();
    expect(component.error()).toBe('Error al cargar los planes');
  });
});
