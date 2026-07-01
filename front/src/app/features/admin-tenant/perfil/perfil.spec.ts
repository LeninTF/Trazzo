import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Perfil } from './perfil';
import { ApiService } from '../../../api/services/api.service';
import { ToastService } from '../../../services/toast.service';
import { RoleService } from '../../../services/role.service';

describe('Perfil', () => {
  let component: Perfil;
  let fixture: ComponentFixture<Perfil>;
  let apiSpy: jasmine.SpyObj<ApiService>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;
  let roleService: RoleService;

  const mockUser = {
    id: 1,
    email: 'juan@test.com',
    phone: '999888777',
    created_at: '2024-01-15T00:00:00Z',
    persona: {
      name: 'Juan',
      father_surname: 'Pérez',
      mother_surname: 'López',
      document_value: '12345678',
      img_url: 'http://example.com/foto.jpg',
    },
    rol: { name: 'Admin' },
    sedes: [{ nombre: 'Sede Central' }],
    areas: [{ nombre: 'IT' }],
  };

  beforeEach(async () => {
    localStorage.clear();
    apiSpy = jasmine.createSpyObj('ApiService', ['users'], {
      users: jasmine.createSpyObj('UsersService', ['getMe', 'patchMe', 'changePassword']),
    });
    (apiSpy.users.getMe as jasmine.Spy).and.returnValue(of(mockUser));
    (apiSpy.users.patchMe as jasmine.Spy).and.returnValue(of(mockUser));
    (apiSpy.users.changePassword as jasmine.Spy).and.returnValue(of(undefined));

    toastServiceSpy = jasmine.createSpyObj('ToastService', ['success']);

    await TestBed.configureTestingModule({
      imports: [Perfil],
      providers: [
        RoleService,
        { provide: ApiService, useValue: apiSpy },
        { provide: ToastService, useValue: toastServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Perfil);
    component = fixture.componentInstance;
    roleService = TestBed.inject(RoleService);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('creates the perfil component', () => {
    expect(component).toBeTruthy();
  });

  it('loads user data on init', async () => {
    await fixture.whenStable();
    fixture.detectChanges();
    expect(component.usuario.nombres).toBe('Juan');
    expect(component.usuario.apellidos).toBe('Pérez López');
    expect(component.usuario.email).toBe('juan@test.com');
    expect(component.loading()).toBeFalse();
    expect((component as any).loaded).toBeTrue();
  });

  it('should handle error when loading user', async () => {
    (apiSpy.users.getMe as jasmine.Spy).and.returnValue(throwError(() => new Error('Network error')));

    const errorSpy = spyOn(console, 'error');
    fixture = TestBed.createComponent(Perfil);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.error()).toBe('No se pudieron cargar los datos del perfil. Verifica tu conexión e intenta nuevamente.');
    expect(component.loading()).toBeFalse();
  });

  describe('subirFoto', () => {
    it('should return early if no selectedFile', async () => {
      await component.subirFoto();
      expect(component.fotoSubiendo()).toBeFalse();
    });

    it('should read file and update fotoPreview', async () => {
      component.selectedFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      component.editando = true;
      component.usuarioEdit = { ...component.usuario };

      await component.subirFoto();

      expect(component.fotoSubiendo()).toBeFalse();
      expect(component.selectedFile).toBeNull();
      expect(toastServiceSpy.success).toHaveBeenCalledWith('Foto de perfil actualizada correctamente.');
    });
  });

  describe('guardarCambios', () => {
    it('should validate required fields', () => {
      component.editar();
      component.usuarioEdit.nombres = '';
      component.guardarCambios();
      expect(component.mensajeError).toBe('Nombres y apellidos son obligatorios.');
    });

    it('should call patchMe on valid data', fakeAsync(() => {
      component.editar();
      component.usuarioEdit.nombres = 'Carlos';
      component.guardarCambios();
      tick();

      expect(apiSpy.users.patchMe).toHaveBeenCalled();
      expect(toastServiceSpy.success).toHaveBeenCalledWith('Datos actualizados correctamente.');
      expect(component.editando).toBeFalse();
    }));

    it('should handle patchMe error', fakeAsync(() => {
      (apiSpy.users.patchMe as jasmine.Spy).and.returnValue(throwError(() => new Error('Save failed')));

      component.editar();
      component.usuarioEdit.nombres = 'Carlos';
      component.guardarCambios();
      tick();

      expect(component.mensajeError).toBe('Error al guardar cambios.');
      expect(component.guardando()).toBeFalse();
    }));
  });

  describe('guardarPassword', () => {
    it('should require current password', () => {
      component.guardarPassword();
      expect(component.errorPasswordActual).toBe('Ingrese su contraseña actual.');
    });

    it('should call changePassword on valid data', fakeAsync(() => {
      component.passwordActual = 'oldpass';
      component.passwordNueva = 'newpass123';
      component.passwordConfirmar = 'newpass123';

      component.guardarPassword();

      expect(apiSpy.users.changePassword).toHaveBeenCalledWith(1, {
        current_password: 'oldpass',
        new_password: 'newpass123',
      });
    }));

    it('should handle changePassword error', fakeAsync(() => {
      (apiSpy.users.changePassword as jasmine.Spy).and.returnValue(throwError(() => new Error('Wrong password')));

      component.passwordActual = 'wrong';
      component.passwordNueva = 'newpass123';
      component.passwordConfirmar = 'newpass123';

      component.guardarPassword();
      tick();

      expect(component.errorPasswordActual).toBe('Contraseña actual incorrecta.');
    }));
  });
});
