import { Component, computed, inject, OnDestroy } from '@angular/core';
import { RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { RoleService } from '../../services/role.service';
import { filter, Subscription } from 'rxjs';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar implements OnDestroy {
  protected readonly roleService = inject(RoleService);
  private readonly router = inject(Router);
  private readonly sub: Subscription;

  constructor() {
    this.sub = this.router.events.pipe(
      filter(e => e instanceof NavigationEnd)
    ).subscribe(() => {
      this.roleService.closeSidebar();
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }

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
