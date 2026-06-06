import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Tenants } from './tenants';

describe('Tenants', () => {
  let component: Tenants;
  let fixture: ComponentFixture<Tenants>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Tenants],
    }).compileComponents();

    fixture = TestBed.createComponent(Tenants);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 10 initial tenants', () => {
    expect(component.tenants.length).toBe(10);
  });

  it('should filter by searchTerm', () => {
    component.searchTerm = 'acme';
    expect(component.tenantsFiltrado.length).toBe(1);
    expect(component.tenantsFiltrado[0].nombre).toBe('Acme Corp');
  });

  it('should filter by plan', () => {
    component.filtroPlan = 'ENTERPRISE';
    const count = component.tenants.filter(t => t.plan === 'ENTERPRISE').length;
    expect(component.tenantsFiltrado.length).toBe(count);
  });

  it('should filter by estado', () => {
    component.filtroEstado = 'Activo';
    const count = component.tenants.filter(t => t.estado === 'Activo').length;
    expect(component.tenantsFiltrado.length).toBe(count);
  });

  it('should filter by industria', () => {
    component.filtroIndustria = 'Tecnología';
    const count = component.tenants.filter(t => t.industria === 'Tecnología').length;
    expect(component.tenantsFiltrado.length).toBe(count);
  });

  it('should combine multiple filters', () => {
    component.searchTerm = 'wayne';
    component.filtroEstado = 'Suspendido';
    expect(component.tenantsFiltrado.length).toBe(1);
  });

  it('should compute tenantsPaginado', () => {
    expect(component.tenantsPaginado.length).toBe(10);
    component.itemsPerPage = 3;
    expect(component.tenantsPaginado.length).toBe(3);
  });

  it('should compute totalPaginas', () => {
    expect(component.totalPaginas).toBe(1);
    component.itemsPerPage = 3;
    expect(component.totalPaginas).toBe(4);
  });

  it('should compute inicioRegistro and finRegistro', () => {
    expect(component.inicioRegistro).toBe(1);
    expect(component.finRegistro).toBe(10);
  });

  it('should filtrarTenants reset page', () => {
    component.paginaActual = 3;
    component.filtrarTenants();
    expect(component.paginaActual).toBe(1);
  });

  it('should cambiarPagina within range', () => {
    component.cambiarPagina(1);
    expect(component.paginaActual).toBe(1);
  });

  it('should not cambiarPagina out of range', () => {
    component.cambiarPagina(0);
    expect(component.paginaActual).toBe(1);
    component.cambiarPagina(999);
    expect(component.paginaActual).toBe(1);
  });

  it('should abrirModalAgregar', () => {
    component.abrirModalAgregar();
    expect(component.editandoTenant).toBeFalse();
    expect(component.modalTenantOpen).toBeTrue();
    expect(component.tenantForm.nombre).toBe('');
  });

  it('should abrirModalEditar', () => {
    component.abrirModalEditar(component.tenants[0]);
    expect(component.editandoTenant).toBeTrue();
    expect(component.tenantForm.nombre).toBe('Acme Corp');
  });

  it('should cerrarModalTenant', () => {
    component.abrirModalAgregar();
    component.cerrarModalTenant();
    expect(component.modalTenantOpen).toBeFalse();
  });

  it('should guardarTenant create new', () => {
    component.abrirModalAgregar();
    component.tenantForm.nombre = 'New Tenant';
    component.tenantForm.idTenant = 'new-tenant';
    component.guardarTenant();
    expect(component.tenants.length).toBe(11);
    expect(component.tenants.find(t => t.nombre === 'New Tenant')).toBeTruthy();
  });

  it('should not guardarTenant without required fields', () => {
    component.abrirModalAgregar();
    component.guardarTenant();
    expect(component.tenants.length).toBe(10);
  });

  it('should guardarTenant update existing', () => {
    component.abrirModalEditar(component.tenants[0]);
    component.tenantForm.nombre = 'Updated Corp';
    component.guardarTenant();
    expect(component.tenants[0].nombre).toBe('Updated Corp');
  });

  it('should abrirModalDetalle', () => {
    component.abrirModalDetalle(component.tenants[0]);
    expect(component.modalDetalleOpen).toBeTrue();
    expect(component.tenantSeleccionado?.nombre).toBe('Acme Corp');
  });

  it('should cerrarModalDetalle', () => {
    component.abrirModalDetalle(component.tenants[0]);
    component.cerrarModalDetalle();
    expect(component.modalDetalleOpen).toBeFalse();
    expect(component.tenantSeleccionado).toBeNull();
  });

  it('should editarDesdeDetalle', () => {
    component.abrirModalDetalle(component.tenants[0]);
    component.editarDesdeDetalle();
    expect(component.modalDetalleOpen).toBeFalse();
    expect(component.modalTenantOpen).toBeTrue();
  });

  it('should eliminarTenant with confirm', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    component.eliminarTenant(1);
    expect(component.tenants.length).toBe(9);
  });

  it('should not eliminarTenant without confirm', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    component.eliminarTenant(1);
    expect(component.tenants.length).toBe(10);
  });

  it('should abrirModalConfiguracion', () => {
    component.abrirModalConfiguracion(component.tenants[0]);
    expect(component.modalConfiguracionOpen).toBeTrue();
    expect(component.tenantSeleccionado?.nombre).toBe('Acme Corp');
  });

  it('should cerrarModalConfiguracion', () => {
    component.abrirModalConfiguracion(component.tenants[0]);
    component.cerrarModalConfiguracion();
    expect(component.modalConfiguracionOpen).toBeFalse();
  });

  it('should toggleBiometrico', () => {
    const wasEnabled = component.configuracion.biometricoHabilitado;
    component.toggleBiometrico();
    expect(component.configuracion.biometricoHabilitado).toBe(!wasEnabled);
  });

  it('should toggleEscaneoBarras', () => {
    const wasEnabled = component.configuracion.escaneoBarrasHabilitado;
    component.toggleEscaneoBarras();
    expect(component.configuracion.escaneoBarrasHabilitado).toBe(!wasEnabled);
  });

  it('should guardarConfiguracion', () => {
    component.abrirModalConfiguracion(component.tenants[0]);
    component.guardarConfiguracion();
    expect(component.modalConfiguracionOpen).toBeFalse();
  });

  it('should suspenderTenant and confirmarSuspenderTenant', () => {
    component.abrirModalConfiguracion(component.tenants[0]);
    component.suspenderTenant();
    expect(component.modalConfirmarSuspensionOpen).toBeTrue();
    component.confirmarSuspenderTenant();
    expect(component.tenants[0].estado).toBe('Suspendido');
  });

  it('should cerrarModalConfirmarSuspension', () => {
    component.suspenderTenant();
    component.cerrarModalConfirmarSuspension();
    expect(component.modalConfirmarSuspensionOpen).toBeFalse();
    expect(component.motivoSuspension).toBe('');
  });

  it('should eliminarTenantConfig with confirm', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    component.abrirModalConfiguracion(component.tenants[0]);
    component.eliminarTenantConfig();
    expect(component.tenants.length).toBe(9);
  });
});
