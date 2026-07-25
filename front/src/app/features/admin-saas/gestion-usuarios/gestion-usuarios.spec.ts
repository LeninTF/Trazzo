import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { GestionUsuarios } from './gestion-usuarios';
import { ApiService } from '../../../api/services/api.service';
import { ToastService } from '../../../services/toast.service';
import { ModalService } from '../../../services/modal.service';
import type { MasterUserProfile, SaasRoleProfile } from '../../../api/types';

describe('GestionUsuarios', () => {
  let component: GestionUsuarios;
  let fixture: ComponentFixture<GestionUsuarios>;

  const mockRoles: SaasRoleProfile[] = [
    { id: 1, name: 'super-administrador', displayName: 'Super Administrador', description: null, permissions: [], systemManaged: false },
    { id: 2, name: 'soporte', displayName: 'Soporte', description: null, permissions: [], systemManaged: false },
  ];

  const mockUser = (id: string, name: string, roleIds: number[]): MasterUserProfile => ({
    id, email: `${name.toLowerCase()}@trazzo.pe`, phone: '999888777',
    tenant_id: null, must_change_password: false, created_at: '2026-01-01T00:00:00',
    persona: { id: 1, img_url: null, document_type: 'DNI', document_value: '12345678', name, father_surname: 'Perez', mother_surname: 'Lopez', birth_date: null },
    MetodoRecuperacion: [],
    roles: roleIds.map(rid => ({ id: rid, name: mockRoles.find(r => r.id === rid)!.name, descripcion: null })),
    tenant_info: null,
  });

  let mockUsers: { listMasters: jasmine.Spy; createMaster: jasmine.Spy; updateMaster: jasmine.Spy; deleteMaster: jasmine.Spy; assignMasterRole: jasmine.Spy };
  let mockRolesService: { list: jasmine.Spy };
  let mockToast: jasmine.SpyObj<ToastService>;
  let mockModal: jasmine.SpyObj<ModalService>;

  beforeEach(async () => {
    mockUsers = {
      listMasters: jasmine.createSpy('listMasters').and.returnValue(of({
        content: [mockUser('u1', 'Ana', [1]), mockUser('u2', 'Luis', [2])],
        page: 0, size: 20, totalElements: 2, totalPages: 1,
      })),
      createMaster: jasmine.createSpy('createMaster').and.returnValue(of(mockUser('u3', 'Nuevo', []))),
      updateMaster: jasmine.createSpy('updateMaster').and.returnValue(of(mockUser('u1', 'Ana', [1]))),
      deleteMaster: jasmine.createSpy('deleteMaster').and.returnValue(of(undefined)),
      assignMasterRole: jasmine.createSpy('assignMasterRole').and.returnValue(of(mockUser('u1', 'Ana', [2]))),
    };
    mockRolesService = { list: jasmine.createSpy('list').and.returnValue(of(mockRoles)) };
    mockToast = jasmine.createSpyObj('ToastService', ['show', 'success', 'error', 'info']);
    mockModal = jasmine.createSpyObj('ModalService', ['show', 'hide']);

    await TestBed.configureTestingModule({
      imports: [GestionUsuarios],
      providers: [
        { provide: ApiService, useValue: { users: mockUsers, roles: mockRolesService } },
        { provide: ToastService, useValue: mockToast },
        { provide: ModalService, useValue: mockModal },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GestionUsuarios);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load usuarios and roles on init', () => {
    expect(mockUsers.listMasters).toHaveBeenCalled();
    expect(mockRolesService.list).toHaveBeenCalled();
    expect(component.usuarios()).toHaveSize(2);
    expect(component.rolesDisponibles()).toHaveSize(2);
  });

  it('should filter by rol', () => {
    component.setFilterRol('1');
    expect(component.usuariosFiltrados().every(u => u.roles.some(r => r.id === 1))).toBeTrue();
  });

  it('should show all with todos filter', () => {
    component.setFilterRol('todos');
    expect(component.usuariosFiltrados()).toHaveSize(2);
  });

  it('should limpiarFiltros', () => {
    component.setFilterRol('1');
    component.limpiarFiltros();
    expect(component.filterRol()).toBe('todos');
  });

  it('should compute total', () => {
    expect(component.total()).toBe(2);
  });

  it('should compute distribucionRoles', () => {
    const dist = component.distribucionRoles();
    expect(dist.find(d => d.rol.id === 1)?.count).toBe(1);
    expect(dist.find(d => d.rol.id === 2)?.count).toBe(1);
  });

  it('should abrirCrear new user', () => {
    component.abrirCrear();
    expect(component.vistaCrear()).toBeTrue();
    expect(component.editandoUsuario()).toBeNull();
    expect(component.paso()).toBe(1);
    expect(component.selectedRoleIds().size).toBe(0);
  });

  it('should abrirCrear for editing prefills form and roles', () => {
    const user = component.usuarios()[0];
    component.abrirCrear(user);
    expect(component.editandoUsuario()).toBe(user);
    expect(component.createForm.get('nombres')?.value).toBe('Ana');
    expect(component.isRoleSelected(1)).toBeTrue();
  });

  it('should cancelarCrear', () => {
    component.abrirCrear();
    component.cancelarCrear();
    expect(component.vistaCrear()).toBeFalse();
  });

  it('should navigate pasos', () => {
    component.abrirCrear();
    expect(component.paso()).toBe(1);
    component.siguientePaso();
    expect(component.paso()).toBe(2);
    component.pasoAnterior();
    expect(component.paso()).toBe(1);
  });

  it('should not go beyond totalPasos', () => {
    component.abrirCrear();
    component.irPaso(5);
    expect(component.paso()).toBe(1);
  });

  it('should toggleRole', () => {
    component.abrirCrear();
    component.toggleRole(2);
    expect(component.isRoleSelected(2)).toBeTrue();
    component.toggleRole(2);
    expect(component.isRoleSelected(2)).toBeFalse();
  });

  it('should registrarPersonal create new user with valid form', () => {
    component.abrirCrear();
    component.createForm.patchValue({
      nombres: 'Test', apellidoPaterno: 'User', apellidoMaterno: 'Lopez',
      tipoDocumento: 'DNI', numDocumento: '87654321', correo: 'test@trazzo.pe', telefono: '999999999',
    });
    component.toggleRole(2);

    component.registrarPersonal();

    expect(mockUsers.createMaster).toHaveBeenCalledWith(jasmine.objectContaining({
      name: 'Test', father_surname: 'User', mother_surname: 'Lopez', email: 'test@trazzo.pe', role_ids: [2],
    }));
    expect(component.vistaCrear()).toBeFalse();
  });

  it('should not registrarPersonal with invalid form', () => {
    component.abrirCrear();
    component.registrarPersonal();
    expect(mockUsers.createMaster).not.toHaveBeenCalled();
    expect(mockToast.show).toHaveBeenCalled();
  });

  it('should registrarPersonal update existing user when editing', () => {
    const user = component.usuarios()[0];
    component.abrirCrear(user);

    component.registrarPersonal();

    expect(mockUsers.updateMaster).toHaveBeenCalledWith('u1', jasmine.objectContaining({ email: user.email }));
    expect(mockUsers.assignMasterRole).toHaveBeenCalledWith('u1', { role_ids: [1] });
  });

  it('should show error toast when registrarPersonal fails', () => {
    mockUsers.createMaster.and.returnValue(throwError(() => new Error('fail')));
    component.abrirCrear();
    component.createForm.patchValue({
      nombres: 'Test', apellidoPaterno: 'User', apellidoMaterno: 'Lopez',
      tipoDocumento: 'DNI', numDocumento: '87654321', correo: 'test@trazzo.pe',
    });

    component.registrarPersonal();

    expect(mockToast.show).toHaveBeenCalledWith('No se pudo registrar el administrador.', 'error');
  });

  it('should confirmarEliminar show modal', () => {
    const user = component.usuarios()[0];
    component.confirmarEliminar(user);
    expect(component.editandoUsuario()).toBe(user);
    expect(mockModal.show).toHaveBeenCalledWith('modalConfirmarEliminar');
  });

  it('should eliminarUsuario delete and refresh', () => {
    const user = component.usuarios()[0];
    component.confirmarEliminar(user);

    component.eliminarUsuario();

    expect(mockUsers.deleteMaster).toHaveBeenCalledWith('u1');
    expect(mockModal.hide).toHaveBeenCalledWith('modalConfirmarEliminar');
  });

  it('should nombreCompleto join name and father surname', () => {
    const user = component.usuarios()[0];
    expect(component.nombreCompleto(user)).toBe('Ana Perez');
  });

  it('should eliminarUsuario early return when no user selected', () => {
    component.editandoUsuario.set(null);
    component.eliminarUsuario();
    expect(mockUsers.deleteMaster).not.toHaveBeenCalled();
  });

  it('should siguientePaso not advance beyond totalPasos', () => {
    component.abrirCrear();
    component.siguientePaso();
    expect(component.paso()).toBe(2);
    component.siguientePaso();
    expect(component.paso()).toBe(2);
  });

  it('should pasoAnterior not go below 1', () => {
    component.abrirCrear();
    expect(component.paso()).toBe(1);
    component.pasoAnterior();
    expect(component.paso()).toBe(1);
  });

  it('should registrarPersonal show error when updateMaster fails during edit', () => {
    mockUsers.updateMaster.and.returnValue(throwError(() => new Error('fail')));
    const user = component.usuarios()[0];
    component.abrirCrear(user);
    component.registrarPersonal();
    expect(mockToast.show).toHaveBeenCalledWith('No se pudo actualizar el administrador.', 'error');
  });

  it('should registrarPersonal show error when assignMasterRole fails during edit', () => {
    mockUsers.assignMasterRole.and.returnValue(throwError(() => new Error('fail')));
    const user = component.usuarios()[0];
    component.abrirCrear(user);
    component.registrarPersonal();
    expect(mockToast.show).toHaveBeenCalledWith('No se pudieron actualizar los roles.', 'error');
  });

  it('should irPaso set step to valid boundary values', () => {
    component.abrirCrear();
    component.irPaso(2);
    expect(component.paso()).toBe(2);
    component.irPaso(1);
    expect(component.paso()).toBe(1);
  });

  it('should cargarUsuarios handle error on load', () => {
    mockUsers.listMasters.and.returnValue(throwError(() => new Error('fail')));
    component['cargarUsuarios']();
    expect(component.error()).toBe('No se pudieron cargar los usuarios.');
  });
});
