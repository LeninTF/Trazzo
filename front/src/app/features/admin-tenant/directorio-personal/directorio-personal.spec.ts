import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ApiService } from '../../../api/services/api.service';
import type { TenantUserProfile } from '../../../api/types';
import { DirectorioPersonal } from './directorio-personal';

const makeUser = (id: number, name: string, father: string, mother: string, sede: string, area: string, dept: string, estado: 'ACTIVO' | 'LICENCIA'): TenantUserProfile => ({
  id, email: `${name.toLocaleLowerCase()}.${father.toLocaleLowerCase()}@colegio.edu.pe`,
  phone: `999${String(100 + id).slice(-3)}`, estado, must_change_password: false,
  created_at: '2024-01-15T00:00:00Z', updated_at: '2024-01-15T00:00:00Z',
  persona: { id, img_url: null, document_type: 'DNI', document_value: String(10000000 + id), name, father_surname: father, mother_surname: mother, birth_date: null },
  MetodoRecuperacion: [], rol: { id: 1, name: 'Docente', descripcion: null, permissions: [] },
  sedes: [{ id: 1, nombre: sede }], areas: [{ id: 1, nombre: area }], departamentos: [{ id: 1, nombre: dept }],
});

let usersData: TenantUserProfile[];

const mockApi = {
  users: {
    list: () => of({ content: usersData, totalElements: usersData.length, page: 0, size: 100, totalPages: 1 }),
    patch: (id: number, data: { name?: string; father_surname?: string }) => {
      const u = usersData.find(x => x.id === id);
      if (u) { u.persona.name = data.name ?? u.persona.name; u.persona.father_surname = data.father_surname ?? u.persona.father_surname; }
      return of({} as any);
    },
    create: () => {
      const newId = Math.max(...usersData.map(u => u.id)) + 1;
      usersData.push(makeUser(newId, 'New', 'Person', '', '', '', '', 'ACTIVO'));
      return of({} as any);
    },
    delete: (id: number) => {
      const idx = usersData.findIndex(u => u.id === id);
      if (idx >= 0) usersData.splice(idx, 1);
      return of({} as any);
    },
  },
};

describe('DirectorioPersonal', () => {
  let component: DirectorioPersonal;
  let fixture: ComponentFixture<DirectorioPersonal>;

  beforeEach(async () => {
    usersData = [
      makeUser(1, 'Dr.', 'Sarah', 'Jenkins', 'Central', 'Académica', 'Academic', 'ACTIVO'),
      makeUser(2, 'Marcus', 'Cooper', '', 'Central', 'Académica', 'Academic', 'ACTIVO'),
      makeUser(3, 'Ana', 'García', 'López', 'Norte', 'Administrativa', 'Human Resources', 'ACTIVO'),
      makeUser(4, 'Luis', 'Martínez', 'Rojas', 'Sur', 'Académica', 'Science', 'ACTIVO'),
      makeUser(5, 'Carmen', 'Vargas', 'Díaz', 'Central', 'Financiera', 'Finance', 'ACTIVO'),
      makeUser(6, 'José', 'Torres', 'Mendoza', 'Norte', 'Académica', 'Math', 'ACTIVO'),
      makeUser(7, 'Rosa', 'Flores', 'Castro', 'Sur', 'Administrativa', 'Human Resources', 'LICENCIA'),
      makeUser(8, 'Pedro', 'Ramos', 'Silva', 'Central', 'Académica', 'Academic', 'ACTIVO'),
      makeUser(9, 'Lucía', 'Morales', 'Paredes', 'Norte', 'Financiera', 'Finance', 'ACTIVO'),
      makeUser(10, 'Miguel', 'Ángel', 'Ríos', 'Sur', 'Académica', 'Science', 'ACTIVO'),
    ];

    await TestBed.configureTestingModule({
      imports: [DirectorioPersonal],
      providers: [{ provide: ApiService, useValue: mockApi }],
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

  it('should guardarPersonal create new', async () => {
    component.abrirModalAgregar();
    component.personalForm.nombre = 'New Person';
    component.personalForm.idPersonal = '#AX-TEST';
    await component.guardarPersonal();
    expect(component.personal().length).toBe(11);
    expect(component.personal().find(p => p.nombre === 'New Person')).toBeTruthy();
  });

  it('should not guardarPersonal without required fields', () => {
    component.abrirModalAgregar();
    component.guardarPersonal();
    expect(component.personal().length).toBe(10);
  });

  it('should guardarPersonal update existing', async () => {
    component.abrirModalEditar(component.personal()[0]);
    component.personalForm.nombre = 'Updated Name';
    await component.guardarPersonal();
    expect(component.personal()[0].nombre).toBe('Updated Name Jenkins');
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

  it('should eliminarPersonal', async () => {
    await component.eliminarPersonal(1);
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
