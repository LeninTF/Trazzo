import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DirectorioPersonal } from './directorio-personal';

describe('DirectorioPersonal', () => {
  let component: DirectorioPersonal;
  let fixture: ComponentFixture<DirectorioPersonal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DirectorioPersonal],
    }).compileComponents();

    fixture = TestBed.createComponent(DirectorioPersonal);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 10 initial personal', () => {
    expect(component.personal().length).toBe(10);
  });

  it('should filter by searchTerm', () => {
    component.searchTerm = 'sarah';
    expect(component.personalFiltrado.length).toBe(1);
    expect(component.personalFiltrado[0].nombre).toBe('Dr. Sarah Jenkins');
  });

  it('should filter by sede', () => {
    component.filtroSede = 'Central';
    const count = component.personal().filter(p => p.sede === 'Central').length;
    expect(component.personalFiltrado.length).toBe(count);
  });

  it('should filter by area', () => {
    component.filtroArea = 'Académica';
    const count = component.personal().filter(p => p.area === 'Académica').length;
    expect(component.personalFiltrado.length).toBe(count);
  });

  it('should filter by departamento', () => {
    component.filtroDepartamento = 'Academic';
    const count = component.personal().filter(p => p.departamento === 'Academic').length;
    expect(component.personalFiltrado.length).toBe(count);
  });

  it('should combine filters', () => {
    component.searchTerm = 'cooper';
    component.filtroSede = 'Central';
    expect(component.personalFiltrado.length).toBe(1);
  });

  it('should compute personalPaginado', () => {
    expect(component.personalPaginado.length).toBe(5);
    component.paginaActual = 2;
    expect(component.personalPaginado.length).toBe(5);
  });

  it('should compute totalPaginas', () => {
    expect(component.totalPaginas).toBe(2);
  });

  it('should compute inicioRegistro and finRegistro', () => {
    expect(component.inicioRegistro).toBe(1);
    expect(component.finRegistro).toBe(5);
    component.paginaActual = 2;
    expect(component.inicioRegistro).toBe(6);
    expect(component.finRegistro).toBe(10);
  });

  it('should filtrarPersonal reset page', () => {
    component.paginaActual = 2;
    component.filtrarPersonal();
    expect(component.paginaActual).toBe(1);
  });

  it('should cambiarPagina within range', () => {
    component.cambiarPagina(2);
    expect(component.paginaActual).toBe(2);
  });

  it('should not cambiarPagina out of range', () => {
    component.cambiarPagina(0);
    expect(component.paginaActual).toBe(1);
    component.cambiarPagina(999);
    expect(component.paginaActual).toBe(1);
  });

  it('should abrirModalAgregar', () => {
    component.abrirModalAgregar();
    expect(component.editandoPersonal).toBeFalse();
    expect(component.modalPersonalOpen).toBeTrue();
    expect(component.personalForm.nombre).toBe('');
  });

  it('should abrirModalEditar', () => {
    component.abrirModalEditar(component.personal()[0]);
    expect(component.editandoPersonal).toBeTrue();
    expect(component.personalForm.nombre).toBe('Dr. Sarah Jenkins');
  });

  it('should cerrarModalPersonal', () => {
    component.abrirModalAgregar();
    component.cerrarModalPersonal();
    expect(component.modalPersonalOpen).toBeFalse();
  });

  it('should guardarPersonal create new', () => {
    component.abrirModalAgregar();
    component.personalForm.nombre = 'New Person';
    component.personalForm.idPersonal = '#AX-TEST';
    component.guardarPersonal();
    expect(component.personal().length).toBe(11);
    expect(component.personal().find(p => p.nombre === 'New Person')).toBeTruthy();
  });

  it('should not guardarPersonal without required fields', () => {
    component.abrirModalAgregar();
    component.guardarPersonal();
    expect(component.personal().length).toBe(10);
  });

  it('should guardarPersonal update existing', () => {
    component.abrirModalEditar(component.personal()[0]);
    component.personalForm.nombre = 'Updated Name';
    component.guardarPersonal();
    expect(component.personal()[0].nombre).toBe('Updated Name');
  });

  it('should abrirModalDetalle', () => {
    component.abrirModalDetalle(component.personal()[0]);
    expect(component.modalDetalleOpen).toBeTrue();
    expect(component.personalSeleccionado?.nombre).toBe('Dr. Sarah Jenkins');
  });

  it('should cerrarModalDetalle', () => {
    component.abrirModalDetalle(component.personal()[0]);
    component.cerrarModalDetalle();
    expect(component.modalDetalleOpen).toBeFalse();
    expect(component.personalSeleccionado).toBeNull();
  });

  it('should editarDesdeDetalle', () => {
    component.abrirModalDetalle(component.personal()[0]);
    component.editarDesdeDetalle();
    expect(component.modalDetalleOpen).toBeFalse();
    expect(component.modalPersonalOpen).toBeTrue();
  });

  it('should eliminarPersonal', () => {
    component.eliminarPersonal(1);
    expect(component.personal().length).toBe(9);
  });

  it('should cambiarModoImagen', () => {
    component.cambiarModoImagen(false);
    expect(component.modoImagenUrl).toBeFalse();
    component.cambiarModoImagen(true);
    expect(component.modoImagenUrl).toBeTrue();
  });

  it('should actualizarPreviewUrl', () => {
    component.personalForm.imagenUrl = 'http://example.com/img.jpg';
    component.actualizarPreviewUrl();
    expect(component.imagenPreviewUrl).toBe('http://example.com/img.jpg');
  });

  it('should handle large file in onFileSelected', () => {
    const blob = new Blob(['a'.repeat(3 * 1024 * 1024)]);
    const file = new File([blob], 'test.jpg', { type: 'image/jpeg' });
    const event = { target: { files: [file] } } as unknown as Event;
    component.onFileSelected(event);
    expect(component.personalForm.imagenUrl).toBe('');
  });
});
