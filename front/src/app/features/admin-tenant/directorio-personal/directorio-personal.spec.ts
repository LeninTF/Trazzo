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
      component.cerrarModalPersonal();
      expect(component.modalPersonalOpen).toBeFalse();
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

    beforeEach(async () => {
      TestBed.resetTestingModule();
      await TestBed.configureTestingModule({
        imports: [DirectorioPersonal],
        providers: [
          provideHttpClient(),
          { provide: ApiService, useValue: mockApi },
          { provide: ToastService, useValue: mockToast },
          { provide: ModalService, useValue: mockModal },
        ],
      }).compileComponents();

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
  });

  describe('paginaActual guard', () => {
    it('should not change to page 0', () => {
      setMockPersonal();
      component.itemsPerPage = 1;
      component.cambiarPagina(0);
      expect(component.paginaActual).toBe(1);
    });
  });
});
