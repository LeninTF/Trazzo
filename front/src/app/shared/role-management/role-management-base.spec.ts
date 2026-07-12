import { fakeAsync, tick } from '@angular/core/testing';
import { BaseGestionRoles } from './role-management-base';
import type { Rol, Modulo } from './role-management-base';

class InstanciaRoles extends BaseGestionRoles {
  roles: Rol[] = [
    { id: 'admin', nombre: 'Admin', descripcion: 'Full access', color: '#ff0000', icono: 'bi-shield' },
    { id: 'user', nombre: 'User', descripcion: 'Basic access', color: '#00ff00', icono: 'bi-person' },
  ];

  modulos: Modulo[] = [
    {
      id: 'dashboard',
      nombre: 'Dashboard',
      icono: 'bi-grid',
      acciones: [
        { id: 'view', nombre: 'Ver', icono: 'bi-eye' },
        { id: 'export', nombre: 'Exportar', icono: 'bi-download' },
      ],
    },
    {
      id: 'users',
      nombre: 'Usuarios',
      icono: 'bi-people',
      acciones: [
        { id: 'create', nombre: 'Crear', icono: 'bi-plus' },
        { id: 'edit', nombre: 'Editar', icono: 'bi-pencil' },
        { id: 'delete', nombre: 'Eliminar', icono: 'bi-trash' },
      ],
    },
  ];

  protected readonly PERMISOS_DEFAULT: Record<string, Record<string, boolean>> = {
    admin: {
      'dashboard.view': true,
      'dashboard.export': true,
      'users.create': true,
      'users.edit': true,
      'users.delete': true,
    },
    user: {
      'dashboard.view': true,
      'dashboard.export': false,
    },
  };

  constructor() {
    super();
    this.inicializarPermisos();
    this.rolSeleccionado = 'admin';
  }
}

describe('BaseGestionRoles', () => {
  let component: InstanciaRoles;

  beforeEach(() => {
    component = new InstanciaRoles();
  });

  it('sets default signal values on construction', () => {
    expect(component.loading()).toBeFalse();
    expect(component.error()).toBe('');
  });

  it('provides 2 test roles', () => {
    expect(component.roles.length).toBe(2);
  });

  it('provides 2 test modules', () => {
    expect(component.modulos.length).toBe(2);
  });

  describe('inicializarPermisos', () => {
    it('should initialize permissions from defaults', () => {
      expect(component.permisos['admin']['dashboard.view']).toBeTrue();
      expect(component.permisos['admin']['dashboard.export']).toBeTrue();
      expect(component.permisos['admin']['users.create']).toBeTrue();
      expect(component.permisos['user']['dashboard.view']).toBeTrue();
      expect(component.permisos['user']['dashboard.export']).toBeFalse();
    });
  });

  describe('rolActual', () => {
    it('should return the selected role', () => {
      expect(component.rolActual.id).toBe('admin');
    });

    it('should return first role if selected role not found', () => {
      component.rolSeleccionado = 'nonexistent';
      expect(component.rolActual.id).toBe('admin');
    });
  });

  describe('permisosActuales', () => {
    it('should return permissions for selected role', () => {
      component.rolSeleccionado = 'user';
      expect(component.permisosActuales['dashboard.view']).toBeTrue();
      expect(component.permisosActuales['dashboard.export']).toBeFalse();
    });

    it('should return empty object for unknown role', () => {
      component.rolSeleccionado = 'unknown';
      expect(component.permisosActuales).toEqual({});
    });
  });

  describe('modulosVisibles', () => {
    it('should return all modulos', () => {
      expect(component.modulosVisibles()).toEqual(component.modulos);
    });
  });

  describe('togglePermiso', () => {
    it('should toggle a single permission', () => {
      const initial = component.permisos['admin']['dashboard.view'];
      component.togglePermiso('dashboard', 'view');
      expect(component.permisos['admin']['dashboard.view']).toBe(!initial);
    });
  });

  describe('toggleModulo', () => {
    it('should set all module actions to true', () => {
      component.rolSeleccionado = 'user';
      component.toggleModulo('users', true);
      expect(component.permisos['user']['users.create']).toBeTrue();
      expect(component.permisos['user']['users.edit']).toBeTrue();
      expect(component.permisos['user']['users.delete']).toBeTrue();
    });

    it('should set all module actions to false', () => {
      component.toggleModulo('dashboard', false);
      expect(component.permisos['admin']['dashboard.view']).toBeFalse();
      expect(component.permisos['admin']['dashboard.export']).toBeFalse();
    });

    it('should do nothing for unknown module', () => {
      const permisosBefore = JSON.stringify(component.permisos);
      component.toggleModulo('unknown', true);
      expect(JSON.stringify(component.permisos)).toBe(permisosBefore);
    });
  });

  describe('getEstadoModulo', () => {
    it('should return completo when all actions active', () => {
      expect(component.getEstadoModulo('dashboard')).toBe('completo');
    });

    it('should return vacio when no actions active', () => {
      component.rolSeleccionado = 'user';
      component.toggleModulo('users', false);
      expect(component.getEstadoModulo('users')).toBe('vacio');
    });

    it('should return parcial when some actions active', () => {
      component.rolSeleccionado = 'user';
      expect(component.getEstadoModulo('dashboard')).toBe('parcial');
    });

    it('should return vacio for unknown module', () => {
      expect(component.getEstadoModulo('unknown')).toBe('vacio');
    });
  });

  describe('getResumenModulo', () => {
    it('should return active/total for known module', () => {
      component.rolSeleccionado = 'user';
      expect(component.getResumenModulo('dashboard')).toBe('1/2');
    });

    it('should return 0/0 for unknown module', () => {
      expect(component.getResumenModulo('unknown')).toBe('0/0');
    });
  });

  describe('seleccionarRol', () => {
    it('should select role and clear guardado message', () => {
      component.rolSeleccionado = 'admin';
      component.mensajeGuardado = true;
      component.seleccionarRol('user');
      expect(component.rolSeleccionado).toBe('user');
      expect(component.mensajeGuardado).toBeFalse();
    });
  });

  describe('restablecer', () => {
    it('should restore permissions from backup', () => {
      component.permisos['admin']['dashboard.view'] = false;
      component.restablecer();
      expect(component.permisos['admin']['dashboard.view']).toBeTrue();
    });
  });

  describe('guardarCambios', () => {
    it('should save current permissions to backup', () => {
      component.permisos['admin']['dashboard.view'] = false;
      component.guardarCambios();
      expect(component.respaldoPermisos['admin']['dashboard.view']).toBeFalse();
      expect(component.mensajeGuardado).toBeTrue();
    });

    it('should reset mensajeGuardado after timeout', fakeAsync(() => {
      component.guardarCambios();
      tick(3000);
      expect(component.mensajeGuardado).toBeFalse();
    }));
  });

  describe('abrirModalNuevoRol', () => {
    it('should reset fields and show modal', () => {
      component.editandoRol = { id: 'x', nombre: 'X', descripcion: 'X', color: '#000', icono: 'bi-x' };
      component.nuevoRolNombre = 'old';
      component.nuevoRolDescripcion = 'old';

      component.abrirModalNuevoRol();

      expect(component.editandoRol).toBeNull();
      expect(component.nuevoRolNombre).toBe('');
      expect(component.nuevoRolDescripcion).toBe('');
      expect(component.mostrarModalRol).toBeTrue();
    });
  });

  describe('abrirModalEditarRol', () => {
    it('should populate fields from role and show modal', () => {
      const rol: Rol = { id: 'test', nombre: 'Test', descripcion: 'Test role', color: '#fff', icono: 'bi-star' };
      component.abrirModalEditarRol(rol);

      expect(component.editandoRol).toEqual(rol);
      expect(component.nuevoRolNombre).toBe('Test');
      expect(component.nuevoRolDescripcion).toBe('Test role');
      expect(component.mostrarModalRol).toBeTrue();
    });
  });

  describe('cerrarModalNuevoRol', () => {
    it('should hide modal and clear editandoRol', () => {
      component.mostrarModalRol = true;
      component.editandoRol = { id: 'x', nombre: 'X', descripcion: 'X', color: '#000', icono: 'bi-x' };

      component.cerrarModalNuevoRol();

      expect(component.mostrarModalRol).toBeFalse();
      expect(component.editandoRol).toBeNull();
    });
  });

  describe('guardarRol', () => {
    it('should add new role when not editing', () => {
      component.abrirModalNuevoRol();
      component.nuevoRolNombre = 'New Role';
      component.nuevoRolDescripcion = 'A new role';

      component.guardarRol();

      expect(component.roles.length).toBe(3);
      expect(component.roles[2].id).toBe('new-role');
      expect(component.roles[2].nombre).toBe('New Role');
      expect(component.rolSeleccionado).toBe('new-role');
      expect(component.mostrarModalRol).toBeFalse();
    });

    it('should edit existing role when editing', () => {
      component.abrirModalEditarRol(component.roles[1]);
      component.nuevoRolNombre = 'Updated User';
      component.nuevoRolDescripcion = 'Updated description';

      component.guardarRol();

      expect(component.roles[1].nombre).toBe('Updated User');
      expect(component.roles[1].descripcion).toBe('Updated description');
      expect(component.mostrarModalRol).toBeFalse();
      expect(component.editandoRol).toBeNull();
    });

    it('should not add role with duplicate id', () => {
      component.abrirModalNuevoRol();
      component.nuevoRolNombre = 'Admin';
      component.guardarRol();
      expect(component.roles.length).toBe(2);
    });

    it('should not add role with empty name', () => {
      component.abrirModalNuevoRol();
      component.guardarRol();
      expect(component.roles.length).toBe(2);
    });
  });

  describe('eliminarRol', () => {
    it('should remove role and its permissions', () => {
      const initialLength = component.roles.length;
      component.eliminarRol('user');

      expect(component.roles.length).toBe(initialLength - 1);
      expect(component.roles.find(r => r.id === 'user')).toBeUndefined();
      expect(component.permisos['user']).toBeUndefined();
    });

    it('should select first role if selected role is deleted', () => {
      component.rolSeleccionado = 'user';
      component.eliminarRol('user');
      expect(component.rolSeleccionado).toBe('admin');
    });

    it('should do nothing for non-existent role', () => {
      const initialLength = component.roles.length;
      component.eliminarRol('nonexistent');
      expect(component.roles.length).toBe(initialLength);
    });

    it('should handle deleting last role', () => {
      component.roles = [{ id: 'only', nombre: 'Only', descripcion: 'Only', color: '#000', icono: 'bi-person' }];
      component.rolSeleccionado = 'only';
      component.eliminarRol('only');
      expect(component.roles.length).toBe(0);
      expect(component.rolSeleccionado).toBe('');
    });
  });
});
