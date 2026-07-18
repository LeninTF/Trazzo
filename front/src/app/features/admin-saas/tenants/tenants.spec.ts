import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Tenants } from './tenants';
import { ApiService } from '../../../api/services/api.service';
import { ToastService } from '../../../services/toast.service';
import type { TenantSaasProfile, TenantMetrics, HoldingProfile, SaasPlanResult } from '../../../api/types';

describe('Tenants', () => {
  let component: Tenants;
  let fixture: ComponentFixture<Tenants>;

  const mockTenant = (id: string, status: TenantSaasProfile['status'] = 'ACTIVE'): TenantSaasProfile => ({
    id, subDomain: `tenant-${id}`, holdingId: 10, holdingName: 'Acme SAC', planId: 1, planName: 'Plan Demo',
    status, activatedAt: '2026-01-01T00:00:00', createdAt: '2026-01-01T00:00:00',
  });

  const mockPlan: SaasPlanResult = {
    id: 1, name: 'Plan Demo', price: 29.99, priceAnnual: 299.9, currency: 'SOLES', billingPeriod: 'MONTHLY',
    active: true, createdAt: '2026-01-01T00:00:00', features: { max_trabajadores: 10, 'soporte-24-7': true },
  };

  const mockHolding: HoldingProfile = {
    id: 10, taxId: '20111111111', legalName: 'Acme SAC', type: 'PRIVADO',
    active: true, createdAt: '2026-01-01T00:00:00', updatedAt: '2026-01-01T00:00:00',
  };

  const mockMetrics: TenantMetrics = {
    total: 10, crecimientoPct: 5, activos: 8, porcentajeActivos: 80,
    nuevos30d: 2, nuevosMeta: 30, tasaChurnPct: 1, variacionChurnPct: -0.5,
  };

  const mockListResponse = { content: [mockTenant('1')], page: 0, size: 10, totalElements: 1, totalPages: 1 };

  const mockApi = {
    tenants: {
      list: jasmine.createSpy('list').and.returnValue(of(mockListResponse)),
      getById: jasmine.createSpy('getById'),
      getMetrics: jasmine.createSpy('getMetrics').and.returnValue(of(mockMetrics)),
      createTrial: jasmine.createSpy('createTrial').and.returnValue(of({ id: 'new-1' })),
      suspend: jasmine.createSpy('suspend').and.returnValue(of(mockTenant('1', 'SUSPENDED'))),
      reactivate: jasmine.createSpy('reactivate').and.returnValue(of(mockTenant('1', 'ACTIVE'))),
      updateBranding: jasmine.createSpy('updateBranding').and.returnValue(of(mockTenant('1'))),
      deleteById: jasmine.createSpy('deleteById').and.returnValue(of(undefined)),
    },
    saas: {
      listActivePlans: jasmine.createSpy('listActivePlans').and.returnValue(of([mockPlan])),
      listHoldings: jasmine.createSpy('listHoldings').and.returnValue(of([mockHolding])),
      getPlan: jasmine.createSpy('getPlan').and.returnValue(of(mockPlan)),
    },
  };

  const mockToast = jasmine.createSpyObj('ToastService', ['show', 'success', 'error', 'info']);

  beforeEach(async () => {
    Object.values(mockApi.tenants).forEach(spy => spy.calls.reset());
    Object.values(mockApi.saas).forEach(spy => spy.calls.reset());
    mockApi.tenants.list.and.returnValue(of(mockListResponse));
    mockApi.tenants.getMetrics.and.returnValue(of(mockMetrics));
    mockApi.saas.listActivePlans.and.returnValue(of([mockPlan]));
    mockApi.saas.listHoldings.and.returnValue(of([mockHolding]));
    mockApi.saas.getPlan.and.returnValue(of(mockPlan));

    await TestBed.configureTestingModule({
      imports: [Tenants],
      providers: [
        { provide: ApiService, useValue: mockApi },
        { provide: ToastService, useValue: mockToast },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Tenants);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load tenants, plans and holdings on init', () => {
    expect(mockApi.tenants.list).toHaveBeenCalled();
    expect(mockApi.saas.listActivePlans).toHaveBeenCalled();
    expect(mockApi.saas.listHoldings).toHaveBeenCalled();
    expect(component.tenants().length).toBe(1);
    expect(component.planesDisponibles().length).toBe(1);
    expect(component.holdingsDisponibles().length).toBe(1);
  });

  it('should load metrics on init', () => {
    expect(mockApi.tenants.getMetrics).toHaveBeenCalled();
    expect(component.metricas()?.total).toBe(10);
  });

  it('should handle cargarTenants error', () => {
    mockApi.tenants.list.and.returnValue(throwError(() => new Error('fail')));
    component.cargarTenants();
    expect(component.error()).toBe('No se pudieron cargar los tenants.');
  });

  it('should filtrarTenants reset page and reload', () => {
    component.paginaActual.set(3);
    mockApi.tenants.list.calls.reset();
    component.filtrarTenants();
    expect(component.paginaActual()).toBe(1);
    expect(mockApi.tenants.list).toHaveBeenCalled();
  });

  it('should cambiarPagina within range', () => {
    component.totalPaginas.set(3);
    component.cambiarPagina(2);
    expect(component.paginaActual()).toBe(2);
  });

  it('should not cambiarPagina out of range', () => {
    component.totalPaginas.set(1);
    component.cambiarPagina(0);
    expect(component.paginaActual()).toBe(1);
    component.cambiarPagina(999);
    expect(component.paginaActual()).toBe(1);
  });

  it('should abrirModalAgregar reset the form', () => {
    component.abrirModalAgregar();
    expect(component.modalTenantOpen()).toBeTrue();
    expect(component.nuevoTenantForm.subDomain).toBe('');
  });

  it('should cerrarModalTenant', () => {
    component.abrirModalAgregar();
    component.cerrarModalTenant();
    expect(component.modalTenantOpen()).toBeFalse();
  });

  it('should not guardarNuevoTenant without required fields', () => {
    component.abrirModalAgregar();
    component.guardarNuevoTenant();
    expect(mockApi.tenants.createTrial).not.toHaveBeenCalled();
    expect(mockToast.error).toHaveBeenCalled();
  });

  it('should guardarNuevoTenant with required fields', () => {
    component.abrirModalAgregar();
    component.nuevoTenantForm.subDomain = 'nuevo-tenant';
    component.nuevoTenantForm.holdingId = 10;
    component.nuevoTenantForm.planId = 1;
    component.guardarNuevoTenant();
    expect(mockApi.tenants.createTrial).toHaveBeenCalledWith(jasmine.objectContaining({
      subDomain: 'nuevo-tenant', holdingId: 10, planId: 1,
    }));
    expect(component.modalTenantOpen()).toBeFalse();
  });

  it('should handle guardarNuevoTenant error', () => {
    mockApi.tenants.createTrial.and.returnValue(throwError(() => new Error('fail')));
    component.abrirModalAgregar();
    component.nuevoTenantForm.subDomain = 'nuevo-tenant';
    component.nuevoTenantForm.holdingId = 10;
    component.nuevoTenantForm.planId = 1;
    component.guardarNuevoTenant();
    expect(mockToast.error).toHaveBeenCalledWith('No se pudo registrar el tenant.');
  });

  it('should abrirModalDetalle and load plan features', () => {
    const tenant = mockTenant('1');
    component.abrirModalDetalle(tenant);
    expect(component.modalDetalleOpen()).toBeTrue();
    expect(component.tenantSeleccionado()).toEqual(tenant);
    expect(mockApi.saas.getPlan).toHaveBeenCalledWith(1);
    expect(component.planFeaturesSeleccionado().length).toBeGreaterThan(0);
  });

  it('should cerrarModalDetalle', () => {
    component.abrirModalDetalle(mockTenant('1'));
    component.cerrarModalDetalle();
    expect(component.modalDetalleOpen()).toBeFalse();
    expect(component.tenantSeleccionado()).toBeNull();
  });

  it('should abrirModalBranding and guardarBranding', () => {
    const tenant = mockTenant('1');
    component.abrirModalBranding(tenant);
    expect(component.modalBrandingOpen()).toBeTrue();

    component.brandingForm.slogan = 'Nuevo eslogan';
    component.guardarBranding();
    expect(mockApi.tenants.updateBranding).toHaveBeenCalledWith('1', jasmine.objectContaining({ slogan: 'Nuevo eslogan' }));
    expect(component.modalBrandingOpen()).toBeFalse();
  });

  it('should handle guardarBranding error', () => {
    mockApi.tenants.updateBranding.and.returnValue(throwError(() => new Error('fail')));
    component.abrirModalBranding(mockTenant('1'));
    component.guardarBranding();
    expect(mockToast.error).toHaveBeenCalledWith('No se pudo actualizar el branding.');
  });

  it('should confirmarSuspender and ejecutarSuspension', () => {
    const tenant = mockTenant('1');
    component.confirmarSuspender(tenant);
    expect(component.modalConfirmarSuspensionOpen()).toBeTrue();

    component.ejecutarSuspension();
    expect(mockApi.tenants.suspend).toHaveBeenCalledWith('1');
    expect(component.modalConfirmarSuspensionOpen()).toBeFalse();
  });

  it('should handle ejecutarSuspension error', () => {
    mockApi.tenants.suspend.and.returnValue(throwError(() => new Error('fail')));
    component.confirmarSuspender(mockTenant('1'));
    component.ejecutarSuspension();
    expect(mockToast.error).toHaveBeenCalledWith('No se pudo suspender el tenant.');
  });

  it('should cerrarModalConfirmarSuspension', () => {
    component.confirmarSuspender(mockTenant('1'));
    component.cerrarModalConfirmarSuspension();
    expect(component.modalConfirmarSuspensionOpen()).toBeFalse();
    expect(component.tenantSeleccionado()).toBeNull();
  });

  it('should reactivarTenant', () => {
    component.reactivarTenant(mockTenant('1', 'SUSPENDED'));
    expect(mockApi.tenants.reactivate).toHaveBeenCalledWith('1');
  });

  it('should handle reactivarTenant error', () => {
    mockApi.tenants.reactivate.and.returnValue(throwError(() => new Error('fail')));
    component.reactivarTenant(mockTenant('1', 'SUSPENDED'));
    expect(mockToast.error).toHaveBeenCalledWith('No se pudo reactivar el tenant.');
  });

  it('should eliminarTenant', () => {
    component.eliminarTenant(mockTenant('1'));
    expect(mockApi.tenants.deleteById).toHaveBeenCalledWith('1');
  });

  it('should handle eliminarTenant error', () => {
    mockApi.tenants.deleteById.and.returnValue(throwError(() => new Error('fail')));
    component.eliminarTenant(mockTenant('1'));
    expect(mockToast.error).toHaveBeenCalledWith('No se pudo eliminar el tenant.');
  });

  it('should compute iniciales from holdingName', () => {
    expect(component.iniciales(mockTenant('1'))).toBe('AS');
  });

  it('should compute iniciales from subDomain when no holdingName', () => {
    const tenant = { ...mockTenant('1'), holdingName: null };
    expect(component.iniciales(tenant)).toBe('T1');
  });

  it('should compute a deterministic color for a tenant', () => {
    const tenant = mockTenant('1');
    expect(component.colorFor(tenant)).toBe(component.colorFor(tenant));
  });

  it('should compute mesesDesde from createdAt', () => {
    const fecha = new Date();
    fecha.setMonth(fecha.getMonth() - 3);
    expect(component.mesesDesde(fecha.toISOString())).toBeGreaterThanOrEqual(2);
  });
});
