import { signal } from '@angular/core';

export interface DatosPersonales {
  nombres: string;
  apellidos: string;
  email: string;
  telefono: string;
  dni: string;
  rol: string;
  fechaIngreso: string;
  img_url: string;
  [key: string]: string;
}

export abstract class PerfilBase {
  abstract usuario: DatosPersonales;

  readonly loading = signal(true);
  readonly error = signal('');
  readonly guardando = signal(false);

  selectedFile: File | null = null;
  fotoPreview: string | null = null;
  readonly fotoSubiendo = signal(false);
  protected loaded = false;

  editando = false;
  usuarioEdit!: DatosPersonales;

  mostrarCambiarPassword = false;
  passwordActual = '';
  passwordNueva = '';
  passwordConfirmar = '';

  mensajeExito = '';
  mensajeError = '';
  errorPasswordActual = '';
  errorPasswordNueva = '';
  errorPasswordConfirmar = '';

  editar(): void {
    this.usuarioEdit = { ...this.usuario };
    this.editando = true;
    this.limpiarMensajes();
  }

  cancelarEdicion(): void {
    this.fotoPreview = this.usuario.img_url || null;
    this.selectedFile = null;
    this.editando = false;
    this.limpiarMensajes();
  }

  onUrlCambio(url: string): void {
    this.usuarioEdit.img_url = url;
    this.fotoPreview = url || null;
    this.selectedFile = null;
  }

  onFotoSeleccionada(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      this.mensajeError = 'Selecciona un archivo de imagen válido.';
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      this.mensajeError = 'La imagen no debe superar los 2 MB.';
      return;
    }

    this.selectedFile = file;
    const reader = new FileReader();
    reader.onload = () => {
      this.fotoPreview = reader.result as string;
    };
    reader.readAsDataURL(file);
  }

  async subirFoto(): Promise<void> {
    if (!this.selectedFile) return;
    this.fotoSubiendo.set(true);
    this.limpiarMensajes();
    try {
      const b64 = await new Promise<string>((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result as string);
        reader.onerror = () => reject();
        reader.readAsDataURL(this.selectedFile!);
      });
      const target = this.editando ? this.usuarioEdit : this.usuario;
      target.img_url = b64;
      this.fotoPreview = b64;
      this.selectedFile = null;
      this.mostrarExito('Foto de perfil actualizada correctamente.');
    } catch {
      this.mensajeError = 'No se pudo leer la imagen. Intenta nuevamente.';
    } finally {
      this.fotoSubiendo.set(false);
    }
  }

  guardarCambios(): void {
    if (!this.usuarioEdit.nombres.trim() || !this.usuarioEdit.apellidos.trim()) {
      this.mensajeError = 'Nombres y apellidos son obligatorios.';
      return;
    }
    if (!this.usuarioEdit.email.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
      this.mensajeError = 'Correo electrónico inválido.';
      return;
    }
    this.usuario = { ...this.usuarioEdit };
    this.editando = false;
    this.mostrarExito('Datos actualizados correctamente.');
  }

  guardarPassword(): void {
    this.errorPasswordActual = '';
    this.errorPasswordNueva = '';
    this.errorPasswordConfirmar = '';
    this.limpiarMensajes();

    if (!this.passwordActual) {
      this.errorPasswordActual = 'Ingrese su contraseña actual.';
      return;
    }
    if (this.passwordNueva.length < 6) {
      this.errorPasswordNueva = 'Debe tener al menos 6 caracteres.';
      return;
    }
    if (this.passwordNueva !== this.passwordConfirmar) {
      this.errorPasswordConfirmar = 'Las contraseñas no coinciden.';
      return;
    }

    this.passwordActual = '';
    this.passwordNueva = '';
    this.passwordConfirmar = '';
    this.mostrarCambiarPassword = false;
    this.mostrarExito('Contraseña actualizada correctamente.');
  }

  mostrarExito(msg: string): void {
    this.mensajeExito = msg;
    this.mensajeError = '';
    setTimeout(() => this.mensajeExito = '', 3500);
  }

  limpiarMensajes(): void {
    this.mensajeExito = '';
    this.mensajeError = '';
    this.errorPasswordActual = '';
    this.errorPasswordNueva = '';
    this.errorPasswordConfirmar = '';
  }
}
