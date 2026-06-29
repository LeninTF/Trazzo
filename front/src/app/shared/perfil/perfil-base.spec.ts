import { fakeAsync, tick } from '@angular/core/testing';
import { PerfilBase, type DatosPersonales } from './perfil-base';

class TestPerfil extends PerfilBase {
  usuario: DatosPersonales = {
    nombres: 'Juan',
    apellidos: 'Pérez',
    email: 'juan@test.com',
    telefono: '123456789',
    dni: '12345678',
    rol: 'Admin',
    fechaIngreso: '2024-01-01',
    img_url: '',
  };
}

describe('PerfilBase', () => {
  let component: TestPerfil;

  beforeEach(() => {
    component = new TestPerfil();
  });

  it('should initialize signals with default values', () => {
    expect(component.loading()).toBeTrue();
    expect(component.error()).toBe('');
    expect(component.guardando()).toBeFalse();
    expect(component.fotoSubiendo()).toBeFalse();
    expect(component.selectedFile).toBeNull();
    expect(component.fotoPreview).toBeNull();
    expect(component.editando).toBeFalse();
  });

  describe('editar', () => {
    it('should set editando to true and copy usuario to usuarioEdit', () => {
      component.editar();
      expect(component.editando).toBeTrue();
      expect(component.usuarioEdit).toEqual(component.usuario);
      expect(component.usuarioEdit).not.toBe(component.usuario);
    });

    it('should clear messages when editing', () => {
      component.mensajeError = 'some error';
      component.mensajeExito = 'some success';
      component.editar();
      expect(component.mensajeError).toBe('');
      expect(component.mensajeExito).toBe('');
    });
  });

  describe('cancelarEdicion', () => {
    it('should set editando to false and reset photo state', () => {
      component.editando = true;
      component.selectedFile = new File([''], 'test.png');
      component.fotoPreview = 'data:image/png;base64,abc';
      component.usuario.img_url = 'http://example.com/foto.jpg';

      component.cancelarEdicion();

      expect(component.editando).toBeFalse();
      expect(component.selectedFile).toBeNull();
      expect(component.fotoPreview).toBe('http://example.com/foto.jpg');
    });

    it('should clear messages on cancel', () => {
      component.editando = true;
      component.mensajeError = 'some error';
      component.mensajeExito = 'some success';
      component.cancelarEdicion();
      expect(component.mensajeError).toBe('');
      expect(component.mensajeExito).toBe('');
    });
  });

  describe('onUrlCambio', () => {
    it('should update usuarioEdit.img_url and fotoPreview', () => {
      component.usuarioEdit = { ...component.usuario };
      component.onUrlCambio('http://new-image.com/foto.jpg');
      expect(component.usuarioEdit.img_url).toBe('http://new-image.com/foto.jpg');
      expect(component.fotoPreview).toBe('http://new-image.com/foto.jpg');
      expect(component.selectedFile).toBeNull();
    });

    it('should set preview to null when url is empty', () => {
      component.usuarioEdit = { ...component.usuario };
      component.fotoPreview = 'old-preview';
      component.onUrlCambio('');
      expect(component.usuarioEdit.img_url).toBe('');
      expect(component.fotoPreview).toBeNull();
    });
  });

  describe('onFotoSeleccionada', () => {
    it('should set error for non-image file', () => {
      const input = document.createElement('input');
      const file = new File(['test'], 'test.txt', { type: 'text/plain' });
      Object.defineProperty(input, 'files', { value: [file] });

      component.onFotoSeleccionada({ target: input } as unknown as Event);
      expect(component.mensajeError).toBe('Selecciona un archivo de imagen válido.');
    });

    it('should set error for file over 2MB', () => {
      const input = document.createElement('input');
      const file = new File(['x'.repeat(3 * 1024 * 1024)], 'large.jpg', { type: 'image/jpeg' });
      Object.defineProperty(input, 'files', { value: [file] });

      component.onFotoSeleccionada({ target: input } as unknown as Event);
      expect(component.mensajeError).toBe('La imagen no debe superar los 2 MB.');
    });

    it('should do nothing when no file selected', () => {
      const input = document.createElement('input');
      Object.defineProperty(input, 'files', { value: [] });

      component.onFotoSeleccionada({ target: input } as unknown as Event);
      expect(component.selectedFile).toBeNull();
    });
  });

  describe('subirFoto', () => {
    it('should return early if no selectedFile', async () => {
      await component.subirFoto();
      expect(component.fotoSubiendo()).toBeFalse();
    });

    it('should read file and update fotoPreview', async () => {
      const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      component.selectedFile = file;
      component.editando = true;
      component.usuarioEdit = { ...component.usuario };

      await component.subirFoto();

      expect(component.fotoSubiendo()).toBeFalse();
      expect(component.selectedFile).toBeNull();
      expect(component.usuarioEdit.img_url).toContain('data:');
      expect(component.mensajeExito).toBe('Foto de perfil actualizada correctamente.');
    });
  });

  describe('guardarCambios', () => {
    it('should validate required fields', () => {
      component.editar();
      component.usuarioEdit.nombres = '';
      component.guardarCambios();
      expect(component.mensajeError).toBe('Nombres y apellidos son obligatorios.');
    });

    it('should validate email format', () => {
      component.editar();
      component.usuarioEdit.email = 'invalid';
      component.guardarCambios();
      expect(component.mensajeError).toBe('Correo electrónico inválido.');
    });

    it('should save changes on valid data', () => {
      const originalUser = { ...component.usuario };
      component.editar();
      component.usuarioEdit.nombres = 'Carlos';
      component.guardarCambios();

      expect(component.usuario.nombres).toBe('Carlos');
      expect(component.editando).toBeFalse();
      expect(component.mensajeExito).toBe('Datos actualizados correctamente.');
    });
  });

  describe('guardarPassword', () => {
    it('should require current password', () => {
      component.guardarPassword();
      expect(component.errorPasswordActual).toBe('Ingrese su contraseña actual.');
    });

    it('should require minimum 6 characters', () => {
      component.passwordActual = 'oldpass';
      component.passwordNueva = '12345';
      component.guardarPassword();
      expect(component.errorPasswordNueva).toBe('Debe tener al menos 6 caracteres.');
    });

    it('should require passwords to match', () => {
      component.passwordActual = 'oldpass';
      component.passwordNueva = 'newpass123';
      component.passwordConfirmar = 'different';
      component.guardarPassword();
      expect(component.errorPasswordConfirmar).toBe('Las contraseñas no coinciden.');
    });

    it('should clear fields and show success on valid password', () => {
      component.passwordActual = 'oldpass';
      component.passwordNueva = 'newpass123';
      component.passwordConfirmar = 'newpass123';
      component.guardarPassword();

      expect(component.passwordActual).toBe('');
      expect(component.passwordNueva).toBe('');
      expect(component.passwordConfirmar).toBe('');
      expect(component.mostrarCambiarPassword).toBeFalse();
      expect(component.mensajeExito).toBe('Contraseña actualizada correctamente.');
    });
  });

  describe('mostrarExito', () => {
    it('should set success message and clear error', () => {
      component.mostrarExito('Success!');
      expect(component.mensajeExito).toBe('Success!');
      expect(component.mensajeError).toBe('');
    });

    it('should clear success message after timeout', fakeAsync(() => {
      component.mostrarExito('Success!');
      tick(3500);
      expect(component.mensajeExito).toBe('');
    }));
  });

  describe('limpiarMensajes', () => {
    it('should clear all messages', () => {
      component.mensajeExito = 'success';
      component.mensajeError = 'error';
      component.errorPasswordActual = 'err1';
      component.errorPasswordNueva = 'err2';
      component.errorPasswordConfirmar = 'err3';

      component.limpiarMensajes();

      expect(component.mensajeExito).toBe('');
      expect(component.mensajeError).toBe('');
      expect(component.errorPasswordActual).toBe('');
      expect(component.errorPasswordNueva).toBe('');
      expect(component.errorPasswordConfirmar).toBe('');
    });
  });
});
