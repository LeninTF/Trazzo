import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

export interface DatosPersonales {
  nombres: string;
  apellidos: string;
  email: string;
  telefono: string;
  dni: string;
  rol: string;
  fechaIngreso: string;
  [key: string]: string;
}

export abstract class PerfilBase {
  abstract usuario: DatosPersonales;

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
    this.editando = false;
    this.limpiarMensajes();
  }

  guardarCambios(): void {
    if (!this.usuarioEdit.nombres.trim() || !this.usuarioEdit.apellidos.trim()) {
      this.mensajeError = 'Nombres y apellidos son obligatorios.';
      return;
    }
    if (!this.usuarioEdit.email.match(/^\S+@\S+\.\S+$/)) {
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
