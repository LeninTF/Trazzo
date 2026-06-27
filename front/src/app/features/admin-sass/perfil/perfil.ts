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
  private loaded = false;

  override usuario: DatosPersonales = {
    nombres: '', apellidos: '', email: '', telefono: '',
    dni: '', rol: '', fechaIngreso: '',
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
      };
      this.roleService.setUserInfo(`${p.name} ${p.father_surname}`.trim(), u.email ?? '');
    } catch {
      this.error.set('No se pudieron cargar los datos del perfil. Verifica tu conexión e intenta nuevamente.');
    } finally {
      this.loading.set(false);
      this.loaded = true;
    }
  }
}
