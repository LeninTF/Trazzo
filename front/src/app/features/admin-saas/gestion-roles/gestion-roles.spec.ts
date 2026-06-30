import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { GestionRoles } from './gestion-roles';

describe('GestionRoles', () => {
  let component: GestionRoles;
  let fixture: ComponentFixture<GestionRoles>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GestionRoles, FormsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(GestionRoles);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 5 initial roles', () => {
    expect(component.roles.length).toBe(5);
  });

  it('should have 4 modulos', () => {
    expect(component.modulos.length).toBe(4);
  });

  it('should have default role selected', () => {
    expect(component.rolSeleccionado).toBe('super-administrador');
  });

  it('should get rolActual', () => {
    expect(component.rolActual.nombre).toBe('Super Administrador');
  });

  it('should get permisosActuales for selected role', () => {
    const permisos = component.permisosActuales;
    expect(permisos['gestion-tenants.crear']).toBeTrue();
  });

  it('should togglePermiso', () => {
    component.togglePermiso('gestion-tenants', 'crear');
    expect(component.permisosActuales['gestion-tenants.crear']).toBeFalse();
    component.togglePermiso('gestion-tenants', 'crear');
    expect(component.permisosActuales['gestion-tenants.crear']).toBeTrue();
  });

  it('should toggleModulo enable all', () => {
    component.toggleModulo('gestion-tenants', true);
    const modulo = component.modulos.find(m => m.id === 'gestion-tenants')!;
    for (const accion of modulo.acciones) {
      expect(component.permisosActuales[`gestion-tenants.${accion.id}`]).toBeTrue();
    }
  });

  it('should toggleModulo disable all', () => {
    component.toggleModulo('gestion-tenants', false);
    const modulo = component.modulos.find(m => m.id === 'gestion-tenants')!;
    for (const accion of modulo.acciones) {
      expect(component.permisosActuales[`gestion-tenants.${accion.id}`]).toBeFalse();
    }
  });

  it('should getEstadoModulo', () => {
    expect(component.getEstadoModulo('gestion-tenants')).toBe('completo');
    component.togglePermiso('gestion-tenants', 'crear');
    expect(component.getEstadoModulo('gestion-tenants')).toBe('parcial');
    component.toggleModulo('gestion-tenants', false);
    expect(component.getEstadoModulo('gestion-tenants')).toBe('vacio');
  });

  it('should getResumenModulo', () => {
    expect(component.getResumenModulo('gestion-tenants')).toBe('8/8');
  });

  it('should seleccionarRol', () => {
    component.seleccionarRol('soporte');
    expect(component.rolSeleccionado).toBe('soporte');
  });

  it('should restablecer permisos', () => {
    component.togglePermiso('gestion-tenants', 'crear');
    component.restablecer();
    expect(component.permisosActuales['gestion-tenants.crear']).toBeTrue();
  });

  it('should guardarCambios', () => {
    component.togglePermiso('gestion-tenants', 'crear');
    component.guardarCambios();
    expect(component.mensajeGuardado).toBeTrue();
  });

  it('should abrirModalNuevoRol', () => {
    component.abrirModalNuevoRol();
    expect(component.mostrarModalRol).toBeTrue();
    expect(component.editandoRol).toBeNull();
  });

  it('should cerrarModalNuevoRol', () => {
    component.abrirModalNuevoRol();
    component.cerrarModalNuevoRol();
    expect(component.mostrarModalRol).toBeFalse();
  });

  it('should guardarRol create new', () => {
    component.abrirModalNuevoRol();
    component.nuevoRolNombre = 'Test Rol';
    component.guardarRol();
    expect(component.roles.length).toBe(6);
    expect(component.roles.find(r => r.nombre === 'Test Rol')).toBeTruthy();
    expect(component.rolSeleccionado).toBe('test-rol');
  });

  it('should not agregarRol with empty name', () => {
    component.abrirModalNuevoRol();
    component.guardarRol();
    expect(component.roles.length).toBe(5);
  });

  it('should guardarEdicionRol', () => {
    component.abrirModalEditarRol(component.roles[0]);
    component.nuevoRolNombre = 'Rol Editado';
    component.guardarRol();
    expect(component.roles[0].nombre).toBe('Rol Editado');
  });

  it('should eliminarRol', () => {
    const lenBefore = component.roles.length;
    component.eliminarRol(component.roles[0].id);
    expect(component.roles.length).toBe(lenBefore - 1);
  });
});
