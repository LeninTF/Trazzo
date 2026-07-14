import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { Subject, of, throwError } from 'rxjs';
import { GestionRoles } from './gestion-roles';
import { ApiService } from '../../../api/services/api.service';
import { ToastService } from '../../../services/toast.service';
import type { SaasRoleProfile } from '../../../api/types';

describe('GestionRoles', () => {
  let component: GestionRoles;
  let fixture: ComponentFixture<GestionRoles>;

  const mockRoles: SaasRoleProfile[] = [
    { id: 1, name: 'admin_trazzo', displayName: 'Administrador Trazzo', description: 'desc', permissions: ['gestion-tenants.crear', 'gestion-tenants.editar', 'gestion-tenants.eliminar', 'gestion-tenants.activar-suspender', 'gestion-tenants.configurar-identidad', 'gestion-tenants.zonas-horarias', 'gestion-tenants.asignacion-planes', 'gestion-tenants.tipos-marcacion', 'billing-suscripciones.gestionar-pagos', 'billing-suscripciones.historial-facturacion', 'billing-suscripciones.bloqueo-impago', 'configuracion-global.modulos-por-plan', 'monitoreo-sistema.dashboard-global', 'monitoreo-sistema.logs-sistema', 'monitoreo-sistema.auditoria-acciones'], systemManaged: true },
    { id: 2, name: 'soporte', displayName: 'Administrador de Soporte', description: 'desc', permissions: ['monitoreo-sistema.dashboard-global'], systemManaged: false },
    { id: 3, name: 'operaciones', displayName: 'Administrador de Operaciones', description: 'desc', permissions: [], systemManaged: false },
    { id: 4, name: 'financiero', displayName: 'Administrador Financiero', description: 'desc', permissions: [], systemManaged: false },
    { id: 5, name: 'consultor', displayName: 'Consultor / Vista', description: 'desc', permissions: [], systemManaged: false },
  ];

  let mockRolesService: { list: jasmine.Spy; create: jasmine.Spy; update: jasmine.Spy; delete: jasmine.Spy; updatePermissions: jasmine.Spy };
  let mockToast: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    mockRolesService = {
      list: jasmine.createSpy('list').and.returnValue(of(mockRoles)),
      create: jasmine.createSpy('create').and.returnValue(of(mockRoles[1])),
      update: jasmine.createSpy('update').and.returnValue(of(mockRoles[1])),
      delete: jasmine.createSpy('delete').and.returnValue(of(undefined)),
      updatePermissions: jasmine.createSpy('updatePermissions').and.returnValue(of(mockRoles[1])),
    };
    mockToast = jasmine.createSpyObj('ToastService', ['show', 'success', 'error', 'info']);

    await TestBed.configureTestingModule({
      imports: [GestionRoles, FormsModule],
      providers: [
        { provide: ApiService, useValue: { roles: mockRolesService } },
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

  it('should load 5 roles from the backend', () => {
    expect(mockRolesService.list).toHaveBeenCalled();
    expect(component.roles).toHaveSize(5);
  });

  it('should have 4 modulos', () => {
    expect(component.modulos).toHaveSize(4);
  });

  it('should select the first role by default', () => {
    expect(component.rolSeleccionado).toBe('1');
    expect(component.rolActual.nombre).toBe('Administrador Trazzo');
  });

  it('should get permisosActuales for selected role', () => {
    const permisos = component.permisosActuales;
    expect(permisos['gestion-tenants.crear']).toBeTrue();
  });

  it('should togglePermiso locally without calling backend', () => {
    component.togglePermiso('gestion-tenants', 'crear');
    expect(component.permisosActuales['gestion-tenants.crear']).toBeFalse();
    expect(mockRolesService.updatePermissions).not.toHaveBeenCalled();
  });

  it('should toggleModulo enable all', () => {
    component.toggleModulo('gestion-tenants', true);
    const modulo = component.modulos.find(m => m.id === 'gestion-tenants')!;
    for (const accion of modulo.acciones) {
      expect(component.permisosActuales[`gestion-tenants.${accion.id}`]).toBeTrue();
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
    component.seleccionarRol('2');
    expect(component.rolSeleccionado).toBe('2');
  });

  it('should restablecer permisos', () => {
    component.togglePermiso('gestion-tenants', 'crear');
    component.restablecer();
    expect(component.permisosActuales['gestion-tenants.crear']).toBeTrue();
  });

  it('should guardarCambios call updatePermissions with active codes', () => {
    component.seleccionarRol('2');
    component.togglePermiso('gestion-tenants', 'crear');

    component.guardarCambios();

    expect(mockRolesService.updatePermissions).toHaveBeenCalledWith(2, {
      permissions: jasmine.arrayContaining(['monitoreo-sistema.dashboard-global', 'gestion-tenants.crear']),
    });
    expect(component.mensajeGuardado).toBeTrue();
  });

  it('should show error toast when guardarCambios fails', () => {
    mockRolesService.updatePermissions.and.returnValue(throwError(() => new Error('fail')));
    component.guardarCambios();
    expect(mockToast.error).toHaveBeenCalled();
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

  it('should guardarRol create new role via backend', () => {
    component.abrirModalNuevoRol();
    component.nuevoRolNombre = 'Test Rol';

    component.guardarRol();

    expect(mockRolesService.create).toHaveBeenCalledWith(jasmine.objectContaining({ displayName: 'Test Rol' }));
    expect(component.mostrarModalRol).toBeFalse();
  });

  it('should not guardarRol with empty name', () => {
    component.abrirModalNuevoRol();
    component.guardarRol();
    expect(mockRolesService.create).not.toHaveBeenCalled();
  });

  it('should guardarRol update existing non-system role', () => {
    component.abrirModalEditarRol(component.roles[1]);
    component.nuevoRolNombre = 'Soporte Editado';

    component.guardarRol();

    expect(mockRolesService.update).toHaveBeenCalledWith(2, jasmine.objectContaining({ displayName: 'Soporte Editado' }));
  });

  it('should block editing a system-managed role', () => {
    component.abrirModalEditarRol(component.roles[0]);
    component.nuevoRolNombre = 'Intento de cambio';

    component.guardarRol();

    expect(mockRolesService.update).not.toHaveBeenCalled();
    expect(mockToast.error).toHaveBeenCalled();
  });

  it('should eliminarRol call backend delete', () => {
    component.eliminarRol('2');
    expect(mockRolesService.delete).toHaveBeenCalledWith(2);
  });

  it('should block deleting a system-managed role', () => {
    component.eliminarRol('1');
    expect(mockRolesService.delete).not.toHaveBeenCalled();
    expect(mockToast.error).toHaveBeenCalled();
  });

  it('should show error toast when eliminarRol fails (role in use)', () => {
    mockRolesService.delete.and.returnValue(throwError(() => new Error('409')));
    component.eliminarRol('2');
    expect(mockToast.error).toHaveBeenCalled();
  });
});

// Regression test for a bug where the first render (before the backend responds)
// threw "Cannot read properties of undefined (reading 'color')" from rolActual,
// leaving the page blank until a second navigation happened to land after the
// response arrived. of(mockRoles) above resolves synchronously on subscribe, so it
// never exercises this race — a Subject that emits later reproduces the real timing.
describe('GestionRoles (loading race)', () => {
  let component: GestionRoles;
  let fixture: ComponentFixture<GestionRoles>;
  let rolesSubject: Subject<SaasRoleProfile[]>;

  beforeEach(async () => {
    rolesSubject = new Subject<SaasRoleProfile[]>();
    const mockRolesService = {
      list: jasmine.createSpy('list').and.returnValue(rolesSubject.asObservable()),
      create: jasmine.createSpy('create'),
      update: jasmine.createSpy('update'),
      delete: jasmine.createSpy('delete'),
      updatePermissions: jasmine.createSpy('updatePermissions'),
    };

    await TestBed.configureTestingModule({
      imports: [GestionRoles, FormsModule],
      providers: [
        { provide: ApiService, useValue: { roles: mockRolesService } },
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
    rolesSubject.next([
      { id: 1, name: 'admin_trazzo', displayName: 'Administrador Trazzo', description: 'desc', permissions: [], systemManaged: true },
    ]);
    fixture.detectChanges();

    expect(component.loading()).toBeFalse();
    expect(component.rolActual.nombre).toBe('Administrador Trazzo');
    expect(fixture.nativeElement.textContent).toContain('Administrador Trazzo');
  });
});
