import { Component, inject } from '@angular/core';
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

  email: string = '';
  password: string = '';
  rememberSession: boolean = false;
  passwordVisible = false;
  isLoading: boolean = false;
  errorMessage: string = '';

  constructor(private router: Router) {}

  togglePasswordVisibility(): void {
    this.passwordVisible = !this.passwordVisible;
  }

  // Método para manejar el inicio de sesión
  onSubmit(event: Event): void {
    event.preventDefault();
    
    if (!this.email || !this.password) {
      this.errorMessage = 'Por favor, complete todos los campos';
      this.toastService.error(this.errorMessage);
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.email)) {
      this.errorMessage = 'Por favor, ingrese un email válido';
      this.toastService.error(this.errorMessage);
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    setTimeout(() => {
      this.isLoading = false;
      
      if (this.rememberSession) {
        localStorage.setItem('userEmail', this.email);
        localStorage.setItem('isLoggedIn', 'true');
      } else {
        sessionStorage.setItem('userEmail', this.email);
        sessionStorage.setItem('isLoggedIn', 'true');
      }
      
      this.router.navigate(['/sass/tenants']);
    }, 1500);
  }
}