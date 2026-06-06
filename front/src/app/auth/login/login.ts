import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

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

  // Datos del formulario
  email: string = 'admin@gmail.com';
  password: string = 'admin123';
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
    
    // Validar campos
    if (!this.email || !this.password) {
      this.errorMessage = 'Por favor, complete todos los campos';
      this.mostrarToastError();
      return;
    }

    // Validar formato de email
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.email)) {
      this.errorMessage = 'Por favor, ingrese un email válido';
      this.mostrarToastError();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    // Simular autenticación (aquí iría tu llamada a API)
    setTimeout(() => {
      this.isLoading = false;
      
      // Simular éxito de autenticación
      // En producción, aquí validarías contra tu backend
      console.log('Login exitoso:', { email: this.email, remember: this.rememberSession });
      
      // Guardar sesión (simulado)
      if (this.rememberSession) {
        localStorage.setItem('userEmail', this.email);
        localStorage.setItem('isLoggedIn', 'true');
      } else {
        sessionStorage.setItem('userEmail', this.email);
        sessionStorage.setItem('isLoggedIn', 'true');
      }
      
      // Redirigir a la vista de tenants en sass
      this.router.navigate(['/sass/tenants']);
    }, 1500);
  }

  private mostrarToastError(): void {
    const toast = document.createElement('div');
    toast.className = 'toast-notification toast-notification--error';
    toast.innerHTML = `
      <div class="toast-notification__content">
        <i class="bi bi-exclamation-triangle-fill me-2"></i>
        <span>${this.errorMessage}</span>
      </div>
    `;
    document.body.appendChild(toast);
    
    setTimeout(() => {
      toast.classList.add('toast-notification--show');
      setTimeout(() => {
        toast.classList.remove('toast-notification--show');
        setTimeout(() => toast.remove(), 300);
      }, 3000);
    }, 10);
  }
}