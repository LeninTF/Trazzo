import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastService } from '../../services/toast.service';
import { ApiService } from '../../api/services/api.service';
import { RoleService } from '../../services/role.service';

type LoginField = {
  id: string;
  label: string;
  placeholder: string;
  autocomplete: string;
  icon: string;
  type: 'email' | 'password';
  hasToggle?: boolean;
  helpText?: string;
};

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  readonly heroTitle = 'SaaS Institucional';
  readonly heroDescription = 'Gestione su institución con una infraestructura digital diseñada para el rendimiento extremo y claridad analítica.';
  readonly securityLabel = 'Seguridad nivel enterprise';
  readonly securityText = 'Encriptación de punta a punta activa';
  readonly panelTitle = 'Acceso a la Plataforma';
  readonly panelSubtitle = 'Ingrese sus credenciales para continuar al panel de control.';
  readonly accessPrompt = '¿Nuevo en la plataforma?';
  readonly accessAction = 'Solicitar acceso institucional';
  readonly emailHelp = '¿Olvidó su contraseña?';
  readonly rememberLabel = 'Mantener sesión iniciada';
  readonly submitLabel = 'Iniciar Sesión';
  
  readonly fields: LoginField[] = [
    {
      id: 'corporateEmail',
      label: 'Email corporativo',
      placeholder: 'nombre@institucion.com',
      autocomplete: 'email',
      icon: 'bi-at',
      type: 'email',
    },
    {
      id: 'password',
      label: 'Contraseña',
      placeholder: '••••••••••••',
      autocomplete: 'current-password',
      icon: 'bi-lock',
      type: 'password',
      hasToggle: true,
      helpText: '¿Olvidó su contraseña?',
    },
  ];

  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);
  private readonly api = inject(ApiService);
  private readonly roleService = inject(RoleService);

  readonly email = signal('');
  readonly password = signal('');
  readonly rememberSession = signal(false);
  readonly passwordVisible = signal(false);
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');

  togglePasswordVisibility(): void {
    this.passwordVisible.update(v => !v);
  }

  onSubmit(event: Event): void {
    event.preventDefault();

    const email = this.email();
    const password = this.password();

    if (!email || !password) {
      this.errorMessage.set('Por favor, complete todos los campos');
      this.toastService.error(this.errorMessage());
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      this.errorMessage.set('Por favor, ingrese un email válido');
      this.toastService.error(this.errorMessage());
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    this.api.auth.login({ email, password }).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        localStorage.setItem('trazzo_token', res.accessToken);
        this.roleService.setUserInfo(res.usuario.nombre, res.usuario.email);
        const isMaster = res.usuario.rol.some(r => r.name === 'admin_trazzo');
        const hasAdminTenant = res.usuario.tenant_permissions?.includes('administrador_tenant') ?? false;

        if (isMaster) {
          this.roleService.setAvailableRoles(['admin-saas']);
          this.roleService.switchRole('admin-saas');
          this.router.navigate(['/saas/tenants']);
        } else if (hasAdminTenant) {
          this.roleService.setAvailableRoles(['admin-tenant']);
          this.roleService.switchRole('admin-tenant');
          this.router.navigate(['/tenant/dashboard']);
        } else {
          this.roleService.setAvailableRoles(['usuario']);
          this.roleService.switchRole('usuario');
          this.router.navigate(['/usuario/dashboard']);
        }
        this.toastService.success(`Bienvenido(a), ${res.usuario.nombre}`);
      },
      error: (err) => {
        this.isLoading.set(false);
        const msg = err.status === 401 ? 'Credenciales inválidas' : 'Error al iniciar sesión';
        this.errorMessage.set(msg);
        this.toastService.error(msg);
      },
    });
  }
}