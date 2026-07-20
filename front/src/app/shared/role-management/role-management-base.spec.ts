import {
  togglePermisoAccion,
  toggleModuloAcciones,
  estadoModulo,
  resumenModulo,
  restablecerPermisos,
  RoleMatrixComponent,
  Modulo,
} from './role-management-base';

class TestRoleMatrix extends RoleMatrixComponent {
  readonly modulos: Modulo[] = [
    { id: 'mod-a', nombre: 'Mod A', icono: 'bi-a', acciones: [
      { id: 'accion-1', nombre: 'Accion 1', icono: 'bi-1' },
      { id: 'accion-2', nombre: 'Accion 2', icono: 'bi-2' },
    ]},
    { id: 'mod-b', nombre: 'Mod B', icono: 'bi-b', acciones: [
      { id: 'accion-3', nombre: 'Accion 3', icono: 'bi-3' },
    ]},
  ];
  guardarCambios(): void {}
  guardarRol(): void {}
  eliminarRol(_rolId: string): void {}
}

describe('role-management-base standalone functions', () => {
  const modulos: Modulo[] = [
    { id: 'gestion', nombre: 'Gestion', icono: 'bi-x', acciones: [
      { id: 'crear', nombre: 'Crear', icono: 'bi-plus' },
      { id: 'editar', nombre: 'Editar', icono: 'bi-pencil' },
    ]},
  ];

  const permisos: Record<string, Record<string, boolean>> = {
    'role-1': { 'gestion.crear': true, 'gestion.editar': false },
  };

  it('togglePermisoAccion should toggle a permission value', () => {
    expect(permisos['role-1']['gestion.crear']).toBeTrue();
    togglePermisoAccion(permisos, 'role-1', 'gestion', 'crear');
    expect(permisos['role-1']['gestion.crear']).toBeFalse();
    togglePermisoAccion(permisos, 'role-1', 'gestion', 'crear');
    expect(permisos['role-1']['gestion.crear']).toBeTrue();
  });

  it('toggleModuloAcciones should set all actions of a modulo', () => {
    toggleModuloAcciones(modulos, permisos, 'role-1', 'gestion', true);
    expect(permisos['role-1']['gestion.crear']).toBeTrue();
    expect(permisos['role-1']['gestion.editar']).toBeTrue();
  });

  it('toggleModuloAcciones should early return for non-existent modulo', () => {
    toggleModuloAcciones(modulos, permisos, 'role-1', 'noexiste', true);
    expect(permisos['role-1']['gestion.crear']).toBeTrue();
  });

  it('estadoModulo should return completo when all actions active', () => {
    expect(estadoModulo(modulos, permisos, 'role-1', 'gestion')).toBe('completo');
  });

  it('estadoModulo should return parcial when some actions active', () => {
    permisos['role-1']['gestion.crear'] = true;
    permisos['role-1']['gestion.editar'] = false;
    expect(estadoModulo(modulos, permisos, 'role-1', 'gestion')).toBe('parcial');
  });

  it('estadoModulo should return vacio when no actions active', () => {
    permisos['role-1']['gestion.crear'] = false;
    permisos['role-1']['gestion.editar'] = false;
    expect(estadoModulo(modulos, permisos, 'role-1', 'gestion')).toBe('vacio');
  });

  it('estadoModulo should return vacio for non-existent modulo', () => {
    expect(estadoModulo(modulos, permisos, 'role-1', 'noexiste')).toBe('vacio');
  });

  it('resumenModulo should return activos/total', () => {
    permisos['role-1']['gestion.crear'] = true;
    permisos['role-1']['gestion.editar'] = false;
    expect(resumenModulo(modulos, permisos, 'role-1', 'gestion')).toBe('1/2');
  });

  it('resumenModulo should return 0/0 for non-existent modulo', () => {
    expect(resumenModulo(modulos, permisos, 'role-1', 'noexiste')).toBe('0/0');
  });

  it('restablecerPermisos should copy backup values back', () => {
    permisos['role-1']['gestion.crear'] = false;
    permisos['role-1']['gestion.editar'] = true;
    const backup: Record<string, Record<string, boolean>> = {
      'role-1': { 'gestion.crear': true, 'gestion.editar': false },
    };
    restablecerPermisos(permisos, backup, 'role-1');
    expect(permisos['role-1']['gestion.crear']).toBeTrue();
    expect(permisos['role-1']['gestion.editar']).toBeFalse();
  });
});

describe('RoleMatrixComponent', () => {
  let component: TestRoleMatrix;

  beforeEach(() => {
    component = new TestRoleMatrix();
    component.permisos = {
      'role-1': { 'mod-a.accion-1': true, 'mod-a.accion-2': false, 'mod-b.accion-3': true },
      'role-2': { 'mod-a.accion-1': false, 'mod-a.accion-2': false, 'mod-b.accion-3': false },
    };
    component.respaldoPermisos = {
      'role-1': { 'mod-a.accion-1': true, 'mod-a.accion-2': false, 'mod-b.accion-3': true },
      'role-2': { 'mod-a.accion-1': false, 'mod-a.accion-2': false, 'mod-b.accion-3': false },
    };
    component.roles = [
      { id: 'role-1', nombre: 'Admin', descripcion: 'desc', color: '#000', icono: 'bi-shield' },
      { id: 'role-2', nombre: 'Viewer', descripcion: '', color: '#111', icono: 'bi-eye' },
    ];
    component.rolSeleccionado = 'role-1';
  });

  it('rolActual should return matching role', () => {
    expect(component.rolActual.id).toBe('role-1');
    expect(component.rolActual.nombre).toBe('Admin');
  });

  it('rolActual should fall back to roles[0] when rolSeleccionado has no match', () => {
    component.rolSeleccionado = 'nonexistent';
    expect(component.rolActual.id).toBe('role-1');
  });

  it('permisosActuales should return permissions for selected role', () => {
    expect(component.permisosActuales['mod-a.accion-1']).toBeTrue();
    expect(component.permisosActuales['mod-a.accion-2']).toBeFalse();
  });

  it('permisosActuales should return empty object for unknown role', () => {
    component.rolSeleccionado = 'unknown-role';
    expect(component.permisosActuales).toEqual({});
  });

  it('togglePermiso should toggle the permission value', () => {
    component.togglePermiso('mod-a', 'accion-2');
    expect(component.permisosActuales['mod-a.accion-2']).toBeTrue();
    component.togglePermiso('mod-a', 'accion-2');
    expect(component.permisosActuales['mod-a.accion-2']).toBeFalse();
  });

  it('toggleModulo should set all actions in a modulo', () => {
    component.toggleModulo('mod-a', true);
    expect(component.permisosActuales['mod-a.accion-1']).toBeTrue();
    expect(component.permisosActuales['mod-a.accion-2']).toBeTrue();
  });

  it('getEstadoModulo should return correct state', () => {
    expect(component.getEstadoModulo('mod-a')).toBe('parcial');
    component.toggleModulo('mod-a', true);
    expect(component.getEstadoModulo('mod-a')).toBe('completo');
    component.toggleModulo('mod-a', false);
    expect(component.getEstadoModulo('mod-a')).toBe('vacio');
  });

  it('getResumenModulo should return activos/total', () => {
    expect(component.getResumenModulo('mod-a')).toBe('1/2');
    expect(component.getResumenModulo('mod-b')).toBe('1/1');
  });

  it('seleccionarRol should change role and reset mensajeGuardado', () => {
    component.mensajeGuardado = true;
    component.seleccionarRol('role-2');
    expect(component.rolSeleccionado).toBe('role-2');
    expect(component.mensajeGuardado).toBeFalse();
  });

  it('restablecer should revert permissions from backup', () => {
    component.togglePermiso('mod-a', 'accion-2');
    expect(component.permisosActuales['mod-a.accion-2']).toBeTrue();
    component.restablecer();
    expect(component.permisosActuales['mod-a.accion-2']).toBeFalse();
  });

  it('abrirModalNuevoRol should reset form state', () => {
    component.editandoRol = component.roles[0];
    component.nuevoRolNombre = 'old';
    component.abrirModalNuevoRol();
    expect(component.mostrarModalRol).toBeTrue();
    expect(component.editandoRol).toBeNull();
    expect(component.nuevoRolNombre).toBe('');
    expect(component.nuevoRolDescripcion).toBe('');
  });

  it('abrirModalEditarRol should prefill form with role data', () => {
    component.abrirModalEditarRol(component.roles[0]);
    expect(component.mostrarModalRol).toBeTrue();
    expect(component.editandoRol).toBe(component.roles[0]);
    expect(component.nuevoRolNombre).toBe('Admin');
    expect(component.nuevoRolDescripcion).toBe('desc');
  });

  it('cerrarModalNuevoRol should close modal and clear editandoRol', () => {
    component.abrirModalNuevoRol();
    component.cerrarModalNuevoRol();
    expect(component.mostrarModalRol).toBeFalse();
    expect(component.editandoRol).toBeNull();
  });

  it('modulosVisibles should return all modulos by default', () => {
    expect(component.modulosVisibles()).toHaveSize(2);
  });
});
