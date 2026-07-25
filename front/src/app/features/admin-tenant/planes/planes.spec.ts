import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Planes } from './planes';
import { ApiService } from '../../../api/services/api.service';
import { ToastService } from '../../../services/toast.service';
import { RedirectService } from '../../../services/redirect.service';
import type { SaasPlanResult, InvoiceProfile } from '../../../api/types';

describe('Planes (tenant)', () => {
  let component: Planes;
  let fixture: ComponentFixture<Planes>;

  const mockPlanActual: SaasPlanResult = {
    id: 2, name: 'Professional', price: 129, priceAnnual: 1290, currency: 'SOLES', billingPeriod: 'MONTHLY',
    active: true, createdAt: '2026-01-01', features: { max_trabajadores: 1000, max_sedes: 5, almacenamiento_gb: 50 },
  };
  const mockPlanStarter: SaasPlanResult = {
    id: 1, name: 'Starter', price: 49, priceAnnual: 490, currency: 'SOLES', billingPeriod: 'MONTHLY',
    active: true, createdAt: '2026-01-01', features: { max_trabajadores: 100 },
  };

  const mockFactura = (id: string, paymentStatus: string): InvoiceProfile => ({
    id, tenantId: 'tenant-1', clientName: 'Cliente Test', clientTaxId: '20123456789',
    invoiceSeries: 'F001', consecutiveNumber: '1', voucherType: 'FACTURA',
    subTotal: 100, taxAmount: 18, total: 118, paymentStatus, expirationDate: null, createdAt: '2026-05-01',
  });

  let mockOrg: { getMyPlan: jasmine.Spy; listAvailablePlans: jasmine.Spy; listMyInvoices: jasmine.Spy; subscribeToPlan: jasmine.Spy };
  let mockUsers: { list: jasmine.Spy };
  let mockToast: jasmine.SpyObj<ToastService>;
  let mockRedirect: { redirectTo: jasmine.Spy };

  beforeEach(async () => {
    mockOrg = {
      getMyPlan: jasmine.createSpy('getMyPlan').and.returnValue(of(mockPlanActual)),
      listAvailablePlans: jasmine.createSpy('listAvailablePlans').and.returnValue(of([mockPlanStarter, mockPlanActual])),
      listMyInvoices: jasmine.createSpy('listMyInvoices').and.returnValue(of({
        content: [mockFactura('inv-1', 'COMPLETADO'), mockFactura('inv-2', 'PENDIENTE')],
        page: 0, size: 10, totalElements: 2, totalPages: 1,
      })),
      subscribeToPlan: jasmine.createSpy('subscribeToPlan').and.returnValue(of({ subscriptionId: 'sub-1', initPoint: 'https://mp/init' })),
    };
    mockUsers = { list: jasmine.createSpy('list').and.returnValue(of({ content: [], page: 1, size: 1, totalElements: 42, totalPages: 42 })) };
    mockToast = jasmine.createSpyObj('ToastService', ['show', 'success', 'error', 'info']);
    mockRedirect = { redirectTo: jasmine.createSpy('redirectTo') };

    await TestBed.configureTestingModule({
      imports: [Planes],
      providers: [
        { provide: ApiService, useValue: { org: mockOrg, users: mockUsers } },
        { provide: ToastService, useValue: mockToast },
        { provide: RedirectService, useValue: mockRedirect },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Planes);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load plan actual, planes, facturas and usuarios activos from the backend', () => {
    expect(mockOrg.getMyPlan).toHaveBeenCalled();
    expect(mockOrg.listAvailablePlans).toHaveBeenCalled();
    expect(mockOrg.listMyInvoices).toHaveBeenCalled();
    expect(mockUsers.list).toHaveBeenCalled();
    expect(component.planActual()?.name).toBe('Professional');
    expect(component.planes()).toHaveSize(2);
    expect(component.facturas()).toHaveSize(2);
    expect(component.usuariosActivos()).toBe(42);
  });

  it('should set error when loading fails', () => {
    mockOrg.getMyPlan.and.returnValue(throwError(() => new Error('fail')));
    component['cargarDatos']();
    expect(component.error()).toBe('No se pudo cargar la información de planes y facturación.');
  });

  it('should compute limiteUsuarios from planActual features', () => {
    expect(component.limiteUsuarios).toBe(1000);
  });

  it('should compute porcentajeUsuarios', () => {
    expect(component.porcentajeUsuarios).toBe(Math.round((42 / 1000) * 100));
  });

  it('should return featureLabel entries for a plan', () => {
    const labels = component.featureLabel(mockPlanActual);
    expect(labels).toContain('Hasta 1000 empleados');
    expect(labels).toContain('Hasta 5 sedes');
    expect(labels).toContain('50 GB de almacenamiento');
  });

  it('should seleccionarPlan', () => {
    component.seleccionarPlan(mockPlanStarter);
    expect(component.planSeleccionadoId).toBe(1);
  });

  it('should get planSeleccionado', () => {
    component.seleccionarPlan(mockPlanStarter);
    expect(component.planSeleccionado?.name).toBe('Starter');
  });

  it('should seleccionarFactura', () => {
    component.seleccionarFactura(component.facturas()[0]);
    expect(component.facturaSeleccionadaId).toBe('inv-1');
    expect(component.modalFacturaOpen).toBeTrue();
  });

  it('should get facturaSeleccionada', () => {
    component.seleccionarFactura(component.facturas()[0]);
    expect(component.facturaSeleccionada?.id).toBe('inv-1');
  });

  it('should abrirModalActualizarPlan and default to a plan other than the current one', () => {
    component.abrirModalActualizarPlan();
    expect(component.modalActualizarOpen).toBeTrue();
    expect(component.planSeleccionadoId).toBe(1);
  });

  it('should cerrarModalActualizar', () => {
    component.abrirModalActualizarPlan();
    component.cerrarModalActualizar();
    expect(component.modalActualizarOpen).toBeFalse();
  });

  it('should confirmarActualizarPlan call subscribeToPlan and redirect to the initPoint', () => {
    component.abrirModalActualizarPlan();
    component.confirmarActualizarPlan();

    expect(mockOrg.subscribeToPlan).toHaveBeenCalledWith(component.planSeleccionadoId);
    expect(mockRedirect.redirectTo).toHaveBeenCalledWith('https://mp/init');
    expect(component.modalActualizarOpen).toBeFalse();
  });

  it('should confirmarActualizarPlan show an error toast when subscribeToPlan fails', () => {
    mockOrg.subscribeToPlan.and.returnValue(throwError(() => new Error('fail')));

    component.abrirModalActualizarPlan();
    component.confirmarActualizarPlan();

    expect(mockToast.error).toHaveBeenCalled();
    expect(component.modalActualizarOpen).toBeFalse();
  });

  it('should cerrarModalFactura', () => {
    component.seleccionarFactura(component.facturas()[0]);
    component.cerrarModalFactura();
    expect(component.modalFacturaOpen).toBeFalse();
    expect(component.facturaSeleccionadaId).toBeNull();
  });

  it('should exportarFacturas show an informational toast', () => {
    component.exportarFacturas();
    expect(mockToast.info).toHaveBeenCalled();
  });

  it('should descargarFactura show an informational toast', () => {
    component.descargarFactura();
    expect(mockToast.info).toHaveBeenCalled();
  });

  it('should return undefined for planSeleccionado when planSeleccionadoId is null', () => {
    component.planSeleccionadoId = null;
    expect(component.planSeleccionado).toBeUndefined();
  });

  it('should return undefined for facturaSeleccionada when facturaSeleccionadaId is null', () => {
    component.facturaSeleccionadaId = null;
    expect(component.facturaSeleccionada).toBeUndefined();
  });

  it('should return null for limiteUsuarios when max is not a number', () => {
    (component as any).planActual.set({ ...mockPlanActual, features: { max_trabajadores: 'unlimited' } });
    expect(component.limiteUsuarios).toBeNull();
  });

  it('should return null for porcentajeUsuarios when limite is null', () => {
    component.planActual.set({ ...mockPlanActual, features: {} });
    expect(component.porcentajeUsuarios).toBeNull();
  });

  it('should default to planActual in abrirModalActualizarPlan when no alternate plan exists', () => {
    component.planes.set([mockPlanActual]);
    component.planSeleccionadoId = null;
    component.abrirModalActualizarPlan();
    expect((component as any).planSeleccionadoId).toBe(mockPlanActual.id);
    expect(component.modalActualizarOpen).toBeTrue();
  });

  it('should return only present features in featureLabel', () => {
    const labels = component.featureLabel(mockPlanStarter);
    expect(labels).toContain('Hasta 100 empleados');
    expect(labels.length).toBe(1);
  });
});
