import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { ApiService } from '../../../api/services/api.service';
import type { MasterUserProfile } from '../../../api/types';
import { Perfil } from './perfil';

const CURRENT_PASSWORD = 'admin123';

const mockUser: MasterUserProfile = {
  id: 1, email: 'jose.alata@trazzo.com', phone: '999888777',
  tenant_id: null, must_change_password: false,
  created_at: '2024-01-15T00:00:00Z',
  persona: {
    id: 1, img_url: null, document_type: 'DNI', document_value: '12345678',
    name: 'Jose', father_surname: 'Alata', mother_surname: 'Pérez', birth_date: null,
  },
  MetodoRecuperacion: [],
  roles: [{ id: 1, name: 'Super Admin', descripcion: null }],
  tenant_info: null,
};

const mockApi = {
  users: {
    getMasterMe: () => of(mockUser),
    patchMasterMe: () => of({} as any),
  },
};

describe('Perfil', () => {
  let component: Perfil;
  let fixture: ComponentFixture<Perfil>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Perfil, FormsModule],
      providers: [{ provide: ApiService, useValue: mockApi }],
    }).compileComponents();

    fixture = TestBed.createComponent(Perfil);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial user data', () => {
    expect(component.usuario.nombres).toBe('Jose');
    expect(component.usuario.email).toBe('jose.alata@trazzo.com');
  });

  it('should editar create copy and set editando', () => {
    component.editar();
    expect(component.editando).toBeTrue();
    expect(component.usuarioEdit.nombres).toBe(component.usuario.nombres);
  });

  it('should cancelarEdicion', () => {
    component.editar();
    component.cancelarEdicion();
    expect(component.editando).toBeFalse();
    expect(component.mensajeError).toBe('');
  });

  it('should guardarCambios with valid data', async () => {
    component.editar();
    component.usuarioEdit.nombres = 'Nuevo Nombre';
    await component.guardarCambios();
    expect(component.usuario.nombres).toBe('Nuevo Nombre');
    expect(component.editando).toBeFalse();
    expect(component.mensajeExito).toBe('Datos actualizados correctamente.');
  });

  it('should reject guardarCambios with empty nombres', () => {
    component.editar();
    component.usuarioEdit.nombres = '';
    component.guardarCambios();
    expect(component.mensajeError).toBe('Nombres y apellidos son obligatorios.');
    expect(component.editando).toBeTrue();
  });

  it('should reject guardarCambios with empty apellidos', () => {
    component.editar();
    component.usuarioEdit.apellidos = '';
    component.guardarCambios();
    expect(component.mensajeError).toBe('Nombres y apellidos son obligatorios.');
  });

  it('should reject guardarCambios with invalid email', () => {
    component.editar();
    component.usuarioEdit.email = 'invalido';
    component.guardarCambios();
    expect(component.mensajeError).toBe('Correo electrónico inválido.');
  });

  it('should guardarPassword require current password', () => {
    component.guardarPassword();
    expect(component.errorPasswordActual).toBe('Ingrese su contraseña actual.');
  });

  it('should guardarPassword reject short new password', () => {
    component.passwordActual = CURRENT_PASSWORD;
    component.passwordNueva = '12345';
    component.guardarPassword();
    expect(component.errorPasswordNueva).toBe('Debe tener al menos 6 caracteres.');
  });

  it('should guardarPassword reject mismatched passwords', () => {
    component.passwordActual = CURRENT_PASSWORD;
    component.passwordNueva = 'newpass1';
    component.passwordConfirmar = 'different';
    component.guardarPassword();
    expect(component.errorPasswordConfirmar).toBe('Las contraseñas no coinciden.');
  });

  it('should guardarPassword succeed', () => {
    component.passwordActual = CURRENT_PASSWORD;
    component.passwordNueva = 'newpass123';
    component.passwordConfirmar = 'newpass123';
    component.guardarPassword();
    expect(component.mostrarCambiarPassword).toBeFalse();
    expect(component.mensajeExito).toBe('Contraseña actualizada correctamente.');
    expect(component.passwordActual).toBe('');
    expect(component.passwordNueva).toBe('');
    expect(component.passwordConfirmar).toBe('');
  });

  it('should clear success message after timeout', fakeAsync(() => {
    component.mostrarExito('Test');
    expect(component.mensajeExito).toBe('Test');
    tick(3500);
    expect(component.mensajeExito).toBe('');
  }));

  it('should limpiarMensajes clear all', () => {
    component.mensajeExito = 'test';
    component.mensajeError = 'test';
    component.errorPasswordActual = 'test';
    component.errorPasswordNueva = 'test';
    component.errorPasswordConfirmar = 'test';
    component.limpiarMensajes();
    expect(component.mensajeExito).toBe('');
    expect(component.mensajeError).toBe('');
    expect(component.errorPasswordActual = '');
    expect(component.errorPasswordNueva = '');
    expect(component.errorPasswordConfirmar = '');
  });
});
