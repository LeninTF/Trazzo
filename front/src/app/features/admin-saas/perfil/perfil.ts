import { Component, effect, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { PerfilBase, DatosPersonales } from '../../../shared/perfil/perfil-base';
import { ApiService } from '../../../api/services/api.service';
import { RoleService } from '../../../services/role.service';

@Component({
  selector: 'app-perfil',
  imports: [FormsModule],
  templateUrl: '../../../shared/perfil/perfil.html',
  styleUrl: '../../../shared/perfil/perfil.css',
})
export class Perfil extends PerfilBase implements OnInit {
  private readonly api = inject(ApiService);
  private readonly roleService = inject(RoleService);

  override usuario: DatosPersonales = {
    nombres: '', apellidos: '', email: '', telefono: '',
    dni: '', rol: '', fechaIngreso: '', img_url: '',
  };

  constructor() {
    super();
    effect(() => {
      this.roleService.role();
      if (this.loaded) {
        this.cargarUsuario();
      }
    });
  }

  ngOnInit(): void {
    this.cargarUsuario();
  }

  async cargarUsuario(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      const u = await firstValueFrom(this.api.users.getMasterMe());
      const p = u.persona;
      this.usuario = {
        nombres: p.name ?? '',
        apellidos: `${p.father_surname ?? ''} ${p.mother_surname ?? ''}`.trim(),
        email: u.email ?? '',
        telefono: u.phone ?? '',
        dni: p.document_value ?? '',
        rol: u.roles[0]?.name ?? '',
        fechaIngreso: u.created_at ? new Date(u.created_at).toLocaleDateString('es-PE') : '',
        img_url: p.img_url ?? '',
      };
      this.fotoPreview = this.usuario.img_url || null;
      this.roleService.setUserInfo(`${p.name} ${p.father_surname}`.trim(), u.email ?? '');
    } catch {
      this.error.set('No se pudieron cargar los datos del perfil. Verifica tu conexión e intenta nuevamente.');
    } finally {
      this.loading.set(false);
      this.loaded = true;
    }
  }

  override guardarCambios(): void {
    if (!this.usuarioEdit.nombres.trim() || !this.usuarioEdit.apellidos.trim()) {
      this.mensajeError = 'Nombres y apellidos son obligatorios.';
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.usuarioEdit.email)) {
      this.mensajeError = 'Correo electrónico inválido.';
      return;
    }

    const apellidos = this.usuarioEdit.apellidos.split(' ');
    this.guardando.set(true);
    this.limpiarMensajes();
    firstValueFrom(
      this.api.users.patchMasterMe({
        img_url: this.usuarioEdit.img_url || null,
        email: this.usuarioEdit.email,
        phone: this.usuarioEdit.telefono || null,
        persona: {
          name: this.usuarioEdit.nombres,
          father_surname: apellidos[0] ?? '',
          mother_surname: apellidos.slice(1).join(' ') || undefined,
        },
      })
    ).then(() => {
      this.usuario = { ...this.usuarioEdit };
      this.editando = false;
      this.mostrarExito('Datos actualizados correctamente.');
    }).catch(() => {
      this.mensajeError = 'No se pudieron guardar los cambios. Intenta nuevamente.';
    }).finally(() => {
      this.guardando.set(false);
    });
  }
}
