import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface DatosPersonales {
  nombres: string;
  apellidos: string;
  email: string;
  telefono: string;
  dni: string;
  rol: string;
  sede: string;
  area: string;
  fechaIngreso: string;
}

@Component({
  selector: 'app-perfil',
  imports: [FormsModule],
  templateUrl: './perfil.html',
  styleUrl: './perfil.css',
})
export class Perfil {
  private passwordFijo = 'admin123';

  usuario: DatosPersonales = {
    nombres: 'Jose',
    apellidos: 'Alata',
    email: 'jose.alata@utp.edu.pe',
    telefono: '+51 999 888 777',
    dni: '71234567',
    rol: 'Administrador',
    sede: 'Sede Principal',
    area: 'Tecnología',
    fechaIngreso: '01/03/2020',
  };

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
    if (this.passwordActual !== this.passwordFijo) {
      this.errorPasswordActual = 'La contraseña actual no es correcta.';
      return;
    }
    if (!this.passwordNueva || this.passwordNueva.length < 6) {
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
