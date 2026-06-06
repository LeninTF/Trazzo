import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GestionUsuarios } from './gestion-usuarios';

describe('GestionUsuarios', () => {
  let component: GestionUsuarios;
  let fixture: ComponentFixture<GestionUsuarios>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GestionUsuarios],
    }).compileComponents();

    fixture = TestBed.createComponent(GestionUsuarios);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 7 initial usuarios', () => {
    expect(component.usuarios().length).toBe(7);
  });

  it('should filter by rol', () => {
    component.setFilterRol('Superadmin');
    expect(component.usuariosFiltrados().every(u => u.rol === 'Superadmin')).toBeTrue();
  });

  it('should filter by estado', () => {
    component.setFilterEstado('activo');
    expect(component.usuariosFiltrados().every(u => u.estado === 'activo')).toBeTrue();
  });

  it('should combine filters', () => {
    component.setFilterRol('Soporte');
    component.setFilterEstado('activo');
    expect(component.usuariosFiltrados().every(u => u.rol === 'Soporte' && u.estado === 'activo')).toBeTrue();
  });

  it('should show all with todos filters', () => {
    component.setFilterRol('todos');
    component.setFilterEstado('todos');
    expect(component.usuariosFiltrados().length).toBe(7);
  });

  it('should limpiarFiltros', () => {
    component.setFilterRol('Superadmin');
    component.setFilterEstado('inactivo');
    component.limpiarFiltros();
    expect(component.filterRol()).toBe('todos');
    expect(component.filterEstado()).toBe('todos');
  });

  it('should compute metricas', () => {
    expect(component.total()).toBe(7);
    expect(component.activos()).toBe(component.usuarios().filter(u => u.estado === 'activo').length);
    expect(component.inactivos()).toBe(component.usuarios().filter(u => u.estado === 'inactivo').length);
  });

  it('should compute actividadSemanal', () => {
    const pct = Math.round((component.activos() / component.total()) * 100);
    expect(component.actividadSemanal()).toBe(pct);
  });

  it('should compute role counts', () => {
    expect(component.superadmins() + component.soportes() + component.facturaciones()).toBe(7);
  });

  it('should abrirCrear new user', () => {
    component.abrirCrear();
    expect(component.vistaCrear()).toBeTrue();
    expect(component.editandoUsuario()).toBeNull();
    expect(component.paso()).toBe(1);
  });

  it('should abrirCrear for editing', () => {
    component.abrirCrear(component.usuarios()[0]);
    expect(component.editandoUsuario()?.nombre).toBe('Carlos Méndez');
  });

  it('should cancelarCrear', () => {
    component.abrirCrear();
    component.cancelarCrear();
    expect(component.vistaCrear()).toBeFalse();
  });

  it('should navigate pasos', () => {
    component.abrirCrear();
    expect(component.paso()).toBe(1);
    component.siguientePaso();
    expect(component.paso()).toBe(2);
    component.pasoAnterior();
    expect(component.paso()).toBe(1);
    component.irPaso(3);
    expect(component.paso()).toBe(3);
  });

  it('should not go below paso 1', () => {
    component.abrirCrear();
    component.pasoAnterior();
    expect(component.paso()).toBe(1);
  });

  it('should compute progreso', () => {
    component.abrirCrear();
    expect(component.progreso()).toBe(33);
    component.irPaso(3);
    expect(component.progreso()).toBe(100);
  });

  it('should registrarPersonal with valid form', () => {
    component.abrirCrear();
    component.createForm.patchValue({
      nombreCompleto: 'Test User',
      tipoDocumento: 'DNI',
      numDocumento: '12345678',
      correo: 'test@test.com',
      telefono: '999999999',
      empleadoId: 'EMP001',
      nombreUsuario: 'testuser',
      contrasena: 'password123',
    });
    component.registrarPersonal();
    expect(component.usuarios().length).toBe(8);
    expect(component.vistaCrear()).toBeFalse();
  });

  it('should not registrarPersonal with invalid form', () => {
    component.abrirCrear();
    component.registrarPersonal();
    expect(component.usuarios().length).toBe(7);
  });

  it('should generarUsuario from nombre', () => {
    component.abrirCrear();
    component.createForm.patchValue({ nombreCompleto: 'Juan Perez' });
    component.generarUsuario();
    expect(component.createForm.get('nombreUsuario')?.value).toBe('juan.perez');
  });

  it('should guardarBorrador', () => {
    component.guardarBorrador();
    expect(component.toast()?.message).toBe('Borrador guardado.');
  });

  it('should toggleEstado', () => {
    const user = component.usuarios()[0];
    const wasActive = user.estado === 'activo';
    component.toggleEstado(user);
    expect(component.usuarios().find(u => u.id === user.id)!.estado).toBe(wasActive ? 'inactivo' : 'activo');
  });
});
