import { Injectable, signal } from '@angular/core';

export type Role = 'admin-tenant' | 'admin-saas' | 'usuario';

const STORAGE_KEY = 'trazzo_role';

@Injectable({
  providedIn: 'root',
})
export class RoleService {
  readonly role = signal<Role>(this.loadRole());

  readonly roleLabel: Record<Role, string> = {
    'admin-tenant': 'Administrador Tenant',
    'admin-saas': 'Administrador SaaS',
    'usuario': 'Usuario',
  };

  readonly userName = signal(this.loadUserName());
  readonly userEmail = signal('');

  readonly sidebarOpen = signal(false);

  setUserInfo(name: string, email: string): void {
    this.userName.set(name);
    this.userEmail.set(email);
    localStorage.setItem('trazzo_user_name', name);
  }

  switchRole(role: Role): void {
    this.role.set(role);
    localStorage.setItem(STORAGE_KEY, role);
  }

  /**
   * RoleService is a app-wide singleton whose signals are only ever read from
   * localStorage once, at injection time — clearing storage alone would leave
   * the in-memory userName/role stale for the rest of this SPA session (no
   * full page reload happens on logout), so the signals must be reset here too.
   */
  clearSession(): void {
    this.userName.set('');
    this.userEmail.set('');
    this.role.set('admin-tenant');
    localStorage.removeItem('trazzo_user_name');
    localStorage.removeItem(STORAGE_KEY);
  }

  toggleSidebar(): void {
    this.sidebarOpen.update(v => !v);
  }

  closeSidebar(): void {
    this.sidebarOpen.set(false);
  }

  private loadUserName(): string {
    return localStorage.getItem('trazzo_user_name') ?? '';
  }

  private loadRole(): Role {
    const stored = localStorage.getItem(STORAGE_KEY) as Role | null;
    if (stored && ['admin-tenant', 'admin-saas', 'usuario'].includes(stored)) {
      return stored;
    }
    return 'admin-tenant';
  }
}
