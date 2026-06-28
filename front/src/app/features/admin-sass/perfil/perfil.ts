import { Component, effect, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { PerfilBase, DatosPersonales } from '../../../shared/perfil/perfil-base';
import { ApiService } from '../../../api/services/api.service';
import { RoleService } from '../../../services/role.service';

@Component({
  selector: 'app-perfil',
  imports: [FormsModule],
  templateUrl: './perfil.html',
  styleUrl: '../../../shared/perfil/perfil.css',
})
export class Perfil extends PerfilBase implements OnInit {
  private readonly api = inject(ApiService);
  private readonly roleService = inject(RoleService);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly guardando = signal(false);

  selectedFile: File | null = null;
  fotoPreview: string | null = null;
  fotoSubiendo = signal(false);
  fotoProgreso = signal(0);
  private loaded = false;

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

  async ngOnInit(): Promise<void> {
    await this.cargarUsuario();
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

  override async guardarCambios(): Promise<void> {
    if (!this.usuarioEdit.nombres.trim() || !this.usuarioEdit.apellidos.trim()) {
      this.mensajeError = 'Nombres y apellidos son obligatorios.';
      return;
    }
    if (!this.usuarioEdit.email.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
      this.mensajeError = 'Correo electrónico inválido.';
      return;
    }

    this.guardando.set(true);
    this.limpiarMensajes();
    try {
      await firstValueFrom(
        this.api.users.patchMasterMe({
          img_url: this.usuarioEdit.img_url || null,
        })
      );
      this.usuario = { ...this.usuarioEdit };
      this.editando = false;
      this.mostrarExito('Datos actualizados correctamente.');
    } catch {
      this.mensajeError = 'No se pudieron guardar los cambios. Intenta nuevamente.';
    } finally {
      this.guardando.set(false);
    }
  }

  override cancelarEdicion(): void {
    this.fotoPreview = this.usuario.img_url || null;
    this.selectedFile = null;
    super.cancelarEdicion();
  }
}
