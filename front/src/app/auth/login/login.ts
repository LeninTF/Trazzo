import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastService } from '../../services/toast.service';

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

    const emailRegex = /^\S+@\S+\.\S+$/;
    if (!emailRegex.test(email)) {
      this.errorMessage.set('Por favor, ingrese un email válido');
      this.toastService.error(this.errorMessage());
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    setTimeout(() => {
      this.isLoading.set(false);
      this.router.navigate(['/sass/tenants']);
    }, 1500);
  }
}