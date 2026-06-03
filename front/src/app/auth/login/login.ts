import { Component } from '@angular/core';

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
  imports: [],
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

  passwordVisible = false;

  togglePasswordVisibility(): void {
    this.passwordVisible = !this.passwordVisible;
  }
}
