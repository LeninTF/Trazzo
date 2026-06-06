import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { RoleService, Role } from '../../services/role.service';


@Component({
  selector: 'app-header',
  imports: [RouterLink],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {
  protected readonly roleService = inject(RoleService);

  protected readonly roles: { value: Role; label: string }[] = [
    { value: 'admin-tenant', label: 'Administrador Tenant' },
    { value: 'admin-sass', label: 'Administrador SaaS' },
    { value: 'usuario', label: 'Usuario' },
  ];

  protected settingsUrl = computed(() => {
    const role = this.roleService.role();
    if (role === 'admin-tenant') return '/tenant/configuracion-tenant';
    if (role === 'admin-sass') return '/sass/dashboard';
    return '/usuario/dashboard';
  });

  protected readonly notificaciones = [
    { icono: 'bi-person-x-fill', titulo: 'Inasistencia detectada', descripcion: '3 empleados no marcaron ingreso', hora: 'Hace 5 min', tipo: 'danger' },
    { icono: 'bi-exclamation-triangle-fill', titulo: 'Incidencia pendiente', descripcion: 'Requiere revisión de horarios', hora: 'Hace 15 min', tipo: 'warning' },
    { icono: 'bi-check-circle-fill', titulo: 'Solicitud aprobada', descripcion: 'Permiso de Maria Lopez', hora: 'Hace 1 h', tipo: 'success' },
  ];

  protected readonly tipoColor: Record<string, string> = {
    danger: '#dc2626',
    warning: '#f59e0b',
    success: '#10b981',
  };
}
