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

  it('should have modulos', () => {
    expect(component.modulos.length).toBeGreaterThan(0);
  });

  it('should have default role selected', () => {
    expect(component.rolSeleccionado).toBe('administrador');
  });

  it('should get rolActual', () => {
    expect(component.rolActual.nombre).toBe('Administrador');
  });

  it('should get permisosActuales for selected role', () => {
    const permisos = component.permisosActuales;
    expect(permisos['gestion-trabajadores.crear']).toBeTrue();
  });

  it('should togglePermiso', () => {
    component.togglePermiso('gestion-trabajadores', 'crear');
    expect(component.permisosActuales['gestion-trabajadores.crear']).toBeFalse();
    component.togglePermiso('gestion-trabajadores', 'crear');
    expect(component.permisosActuales['gestion-trabajadores.crear']).toBeTrue();
  });

  it('should toggleModulo enable all', () => {
    component.toggleModulo('gestion-trabajadores', true);
    const modulo = component.modulos.find(m => m.id === 'gestion-trabajadores')!;
    for (const accion of modulo.acciones) {
      expect(component.permisosActuales[`gestion-trabajadores.${accion.id}`]).toBeTrue();
    }
  });

  it('should toggleModulo disable all', () => {
    component.toggleModulo('gestion-trabajadores', false);
    const modulo = component.modulos.find(m => m.id === 'gestion-trabajadores')!;
    for (const accion of modulo.acciones) {
      expect(component.permisosActuales[`gestion-trabajadores.${accion.id}`]).toBeFalse();
    }
  });

  it('should getEstadoModulo', () => {
    expect(component.getEstadoModulo('gestion-trabajadores')).toBe('completo');
    component.togglePermiso('gestion-trabajadores', 'crear');
    expect(component.getEstadoModulo('gestion-trabajadores')).toBe('parcial');
    component.toggleModulo('gestion-trabajadores', false);
    expect(component.getEstadoModulo('gestion-trabajadores')).toBe('vacio');
  });

  it('should getResumenModulo', () => {
    expect(component.getResumenModulo('gestion-trabajadores')).toBe('4/4');
  });

  it('should modulosVisibles for docente', () => {
    component.seleccionarRol('docente');
    const visibles = component.modulosVisibles();
    expect(visibles.every(m =>
      ['asistencia-docente', 'horarios-docente', 'solicitudes', 'notificaciones-docente', 'perfil'].includes(m.id)
    )).toBeTrue();
  });

  it('should modulosVisibles for admin', () => {
    const visibles = component.modulosVisibles();
    expect(visibles.every(m =>
      !['asistencia-docente', 'horarios-docente', 'solicitudes', 'notificaciones-docente', 'perfil'].includes(m.id)
    )).toBeTrue();
  });

  it('should seleccionarRol', () => {
    component.seleccionarRol('director');
    expect(component.rolSeleccionado).toBe('director');
  });

  it('should restablecer permisos', () => {
    component.togglePermiso('gestion-trabajadores', 'crear');
    component.restablecer();
    expect(component.permisosActuales['gestion-trabajadores.crear']).toBeTrue();
  });

  it('should guardarCambios', () => {
    component.togglePermiso('gestion-trabajadores', 'crear');
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

  it('should eliminarRol with confirm', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    const lenBefore = component.roles.length;
    component.eliminarRol(component.roles[0].id);
    expect(component.roles.length).toBe(lenBefore - 1);
  });

  it('should not eliminarRol without confirm', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    const lenBefore = component.roles.length;
    component.eliminarRol(component.roles[0].id);
    expect(component.roles.length).toBe(lenBefore);
  });
});
