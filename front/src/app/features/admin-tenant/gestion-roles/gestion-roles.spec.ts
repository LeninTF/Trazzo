import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { Subject, of, throwError } from 'rxjs';
import { GestionRoles } from './gestion-roles';
import { OrgService } from '../../../api/services/org.service';
import { ToastService } from '../../../services/toast.service';
import type { OrgRoleResult, OrgPaginatedResult, OrgPermissionResult, OrgRolePermissionResult } from '../../../api/types';

describe('GestionRoles (tenant)', () => {
  let component: GestionRoles;
  let fixture: ComponentFixture<GestionRoles>;

  const mockRoles: OrgRoleResult[] = [
    { id: 'role-admin', name: 'administrador', description: 'desc admin', createdAt: '2026-01-01', updatedAt: '2026-01-01' },
    { id: 'role-docente', name: 'docente', description: 'desc docente', createdAt: '2026-01-01', updatedAt: '2026-01-01' },
  ];

  const mockPermissions: OrgPermissionResult[] = [
    { id: 'perm-crear', name: 'gestion-trabajadores.crear', description: null, masterFeaturesCode: null, createdAt: '2026-01-01', updatedAt: '2026-01-01' },
    { id: 'perm-editar', name: 'gestion-trabajadores.editar', description: null, masterFeaturesCode: null, createdAt: '2026-01-01', updatedAt: '2026-01-01' },
    { id: 'perm-perfil', name: 'perfil.ver-actualizar-datos', description: null, masterFeaturesCode: null, createdAt: '2026-01-01', updatedAt: '2026-01-01' },
  ];

  const grantsFor = (roleId: string, permissionIds: string[]): OrgRolePermissionResult[] =>
    permissionIds.map(permissionId => ({ roleId, permissionId, createdAt: '2026-01-01' }));

  let mockOrg: {
    listRoles: jasmine.Spy; createRole: jasmine.Spy; updateRole: jasmine.Spy; deleteRole: jasmine.Spy;
    listRolePermissions: jasmine.Spy; assignPermissionToRole: jasmine.Spy; removePermissionFromRole: jasmine.Spy;
    listPermissions: jasmine.Spy;
  };
  let mockToast: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    mockOrg = {
      listRoles: jasmine.createSpy('listRoles').and.returnValue(of({ content: mockRoles, page: 0, size: 100, total: 2, totalPages: 1 })),
      createRole: jasmine.createSpy('createRole').and.returnValue(of(mockRoles[0])),
      updateRole: jasmine.createSpy('updateRole').and.returnValue(of(mockRoles[0])),
      deleteRole: jasmine.createSpy('deleteRole').and.returnValue(of(undefined)),
      listRolePermissions: jasmine.createSpy('listRolePermissions').and.callFake((roleId: string) => {
        if (roleId === 'role-admin') return of(grantsFor('role-admin', ['perm-crear', 'perm-editar']));
        return of(grantsFor('role-docente', ['perm-perfil']));
      }),
      assignPermissionToRole: jasmine.createSpy('assignPermissionToRole').and.returnValue(of({ roleId: 'role-admin', permissionId: 'perm-perfil', createdAt: '2026-01-01' })),
      removePermissionFromRole: jasmine.createSpy('removePermissionFromRole').and.returnValue(of(undefined)),
      listPermissions: jasmine.createSpy('listPermissions').and.returnValue(of({ content: mockPermissions, page: 0, size: 200, total: 3, totalPages: 1 })),
    };
    mockToast = jasmine.createSpyObj('ToastService', ['show', 'success', 'error', 'info']);

    await TestBed.configureTestingModule({
      imports: [GestionRoles, FormsModule],
      providers: [
        { provide: OrgService, useValue: mockOrg },
        { provide: ToastService, useValue: mockToast },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GestionRoles);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load roles and permissions from the backend', () => {
    expect(mockOrg.listRoles).toHaveBeenCalled();
    expect(mockOrg.listPermissions).toHaveBeenCalled();
    expect(component.roles).toHaveSize(2);
  });

  it('should have the 14 predefined modulos', () => {
    expect(component.modulos).toHaveSize(14);
  });

  it('should select the first role by default', () => {
    expect(component.rolSeleccionado).toBe('role-admin');
    expect(component.rolActual.nombre).toBe('Administrador');
  });

  it('should mark granted permissions as active from listRolePermissions', () => {
    expect(component.permisosActuales['gestion-trabajadores.crear']).toBeTrue();
    expect(component.permisosActuales['gestion-trabajadores.editar']).toBeTrue();
    expect(component.permisosActuales['gestion-trabajadores.eliminar']).toBeFalse();
  });

  it('should filter modulosVisibles to admin modules by default', () => {
    const ids = component.modulosVisibles().map(m => m.id);
    expect(ids).toContain('gestion-trabajadores');
    expect(ids).not.toContain('asistencia-docente');
  });

  it('should filter modulosVisibles to docente modules when docente role is selected', () => {
    component.seleccionarRol('role-docente');
    const ids = component.modulosVisibles().map(m => m.id);
    expect(ids).toContain('asistencia-docente');
    expect(ids).not.toContain('gestion-trabajadores');
  });

  it('should togglePermiso locally without calling backend', () => {
    component.togglePermiso('gestion-trabajadores', 'crear');
    expect(component.permisosActuales['gestion-trabajadores.crear']).toBeFalse();
    expect(mockOrg.assignPermissionToRole).not.toHaveBeenCalled();
    expect(mockOrg.removePermissionFromRole).not.toHaveBeenCalled();
  });

  it('should restablecer revert unsaved changes', () => {
    component.togglePermiso('gestion-trabajadores', 'crear');
    component.restablecer();
    expect(component.permisosActuales['gestion-trabajadores.crear']).toBeTrue();
  });

  it('should guardarCambios assign newly-checked permissions', () => {
    component.togglePermiso('perfil', 'ver-actualizar-datos');

    component.guardarCambios();

    expect(mockOrg.assignPermissionToRole).toHaveBeenCalledWith('role-admin', 'perm-perfil');
    expect(component.mensajeGuardado).toBeTrue();
  });

  it('should guardarCambios remove newly-unchecked permissions', () => {
    component.togglePermiso('gestion-trabajadores', 'crear');

    component.guardarCambios();

    expect(mockOrg.removePermissionFromRole).toHaveBeenCalledWith('role-admin', 'perm-crear');
  });

  it('should do nothing when guardarCambios has no changes', () => {
    component.guardarCambios();
    expect(mockOrg.assignPermissionToRole).not.toHaveBeenCalled();
    expect(mockOrg.removePermissionFromRole).not.toHaveBeenCalled();
    expect(component.mensajeGuardado).toBeTrue();
  });

  it('should show error toast when guardarCambios fails', () => {
    mockOrg.assignPermissionToRole.and.returnValue(throwError(() => new Error('fail')));
    component.togglePermiso('perfil', 'ver-actualizar-datos');
    component.guardarCambios();
    expect(mockToast.error).toHaveBeenCalled();
  });

  it('should abrirModalNuevoRol reset the form', () => {
    component.abrirModalNuevoRol();
    expect(component.mostrarModalRol).toBeTrue();
    expect(component.editandoRol).toBeNull();
  });

  it('should cerrarModalNuevoRol', () => {
    component.abrirModalNuevoRol();
    component.cerrarModalNuevoRol();
    expect(component.mostrarModalRol).toBeFalse();
  });

  it('should guardarRol create a new role via backend', () => {
    component.abrirModalNuevoRol();
    component.nuevoRolNombre = 'Supervisor de piso';

    component.guardarRol();

    expect(mockOrg.createRole).toHaveBeenCalledWith({ name: 'Supervisor de piso', description: undefined });
    expect(component.mostrarModalRol).toBeFalse();
  });

  it('should not guardarRol with an empty name', () => {
    component.abrirModalNuevoRol();
    component.guardarRol();
    expect(mockOrg.createRole).not.toHaveBeenCalled();
  });

  it('should guardarRol update an existing role', () => {
    component.abrirModalEditarRol(component.roles[0]);
    component.nuevoRolNombre = 'Administrador General';

    component.guardarRol();

    expect(mockOrg.updateRole).toHaveBeenCalledWith('role-admin', { name: 'Administrador General', description: 'desc admin' });
  });

  it('should eliminarRol call backend delete', () => {
    component.eliminarRol('role-docente');
    expect(mockOrg.deleteRole).toHaveBeenCalledWith('role-docente');
  });

  it('should show error toast when eliminarRol fails', () => {
    mockOrg.deleteRole.and.returnValue(throwError(() => new Error('409')));
    component.eliminarRol('role-docente');
    expect(mockToast.error).toHaveBeenCalled();
  });

  it('should handle an empty role list without error', () => {
    mockOrg.listRoles.and.returnValue(of({ content: [], page: 0, size: 100, total: 0, totalPages: 0 }));
    component['cargarRoles']();
    expect(component.roles).toHaveSize(0);
    expect(component.error()).toBe('');
  });

  it('should set error when loading roles fails', () => {
    mockOrg.listRoles.and.returnValue(throwError(() => new Error('fail')));
    component['cargarRoles']();
    expect(component.error()).toBe('No se pudieron cargar los roles.');
  });

  it('should guardarCambios skip unchanged permissions and produce no API calls when toggled back', () => {
    component.togglePermiso('gestion-trabajadores', 'crear');
    component.togglePermiso('gestion-trabajadores', 'crear');

    component.guardarCambios();

    expect(mockOrg.assignPermissionToRole).not.toHaveBeenCalled();
    expect(mockOrg.removePermissionFromRole).not.toHaveBeenCalled();
    expect(component.mensajeGuardado).toBeTrue();
  });

  it('should guardarRol update path clear editandoRol and close modal on success', () => {
    component.abrirModalEditarRol(component.roles[0]);
    component.nuevoRolNombre = 'Admin Actualizado';
    component.nuevoRolDescripcion = 'desc updated';

    component.guardarRol();

    expect(mockOrg.updateRole).toHaveBeenCalledWith('role-admin', { name: 'Admin Actualizado', description: 'desc updated' });
    expect(component.editandoRol).toBeNull();
    expect(component.mostrarModalRol).toBeFalse();
  });

  it('should eliminarRol clear rolSeleccionado when deleting the selected role', () => {
    component.seleccionarRol('role-docente');
    expect(component.rolSeleccionado).toBe('role-docente');

    component.eliminarRol('role-docente');

    expect(component.rolSeleccionado).toBe('role-admin');
  });

  it('should set error when listRolePermissions fails', () => {
    mockOrg.listRolePermissions.and.returnValue(throwError(() => new Error('fail')));
    component['cargarRoles']();
    expect(component.error()).toBe('No se pudieron cargar los permisos de los roles.');
  });

  it('should show error toast when guardarRol create fails', () => {
    mockOrg.createRole.and.returnValue(throwError(() => new Error('fail')));
    component.abrirModalNuevoRol();
    component.nuevoRolNombre = 'Fallo al crear';
    component.guardarRol();
    expect(mockToast.error).toHaveBeenCalledWith('No se pudo crear el rol.');
  });

  it('should show error toast when guardarRol update fails', () => {
    mockOrg.updateRole.and.returnValue(throwError(() => new Error('fail')));
    component.abrirModalEditarRol(component.roles[0]);
    component.nuevoRolNombre = 'Fallo al editar';
    component.guardarRol();
    expect(mockToast.error).toHaveBeenCalledWith('No se pudo actualizar el rol.');
  });

  it('should toRol use DEFAULT_META for unknown role name', () => {
    mockOrg.listRoles.and.returnValue(of({
      content: [{ id: 'role-unknown', name: 'desconocido', description: 'x', createdAt: '2026-01-01', updatedAt: '2026-01-01' }],
      page: 0, size: 100, total: 1, totalPages: 1,
    }));
    mockOrg.listRolePermissions.and.returnValue(of([]));
    component['cargarRoles']();
    const unknownRole = component.roles.find(r => r.id === 'role-unknown');
    expect(unknownRole).toBeDefined();
    expect(unknownRole!.color).toBe('#6B7280');
    expect(unknownRole!.icono).toBe('bi-person');
  });

  it('should guardarCambios with both assign and remove changes', () => {
    component.togglePermiso('perfil', 'ver-actualizar-datos');
    component.togglePermiso('gestion-trabajadores', 'crear');
    component.guardarCambios();
    expect(mockOrg.assignPermissionToRole).toHaveBeenCalledWith('role-admin', 'perm-perfil');
    expect(mockOrg.removePermissionFromRole).toHaveBeenCalledWith('role-admin', 'perm-crear');
  });

  it('should modulosVisibles filter to admin modules for non-docente non-admin roles', () => {
    mockOrg.listRoles.and.returnValue(of({
      content: [{ id: 'role-coord', name: 'coordinador', description: 'x', createdAt: '2026-01-01', updatedAt: '2026-01-01' }],
      page: 0, size: 100, total: 1, totalPages: 1,
    }));
    mockOrg.listRolePermissions.and.returnValue(of([]));
    component['cargarRoles']();
    const ids = component.modulosVisibles().map(m => m.id);
    expect(ids).toContain('gestion-trabajadores');
    expect(ids).not.toContain('asistencia-docente');
  });
});

// Regression test for a bug where the first render (before the backend responds)
// threw "Cannot read properties of undefined (reading 'color')" from rolActual,
// leaving the page blank until a second navigation happened to land after the
// response arrived. of(...) above resolves synchronously on subscribe, so it never
// exercises this race — a Subject that emits later reproduces the real timing.
describe('GestionRoles (tenant, loading race)', () => {
  let component: GestionRoles;
  let fixture: ComponentFixture<GestionRoles>;
  let rolesSubject: Subject<OrgPaginatedResult<OrgRoleResult>>;

  beforeEach(async () => {
    rolesSubject = new Subject<OrgPaginatedResult<OrgRoleResult>>();
    const mockOrg = {
      listRoles: jasmine.createSpy('listRoles').and.returnValue(rolesSubject.asObservable()),
      createRole: jasmine.createSpy('createRole'),
      updateRole: jasmine.createSpy('updateRole'),
      deleteRole: jasmine.createSpy('deleteRole'),
      listRolePermissions: jasmine.createSpy('listRolePermissions').and.returnValue(of([])),
      assignPermissionToRole: jasmine.createSpy('assignPermissionToRole'),
      removePermissionFromRole: jasmine.createSpy('removePermissionFromRole'),
      listPermissions: jasmine.createSpy('listPermissions').and.returnValue(of({ content: [], page: 0, size: 200, total: 0, totalPages: 0 })),
    };

    await TestBed.configureTestingModule({
      imports: [GestionRoles, FormsModule],
      providers: [
        { provide: OrgService, useValue: mockOrg },
        { provide: ToastService, useValue: jasmine.createSpyObj('ToastService', ['show', 'success', 'error', 'info']) },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GestionRoles);
    component = fixture.componentInstance;
  });

  it('should not throw and should show the loading state before the backend responds', () => {
    expect(() => fixture.detectChanges()).not.toThrow();
    expect(component.loading()).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Cargando roles');
  });

  it('should render the matrix once the backend responds', () => {
    fixture.detectChanges();
    // forkJoin (used internally by cargarRoles) only emits once every source
    // completes, not merely on next() — a bare Subject.next() never resolves it.
    rolesSubject.next({
      content: [{ id: 'role-admin', name: 'administrador', description: 'desc', createdAt: '2026-01-01', updatedAt: '2026-01-01' }],
      page: 0, size: 100, total: 1, totalPages: 1,
    });
    rolesSubject.complete();
    fixture.detectChanges();

    expect(component.loading()).toBeFalse();
    expect(component.rolActual.nombre).toBe('Administrador');
    expect(fixture.nativeElement.textContent).toContain('Administrador');
  });
});
