import { Injectable, signal } from '@angular/core';

export type Role = 'admin-tenant' | 'admin-sass' | 'usuario';

const STORAGE_KEY = 'trazzo_role';

@Injectable({
  providedIn: 'root',
})
export class RoleService {
  readonly role = signal<Role>(this.loadRole());

  readonly roleLabel: Record<Role, string> = {
    'admin-tenant': 'Administrador Tenant',
    'admin-sass': 'Administrador SaaS',
    'usuario': 'Usuario',
  };

  readonly sidebarOpen = signal(false);

  switchRole(role: Role): void {
    this.role.set(role);
    localStorage.setItem(STORAGE_KEY, role);
  }

  toggleSidebar(): void {
    this.sidebarOpen.update(v => !v);
  }

  closeSidebar(): void {
    this.sidebarOpen.set(false);
  }

  private loadRole(): Role {
    const stored = localStorage.getItem(STORAGE_KEY) as Role | null;
    if (stored && ['admin-tenant', 'admin-sass', 'usuario'].includes(stored)) {
      return stored;
    }
    return 'admin-tenant';
  }
}
