import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { DirectorioPersonal, ENROLL_POLL_MS, ENROLL_TIMEOUT_MS } from './directorio-personal';
import { ApiService } from '../../../api/services/api.service';
import { ToastService } from '../../../services/toast.service';
import { ModalService } from '../../../services/modal.service';
import { MiddlewareWebSocketService } from '../../../services/middleware-websocket.service';
import { of, throwError } from 'rxjs';
import { fakeAsync, tick } from '@angular/core/testing';

describe('DirectorioPersonal', () => {
  let component: DirectorioPersonal;
  let fixture: ComponentFixture<DirectorioPersonal>;
  let middlewareWs: MiddlewareWebSocketService;

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

  const enrolDevice = (id: number, code: string) => ({
    id, code, name: `Lector ${id}`, branch_id: 1, branch_name: 'Sede San Isidro',
    ip: null, puerto: null, ubicacion: null, state: true, created_at: '2026-10-01T08:00:00',
  });
  const enrolSession = {
    enroll_token: 'tok-1', tenant_user_id: 1, device_id: 7, finger_index: 1, device_code: 'ZK-007',
    expires_at: '2026-10-23T09:02:00',
  };
  const enrolPage = (content: any[]) => ({ content, page: 0, size: 200, totalElements: content.length, totalPages: 1 });

  const mockApi = {
    corehr: {
      listDevices: jasmine.createSpy('listDevices'),
      listBiometria: jasmine.createSpy('listBiometria'),
      initEnroll: jasmine.createSpy('initEnroll'),
    },
    users: {
      list: jasmine.createSpy('list').and.returnValue(of(mockUsersResponse)),
      create: jasmine.createSpy('create').and.returnValue(of(mockUsersResponse.content[0])),
      patch: jasmine.createSpy('patch').and.returnValue(of(mockUsersResponse.content[0])),
      delete: jasmine.createSpy('delete').and.returnValue(of({ id: 1, status: 'INACTIVO', deleted_at: new Date().toISOString(), deleted_by: 1 })),
    },
  };

  const mockToast = jasmine.createSpyObj('ToastService', ['info', 'success', 'error']);
  const mockModal = jasmine.createSpyObj('ModalService', ['show', 'hide']);

  function setMockPersonal() {
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
  }

  beforeEach(async () => {
    mockApi.users.list.calls.reset();
    mockApi.users.create.calls.reset();
    mockApi.users.patch.calls.reset();
    mockApi.users.delete.calls.reset();
    mockApi.corehr.listDevices.calls.reset();
    mockApi.corehr.listBiometria.calls.reset();
    mockApi.corehr.initEnroll.calls.reset();
    mockApi.corehr.listDevices.and.returnValue(of(enrolPage([enrolDevice(7, 'ZK-007'), enrolDevice(8, 'ZK-008')])));
    mockApi.corehr.listBiometria.and.returnValue(of(enrolPage([])));
    mockApi.corehr.initEnroll.and.returnValue(of(enrolSession));
    mockToast.success.calls.reset();
    mockToast.error.calls.reset();
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
    middlewareWs = TestBed.inject(MiddlewareWebSocketService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load personal on init', () => {
    expect(mockApi.users.list).toHaveBeenCalled();
  });

  describe('filtering', () => {
    beforeEach(() => {
      setMockPersonal();
    });

    it('should filter personal by search term', () => {
      component.searchTerm = 'José';
      expect(component.personalFiltrado.length).toBe(1);
      component.searchTerm = '';
      expect(component.personalFiltrado.length).toBe(2);
    });

    it('should filter by sede', () => {
      component.filtroSede = 'Sede San Isidro';
      expect(component.personalFiltrado.length).toBe(1);
      expect(component.personalFiltrado[0].nombre).toContain('José');
      component.filtroSede = '';
      expect(component.personalFiltrado.length).toBe(2);
    });

    it('should filter by area', () => {
      component.filtroArea = 'Administración';
      expect(component.personalFiltrado.length).toBe(1);
      expect(component.personalFiltrado[0].nombre).toContain('Ana');
      component.filtroArea = '';
    });

    it('should filter by departamento', () => {
      component.filtroDepartamento = 'Matemáticas';
      expect(component.personalFiltrado.length).toBe(1);
      component.filtroDepartamento = '';
    });

    it('should search by idPersonal', () => {
      component.searchTerm = '76543210';
      expect(component.personalFiltrado.length).toBe(1);
    });

    it('should combine multiple filters', () => {
      component.searchTerm = 'José';
      component.filtroSede = 'Sede San Isidro';
      component.filtroArea = 'Dirección Académica';
      expect(component.personalFiltrado.length).toBe(1);
    });
  });

  describe('pagination', () => {
    beforeEach(() => {
      setMockPersonal();
    });

    it('should paginate personal', () => {
      component.itemsPerPage = 1;
      expect(component.totalPaginas).toBe(2);
      expect(component.personalPaginado.length).toBe(1);
    });

    it('should change page within bounds', () => {
      component.itemsPerPage = 1;
      component.cambiarPagina(2);
      expect(component.paginaActual).toBe(2);
      component.cambiarPagina(0);
      expect(component.paginaActual).toBe(2);
    });

    it('should not change page beyond totalPaginas', () => {
      component.itemsPerPage = 1;
      component.cambiarPagina(99);
      expect(component.paginaActual).toBe(1);
    });

    it('should return 0 totalPaginas and finRegistro when filtered list is empty', () => {
      component.searchTerm = 'NONEXISTENT';
      expect(component.totalPaginas).toBe(0);
      expect(component.finRegistro).toBe(0);
    });

    it('should compute inicioRegistro and finRegistro', () => {
      component.itemsPerPage = 1;
      expect(component.inicioRegistro).toBe(1);
      expect(component.finRegistro).toBe(1);
      component.cambiarPagina(2);
      expect(component.inicioRegistro).toBe(2);
      expect(component.finRegistro).toBe(2);
    });
  });

  describe('modal lifecycle', () => {
    it('should open and close add modal', () => {
      component.abrirModalAgregar();
      expect(component.modalPersonalOpen).toBeTrue();
      expect(component.editandoPersonal).toBeFalse();
      expect(component.personalForm.estado).toBe('ACTIVO');
      component.cerrarModalPersonal();
      expect(component.modalPersonalOpen).toBeFalse();
    });

    it('should set default sede/area/departamento from available options in add modal', () => {
      setMockPersonal();
      component.abrirModalAgregar();
      expect(component.sedesDisponibles).toContain(component.personalForm.sede);
      expect(component.areasDisponibles).toContain(component.personalForm.area);
      expect(component.departamentosDisponibles).toContain(component.personalForm.departamento);
    });

    it('should open and close edit modal', () => {
      setMockPersonal();
      const persona = component.personal()[0];
      component.abrirModalEditar(persona);
      expect(component.modalPersonalOpen).toBeTrue();
      expect(component.editandoPersonal).toBeTrue();
      expect(component.personalForm.nombre).toBe(persona.nombre);
      component.cerrarModalPersonal();
      expect(component.modalPersonalOpen).toBeFalse();
      expect(component.imagenPreviewUrl).toBeNull();
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
      setMockPersonal();
      component.abrirModalDetalle(component.personal()[0]);
      component.editarDesdeDetalle();
      expect(component.modalDetalleOpen).toBeFalse();
      expect(component.modalPersonalOpen).toBeTrue();
      component.cerrarModalPersonal();
    });
  });

  describe('image handling', () => {
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

    it('should do nothing when no file selected', () => {
      const event = { target: { files: null } } as any;
      component.onFileSelected(event);
      expect(component.imagenPreviewUrl).toBeNull();
    });

    it('should toggle image mode to URL', () => {
      component.cambiarModoImagen(true);
      expect(component.modoImagenUrl).toBeTrue();
    });

    it('should toggle image mode to file', () => {
      component.cambiarModoImagen(false);
      expect(component.modoImagenUrl).toBeFalse();
    });

    it('should update preview URL from form', () => {
      component.personalForm.imagenUrl = 'http://example.com/img.jpg';
      component.actualizarPreviewUrl();
      expect(component.imagenPreviewUrl).toBe('http://example.com/img.jpg');
    });

    it('should set preview to null when imagenUrl is empty', () => {
      component.personalForm.imagenUrl = '';
      component.actualizarPreviewUrl();
      expect(component.imagenPreviewUrl).toBeNull();
    });

    it('should abrirSelectorArchivo trigger file input click', () => {
      const clickSpy = jasmine.createSpy('click');
      const mockInput = { click: clickSpy } as any;
      spyOn(document, 'getElementById').and.returnValue(mockInput);
      component.abrirSelectorArchivo();
      expect(document.getElementById).toHaveBeenCalledWith('fileInput');
      expect(clickSpy).toHaveBeenCalled();
    });
  });

  describe('guardarPersonal', () => {
    it('should create new user', async () => {
      component.editandoPersonal = false;
      component.personalForm.nombre = 'Nuevo Usuario';
      component.personalForm.idPersonal = '12345';
      component.personalForm.email = 'nuevo@colegio.edu.pe';
      await component.guardarPersonal();
      expect(mockApi.users.create).toHaveBeenCalled();
      expect(component.modalPersonalOpen).toBeFalse();
    });

    it('should edit existing user', async () => {
      component.editandoPersonal = true;
      component.personalForm.id = 1;
      component.personalForm.nombre = 'Editado Nombre';
      component.personalForm.idPersonal = '12345';
      await component.guardarPersonal();
      expect(mockApi.users.patch).toHaveBeenCalled();
      expect(component.modalPersonalOpen).toBeFalse();
    });

    it('should not save with empty name', async () => {
      component.editandoPersonal = false;
      component.personalForm.nombre = '';
      component.personalForm.idPersonal = '';
      await component.guardarPersonal();
      expect(mockApi.users.create).not.toHaveBeenCalled();
      expect(mockToast.info).toHaveBeenCalled();
    });

    it('should handle create error', async () => {
      mockApi.users.create.and.returnValue(throwError(() => new Error('fail')));
      component.editandoPersonal = false;
      component.personalForm.nombre = 'Test User';
      component.personalForm.idPersonal = '99999';
      await component.guardarPersonal();
      expect(mockToast.info).toHaveBeenCalledWith('Error al guardar');
      mockApi.users.create.and.returnValue(of(mockUsersResponse.content[0]));
    });

    it('should handle edit error', async () => {
      mockApi.users.patch.and.returnValue(throwError(() => new Error('fail')));
      component.editandoPersonal = true;
      component.personalForm.nombre = 'Test';
      component.personalForm.idPersonal = '99999';
      await component.guardarPersonal();
      expect(mockToast.info).toHaveBeenCalledWith('Error al guardar');
      mockApi.users.patch.and.returnValue(of(mockUsersResponse.content[0]));
    });

    it('should create user with single-word name', async () => {
      component.editandoPersonal = false;
      component.personalForm.nombre = 'SoloNombre';
      component.personalForm.idPersonal = '11111';
      await component.guardarPersonal();
      expect(mockApi.users.create).toHaveBeenCalled();
      expect(component.modalPersonalOpen).toBeFalse();
    });
  });

  describe('eliminarPersonal', () => {
    it('should delete user', async () => {
      await component.eliminarPersonal(1);
      expect(mockApi.users.delete).toHaveBeenCalledWith(1);
      expect(mockToast.info).toHaveBeenCalled();
    });

    it('should handle delete error', async () => {
      mockApi.users.delete.and.returnValue(throwError(() => new Error('fail')));
      await component.eliminarPersonal(1);
      expect(mockToast.info).toHaveBeenCalledWith('Error al eliminar');
      mockApi.users.delete.and.returnValue(of({ id: 1, status: 'INACTIVO', deleted_at: new Date().toISOString(), deleted_by: 1 }));
    });
  });

  describe('addEnrolLog', () => {
    it('should add log entries', () => {
      (component as any).addEnrolLog('test message', 'info');
      expect(component.enrolLogs().length).toBe(1);
      expect(component.enrolLogs()[0].message).toBe('test message');
      expect(component.enrolLogs()[0].type).toBe('info');
    });

    it('should keep max 100 logs', () => {
      for (let i = 0; i < 150; i++) {
        (component as any).addEnrolLog(`msg ${i}`, 'info');
      }
      expect(component.enrolLogs().length).toBe(100);
    });

    it('should default to info type when not specified', () => {
      (component as any).addEnrolLog('no type');
      expect(component.enrolLogs().length).toBe(1);
      expect(component.enrolLogs()[0].type).toBe('info');
    });
  });

  describe('ngOnDestroy', () => {
    it('should cleanup enrollment subscriptions', () => {
      const cleanupSpy = spyOn(component as any, 'cleanupEnrollmentSubscriptions');
      component.ngOnDestroy();
      expect(cleanupSpy).toHaveBeenCalled();
    });
  });

  describe('initial state (before ngOnInit)', () => {
    let freshComponent: DirectorioPersonal;
    let freshFixture: ComponentFixture<DirectorioPersonal>;

    beforeEach(() => {
      freshFixture = TestBed.createComponent(DirectorioPersonal);
      freshComponent = freshFixture.componentInstance;
    });

    it('should have loading and error signals', () => {
      expect(freshComponent.loading()).toBeFalse();
      expect(freshComponent.error()).toBe('');
    });

    it('should have initial metricas at zero', () => {
      expect(freshComponent.metricas.personalTotal).toBe(0);
      expect(freshComponent.metricas.activosHoy).toBe(0);
    });

    it('should have empty filter options', () => {
      expect(freshComponent.sedesDisponibles).toEqual([]);
      expect(freshComponent.areasDisponibles).toEqual([]);
      expect(freshComponent.departamentosDisponibles).toEqual([]);
    });

    it('should have enrollment state initialized', () => {
      expect(freshComponent.modalEnrolarOpen).toBeFalse();
      expect(freshComponent.personaEnrolar).toBeNull();
      expect(freshComponent.enrolConnectionState()).toBe('disconnected');
      expect(freshComponent.enrolOperationLabel()).toBe('');
      expect(freshComponent.enrolInProgress()).toBeFalse();
      expect(freshComponent.enrolProgress()).toBe('');
      expect(freshComponent.enrolFpStatus()).toBe('');
      expect(freshComponent.enrolLogs()).toEqual([]);
      expect(freshComponent.enrolCompleted()).toBeFalse();
      expect(freshComponent.enrolSuccess()).toBeFalse();
      expect(freshComponent.enrolDevices()).toEqual([]);
      expect(freshComponent.enrolDeviceId()).toBeNull();
      expect(freshComponent.enrolFingerIndex()).toBe(1);
    });

    afterEach(() => {
      freshFixture.destroy();
    });
  });

  describe('cargarPersonal', () => {
    it('should load personal, populate filters, and compute metricas', async () => {
      expect(component.sedesDisponibles).toContain('Sede San Isidro');
      expect(component.areasDisponibles).toContain('Dirección Académica');
      expect(component.departamentosDisponibles).toContain('Matemáticas');
      expect(component.metricas.personalTotal).toBe(2);
      expect(component.metricas.activosHoy).toBe(1);
      expect(component.metricas.deLicencia).toBe(1);
    });

    it('should set error on failure', async () => {
      mockApi.users.list.and.returnValue(throwError(() => new Error('fail')));
      await component.cargarPersonal();
      expect(component.error()).toBe('Error al cargar personal. Verifica tu conexión e intenta nuevamente.');
      mockApi.users.list.and.returnValue(of(mockUsersResponse));
    });
  });

  describe('filtrarPersonal', () => {
    it('should reset page to 1', () => {
      component.paginaActual = 3;
      component.filtrarPersonal();
      expect(component.paginaActual).toBe(1);
    });
  });

  describe('cambiarModoImagen', () => {
    it('should show imagenUrl preview when switching to URL mode', () => {
      component.personalForm.imagenUrl = 'http://example.com/img.jpg';
      component.cambiarModoImagen(true);
      expect(component.imagenPreviewUrl).toBe('http://example.com/img.jpg');
    });

    it('should set preview to null when switching to URL mode with empty form', () => {
      component.personalForm.imagenUrl = '';
      component.cambiarModoImagen(true);
      expect(component.imagenPreviewUrl).toBeNull();
    });
  });

  describe('paginaActual guard', () => {
    it('should not change to page 0', () => {
      setMockPersonal();
      component.itemsPerPage = 1;
      component.cambiarPagina(0);
      expect(component.paginaActual).toBe(1);
    });
  });

  describe('personalFiltrado - uncovered branches', () => {
    beforeEach(() => {
      setMockPersonal();
    });

    it('should return all when no filters applied', () => {
      component.searchTerm = '';
      component.filtroSede = '';
      component.filtroArea = '';
      component.filtroDepartamento = '';
      expect(component.personalFiltrado.length).toBe(2);
    });

    it('should match by nombre when searchTerm hits first operand of OR', () => {
      component.searchTerm = 'José';
      const result = component.personalFiltrado;
      expect(result.length).toBe(1);
      expect(result[0].nombre).toContain('José');
    });

    it('should match by idPersonal when first operand of OR is false', () => {
      component.searchTerm = '87654321';
      const result = component.personalFiltrado;
      expect(result.length).toBe(1);
      expect(result[0].idPersonal).toBe('87654321');
    });

    it('should return empty when searchTerm matches neither nombre nor idPersonal', () => {
      component.searchTerm = 'ZZZZZ999';
      expect(component.personalFiltrado.length).toBe(0);
    });

    it('should combine all four filters simultaneously', () => {
      component.searchTerm = 'Ana';
      component.filtroSede = 'Sede Miraflores';
      component.filtroArea = 'Administración';
      component.filtroDepartamento = 'Comunicación';
      expect(component.personalFiltrado.length).toBe(1);
      expect(component.personalFiltrado[0].id).toBe(2);
    });

    it('should return empty when combined filters have no match', () => {
      component.searchTerm = 'Ana';
      component.filtroSede = 'Sede San Isidro';
      component.filtroArea = 'Administración';
      component.filtroDepartamento = 'Comunicación';
      expect(component.personalFiltrado.length).toBe(0);
    });

    it('should filter by departamento only without other filters', () => {
      component.filtroDepartamento = 'Comunicación';
      expect(component.personalFiltrado.length).toBe(1);
      expect(component.personalFiltrado[0].id).toBe(2);
    });

    it('should filter by sede + departamento combined', () => {
      component.filtroSede = 'Sede Miraflores';
      component.filtroDepartamento = 'Comunicación';
      expect(component.personalFiltrado.length).toBe(1);
    });
  });

  describe('cambiarPagina - boundary branches', () => {
    beforeEach(() => {
      setMockPersonal();
      component.itemsPerPage = 1;
    });

    it('should not change page to negative value', () => {
      component.paginaActual = 1;
      component.cambiarPagina(-5);
      expect(component.paginaActual).toBe(1);
    });

    it('should not change page above totalPaginas', () => {
      component.paginaActual = 1;
      component.cambiarPagina(3);
      expect(component.paginaActual).toBe(1);
    });

    it('should change page to last valid page', () => {
      component.cambiarPagina(2);
      expect(component.paginaActual).toBe(2);
    });
  });

  describe('onFileSelected - size boundary branches', () => {
    it('should reject file exceeding 2MB', () => {
      const largeFile = new File([new ArrayBuffer(2 * 1024 * 1024 + 1)], 'over.jpg', { type: 'image/jpeg' });
      const event = { target: { files: [largeFile] } } as any;
      component.onFileSelected(event);
      expect(mockToast.info).toHaveBeenCalledWith('El archivo no puede superar los 2MB');
    });

    it('should accept file exactly at 2MB without rejection toast', () => {
      const exactFile = new File([new ArrayBuffer(2 * 1024 * 1024)], 'exact.jpg', { type: 'image/jpeg' });
      const event = { target: { files: [exactFile] } } as any;
      mockToast.info.calls.reset();
      component.onFileSelected(event);
      const rejectionCalls = mockToast.info.calls.allArgs().filter((args: any[]) => args[0] === 'El archivo no puede superar los 2MB');
      expect(rejectionCalls.length).toBe(0);
    });

    it('should do nothing when no file selected', () => {
      const prev = component.imagenPreviewUrl;
      const event = { target: { files: [] } } as any;
      component.onFileSelected(event);
      expect(component.imagenPreviewUrl).toBe(prev);
    });
  });

  describe('inicioRegistro and finRegistro getters', () => {
    beforeEach(() => {
      setMockPersonal();
    });

    it('should compute correct inicioRegistro and finRegistro on page 1', () => {
      component.itemsPerPage = 1;
      component.paginaActual = 1;
      expect(component.inicioRegistro).toBe(1);
      expect(component.finRegistro).toBe(1);
    });

    it('should compute correct inicioRegistro and finRegistro on page 2', () => {
      component.itemsPerPage = 1;
      component.paginaActual = 2;
      expect(component.inicioRegistro).toBe(2);
      expect(component.finRegistro).toBe(2);
    });

    it('should cap finRegistro at filtered list length on last page', () => {
      component.itemsPerPage = 5;
      component.paginaActual = 1;
      expect(component.inicioRegistro).toBe(1);
      expect(component.finRegistro).toBe(2);
    });

    it('should return 0 finRegistro when filtered list is empty', () => {
      component.searchTerm = 'NONEXISTENT';
      expect(component.finRegistro).toBe(0);
      expect(component.inicioRegistro).toBe(1);
    });
  });


  // El panel abre la sesión en el backend; el agente del lector la completa y el panel lo confirma consultando
  // las huellas activas del usuario. El progreso por WebSocket solo llega si el agente corre en esta PC.
  describe('enrolamiento desde el panel (backend)', () => {
    let handlers: Record<string, (msg: any) => void>;
    const persona = { id: 1, nombre: 'José Pérez', idPersonal: '76543210', sede: '', area: '', departamento: '', cargo: '', estado: 'ACTIVO' as const };
    const biometria = (id: number, fingerIndex: number) => ({
      id, tenant_user_id: 1, device_id: 7, device_code: 'ZK-007', finger_index: fingerIndex, activo: true,
      capturado_en: null, created_at: '2026-10-23T09:00:00', updated_at: '2026-10-23T09:00:00',
    });
    const page = (content: any[]) => ({ content, page: 0, size: 50, totalElements: content.length, totalPages: 1 });

    beforeEach(() => {
      handlers = {};
      spyOn(middlewareWs, 'connect');
      spyOn(middlewareWs, 'send');
      spyOn(middlewareWs, 'on').and.callFake((type: string, handler: (msg: any) => void) => {
        handlers[type] = handler;
        return () => {};
      });
      mockApi.corehr.listBiometria.and.returnValue(of(page([biometria(20, 3)])));
    });

    afterEach(() => component.cerrarModalEnrolar());

    it('al abrir carga los lectores activos y elige el primero', () => {
      component.abrirModalEnrolar(persona);

      expect(mockApi.corehr.listDevices).toHaveBeenCalledWith({ state: true, size: 200 });
      expect(component.enrolDevices().map(d => d.code)).toEqual(['ZK-007', 'ZK-008']);
      expect(component.enrolDeviceId()).toBe(7);
      expect(component.enrolFingerIndex()).toBe(1);
      expect(middlewareWs.connect).toHaveBeenCalled();
    });

    it('sin lectores activos no se puede iniciar', () => {
      mockApi.corehr.listDevices.and.returnValue(of(page([])));
      component.abrirModalEnrolar(persona);

      component.iniciarEnrolamiento();

      expect(component.enrolFpStatus()).toContain('No hay lectores activos');
      expect(mockApi.corehr.initEnroll).not.toHaveBeenCalled();
      expect(mockToast.error).toHaveBeenCalled();
    });

    it('inicia la sesión en el backend con el lector y el dedo elegidos', () => {
      component.abrirModalEnrolar(persona);
      component.onEnrolDeviceChange('8');
      component.onEnrolFingerChange('6');

      component.iniciarEnrolamiento();

      expect(mockApi.corehr.initEnroll).toHaveBeenCalledWith({ tenant_user_id: 1, device_id: 8, finger_index: 6 });
      expect(component.enrolInProgress()).toBeTrue();
      expect(component.enrolFpStatus()).toContain('ZK-007');
      expect(middlewareWs.send).not.toHaveBeenCalled();
    });

    it('confirma el enrolamiento cuando la huella nueva aparece en el servidor y deja de consultar', fakeAsync(() => {
      component.abrirModalEnrolar(persona);
      component.iniciarEnrolamiento();

      tick(ENROLL_POLL_MS);
      expect(component.enrolInProgress()).toBeTrue();

      mockApi.corehr.listBiometria.and.returnValue(of(page([biometria(20, 3), biometria(21, 1)])));
      tick(ENROLL_POLL_MS);

      expect(component.enrolCompleted()).toBeTrue();
      expect(component.enrolSuccess()).toBeTrue();
      expect(mockToast.success).toHaveBeenCalled();
      const calls = mockApi.corehr.listBiometria.calls.count();
      tick(ENROLL_POLL_MS * 3);
      expect(mockApi.corehr.listBiometria.calls.count()).toBe(calls);
    }));

    it('una huella de otro dedo no cuenta como el enrolamiento pedido', fakeAsync(() => {
      component.abrirModalEnrolar(persona);
      component.iniciarEnrolamiento();
      mockApi.corehr.listBiometria.and.returnValue(of(page([biometria(20, 3), biometria(22, 4)])));

      tick(ENROLL_POLL_MS);

      expect(component.enrolCompleted()).toBeFalse();
      expect(component.enrolInProgress()).toBeTrue();
      component.cancelarEnrolamiento();
    }));

    it('muestra el rechazo del backend al iniciar', () => {
      mockApi.corehr.initEnroll.and.returnValue(throwError(() => ({
        status: 400, error: { message: 'Ya existe una sesión de enrolamiento activa para este usuario y dispositivo' },
      })));
      component.abrirModalEnrolar(persona);

      component.iniciarEnrolamiento();

      expect(component.enrolCompleted()).toBeTrue();
      expect(component.enrolSuccess()).toBeFalse();
      expect(component.enrolInProgress()).toBeFalse();
      expect(component.enrolFpStatus()).toContain('Ya existe una sesión');
    });

    it('si el lector no completa a tiempo, falla y deja de consultar', fakeAsync(() => {
      component.abrirModalEnrolar(persona);
      component.iniciarEnrolamiento();

      tick(ENROLL_TIMEOUT_MS + ENROLL_POLL_MS);

      expect(component.enrolCompleted()).toBeTrue();
      expect(component.enrolSuccess()).toBeFalse();
      expect(component.enrolFpStatus()).toContain('a tiempo');
      const calls = mockApi.corehr.listBiometria.calls.count();
      tick(ENROLL_POLL_MS * 3);
      expect(mockApi.corehr.listBiometria.calls.count()).toBe(calls);
    }));

    it('muestra el progreso que publica el agente local para esta persona', () => {
      component.abrirModalEnrolar(persona);
      component.iniciarEnrolamiento();

      handlers['fingerprint.enroll.progress']({
        type: 'fingerprint.enroll.progress', step: 2, totalSteps: 3, message: 'Captura 2 de 3',
        source: 'backend', tenantUserId: 1, fingerIndex: 1,
      });

      expect(component.enrolProgress()).toBe('2/3');
      expect(component.enrolFpStatus()).toBe('Captura 2 de 3');
    });

    it('ignora el progreso de otro enrolamiento del agente', () => {
      component.abrirModalEnrolar(persona);
      component.iniciarEnrolamiento();

      handlers['fingerprint.enroll.progress']({
        type: 'fingerprint.enroll.progress', step: 2, totalSteps: 3, message: 'Captura 2 de 3',
        source: 'backend', tenantUserId: 99, fingerIndex: 1,
      });

      expect(component.enrolProgress()).toBe('0/3');
    });

    it('un fallo que publica el agente termina el enrolamiento sin esperar al servidor', fakeAsync(() => {
      component.abrirModalEnrolar(persona);
      component.iniciarEnrolamiento();

      handlers['fingerprint.enroll.result']({
        type: 'fingerprint.enroll.result', success: false, message: 'Calidad insuficiente',
        source: 'backend', tenantUserId: 1, fingerIndex: 1,
      });

      expect(component.enrolCompleted()).toBeTrue();
      expect(component.enrolSuccess()).toBeFalse();
      expect(component.enrolFpStatus()).toBe('Calidad insuficiente');
      const calls = mockApi.corehr.listBiometria.calls.count();
      tick(ENROLL_POLL_MS * 2);
      expect(mockApi.corehr.listBiometria.calls.count()).toBe(calls);
    }));

    it('cancelar deja de esperar al lector', fakeAsync(() => {
      component.abrirModalEnrolar(persona);
      component.iniciarEnrolamiento();

      component.cancelarEnrolamiento();

      expect(component.enrolInProgress()).toBeFalse();
      expect(component.enrolOperationLabel()).toBe('Cancelado');
      const calls = mockApi.corehr.listBiometria.calls.count();
      tick(ENROLL_POLL_MS * 2);
      expect(mockApi.corehr.listBiometria.calls.count()).toBe(calls);
    }));

    it('cerrar el modal deja de esperar y no envía nada al agente', fakeAsync(() => {
      component.abrirModalEnrolar(persona);
      component.iniciarEnrolamiento();

      component.cerrarModalEnrolar();

      expect(component.modalEnrolarOpen).toBeFalse();
      expect(component.personaEnrolar).toBeNull();
      expect(middlewareWs.send).not.toHaveBeenCalled();
      const calls = mockApi.corehr.listBiometria.calls.count();
      tick(ENROLL_POLL_MS * 2);
      expect(mockApi.corehr.listBiometria.calls.count()).toBe(calls);
    }));

    it('no inicia otra sesión si ya hay una en curso', () => {
      component.abrirModalEnrolar(persona);
      component.iniciarEnrolamiento();

      component.iniciarEnrolamiento();

      expect(mockApi.corehr.initEnroll).toHaveBeenCalledTimes(1);
    });
  });

});
