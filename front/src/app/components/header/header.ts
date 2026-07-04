import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { RoleService, Role } from '../../services/role.service';
import { NotificationService, Notification } from '../../services/notification.service';
import { ModalService } from '../../services/modal.service';
import { NotificationsModal } from '../notifications-modal/notifications-modal';

const ROLE_DASHBOARD: Record<Role, string> = {
  'admin-tenant': '/tenant/dashboard',
  'admin-sass': '/sass/tenants',
  'usuario': '/usuario/dashboard',
};

@Component({
  selector: 'app-header',
  imports: [RouterLink, NotificationsModal],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {
  protected readonly roleService = inject(RoleService);
  protected readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly modalService = inject(ModalService);

  protected readonly roles: { value: Role; label: string }[] = [
    { value: 'admin-tenant', label: 'Administrador Tenant' },
    { value: 'admin-sass', label: 'Administrador SaaS' },
    { value: 'usuario', label: 'Usuario' },
  ];

  protected settingsUrl = computed(() => {
    const role = this.roleService.role();
    if (role === 'admin-tenant') return '/tenant/configuracion-tenant';
    if (role === 'admin-sass') return '/sass/perfil';
    return '/usuario/perfil';
  });

  protected readonly tipoColor: Record<string, string> = {
    danger: '#dc2626',
    warning: '#f59e0b',
    success: '#10b981',
    info: '#3b82f6',
  };

  protected abrirModalNotificaciones(): void {
    this.modalService.show('notificationsModal');
  }

  protected irANotificacion(notif: Notification): void {
    this.notificationService.marcarComoLeida(notif.id);
    this.router.navigateByUrl(notif.route);
  }

  protected switchRoleAndNavigate(role: Role): void {
    this.roleService.switchRole(role);
    this.router.navigateByUrl(ROLE_DASHBOARD[role]);
  }

  protected onUserChipEnter(event: Event): void {
    const target = event.currentTarget as HTMLElement;
    target?.click();
  }
}
