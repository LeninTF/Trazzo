import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { PerfilBase, DatosPersonales } from '../../../shared/perfil/perfil-base';
import { ApiService } from '../../../api/services/api.service';
import { ToastService } from '../../../services/toast.service';
import { RoleService } from '../../../services/role.service';

@Component({
  selector: 'app-perfil',
  imports: [FormsModule],
  templateUrl: './perfil.html',
  styleUrl: '../../../shared/perfil/perfil.css',
})
export class Perfil extends PerfilBase implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toastService = inject(ToastService);
  private readonly roleService = inject(RoleService);

  readonly loading = signal(true);
  readonly error = signal('');

  override usuario: DatosPersonales = {
    nombres: '', apellidos: '', email: '', telefono: '',
    dni: '', rol: '', sede: '', area: '', fechaIngreso: '',
  };
  private userId = 0;

  async ngOnInit(): Promise<void> {
    await this.cargarUsuario();
  }

  async cargarUsuario(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      const u = await firstValueFrom(this.api.users.getMe());
      this.userId = u.id;
      const p = u.persona;
      this.usuario = {
        nombres: p.name ?? '',
        apellidos: `${p.father_surname ?? ''} ${p.mother_surname ?? ''}`.trim(),
        email: u.email ?? '',
        telefono: u.phone ?? '',
        dni: p.document_value ?? '',
        rol: u.rol?.name ?? '',
        sede: u.sedes[0]?.nombre ?? '',
        area: u.areas[0]?.nombre ?? '',
        fechaIngreso: u.created_at ? new Date(u.created_at).toLocaleDateString('es-PE') : '',
      };
      this.roleService.setUserInfo(`${p.name} ${p.father_surname}`.trim(), u.email ?? '');
    } catch {
      this.error.set('No se pudieron cargar los datos del perfil. Verifica tu conexión e intenta nuevamente.');
    } finally {
      this.loading.set(false);
    }
  }

  override guardarCambios(): void {
    if (!this.usuarioEdit.nombres.trim() || !this.usuarioEdit.apellidos.trim()) {
      this.mensajeError = 'Nombres y apellidos son obligatorios.';
      return;
    }
    if (!this.usuarioEdit.email.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
      this.mensajeError = 'Correo electrónico inválido.';
      return;
    }
    const [name, ...rest] = this.usuarioEdit.nombres.trim().split(/\s+/);
    const fatherSurname = this.usuarioEdit.apellidos.trim().split(/\s+/)[0] ?? '';
    const motherSurname = this.usuarioEdit.apellidos.trim().split(/\s+/).slice(1).join(' ') || undefined;
    firstValueFrom(this.api.users.patchMe({
      phone: this.usuarioEdit.telefono || null,
    })).then(() => {
      this.usuario = { ...this.usuarioEdit };
      this.editando = false;
      this.toastService.success('Datos actualizados correctamente.');
    }).catch(() => {
      this.mensajeError = 'Error al guardar cambios.';
    });
  }

  override guardarPassword(): void {
    this.errorPasswordActual = '';
    this.errorPasswordNueva = '';
    this.errorPasswordConfirmar = '';

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

    firstValueFrom(this.api.users.changePassword(this.userId, {
      current_password: this.passwordActual,
      new_password: this.passwordNueva,
    })).then(() => {
      this.passwordActual = '';
      this.passwordNueva = '';
      this.passwordConfirmar = '';
      this.mostrarCambiarPassword = false;
      this.toastService.success('Contraseña actualizada correctamente.');
    }).catch(() => {
      this.errorPasswordActual = 'Contraseña actual incorrecta.';
    });
  }
}
