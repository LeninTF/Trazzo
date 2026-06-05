import { Injectable, signal } from '@angular/core';

export type Role = 'admin-tenant' | 'admin-sass' | 'usuario';

@Injectable({
  providedIn: 'root',
})
export class RoleService {
  readonly role = signal<Role>('admin-tenant');

  readonly roleLabel: Record<Role, string> = {
    'admin-tenant': 'Administrador Tenant',
    'admin-sass': 'Administrador SaaS',
    'usuario': 'Usuario',
  };

  switchRole(role: Role): void {
    this.role.set(role);
  }
}
