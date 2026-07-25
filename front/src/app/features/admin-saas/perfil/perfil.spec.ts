import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { ApiService } from '../../../api/services/api.service';
import type { MasterUserProfile } from '../../../api/types';
import { Perfil } from './perfil';

const CURRENT_PASSWORD = 'admin123';

const mockUser: MasterUserProfile = {
  id: 'user-1', email: 'jose.alata@trazzo.com', phone: '999888777',
  tenant_id: null, must_change_password: false,
  created_at: '2024-01-15T00:00:00Z',
  persona: {
    id: 1, img_url: null, document_type: 'DNI', document_value: '12345678',
    name: 'Jose', father_surname: 'Alata', mother_surname: 'Pérez', birth_date: null,
  },
  MetodoRecuperacion: [],
  roles: [{ id: 1, name: 'Super Admin', descripcion: null }],
  tenant_info: null,
};

const mockApi = {
  users: {
    getMasterMe: () => of(mockUser),
    patchMasterMe: (_body?: any) => of({} as any),
  },
};

describe('Perfil', () => {
  let component: Perfil;
  let fixture: ComponentFixture<Perfil>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Perfil, FormsModule],
      providers: [{ provide: ApiService, useValue: mockApi }],
    }).compileComponents();

    fixture = TestBed.createComponent(Perfil);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial user data', () => {
    expect(component.usuario.nombres).toBe('Jose');
    expect(component.usuario.email).toBe('jose.alata@trazzo.com');
  });

  it('should editar create copy and set editando', () => {
    component.editar();
    expect(component.editando).toBeTrue();
    expect(component.usuarioEdit.nombres).toBe(component.usuario.nombres);
  });

  it('should cancelarEdicion', () => {
    component.editar();
    component.cancelarEdicion();
    expect(component.editando).toBeFalse();
    expect(component.mensajeError).toBe('');
  });

  it('should guardarCambios with valid data', async () => {
    component.editar();
    component.usuarioEdit.nombres = 'Nuevo Nombre';
    await component.guardarCambios();
    expect(component.usuario.nombres).toBe('Nuevo Nombre');
    expect(component.editando).toBeFalse();
    expect(component.mensajeExito).toBe('Datos actualizados correctamente.');
  });

  it('should reject guardarCambios with empty nombres', () => {
    component.editar();
    component.usuarioEdit.nombres = '';
    component.guardarCambios();
    expect(component.mensajeError).toBe('Nombres y apellidos son obligatorios.');
    expect(component.editando).toBeTrue();
  });

  it('should reject guardarCambios with empty apellidos', () => {
    component.editar();
    component.usuarioEdit.apellidos = '';
    component.guardarCambios();
    expect(component.mensajeError).toBe('Nombres y apellidos son obligatorios.');
  });

  it('should reject guardarCambios with invalid email', () => {
    component.editar();
    component.usuarioEdit.email = 'invalido';
    component.guardarCambios();
    expect(component.mensajeError).toBe('Correo electrónico inválido.');
  });

  it('should guardarPassword require current password', () => {
    component.guardarPassword();
    expect(component.errorPasswordActual).toBe('Ingrese su contraseña actual.');
  });

  it('should guardarPassword reject short new password', () => {
    component.passwordActual = CURRENT_PASSWORD;
    component.passwordNueva = '12345';
    component.guardarPassword();
    expect(component.errorPasswordNueva).toBe('Debe tener al menos 6 caracteres.');
  });

  it('should guardarPassword reject mismatched passwords', () => {
    component.passwordActual = CURRENT_PASSWORD;
    component.passwordNueva = 'newpass1';
    component.passwordConfirmar = 'different';
    component.guardarPassword();
    expect(component.errorPasswordConfirmar).toBe('Las contraseñas no coinciden.');
  });

  it('should guardarPassword succeed', () => {
    component.passwordActual = CURRENT_PASSWORD;
    component.passwordNueva = 'newpass123';
    component.passwordConfirmar = 'newpass123';
    component.guardarPassword();
    expect(component.mostrarCambiarPassword).toBeFalse();
    expect(component.mensajeExito).toBe('Contraseña actualizada correctamente.');
    expect(component.passwordActual).toBe('');
    expect(component.passwordNueva).toBe('');
    expect(component.passwordConfirmar).toBe('');
  });

  it('should clear success message after timeout', fakeAsync(() => {
    component.mostrarExito('Test');
    expect(component.mensajeExito).toBe('Test');
    tick(3500);
    expect(component.mensajeExito).toBe('');
  }));

  it('should limpiarMensajes clear all', () => {
    component.mensajeExito = 'test';
    component.mensajeError = 'test';
    component.errorPasswordActual = 'test';
    component.errorPasswordNueva = 'test';
    component.errorPasswordConfirmar = 'test';
    component.limpiarMensajes();
    expect(component.mensajeExito).toBe('');
    expect(component.mensajeError).toBe('');
    expect(component.errorPasswordActual = '');
    expect(component.errorPasswordNueva = '');
    expect(component.errorPasswordConfirmar = '');
  });

  it('should handle cargarUsuario API error gracefully', async () => {
    spyOn(mockApi.users, 'getMasterMe').and.returnValue(throwError(() => new Error('network')));
    await component.cargarUsuario();
    expect(component.error()).toBe('No se pudieron cargar los datos del perfil. Verifica tu conexión e intenta nuevamente.');
    expect(component.loading()).toBeFalse();
    expect(component['loaded']).toBeTrue();
  });

  it('should set rol to empty string when user has no roles', async () => {
    spyOn(mockApi.users, 'getMasterMe').and.returnValue(of({ ...mockUser, roles: [] }));
    await component.cargarUsuario();
    expect(component.usuario.rol).toBe('');
  });

  it('should set fechaIngreso to empty when created_at is null', async () => {
    spyOn(mockApi.users, 'getMasterMe').and.returnValue(of({ ...mockUser, created_at: null } as any));
    await component.cargarUsuario();
    expect(component.usuario.fechaIngreso).toBe('');
  });

  it('should fallback to empty strings when persona fields are null', async () => {
    spyOn(mockApi.users, 'getMasterMe').and.returnValue(of({
      ...mockUser,
      email: null,
      phone: null,
      persona: { ...mockUser.persona, name: null, father_surname: null, mother_surname: null, document_value: null, img_url: 'http://img.test/a.jpg' },
    } as any));
    await component.cargarUsuario();
    expect(component.usuario.nombres).toBe('');
    expect(component.usuario.apellidos).toBe('');
    expect(component.usuario.email).toBe('');
    expect(component.usuario.telefono).toBe('');
    expect(component.usuario.dni).toBe('');
    expect(component.usuario.img_url).toBe('http://img.test/a.jpg');
    expect(component.fotoPreview).toBe('http://img.test/a.jpg');
  });

  it('should set mother_surname undefined when apellidos has single word', async () => {
    const spy = spyOn(mockApi.users, 'patchMasterMe').and.returnValue(of({}));
    component.editar();
    component.usuarioEdit.apellidos = 'Alata';
    component.guardarCambios();
    await new Promise(r => setTimeout(r, 0));
    expect(spy).toHaveBeenCalledWith(jasmine.objectContaining({
      persona: jasmine.objectContaining({ father_surname: 'Alata', mother_surname: undefined }),
    }));
  });

  it('should join remaining surnames for mother_surname', async () => {
    const spy = spyOn(mockApi.users, 'patchMasterMe').and.returnValue(of({}));
    component.editar();
    component.usuarioEdit.apellidos = 'Alata Perez de la Cruz';
    component.guardarCambios();
    await new Promise(r => setTimeout(r, 0));
    expect(spy).toHaveBeenCalledWith(jasmine.objectContaining({
      persona: jasmine.objectContaining({ father_surname: 'Alata', mother_surname: 'Perez de la Cruz' }),
    }));
  });

  it('should set error message when guardarCambios patchMasterMe fails', async () => {
    spyOn(mockApi.users, 'patchMasterMe').and.returnValue(throwError(() => new Error('fail')));
    component.editar();
    component.usuarioEdit.nombres = 'Test';
    component.usuarioEdit.apellidos = 'User';
    component.usuarioEdit.email = 'test@test.com';
    component.guardarCambios();
    await new Promise(r => setTimeout(r, 0));
    expect(component.mensajeError).toBe('No se pudieron guardar los cambios. Intenta nuevamente.');
    expect(component.guardando()).toBeFalse();
  });

  it('should reject guardarCambios with whitespace-only nombres', () => {
    component.editar();
    component.usuarioEdit.nombres = '   ';
    component.guardarCambios();
    expect(component.mensajeError).toBe('Nombres y apellidos son obligatorios.');
  });

  it('should reject guardarCambios with whitespace-only apellidos', () => {
    component.editar();
    component.usuarioEdit.nombres = 'Nombre';
    component.usuarioEdit.apellidos = '   ';
    component.guardarCambios();
    expect(component.mensajeError).toBe('Nombres y apellidos son obligatorios.');
  });

  it('should send truthy img_url and null phone when telefono is empty', async () => {
    const spy = spyOn(mockApi.users, 'patchMasterMe').and.returnValue(of({}));
    component.editar();
    component.usuarioEdit.nombres = 'Test';
    component.usuarioEdit.apellidos = 'User';
    component.usuarioEdit.email = 'test@test.com';
    component.usuarioEdit.img_url = 'http://example.com/photo.jpg';
    component.usuarioEdit.telefono = '';
    component.guardarCambios();
    await new Promise(r => setTimeout(r, 0));
    expect(spy).toHaveBeenCalledWith(jasmine.objectContaining({
      img_url: 'http://example.com/photo.jpg',
      phone: null,
    }));
  });

  it('should return early from subirFoto when no file is selected', async () => {
    component.selectedFile = null;
    await component.subirFoto();
    expect(component.fotoSubiendo()).toBeFalse();
    expect(component.mensajeError).toBe('');
  });

  it('should show error message when FileReader fails during subirFoto', async () => {
    const OrigFileReader = window.FileReader;
    (window as any).FileReader = class {
      readAsDataURL() { this.onerror?.(new ProgressEvent('error')); }
      onerror: any; onload: any; result: any;
    };
    component.selectedFile = new File(['x'], 'test.png', { type: 'image/png' });
    await component.subirFoto();
    expect(component.mensajeError).toBe('No se pudo leer la imagen. Intenta nuevamente.');
    expect(component.fotoSubiendo()).toBeFalse();
    (window as any).FileReader = OrigFileReader;
  });

  it('should set img_url on usuarioEdit when subirFoto succeeds while editing', async () => {
    component.editar();
    component.selectedFile = new File(['data'], 'test.png', { type: 'image/png' });
    await component.subirFoto();
    expect(component.usuarioEdit.img_url).toContain('data:');
    expect(component.fotoPreview).toContain('data:');
    expect(component.selectedFile).toBeNull();
    expect(component.mensajeExito).toBe('Foto de perfil actualizada correctamente.');
  });

  it('should set img_url on usuario when subirFoto succeeds while not editing', async () => {
    component.editando = false;
    component.selectedFile = new File(['data'], 'test.png', { type: 'image/png' });
    await component.subirFoto();
    expect(component.usuario.img_url).toContain('data:');
    expect(component.fotoPreview).toContain('data:');
  });
});
