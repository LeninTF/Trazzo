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

  protected roleUrlPrefix = computed(() =>
    this.roleService.role() === 'admin-tenant' ? 'tenant' : 'sass'
  );

  protected roleLabel = computed(() => {
    const role = this.roleService.role();
    return role === 'admin-tenant' ? 'ADMINISTRADOR TENANT' : 'ADMINISTRADOR SAAS';
  });
}
