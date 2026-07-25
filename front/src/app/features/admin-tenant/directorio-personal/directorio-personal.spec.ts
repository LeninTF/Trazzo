import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { DirectorioPersonal } from './directorio-personal';
import { ApiService } from '../../../api/services/api.service';
import { ToastService } from '../../../services/toast.service';
import { ModalService } from '../../../services/modal.service';
import { MiddlewareWebSocketService } from '../../../services/middleware-websocket.service';
import { FingerprintStoreService } from '../../../services/fingerprint-store.service';
import { of, throwError } from 'rxjs';
import { fakeAsync, tick } from '@angular/core/testing';

describe('DirectorioPersonal', () => {
  let component: DirectorioPersonal;
  let fixture: ComponentFixture<DirectorioPersonal>;
  let middlewareWs: MiddlewareWebSocketService;
  let fingerprintStore: FingerprintStoreService;

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
    fingerprintStore = TestBed.inject(FingerprintStoreService);
    fixture.detectChanges();
  });

  afterEach(() => {
    fingerprintStore.clear();
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

  describe('enrollment modal', () => {
    const mockPersona = { id: 1, nombre: 'José Pérez', idPersonal: '76543210', sede: '', area: '', departamento: '', cargo: '', estado: 'ACTIVO' as const };

    it('should open enrollment modal and connect middleware', () => {
      const connectSpy = spyOn(middlewareWs, 'connect');
      component.abrirModalEnrolar(mockPersona);
      expect(component.modalEnrolarOpen).toBeTrue();
      expect(component.personaEnrolar).toBe(mockPersona);
      expect(component.enrolCompleted()).toBeFalse();
      expect(component.enrolSuccess()).toBeFalse();
      expect(component.enrolFpStatus()).toBe('Preparando conexión...');
      expect(connectSpy).toHaveBeenCalled();
    });

    it('should close enrollment modal and cancel ongoing enrollment', () => {
      component.abrirModalEnrolar(mockPersona);
      component.enrolInProgress.set(true);
      const sendSpy = spyOn(middlewareWs, 'send');
      component.cerrarModalEnrolar();
      expect(component.modalEnrolarOpen).toBeFalse();
      expect(component.personaEnrolar).toBeNull();
      expect(sendSpy).toHaveBeenCalledWith('fingerprint.enroll.cancel');
    });

    it('should close enrollment modal without cancel if not in progress', () => {
      component.abrirModalEnrolar(mockPersona);
      const sendSpy = spyOn(middlewareWs, 'send');
      component.cerrarModalEnrolar();
      expect(sendSpy).not.toHaveBeenCalled();
    });
  });

  describe('iniciarEnrolamiento', () => {
    const mockPersona = { id: 1, nombre: 'José Pérez', idPersonal: '76543210', sede: '', area: '', departamento: '', cargo: '', estado: 'ACTIVO' as const };

    it('should send fingerprint.capture when starting enrollment', () => {
      component.abrirModalEnrolar(mockPersona);
      const sendSpy = spyOn(middlewareWs, 'send');
      component.iniciarEnrolamiento();
      expect(component.enrolInProgress()).toBeTrue();
      expect(component.enrolAwaitingReference()).toBeTrue();
      expect(component.enrolOperationLabel()).toBe('Capturando referencia...');
      expect(sendSpy).toHaveBeenCalledWith('fingerprint.capture');
    });

    it('should not start if already in progress', () => {
      component.abrirModalEnrolar(mockPersona);
      component.enrolInProgress.set(true);
      const sendSpy = spyOn(middlewareWs, 'send');
      component.iniciarEnrolamiento();
      expect(sendSpy).not.toHaveBeenCalled();
    });
  });

  describe('cancelarEnrolamiento', () => {
    it('should cancel enrollment and reset state', () => {
      component.enrolInProgress.set(true);
      const sendSpy = spyOn(middlewareWs, 'send');
      component.cancelarEnrolamiento();
      expect(component.enrolInProgress()).toBeFalse();
      expect(component.enrolOperationLabel()).toBe('Cancelado');
      expect(component.enrolFpStatus()).toBe('Enrolamiento cancelado.');
      expect(sendSpy).toHaveBeenCalledWith('fingerprint.enroll.cancel');
    });

    it('should not cancel if not in progress', () => {
      const sendSpy = spyOn(middlewareWs, 'send');
      component.cancelarEnrolamiento();
      expect(sendSpy).not.toHaveBeenCalled();
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
      expect(freshComponent.enrolFpImageUrl()).toBeNull();
      expect(freshComponent.enrolFpStatus()).toBe('');
      expect(freshComponent.enrolQuality()).toBeNull();
      expect(freshComponent.enrolTemplateSize()).toBe(0);
      expect(freshComponent.enrolTemplatePreview()).toBe('');
      expect(freshComponent.enrolLogs()).toEqual([]);
      expect(freshComponent.enrolCompleted()).toBeFalse();
      expect(freshComponent.enrolSuccess()).toBeFalse();
      expect(freshComponent.enrolAwaitingReference()).toBeFalse();
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

  describe('cerrarModalEnrolar - branch coverage', () => {
    const mockPersona = { id: 1, nombre: 'José Pérez', idPersonal: '76543210', sede: '', area: '', departamento: '', cargo: '', estado: 'ACTIVO' as const };

    it('should reset awaitingReference and referenceData when closing', () => {
      component.abrirModalEnrolar(mockPersona);
      component.enrolAwaitingReference.set(true);
      spyOn(middlewareWs, 'send');
      component.cerrarModalEnrolar();
      expect(component.enrolAwaitingReference()).toBeFalse();
      expect(component.modalEnrolarOpen).toBeFalse();
      expect(component.personaEnrolar).toBeNull();
    });

    it('should send cancel when enrolInProgress is true', () => {
      component.abrirModalEnrolar(mockPersona);
      component.enrolInProgress.set(true);
      const sendSpy = spyOn(middlewareWs, 'send');
      component.cerrarModalEnrolar();
      expect(sendSpy).toHaveBeenCalledWith('fingerprint.enroll.cancel');
    });

    it('should not send cancel when enrolInProgress is false', () => {
      component.abrirModalEnrolar(mockPersona);
      const sendSpy = spyOn(middlewareWs, 'send');
      component.cerrarModalEnrolar();
      expect(sendSpy).not.toHaveBeenCalledWith('fingerprint.enroll.cancel');
    });
  });

  describe('iniciarEnrolamiento - guard branch', () => {
    const mockPersona = { id: 1, nombre: 'José Pérez', idPersonal: '76543210', sede: '', area: '', departamento: '', cargo: '', estado: 'ACTIVO' as const };

    it('should early return when enrolInProgress is true', () => {
      component.abrirModalEnrolar(mockPersona);
      component.enrolInProgress.set(true);
      const sendSpy = spyOn(middlewareWs, 'send');
      component.iniciarEnrolamiento();
      expect(sendSpy).not.toHaveBeenCalled();
      expect(component.enrolOperationLabel()).not.toBe('Capturando referencia...');
    });

    it('should proceed normally when enrolInProgress is false', () => {
      component.abrirModalEnrolar(mockPersona);
      component.enrolInProgress.set(false);
      const sendSpy = spyOn(middlewareWs, 'send');
      component.iniciarEnrolamiento();
      expect(sendSpy).toHaveBeenCalledWith('fingerprint.capture');
      expect(component.enrolInProgress()).toBeTrue();
      expect(component.enrolAwaitingReference()).toBeTrue();
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

  describe('enrollment result handler - fingerprint.enroll.result', () => {
    let handlers: Record<string, (msg: any) => void>;
    const mockPersona = { id: 1, nombre: 'José Pérez', idPersonal: '76543210', sede: 'Sede San Isidro', area: 'Dirección', departamento: 'Matemáticas', cargo: '', estado: 'ACTIVO' as const };

    beforeEach(() => {
      handlers = {};
      spyOn(middlewareWs, 'connect');
      spyOn(middlewareWs, 'send');
      spyOn(middlewareWs, 'on').and.callFake((type: string, handler: (msg: any) => void) => {
        handlers[type] = handler;
        return () => {};
      });
    });

    it('should save to fingerprintStore on success with personaEnrolar', () => {
      component.abrirModalEnrolar(mockPersona);
      const resultData = {
        type: 'fingerprint.enroll.result',
        success: true,
        message: 'Enrolamiento completado',
        registeredTemplateBase64: 'abc123template',
        encryptedRegisteredTemplate: {
          encryptedTemplateBase64: 'encTemplate',
          encryptedAesKeyBase64: 'encKey',
          ivBase64: 'iv',
          tagBase64: 'tag',
        },
        registeredTemplateSize: 512,
        deviceId: 'dev1',
        capturedSamples: 3,
        capturedAtUtc: new Date().toISOString(),
      };
      handlers['fingerprint.enroll.result'](resultData);
      expect(component.enrolCompleted()).toBeTrue();
      expect(component.enrolSuccess()).toBeTrue();
      expect(component.enrolInProgress()).toBeFalse();
      expect(component.enrolTemplateSize()).toBe(512);
      expect(component.enrolOperationLabel()).toBe('Completado');
      expect(fingerprintStore.count).toBe(1);
      expect(mockToast.success).toHaveBeenCalled();
    });

    it('should save to fingerprintStore with null encryptedRegisteredTemplate', () => {
      component.abrirModalEnrolar(mockPersona);
      const resultData = {
        type: 'fingerprint.enroll.result',
        success: true,
        message: 'OK',
        registeredTemplateBase64: 'tpl',
        encryptedRegisteredTemplate: null,
        registeredTemplateSize: 100,
        deviceId: 'dev1',
        capturedSamples: 3,
        capturedAtUtc: new Date().toISOString(),
      };
      handlers['fingerprint.enroll.result'](resultData);
      expect(fingerprintStore.count).toBe(1);
      expect(mockToast.success).toHaveBeenCalled();
    });

    it('should save with fallback when registeredTemplateBase64 is null', () => {
      component.abrirModalEnrolar(mockPersona);
      const resultData = {
        type: 'fingerprint.enroll.result',
        success: true,
        message: 'OK',
        registeredTemplateBase64: null,
        encryptedRegisteredTemplate: null,
        registeredTemplateSize: 0,
        deviceId: 'dev1',
        capturedSamples: 3,
        capturedAtUtc: new Date().toISOString(),
      };
      handlers['fingerprint.enroll.result'](resultData);
      expect(fingerprintStore.count).toBe(1);
      expect(component.enrolTemplatePreview()).toBe('(cifrado)');
    });

    it('should show template preview when registeredTemplateBase64 is present', () => {
      component.abrirModalEnrolar(mockPersona);
      const resultData = {
        type: 'fingerprint.enroll.result',
        success: true,
        message: 'OK',
        registeredTemplateBase64: 'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
        encryptedRegisteredTemplate: null,
        registeredTemplateSize: 100,
        deviceId: 'dev1',
        capturedSamples: 3,
        capturedAtUtc: new Date().toISOString(),
      };
      handlers['fingerprint.enroll.result'](resultData);
      expect(component.enrolTemplatePreview()).toContain('…');
    });

    it('should use capturedSamples in progress display', () => {
      component.abrirModalEnrolar(mockPersona);
      const resultData = {
        type: 'fingerprint.enroll.result',
        success: true,
        message: 'OK',
        registeredTemplateBase64: 'tpl',
        encryptedRegisteredTemplate: null,
        registeredTemplateSize: 100,
        deviceId: 'dev1',
        capturedSamples: 2,
        capturedAtUtc: new Date().toISOString(),
      };
      handlers['fingerprint.enroll.result'](resultData);
      expect(component.enrolProgress()).toBe('2/3');
    });

    it('should default capturedSamples to 0 when null', () => {
      component.abrirModalEnrolar(mockPersona);
      const resultData = {
        type: 'fingerprint.enroll.result',
        success: true,
        message: 'OK',
        registeredTemplateBase64: 'tpl',
        encryptedRegisteredTemplate: null,
        registeredTemplateSize: 100,
        deviceId: 'dev1',
        capturedSamples: null,
        capturedAtUtc: new Date().toISOString(),
      };
      handlers['fingerprint.enroll.result'](resultData);
      expect(component.enrolProgress()).toBe('0/3');
    });

    it('should use message fallback when message is empty on success', () => {
      component.abrirModalEnrolar(mockPersona);
      const resultData = {
        type: 'fingerprint.enroll.result',
        success: true,
        message: '',
        registeredTemplateBase64: 'tpl',
        encryptedRegisteredTemplate: null,
        registeredTemplateSize: 100,
        deviceId: 'dev1',
        capturedSamples: 3,
        capturedAtUtc: new Date().toISOString(),
      };
      handlers['fingerprint.enroll.result'](resultData);
      expect(component.enrolFpStatus()).toBe('Enrolamiento completado.');
    });

    it('should use message fallback when message is empty on failure', () => {
      component.abrirModalEnrolar(mockPersona);
      const resultData = {
        type: 'fingerprint.enroll.result',
        success: false,
        message: '',
        registeredTemplateBase64: null,
        encryptedRegisteredTemplate: null,
        registeredTemplateSize: 0,
        deviceId: 'dev1',
        capturedSamples: 0,
        capturedAtUtc: new Date().toISOString(),
      };
      handlers['fingerprint.enroll.result'](resultData);
      expect(component.enrolFpStatus()).toBe('Enrolamiento fallido.');
      expect(component.enrolOperationLabel()).toBe('Fallido');
      expect(mockToast.error).toHaveBeenCalled();
    });

    it('should handle failure when personaEnrolar is null', () => {
      component.abrirModalEnrolar(mockPersona);
      component.personaEnrolar = null;
      const resultData = {
        type: 'fingerprint.enroll.result',
        success: true,
        message: 'Done',
        registeredTemplateBase64: 'tpl',
        encryptedRegisteredTemplate: null,
        registeredTemplateSize: 100,
        deviceId: 'dev1',
        capturedSamples: 3,
        capturedAtUtc: new Date().toISOString(),
      };
      handlers['fingerprint.enroll.result'](resultData);
      expect(component.enrolCompleted()).toBeTrue();
      expect(component.enrolSuccess()).toBeTrue();
      expect(fingerprintStore.count).toBe(0);
      expect(mockToast.error).toHaveBeenCalled();
    });

    it('should show enrol progress on enroll.progress event', () => {
      component.abrirModalEnrolar(mockPersona);
      const progressData = {
        type: 'fingerprint.enroll.progress',
        step: 2,
        totalSteps: 3,
        message: 'Coloca el dedo',
      };
      handlers['fingerprint.enroll.progress'](progressData);
      expect(component.enrolProgress()).toBe('2/3');
      expect(component.enrolFpStatus()).toBe('Coloca el dedo');
      expect(component.enrolInProgress()).toBeTrue();
      expect(component.enrolOperationLabel()).toBe('Enrolando huella...');
    });

    it('should default progress message when message is empty', () => {
      component.abrirModalEnrolar(mockPersona);
      const progressData = {
        type: 'fingerprint.enroll.progress',
        step: 1,
        totalSteps: 3,
        message: '',
      };
      handlers['fingerprint.enroll.progress'](progressData);
      expect(component.enrolFpStatus()).toBe('Coloca el dedo en el lector...');
    });
  });

  describe('enrollment capture result handler - fingerprint.capture.result', () => {
    let handlers: Record<string, (msg: any) => void>;
    const mockPersona = { id: 1, nombre: 'José Pérez', idPersonal: '76543210', sede: '', area: '', departamento: '', cargo: '', estado: 'ACTIVO' as const };

    beforeEach(() => {
      handlers = {};
      spyOn(middlewareWs, 'connect');
      spyOn(middlewareWs, 'send');
      spyOn(middlewareWs, 'on').and.callFake((type: string, handler: (msg: any) => void) => {
        handlers[type] = handler;
        return () => {};
      });
    });

    it('should set fingerprint image url when data.fingerprintImageDataUrl is present', () => {
      component.abrirModalEnrolar(mockPersona);
      const captureData = {
        type: 'fingerprint.capture.result',
        success: true,
        message: 'OK',
        templateBase64: null,
        templateSize: 0,
        fingerprintImageDataUrl: 'data:image/png;base64,abc',
        quality: null,
      };
      handlers['fingerprint.capture.result'](captureData);
      expect(component.enrolFpImageUrl()).toBe('data:image/png;base64,abc');
    });

    it('should set quality when data.quality is present', () => {
      component.abrirModalEnrolar(mockPersona);
      const quality = { isAcceptable: true, foregroundPixelCount: 100, foregroundCoveragePercent: 50, contrastScore: 80, isCentered: true, message: 'Good' };
      const captureData = {
        type: 'fingerprint.capture.result',
        success: true,
        message: 'OK',
        templateBase64: null,
        templateSize: 0,
        fingerprintImageDataUrl: null,
        quality,
      };
      handlers['fingerprint.capture.result'](captureData);
      expect(component.enrolQuality()).toEqual(quality);
    });

    it('should store reference when awaiting reference with success and templateBase64', () => {
      component.abrirModalEnrolar(mockPersona);
      component.iniciarEnrolamiento();
      expect(component.enrolAwaitingReference()).toBeTrue();

      const captureData = {
        type: 'fingerprint.capture.result',
        success: true,
        message: 'OK',
        templateBase64: 'refTemplate123',
        templateSize: 256,
        fingerprintImageDataUrl: null,
        quality: null,
      };
      handlers['fingerprint.capture.result'](captureData);

      expect(component.enrolAwaitingReference()).toBeFalse();
      expect(middlewareWs.send).toHaveBeenCalledWith('fingerprint.enroll.start');
      expect(component.enrolOperationLabel()).toBe('Enrolando huella...');
      expect(component.enrolFpStatus()).toBe('Coloca el dedo para captura 1 de 3...');
    });

    it('should handle reference capture failure when not success', () => {
      component.abrirModalEnrolar(mockPersona);
      component.iniciarEnrolamiento();

      const captureData = {
        type: 'fingerprint.capture.result',
        success: false,
        message: 'No se detectó dedo',
        templateBase64: null,
        templateSize: 0,
        fingerprintImageDataUrl: null,
        quality: null,
      };
      handlers['fingerprint.capture.result'](captureData);

      expect(component.enrolAwaitingReference()).toBeFalse();
      expect(component.enrolCompleted()).toBeTrue();
      expect(component.enrolSuccess()).toBeFalse();
      expect(component.enrolInProgress()).toBeFalse();
      expect(component.enrolOperationLabel()).toBe('Error');
      expect(component.enrolFpStatus()).toBe('No se detectó dedo');
    });

    it('should handle reference capture failure when success but no templateBase64', () => {
      component.abrirModalEnrolar(mockPersona);
      component.iniciarEnrolamiento();

      const captureData = {
        type: 'fingerprint.capture.result',
        success: true,
        message: 'Captured',
        templateBase64: null,
        templateSize: 0,
        fingerprintImageDataUrl: null,
        quality: null,
      };
      handlers['fingerprint.capture.result'](captureData);

      expect(component.enrolAwaitingReference()).toBeFalse();
      expect(component.enrolCompleted()).toBeTrue();
      expect(component.enrolSuccess()).toBeFalse();
      expect(component.enrolInProgress()).toBeFalse();
    });

    it('should use fallback message when capture reference fails with empty message', () => {
      component.abrirModalEnrolar(mockPersona);
      component.iniciarEnrolamiento();

      const captureData = {
        type: 'fingerprint.capture.result',
        success: false,
        message: '',
        templateBase64: null,
        templateSize: 0,
        fingerprintImageDataUrl: null,
        quality: null,
      };
      handlers['fingerprint.capture.result'](captureData);
      expect(component.enrolFpStatus()).toBe('Error capturando referencia.');
    });

    it('should not process reference logic when not awaiting reference', () => {
      component.abrirModalEnrolar(mockPersona);
      expect(component.enrolAwaitingReference()).toBeFalse();

      const captureData = {
        type: 'fingerprint.capture.result',
        success: true,
        message: 'OK',
        templateBase64: 'tpl',
        templateSize: 100,
        fingerprintImageDataUrl: 'data:image/png;base64,xyz',
        quality: { isAcceptable: true, foregroundPixelCount: 50, foregroundCoveragePercent: 30, contrastScore: 70, isCentered: true, message: 'OK' },
      };
      handlers['fingerprint.capture.result'](captureData);
      expect(component.enrolFpImageUrl()).toBe('data:image/png;base64,xyz');
      expect(middlewareWs.send).not.toHaveBeenCalledWith('fingerprint.enroll.start');
    });

    it('should handle capture with both image and quality and template together', () => {
      component.abrirModalEnrolar(mockPersona);
      component.iniciarEnrolamiento();

      const quality = { isAcceptable: false, foregroundPixelCount: 10, foregroundCoveragePercent: 5, contrastScore: 20, isCentered: false, message: 'Poor' };
      const captureData = {
        type: 'fingerprint.capture.result',
        success: true,
        message: 'OK',
        templateBase64: 'refTpl',
        templateSize: 128,
        fingerprintImageDataUrl: 'data:image/png;base64,fullData',
        quality,
      };
      handlers['fingerprint.capture.result'](captureData);
      expect(component.enrolFpImageUrl()).toBe('data:image/png;base64,fullData');
      expect(component.enrolQuality()).toEqual(quality);
      expect(component.enrolAwaitingReference()).toBeFalse();
      expect(middlewareWs.send).toHaveBeenCalledWith('fingerprint.enroll.start');
    });
  });

  describe('enrollment progress handler', () => {
    let handlers: Record<string, (msg: any) => void>;
    const mockPersona = { id: 1, nombre: 'José Pérez', idPersonal: '76543210', sede: '', area: '', departamento: '', cargo: '', estado: 'ACTIVO' as const };

    beforeEach(() => {
      handlers = {};
      spyOn(middlewareWs, 'connect');
      spyOn(middlewareWs, 'send');
      spyOn(middlewareWs, 'on').and.callFake((type: string, handler: (msg: any) => void) => {
        handlers[type] = handler;
        return () => {};
      });
    });

    it('should update progress, status and operation label on progress event', () => {
      component.abrirModalEnrolar(mockPersona);
      handlers['fingerprint.enroll.progress']({
        type: 'fingerprint.enroll.progress',
        step: 1,
        totalSteps: 3,
        message: 'Coloca el dedo',
      });
      expect(component.enrolProgress()).toBe('1/3');
      expect(component.enrolFpStatus()).toBe('Coloca el dedo');
      expect(component.enrolInProgress()).toBeTrue();
      expect(component.enrolOperationLabel()).toBe('Enrolando huella...');
    });

    it('should default message on progress event with empty message', () => {
      component.abrirModalEnrolar(mockPersona);
      handlers['fingerprint.enroll.progress']({
        type: 'fingerprint.enroll.progress',
        step: 2,
        totalSteps: 3,
        message: '',
      });
      expect(component.enrolFpStatus()).toBe('Coloca el dedo en el lector...');
    });
  });
});
