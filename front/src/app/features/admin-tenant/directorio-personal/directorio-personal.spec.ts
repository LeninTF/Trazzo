import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { DirectorioPersonal } from './directorio-personal';
import { ApiService } from '../../../api/services/api.service';
import { ToastService } from '../../../services/toast.service';
import { ModalService } from '../../../services/modal.service';
import { of } from 'rxjs';
import { fakeAsync, tick } from '@angular/core/testing';

describe('DirectorioPersonal', () => {
  let component: DirectorioPersonal;
  let fixture: ComponentFixture<DirectorioPersonal>;

  const mockUsersResponse = {
    content: [
      {
        id: 1, email: 'jose@colegio.edu.pe', phone: '+51987000001',
        estado: 'ACTIVO', must_change_password: false,
        created_at: '2025-01-01', updated_at: '2025-06-01',
        persona: { id: 1, img_url: null, document_type: 'DNI', document_value: '76543210', name: 'José', father_surname: 'Pérez', mother_surname: 'López', birth_date: '1990-01-15' },
        MetodoRecuperacion: [],
        rol: { id: 1, name: 'Trabajador', descripcion: 'Acceso básico', permissions: [] },
        sedes: [{ id: 1, nombre: 'Sede San Isidro' }],
        areas: [{ id: 1, nombre: 'Dirección Académica' }],
        departamentos: [{ id: 1, nombre: 'Matemáticas' }],
      },
      {
        id: 2, email: 'ana@colegio.edu.pe', phone: '+51987000002',
        estado: 'LICENCIA', must_change_password: false,
        created_at: '2025-02-01', updated_at: '2025-06-01',
        persona: { id: 2, img_url: null, document_type: 'DNI', document_value: '87654321', name: 'Ana', father_surname: 'Rojas', mother_surname: 'Torres', birth_date: '1992-03-20' },
        MetodoRecuperacion: [],
        rol: { id: 2, name: 'Supervisor', descripcion: 'Supervisión', permissions: [] },
        sedes: [{ id: 2, nombre: 'Sede Miraflores' }],
        areas: [{ id: 2, nombre: 'Administración' }],
        departamentos: [{ id: 2, nombre: 'Comunicación' }],
      },
    ],
    page: 0, size: 100, totalElements: 2, totalPages: 1,
  };

  const mockApi = {
    users: {
      list: jasmine.createSpy('list').and.returnValue(of(mockUsersResponse)),
      create: jasmine.createSpy('create').and.returnValue(of(mockUsersResponse.content[0])),
      patch: jasmine.createSpy('patch').and.returnValue(of(mockUsersResponse.content[0])),
      delete: jasmine.createSpy('delete').and.returnValue(of({ id: 1, status: 'INACTIVO', deleted_at: new Date().toISOString(), deleted_by: 1 })),
    },
  };

  const mockToast = jasmine.createSpyObj('ToastService', ['info']);
  const mockModal = jasmine.createSpyObj('ModalService', ['open', 'close']);

  beforeEach(async () => {
    mockApi.users.list.calls.reset();
    mockApi.users.create.calls.reset();
    mockApi.users.patch.calls.reset();
    mockApi.users.delete.calls.reset();
    await TestBed.configureTestingModule({
      imports: [DirectorioPersonal],
      providers: [
        provideHttpClient(),
        { provide: ApiService, useValue: mockApi },
        { provide: ToastService, useValue: mockToast },
        { provide: ModalService, useValue: mockModal },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DirectorioPersonal);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load personal on init', () => {
    expect(mockApi.users.list).toHaveBeenCalled();
  });

  it('should filter personal by search term', () => {
    component.personal.set(mockUsersResponse.content.map(u => ({
      id: u.id,
      nombre: u.persona.name + ' ' + u.persona.father_surname,
      idPersonal: u.persona.document_value,
      sede: u.sedes[0]?.nombre ?? '',
      area: u.areas[0]?.nombre ?? '',
      departamento: u.departamentos[0]?.nombre ?? '',
      cargo: '',
      estado: u.estado as any,
      email: u.email,
    })));
    component.searchTerm = 'José';
    expect(component.personalFiltrado.length).toBe(1);
    component.searchTerm = '';
    expect(component.personalFiltrado.length).toBe(2);
  });

  it('should paginate personal', () => {
    component.personal.set(mockUsersResponse.content.map(u => ({
      id: u.id, nombre: u.persona.name, idPersonal: u.persona.document_value,
      sede: u.sedes[0]?.nombre ?? '', area: '', departamento: '',
      cargo: '', estado: u.estado as any, email: u.email,
    })));
    component.itemsPerPage = 1;
    expect(component.totalPaginas).toBe(2);
    expect(component.personalPaginado.length).toBe(1);
  });

  it('should change page within bounds', () => {
    component.personal.set(mockUsersResponse.content.map(u => ({
      id: u.id, nombre: u.persona.name, idPersonal: u.persona.document_value,
      sede: '', area: '', departamento: '', cargo: '',
      estado: 'ACTIVO' as any, email: '',
    })));
    component.itemsPerPage = 1;
    component.cambiarPagina(2);
    expect(component.paginaActual).toBe(2);
    component.cambiarPagina(0);
    expect(component.paginaActual).toBe(2);
  });

  it('should open and close add modal', () => {
    component.abrirModalAgregar();
    expect(component.modalPersonalOpen).toBeTrue();
    expect(component.editandoPersonal).toBeFalse();
    component.cerrarModalPersonal();
    expect(component.modalPersonalOpen).toBeFalse();
  });

  it('should open and close edit modal', () => {
    component.personal.set([{
      id: 1, nombre: 'José Pérez', idPersonal: '76543210',
      sede: 'Sede San Isidro', area: 'Dirección Académica',
      departamento: 'Matemáticas', cargo: '', estado: 'ACTIVO',
      email: 'jose@colegio.edu.pe',
    }]);
    const persona = component.personal()[0];
    component.abrirModalEditar(persona);
    expect(component.modalPersonalOpen).toBeTrue();
    expect(component.editandoPersonal).toBeTrue();
    expect(component.personalForm.nombre).toBe(persona.nombre);
    component.cerrarModalPersonal();
    expect(component.modalPersonalOpen).toBeFalse();
  });

  it('should open and close detail modal', () => {
    const persona = { id: 1, nombre: 'Test', idPersonal: '123', sede: '', area: '', departamento: '', cargo: '', estado: 'ACTIVO' as any };
    component.abrirModalDetalle(persona);
    expect(component.modalDetalleOpen).toBeTrue();
    expect(component.personalSeleccionado).toBe(persona);
    component.cerrarModalDetalle();
    expect(component.modalDetalleOpen).toBeFalse();
    expect(component.personalSeleccionado).toBeNull();
  });

  it('should edit from detail modal', () => {
    component.personal.set([{ id: 1, nombre: 'Test', idPersonal: '123', sede: '', area: '', departamento: '', cargo: '', estado: 'ACTIVO' }] as any);
    component.abrirModalDetalle(component.personal()[0]);
    component.editarDesdeDetalle();
    expect(component.modalDetalleOpen).toBeFalse();
    expect(component.modalPersonalOpen).toBeTrue();
    component.cerrarModalPersonal();
  });

  it('should handle image file selection', () => {
    const file = new File([''], 'test.jpg', { type: 'image/jpeg' });
    const event = { target: { files: [file] } } as any;
    component.onFileSelected(event);
    expect(component.imagenPreviewUrl).toBeDefined();
  });

  it('should reject files over 2MB', () => {
    const largeFile = new File(['x'.repeat(3 * 1024 * 1024)], 'large.jpg', { type: 'image/jpeg' });
    const event = { target: { files: [largeFile] } } as any;
    component.onFileSelected(event);
    expect(mockToast.info).toHaveBeenCalled();
  });

  it('should toggle image mode', () => {
    component.cambiarModoImagen(false);
    expect(component.modoImagenUrl).toBeFalse();
    component.cambiarModoImagen(true);
    expect(component.modoImagenUrl).toBeTrue();
  });
});
