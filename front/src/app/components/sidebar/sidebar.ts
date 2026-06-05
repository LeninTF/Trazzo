import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { RoleService } from '../../services/role.service';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar {
  protected readonly roleService = inject(RoleService);

  protected roleUrlPrefix = computed(() => {
    const role = this.roleService.role();
    if (role === 'admin-sass') return 'sass';
    if (role === 'usuario') return 'usuario';
    return 'tenant';
  });

  protected roleLabel = computed(() => {
    const role = this.roleService.role();
    if (role === 'admin-tenant') return 'ADMINISTRADOR TENANT';
    if (role === 'admin-sass') return 'ADMINISTRADOR SAAS';
    return 'USUARIO';
  });
}
