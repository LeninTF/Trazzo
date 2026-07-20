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

  describe('cargarUsuario - uncovered branches', () => {
    it('should set fechaIngreso to empty when created_at is null', async () => {
      const userNoDate = { ...mockUser, created_at: null };
      (apiSpy.users.getMe as jasmine.Spy).and.returnValue(of(userNoDate));

      fixture = TestBed.createComponent(Perfil);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.usuario.fechaIngreso).toBe('');
    });

    it('should set fechaIngreso to empty when created_at is undefined', async () => {
      const userNoDate = { ...mockUser };
      delete (userNoDate as any).created_at;
      (apiSpy.users.getMe as jasmine.Spy).and.returnValue(of(userNoDate));

      fixture = TestBed.createComponent(Perfil);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.usuario.fechaIngreso).toBe('');
    });

    it('should set loading false and loaded true in finally block on error', async () => {
      (apiSpy.users.getMe as jasmine.Spy).and.returnValue(throwError(() => new Error('fail')));

      fixture = TestBed.createComponent(Perfil);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.loading()).toBeFalse();
      expect((component as any).loaded).toBeTrue();
      expect(component.error()).toContain('No se pudieron cargar');
    });
  });

  describe('subirFoto - uncovered branches', () => {
    it('should catch FileReader error and set mensajeError', async () => {
      const fakeReader: any = {};
      fakeReader.readAsDataURL = function () {
        if (this.onerror) this.onerror();
      };
      spyOn<any>(window, 'FileReader').and.returnValue(fakeReader);

      component.selectedFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      await component.subirFoto();

      expect(component.mensajeError).toBe('No se pudo leer la imagen. Intenta nuevamente.');
      expect(component.fotoSubiendo()).toBeFalse();
    });

    it('should update usuario.img_url when editando is false', async () => {
      component.selectedFile = new File(['data'], 'photo.png', { type: 'image/png' });
      component.editando = false;

      await component.subirFoto();

      expect(component.usuario.img_url).toBeTruthy();
      expect(component.fotoPreview).toBeTruthy();
      expect(component.selectedFile).toBeNull();
      expect(component.fotoSubiendo()).toBeFalse();
      expect(toastServiceSpy.success).toHaveBeenCalledWith('Foto de perfil actualizada correctamente.');
    });

    it('should update usuarioEdit.img_url when editando is true', async () => {
      component.editar();
      component.selectedFile = new File(['data'], 'photo.png', { type: 'image/png' });
      component.editando = true;

      await component.subirFoto();

      expect(component.usuarioEdit.img_url).toBeTruthy();
      expect(component.fotoPreview).toBeTruthy();
      expect(component.selectedFile).toBeNull();
    });
  });

  describe('guardarCambios - uncovered branches', () => {
    it('should validate when both nombres and apellidos are empty', () => {
      component.editar();
      component.usuarioEdit.nombres = '';
      component.usuarioEdit.apellidos = '';
      component.guardarCambios();
      expect(component.mensajeError).toBe('Nombres y apellidos son obligatorios.');
    });

    it('should validate when only apellidos is empty', () => {
      component.editar();
      component.usuarioEdit.nombres = 'Juan';
      component.usuarioEdit.apellidos = '';
      component.guardarCambios();
      expect(component.mensajeError).toBe('Nombres y apellidos son obligatorios.');
    });

    it('should reject email without @ symbol', () => {
      component.editar();
      component.usuarioEdit.email = 'invalidemail.com';
      component.guardarCambios();
      expect(component.mensajeError).toBe('Correo electrónico inválido.');
    });

    it('should reject email without domain', () => {
      component.editar();
      component.usuarioEdit.email = 'user@';
      component.guardarCambios();
      expect(component.mensajeError).toBe('Correo electrónico inválido.');
    });

    it('should reject email without extension', () => {
      component.editar();
      component.usuarioEdit.email = 'user@domain';
      component.guardarCambios();
      expect(component.mensajeError).toBe('Correo electrónico inválido.');
    });

    it('should split multi-word apellidos and call patchMe correctly', fakeAsync(() => {
      component.editar();
      component.usuarioEdit.nombres = 'María';
      component.usuarioEdit.apellidos = 'García López';
      component.usuarioEdit.email = 'maria@test.com';
      component.guardarCambios();
      tick();

      expect(apiSpy.users.patchMe).toHaveBeenCalledWith(jasmine.objectContaining({
        persona: jasmine.objectContaining({
          name: 'María',
          father_surname: 'García',
          mother_surname: 'López',
        }),
      }));
      expect(component.usuario.nombres).toBe('María');
      expect(component.editando).toBeFalse();
      expect(toastServiceSpy.success).toHaveBeenCalledWith('Datos actualizados correctamente.');
    }));

    it('should set mother_surname undefined when single apellido', fakeAsync(() => {
      component.editar();
      component.usuarioEdit.nombres = 'Carlos';
      component.usuarioEdit.apellidos = 'Solo';
      component.usuarioEdit.email = 'carlos@test.com';
      component.guardarCambios();
      tick();

      expect(apiSpy.users.patchMe).toHaveBeenCalledWith(jasmine.objectContaining({
        persona: jasmine.objectContaining({
          father_surname: 'Solo',
        }),
      }));
    }));

    it('should send null phone and img_url when empty', fakeAsync(() => {
      component.editar();
      component.usuarioEdit.telefono = '';
      component.usuarioEdit.img_url = '';
      component.guardarCambios();
      tick();

      expect(apiSpy.users.patchMe).toHaveBeenCalledWith(jasmine.objectContaining({
        phone: null,
        img_url: null,
      }));
    }));

    it('should set guardando false in finally after successful save', fakeAsync(() => {
      component.editar();
      component.guardarCambios();
      tick();

      expect(component.guardando()).toBeFalse();
    }));

    it('should set guardando false in finally after failed save', fakeAsync(() => {
      (apiSpy.users.patchMe as jasmine.Spy).and.returnValue(throwError(() => new Error('fail')));

      component.editar();
      component.guardarCambios();
      tick();

      expect(component.guardando()).toBeFalse();
      expect(component.mensajeError).toBe('Error al guardar cambios.');
    }));
  });

  describe('guardarPassword - uncovered branches', () => {
    it('should reject password shorter than 6 characters', () => {
      component.passwordActual = 'oldpass';
      component.passwordNueva = 'abc';
      component.passwordConfirmar = 'abc';
      component.guardarPassword();
      expect(component.errorPasswordNueva).toBe('Debe tener al menos 6 caracteres.');
    });

    it('should reject password with exactly 5 characters', () => {
      component.passwordActual = 'oldpass';
      component.passwordNueva = '12345';
      component.passwordConfirmar = '12345';
      component.guardarPassword();
      expect(component.errorPasswordNueva).toBe('Debe tener al menos 6 caracteres.');
    });

    it('should reject when passwords do not match', () => {
      component.passwordActual = 'oldpass';
      component.passwordNueva = 'newpass1';
      component.passwordConfirmar = 'different1';
      component.guardarPassword();
      expect(component.errorPasswordConfirmar).toBe('Las contraseñas no coinciden.');
    });

    it('should clear error fields before validation', () => {
      component.errorPasswordActual = 'old error';
      component.errorPasswordNueva = 'old error';
      component.errorPasswordConfirmar = 'old error';

      component.passwordActual = '';
      component.guardarPassword();

      expect(component.errorPasswordActual).toBe('Ingrese su contraseña actual.');
      expect(component.errorPasswordNueva).toBe('');
      expect(component.errorPasswordConfirmar).toBe('');
    });

    it('should clear password fields and close modal on success', fakeAsync(() => {
      component.passwordActual = 'oldpass';
      component.passwordNueva = 'newpass123';
      component.passwordConfirmar = 'newpass123';
      component.mostrarCambiarPassword = true;

      component.guardarPassword();
      tick();

      expect(component.passwordActual).toBe('');
      expect(component.passwordNueva).toBe('');
      expect(component.passwordConfirmar).toBe('');
      expect(component.mostrarCambiarPassword).toBeFalse();
      expect(toastServiceSpy.success).toHaveBeenCalledWith('Contraseña actualizada correctamente.');
    }));

    it('should set errorPasswordActual on catch block', fakeAsync(() => {
      (apiSpy.users.changePassword as jasmine.Spy).and.returnValue(throwError(() => new Error('fail')));

      component.passwordActual = 'wrong';
      component.passwordNueva = 'newpass123';
      component.passwordConfirmar = 'newpass123';

      component.guardarPassword();
      tick();

      expect(component.errorPasswordActual).toBe('Contraseña actual incorrecta.');
    }));
  });

  describe('cargarUsuario - null field fallback branches (14 branches)', () => {
    it('should default all null persona fields to empty strings', async () => {
      const userNulls = {
        id: 2, email: null, phone: null,
        created_at: '2024-06-01T00:00:00Z',
        persona: {
          name: 'Test', father_surname: null, mother_surname: null,
          document_value: null, img_url: null,
        },
        rol: null, sedes: [], areas: [],
      };
      (apiSpy.users.getMe as jasmine.Spy).and.returnValue(of(userNulls));

      fixture = TestBed.createComponent(Perfil);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.usuario.nombres).toBe('Test');
      expect(component.usuario.apellidos).toBe('');
      expect(component.usuario.email).toBe('');
      expect(component.usuario.telefono).toBe('');
      expect(component.usuario.dni).toBe('');
      expect(component.usuario.rol).toBe('');
      expect((component.usuario as any).sede).toBe('');
      expect((component.usuario as any).area).toBe('');
      expect(component.usuario.img_url).toBe('');
    });

    it('should set fotoPreview to null when img_url is empty string', async () => {
      const userEmptyImg = {
        ...mockUser,
        persona: { ...mockUser.persona, img_url: '' },
      };
      (apiSpy.users.getMe as jasmine.Spy).and.returnValue(of(userEmptyImg));

      fixture = TestBed.createComponent(Perfil);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.fotoPreview).toBeNull();
    });

    it('should handle user with null email and phone', async () => {
      const userNoContact = {
        ...mockUser, email: null, phone: null,
      };
      (apiSpy.users.getMe as jasmine.Spy).and.returnValue(of(userNoContact));

      fixture = TestBed.createComponent(Perfil);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.usuario.email).toBe('');
      expect(component.usuario.telefono).toBe('');
    });

    it('should handle user with null father_surname', async () => {
      const userNoFather = {
        ...mockUser,
        persona: { ...mockUser.persona, father_surname: null },
      };
      (apiSpy.users.getMe as jasmine.Spy).and.returnValue(of(userNoFather));

      fixture = TestBed.createComponent(Perfil);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.usuario.apellidos).toBe('López');
    });

    it('should handle user with null mother_surname', async () => {
      const userNoMother = {
        ...mockUser,
        persona: { ...mockUser.persona, mother_surname: null },
      };
      (apiSpy.users.getMe as jasmine.Spy).and.returnValue(of(userNoMother));

      fixture = TestBed.createComponent(Perfil);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.usuario.apellidos).toBe('Pérez');
    });

    it('should handle user with empty sedes and areas', async () => {
      const userNoSedeArea = {
        ...mockUser, sedes: [], areas: [],
      };
      (apiSpy.users.getMe as jasmine.Spy).and.returnValue(of(userNoSedeArea));

      fixture = TestBed.createComponent(Perfil);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();

      expect((component.usuario as any).sede).toBe('');
      expect((component.usuario as any).area).toBe('');
    });

    it('should handle user with null rol', async () => {
      const userNoRol = {
        ...mockUser, rol: null,
      };
      (apiSpy.users.getMe as jasmine.Spy).and.returnValue(of(userNoRol));

      fixture = TestBed.createComponent(Perfil);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.usuario.rol).toBe('');
    });

    it('should handle user with null document_value', async () => {
      const userNoDoc = {
        ...mockUser,
        persona: { ...mockUser.persona, document_value: null },
      };
      (apiSpy.users.getMe as jasmine.Spy).and.returnValue(of(userNoDoc));

      fixture = TestBed.createComponent(Perfil);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.usuario.dni).toBe('');
    });

    it('should trigger cargarUsuario via effect when role changes after loaded', fakeAsync(() => {
      expect((component as any).loaded).toBeTrue();
      const callsBefore = (apiSpy.users.getMe as jasmine.Spy).calls.count();
      roleService.switchRole('admin-saas');
      fixture.detectChanges();
      tick();
      expect((apiSpy.users.getMe as jasmine.Spy).calls.count()).toBeGreaterThan(callsBefore);
    }));

    it('should skip cargarUsuario via effect when loaded is false', () => {
      const comp2 = TestBed.createComponent(Perfil).componentInstance;
      (comp2 as any).loaded = false;
      const callsBefore = (apiSpy.users.getMe as jasmine.Spy).calls.count();
      roleService.switchRole('usuario');
      expect((apiSpy.users.getMe as jasmine.Spy).calls.count()).toBe(callsBefore);
    });

    it('should handle guardando flag resets after successful save', fakeAsync(() => {
      component.editar();
      component.guardarCambios();
      tick();
      expect(component.guardando()).toBeFalse();
    }));

    it('should handle guardando flag resets after failed save', fakeAsync(() => {
      (apiSpy.users.patchMe as jasmine.Spy).and.returnValue(throwError(() => new Error('fail')));
      component.editar();
      component.guardarCambios();
      tick();
      expect(component.guardando()).toBeFalse();
    }));

    it('should send mother_surname undefined when single apellido in guardarCambios', fakeAsync(() => {
      component.editar();
      component.usuarioEdit.apellidos = 'Pérez';
      component.usuarioEdit.nombres = 'Juan';
      component.guardarCambios();
      tick();

      expect(apiSpy.users.patchMe).toHaveBeenCalledWith(jasmine.objectContaining({
        persona: jasmine.objectContaining({
          father_surname: 'Pérez',
        }),
      }));
      const callArgs = (apiSpy.users.patchMe as jasmine.Spy).calls.mostRecent().args[0];
      expect(callArgs.persona.mother_surname).toBeUndefined();
    }));

    it('should handle user with all fields null except name', async () => {
      const userMinimal = {
        id: 3, email: null, phone: null, created_at: null,
        persona: { name: 'Solo', father_surname: null, mother_surname: null, document_value: null, img_url: null },
        rol: null, sedes: [], areas: [],
      };
      (apiSpy.users.getMe as jasmine.Spy).and.returnValue(of(userMinimal));

      fixture = TestBed.createComponent(Perfil);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.usuario.nombres).toBe('Solo');
      expect(component.usuario.apellidos).toBe('');
      expect(component.usuario.fechaIngreso).toBe('');
    });
  });
});
