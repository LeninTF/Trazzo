import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Login } from './login';
import { ApiService } from '../../api/services/api.service';
import { ToastService } from '../../services/toast.service';
import { RoleService } from '../../services/role.service';
import type { AuthResponse } from '../../api/types';

describe('Login', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;
  let routerSpy: jasmine.SpyObj<Router>;
  let toastSpy: jasmine.SpyObj<ToastService>;
  let roleSpy: jasmine.SpyObj<RoleService>;
  let apiSpy: jasmine.SpyObj<ApiService>;
  let authLoginSpy: jasmine.Spy;

  const mockMasterResponse: AuthResponse = {
    accessToken: 'master-token',
    tokenType: 'Bearer',
    usuario: {
      id: 1,
      nombre: 'Admin Trazzo',
      apellido_paterno: '',
      apellido_materno: '',
      email: 'admin@trazzo.com',
      status: 'ACTIVO',
      ultimo_acceso: '',
      rol: [{ id: 1, name: 'admin_trazzo', permissions: {} }],
      tenant_permissions: [],
    },
  };

  const mockTenantResponse: AuthResponse = {
    accessToken: 'tenant-token',
    tokenType: 'Bearer',
    usuario: {
      id: 2,
      nombre: 'User Tenant',
      apellido_paterno: '',
      apellido_materno: '',
      email: 'user@tenant.com',
      status: 'ACTIVO',
      ultimo_acceso: '',
      rol: [{ id: 2, name: 'admin_tenant', permissions: {} }],
      tenant_permissions: [],
    },
  };

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);
    toastSpy = jasmine.createSpyObj<ToastService>('ToastService', ['error', 'success']);
    roleSpy = jasmine.createSpyObj<RoleService>('RoleService', ['setUserInfo', 'switchRole']);

    authLoginSpy = jasmine.createSpy('auth.login');

    apiSpy = jasmine.createSpyObj<ApiService>('ApiService', [], {
      auth: { login: authLoginSpy } as any,
    });

    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        { provide: Router, useValue: routerSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: RoleService, useValue: roleSpy },
        { provide: ApiService, useValue: apiSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('signal initial values', () => {
    it('should have empty email', () => {
      expect(component.email()).toBe('');
    });

    it('should have empty password', () => {
      expect(component.password()).toBe('');
    });

    it('should have rememberSession as false', () => {
      expect(component.rememberSession()).toBeFalse();
    });

    it('should have passwordVisible as false', () => {
      expect(component.passwordVisible()).toBeFalse();
    });

    it('should have isLoading as false', () => {
      expect(component.isLoading()).toBeFalse();
    });

    it('should have errorMessage as empty', () => {
      expect(component.errorMessage()).toBe('');
    });
  });

  describe('togglePasswordVisibility', () => {
    it('should toggle passwordVisible from false to true', () => {
      component.togglePasswordVisibility();
      expect(component.passwordVisible()).toBeTrue();
    });

    it('should toggle passwordVisible from true to false', () => {
      component.togglePasswordVisibility();
      component.togglePasswordVisibility();
      expect(component.passwordVisible()).toBeFalse();
    });
  });

  describe('onSubmit', () => {
    function createSubmitEvent(): Event {
      const event = new Event('submit');
      spyOn(event, 'preventDefault');
      return event;
    }

    it('should show error when email and password are empty', () => {
      const event = createSubmitEvent();
      component.onSubmit(event);

      expect(event.preventDefault).toHaveBeenCalled();
      expect(component.errorMessage()).toBe('Por favor, complete todos los campos');
      expect(toastSpy.error).toHaveBeenCalledWith('Por favor, complete todos los campos');
      expect(authLoginSpy).not.toHaveBeenCalled();
    });

    it('should show error when only email is filled', () => {
      component.email.set('test@test.com');

      const event = createSubmitEvent();
      component.onSubmit(event);

      expect(event.preventDefault).toHaveBeenCalled();
      expect(component.errorMessage()).toBe('Por favor, complete todos los campos');
      expect(toastSpy.error).toHaveBeenCalledWith('Por favor, complete todos los campos');
      expect(authLoginSpy).not.toHaveBeenCalled();
    });

    it('should show error when only password is filled', () => {
      component.password.set('somepass');

      const event = createSubmitEvent();
      component.onSubmit(event);

      expect(event.preventDefault).toHaveBeenCalled();
      expect(component.errorMessage()).toBe('Por favor, complete todos los campos');
      expect(toastSpy.error).toHaveBeenCalledWith('Por favor, complete todos los campos');
      expect(authLoginSpy).not.toHaveBeenCalled();
    });

    it('should show error for invalid email format', () => {
      component.email.set('invalid-email');
      component.password.set('somepass');

      const event = createSubmitEvent();
      component.onSubmit(event);

      expect(event.preventDefault).toHaveBeenCalled();
      expect(component.errorMessage()).toBe('Por favor, ingrese un email válido');
      expect(toastSpy.error).toHaveBeenCalledWith('Por favor, ingrese un email válido');
      expect(authLoginSpy).not.toHaveBeenCalled();
    });

    it('should navigate to /saas/tenants for master user (admin_trazzo)', () => {
      component.email.set('admin@trazzo.com');
      component.password.set('validpass');
      authLoginSpy.and.returnValue(of(mockMasterResponse));

      const event = createSubmitEvent();
      component.onSubmit(event);

      expect(event.preventDefault).toHaveBeenCalled();
      expect(authLoginSpy).toHaveBeenCalledWith({ email: 'admin@trazzo.com', password: 'validpass' });
      expect(localStorage.getItem('trazzo_token')).toBe('master-token');
      expect(roleSpy.setUserInfo).toHaveBeenCalledWith('Admin Trazzo', 'admin@trazzo.com');
      expect(roleSpy.switchRole).toHaveBeenCalledWith('admin-saas');
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/saas/tenants']);
      expect(toastSpy.success).toHaveBeenCalledWith('Bienvenido(a), Admin Trazzo');
      expect(component.isLoading()).toBeFalse();
    });

    it('should navigate to /tenant/dashboard for tenant user', () => {
      component.email.set('user@tenant.com');
      component.password.set('validpass');
      authLoginSpy.and.returnValue(of(mockTenantResponse));

      const event = createSubmitEvent();
      component.onSubmit(event);

      expect(event.preventDefault).toHaveBeenCalled();
      expect(authLoginSpy).toHaveBeenCalledWith({ email: 'user@tenant.com', password: 'validpass' });
      expect(localStorage.getItem('trazzo_token')).toBe('tenant-token');
      expect(roleSpy.setUserInfo).toHaveBeenCalledWith('User Tenant', 'user@tenant.com');
      expect(roleSpy.switchRole).toHaveBeenCalledWith('admin-tenant');
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/tenant/dashboard']);
      expect(toastSpy.success).toHaveBeenCalledWith('Bienvenido(a), User Tenant');
      expect(component.isLoading()).toBeFalse();
    });

    it('should set loading state during API call', () => {
      component.email.set('user@tenant.com');
      component.password.set('validpass');
      authLoginSpy.and.returnValue(of(mockTenantResponse));

      component.onSubmit(createSubmitEvent());
      expect(component.isLoading()).toBeFalse();
    });

    it('should handle 401 error with invalid credentials message', () => {
      component.email.set('user@tenant.com');
      component.password.set('wrongpass');
      authLoginSpy.and.returnValue(throwError(() => ({ status: 401 })));

      const event = createSubmitEvent();
      component.onSubmit(event);

      expect(component.errorMessage()).toBe('Credenciales inválidas');
      expect(toastSpy.error).toHaveBeenCalledWith('Credenciales inválidas');
      expect(component.isLoading()).toBeFalse();
    });

    it('should handle generic API error', () => {
      component.email.set('user@tenant.com');
      component.password.set('validpass');
      authLoginSpy.and.returnValue(throwError(() => ({ status: 500 })));

      const event = createSubmitEvent();
      component.onSubmit(event);

      expect(component.errorMessage()).toBe('Error al iniciar sesión');
      expect(toastSpy.error).toHaveBeenCalledWith('Error al iniciar sesión');
      expect(component.isLoading()).toBeFalse();
    });
  });

  describe('template', () => {
    it('should display error message when errorMessage signal is set', () => {
      fixture.detectChanges();
      component.errorMessage.set('Test error');
      fixture.detectChanges();

      const alertEl: HTMLElement = fixture.nativeElement.querySelector('.alert-danger');
      expect(alertEl).toBeTruthy();
      expect(alertEl.textContent).toContain('Test error');
    });

    it('should hide error alert when errorMessage is empty', () => {
      fixture.detectChanges();
      const alertEl: HTMLElement = fixture.nativeElement.querySelector('.alert-danger');
      expect(alertEl).toBeFalsy();
    });

    it('should toggle password field type based on passwordVisible', () => {
      fixture.detectChanges();
      const passwordInput: HTMLInputElement = fixture.nativeElement.querySelector('#password');
      expect(passwordInput.type).toBe('password');

      component.passwordVisible.set(true);
      fixture.detectChanges();
      expect(passwordInput.type).toBe('text');
    });

    it('should show spinner when isLoading is true', () => {
      fixture.detectChanges();
      component.isLoading.set(true);
      fixture.detectChanges();

      const spinner: HTMLElement = fixture.nativeElement.querySelector('.spinner-border');
      expect(spinner).toBeTruthy();
      const submitBtn: HTMLButtonElement = fixture.nativeElement.querySelector('.login-submit');
      expect(submitBtn.disabled).toBeTrue();
    });

    it('should show submit label when isLoading is false', () => {
      fixture.detectChanges();
      const submitBtn: HTMLElement = fixture.nativeElement.querySelector('.login-submit');
      expect(submitBtn.textContent).toContain('Iniciar Sesión');
    });

    it('should bind email input via ngModel', () => {
      fixture.detectChanges();
      const emailInput: HTMLInputElement = fixture.nativeElement.querySelector('#corporateEmail');
      emailInput.value = 'test@test.com';
      emailInput.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(component.email()).toBe('test@test.com');
    });

    it('should bind password input via ngModel', () => {
      fixture.detectChanges();
      const passwordInput: HTMLInputElement = fixture.nativeElement.querySelector('#password');
      passwordInput.value = 'mypassword';
      passwordInput.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(component.password()).toBe('mypassword');
    });

    it('should call togglePasswordVisibility when password toggle button is clicked', () => {
      spyOn(component, 'togglePasswordVisibility');
      fixture.detectChanges();

      const toggleBtn: HTMLButtonElement = fixture.nativeElement.querySelector('.password-toggle');
      toggleBtn.click();

      expect(component.togglePasswordVisibility).toHaveBeenCalled();
    });

    it('should call onSubmit when form is submitted', () => {
      spyOn(component, 'onSubmit');
      fixture.detectChanges();

      const form: HTMLFormElement = fixture.nativeElement.querySelector('form');
      form.dispatchEvent(new Event('submit'));

      expect(component.onSubmit).toHaveBeenCalled();
    });
  });
});
