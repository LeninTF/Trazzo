import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { Monitoreo } from './monitoreo';
import { MiddlewareWebSocketService } from '../../../services/middleware-websocket.service';
import { FingerprintStoreService } from '../../../services/fingerprint-store.service';
import { ToastService } from '../../../services/toast.service';

describe('Monitoreo', () => {
  let component: Monitoreo;
  let fixture: ComponentFixture<Monitoreo>;
  let middlewareWs: MiddlewareWebSocketService;
  let fingerprintStore: FingerprintStoreService;
  let toastService: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    toastService = jasmine.createSpyObj('ToastService', ['success', 'error', 'info']);
    await TestBed.configureTestingModule({
      imports: [Monitoreo],
      providers: [
        { provide: ToastService, useValue: toastService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Monitoreo);
    component = fixture.componentInstance;
    middlewareWs = TestBed.inject(MiddlewareWebSocketService);
    fingerprintStore = TestBed.inject(FingerprintStoreService);
    await fixture.whenStable();
  });

  afterEach(() => {
    fixture.destroy();
    fingerprintStore.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial metrics', () => {
    expect(component.metricas.presentesHoy).toBe(1284);
    expect(component.metricas.tardanzas).toBe(42);
    expect(component.metricas.dispositivosActivos).toBe(2);
  });

  it('should have 4 initial events', () => {
    expect(component.eventos.length).toBe(4);
  });

  it('should have 3 escaneres', () => {
    expect(component.escaneres.length).toBe(3);
  });

  it('should compute totalDispositivosTexto', () => {
    expect(component.totalDispositivosTexto).toBe('2/3');
  });

  it('should compute eventosATiempo', () => {
    const aTiempo = component.eventos.filter(e => e.estado === 'A TIEMPO').length;
    expect(component.eventosATiempo).toBe(aTiempo);
  });

  it('should compute eventosTarde', () => {
    const tarde = component.eventos.filter(e => e.estado === 'TARDE').length;
    expect(component.eventosTarde).toBe(tarde);
  });

  it('should show ultimaActualizacionTexto', () => {
    expect(component.ultimaActualizacionTexto()).toContain('segundos');
  });

  it('should actualizarDatosTiempoReal', () => {
    const lengthBefore = component.eventos.length;
    component.actualizarDatosTiempoReal();
    expect(component.eventos.length).toBeGreaterThanOrEqual(lengthBefore);
    expect(component.metricas.presentesHoy).toBeGreaterThan(1284);
  });

  it('should keep max 10 events after real-time update', () => {
    for (let i = 0; i < 10; i++) {
      component.actualizarDatosTiempoReal();
    }
    expect(component.eventos.length).toBeLessThanOrEqual(10);
  });

  it('should registrarEscaner', () => {
    component.nuevoEscaner = { nombre: 'Test Escaner', ubicacion: 'Test Ubicacion', online: true };
    component.registrarEscaner();
    expect(component.escaneres.length).toBe(4);
    expect(component.metricas.totalDispositivos).toBe(4);
  });

  it('should not registrarEscaner with empty nombre', () => {
    component.nuevoEscaner = { nombre: '', ubicacion: 'Ubicacion', online: true };
    component.registrarEscaner();
    expect(component.escaneres.length).toBe(3);
  });

  it('should not registrarEscaner with empty ubicacion', () => {
    component.nuevoEscaner = { nombre: 'Nombre', ubicacion: '', online: true };
    component.registrarEscaner();
    expect(component.escaneres.length).toBe(3);
  });

  it('should eliminarEscaner find and set', () => {
    component.eliminarEscaner(1);
    expect(component.escanerAEliminar).toBeTruthy();
    expect(component.escanerAEliminar!.id).toBe(1);
  });

  it('should confirmarEliminarEscaner', () => {
    component.escanerAEliminar = component.escaneres[0];
    component.confirmarEliminarEscaner();
    expect(component.escaneres.length).toBe(2);
    expect(component.escanerAEliminar).toBeNull();
  });

  it('should toggleEscaner online/offline', () => {
    const escaner = component.escaneres[0];
    const wasOnline = escaner.online;
    component.toggleEscaner(escaner.id);
    expect(escaner.online).toBe(!wasOnline);
  });

  it('should toggleEscaner update metricas', () => {
    component.toggleEscaner(1);
    expect(component.metricas.dispositivosActivos).toBe(component.escaneres.filter(e => e.online).length);
  });

  it('should limpiarFormularioEscaner', () => {
    component.nuevoEscaner = { nombre: 'Test', ubicacion: 'Test', online: false };
    component.limpiarFormularioEscaner();
    expect(component.nuevoEscaner.nombre).toBe('');
    expect(component.nuevoEscaner.ubicacion).toBe('');
    expect(component.nuevoEscaner.online).toBeTrue();
  });

  it('should agregarEventoDeSistema', () => {
    const lenBefore = component.eventos.length;
    component.agregarEventoDeSistema('Test event');
    expect(component.eventos.length).toBe(lenBefore + 1);
    expect(component.eventos[0].nombre).toBe('SISTEMA');
  });

  it('should eliminarEvento', () => {
    const lenBefore = component.eventos.length;
    component.eliminarEvento(1);
    expect(component.eventos.length).toBe(lenBefore - 1);
  });

  it('should not eliminarEvento with non-existent id', () => {
    const lenBefore = component.eventos.length;
    component.eliminarEvento(999);
    expect(component.eventos.length).toBe(lenBefore);
  });

  it('should refrescarDatos', () => {
    const presentesBefore = component.metricas.presentesHoy;
    component.refrescarDatos();
    expect(component.metricas.presentesHoy).toBeGreaterThan(presentesBefore);
  });

  it('should update tardanza level on many late events', () => {
    for (let i = 0; i < 30; i++) {
      component.actualizarDatosTiempoReal();
    }
  });

  it('should set ultimaActualizacion on init', () => {
    expect(component.ultimaActualizacion).toBeDefined();
  });

  it('should clean up interval on destroy', () => {
    spyOn(window, 'clearInterval');
    component.ngOnDestroy();
    expect(window.clearInterval).toHaveBeenCalled();
  });

  // =============== NEW COVERAGE TESTS ===============

  describe('fingerprint state signals', () => {
    it('should have initial operation signals', () => {
      expect(component.operationLabel()).toBe('Inactivo');
      expect(component.operationInProgress()).toBeFalse();
      expect(component.enrollmentInProgress()).toBeFalse();
      expect(component.enrollmentProgress()).toBe('');
      expect(component.readerConnected()).toBeFalse();
      expect(component.readerDetail()).toBe('Sin consultar');
      expect(component.fpImageUrl()).toBeNull();
      expect(component.fpStatus()).toBe('Sin captura todavía');
      expect(component.fpQuality()).toBeNull();
      expect(component.fpTemplateSize()).toBe(0);
      expect(component.fpTemplatePreview()).toBe('');
      expect(component.logs()).toEqual([]);
    });
  });

  describe('quality getters', () => {
    it('should return — when quality is null', () => {
      expect(component.calidadEstimada).toBe('—');
      expect(component.areaCruda).toBe('—');
      expect(component.contraste).toBe('—');
      expect(component.centrada).toBe('—');
      expect(component.calidadMensaje).toBe('—');
    });

    it('should return quality values when set', () => {
      component.fpQuality.set({
        isAcceptable: true,
        foregroundPixelCount: 100,
        foregroundCoveragePercent: 75.5,
        contrastScore: 0.8,
        isCentered: true,
        message: 'Buena calidad',
      });
      expect(component.calidadEstimada).toBe('75.5%');
      expect(component.areaCruda).toBe('75.5%');
      expect(component.contraste).toBe('0.8');
      expect(component.centrada).toBe('Sí');
      expect(component.calidadMensaje).toBe('Buena calidad');
    });
  });

  describe('capturarHuella', () => {
    it('should send fingerprint.capture and update operation state', () => {
      const sendSpy = spyOn(middlewareWs, 'send');
      component.capturarHuella();
      expect(component.operationLabel()).toBe('Esperando huella…');
      expect(component.operationInProgress()).toBeTrue();
      expect(component.fpStatus()).toBe('Coloca el dedo en el lector…');
      expect(sendSpy).toHaveBeenCalledWith('fingerprint.capture');
    });

    it('should not send if operation already in progress', () => {
      component.operationInProgress.set(true);
      const sendSpy = spyOn(middlewareWs, 'send');
      component.capturarHuella();
      expect(sendSpy).not.toHaveBeenCalled();
    });
  });

  describe('identificarHuella', () => {
    it('should send fingerprint.identify when enrolled users exist', () => {
      fingerprintStore.save({
        userId: 1, userName: 'Test', userDisplayId: '123',
        templateBase64: 'x', templateSize: 100,
        referenceTemplateBase64: 'y', referenceTemplateSize: 50,
        encryptedTemplate: null, enrolledAt: '',
      });
      const sendSpy = spyOn(middlewareWs, 'send');
      component.identificarHuella();
      expect(component.operationInProgress()).toBeTrue();
      expect(sendSpy).toHaveBeenCalledWith('fingerprint.identify');
    });

    it('should show info toast when no enrolled users', () => {
      const sendSpy = spyOn(middlewareWs, 'send');
      component.identificarHuella();
      expect(toastService.info).toHaveBeenCalledWith('No hay usuarios enrolados. Enrole al menos una huella primero.');
      expect(sendSpy).not.toHaveBeenCalled();
    });

    it('should not send if operation already in progress', () => {
      component.operationInProgress.set(true);
      const sendSpy = spyOn(middlewareWs, 'send');
      component.identificarHuella();
      expect(sendSpy).not.toHaveBeenCalled();
    });
  });

  describe('enrolarHuella', () => {
    it('should send fingerprint.enroll.start and update state', () => {
      const sendSpy = spyOn(middlewareWs, 'send');
      component.enrolarHuella();
      expect(component.enrollmentProgress()).toBe('0/3');
      expect(component.operationLabel()).toBe('Enrolando huella…');
      expect(component.operationInProgress()).toBeTrue();
      expect(component.enrollmentInProgress()).toBeTrue();
      expect(sendSpy).toHaveBeenCalledWith('fingerprint.enroll.start');
    });

    it('should not send if operation already in progress', () => {
      component.operationInProgress.set(true);
      const sendSpy = spyOn(middlewareWs, 'send');
      component.enrolarHuella();
      expect(sendSpy).not.toHaveBeenCalled();
    });
  });

  describe('cancelarEnrolamiento', () => {
    it('should send fingerprint.enroll.cancel when enrollment in progress', () => {
      component.enrollmentInProgress.set(true);
      const sendSpy = spyOn(middlewareWs, 'send');
      component.cancelarEnrolamiento();
      expect(sendSpy).toHaveBeenCalledWith('fingerprint.enroll.cancel');
    });

    it('should not send if no enrollment in progress', () => {
      const sendSpy = spyOn(middlewareWs, 'send');
      component.cancelarEnrolamiento();
      expect(sendSpy).not.toHaveBeenCalled();
    });
  });

  describe('conectarWs / desconectarWs', () => {
    it('should connect to middleware', () => {
      const connectSpy = spyOn(middlewareWs, 'connect');
      component.conectarWs();
      expect(connectSpy).toHaveBeenCalled();
    });

    it('should disconnect and reset all biometric state', () => {
      const disconnectSpy = spyOn(middlewareWs, 'disconnect');
      component.readerConnected.set(true);
      component.readerDetail.set('Conectado');
      component.operationLabel.set('Activo');
      component.operationInProgress.set(true);
      component.enrollmentInProgress.set(true);
      component.fpImageUrl.set('data:image/png;base64,abc');
      component.fpStatus.set('Capturada');
      component.fpQuality.set({} as any);
      component.fpTemplateSize.set(500);
      component.fpTemplatePreview.set('abc…');
      component.enrollmentProgress.set('2/3');

      component.desconectarWs();

      expect(disconnectSpy).toHaveBeenCalled();
      expect(component.readerConnected()).toBeFalse();
      expect(component.readerDetail()).toBe('Sin consultar');
      expect(component.operationLabel()).toBe('Inactivo');
      expect(component.operationInProgress()).toBeFalse();
      expect(component.enrollmentInProgress()).toBeFalse();
      expect(component.fpImageUrl()).toBeNull();
      expect(component.fpStatus()).toBe('Sin captura todavía');
      expect(component.fpQuality()).toBeNull();
      expect(component.fpTemplateSize()).toBe(0);
      expect(component.fpTemplatePreview()).toBe('');
      expect(component.enrollmentProgress()).toBe('');
    });
  });

  describe('consultarEstadoLector', () => {
    it('should send device.status', () => {
      const sendSpy = spyOn(middlewareWs, 'send');
      component.consultarEstadoLector();
      expect(sendSpy).toHaveBeenCalledWith('device.status');
    });
  });

  describe('addLog / limpiarLogs', () => {
    it('should add log entries with timestamp', () => {
      (component as any).addLog('test message', 'info');
      expect(component.logs().length).toBe(1);
      expect(component.logs()[0].message).toBe('test message');
      expect(component.logs()[0].type).toBe('info');
      expect(component.logs()[0].timestamp).toBeTruthy();
    });

    it('should default to info type', () => {
      (component as any).addLog('test message');
      expect(component.logs()[0].type).toBe('info');
    });

    it('should keep max 100 logs', () => {
      for (let i = 0; i < 150; i++) {
        (component as any).addLog(`msg ${i}`, 'info');
      }
      expect(component.logs().length).toBe(100);
    });

    it('should clear all logs', () => {
      (component as any).addLog('test', 'info');
      component.limpiarLogs();
      expect(component.logs()).toEqual([]);
    });
  });

  describe('handleFingerprintResult with capture', () => {
    it('should handle successful capture result', fakeAsync(() => {
      const data = {
        type: 'fingerprint.capture.result',
        success: true,
        message: 'Huella capturada',
        templateBase64: 'dGVzdA==',
        templateSize: 100,
        quality: { isAcceptable: true, foregroundPixelCount: 100, foregroundCoveragePercent: 80, contrastScore: 0.9, isCentered: true, message: 'OK' },
        fingerprintImageDataUrl: 'data:image/png;base64,img',
        encryptedTemplate: null,
        deviceId: null,
        fingerprintImageBase64: null,
        fingerprintImageMimeType: null,
        capturedAtUtc: '',
      } as any;

      (component as any).handleFingerprintResult(data);
      expect(component.fpQuality()).toBeTruthy();
      expect(component.fpTemplateSize()).toBe(100);
      expect(component.fpImageUrl()).toBe('data:image/png;base64,img');
      expect(toastService.success).toHaveBeenCalledWith('Huella verificada correctamente');
      tick(800);
    }));

    it('should handle failed capture result', fakeAsync(() => {
      const data = {
        type: 'fingerprint.capture.result',
        success: false,
        message: 'Error de captura',
        templateBase64: null,
        templateSize: 0,
        quality: null,
      } as any;

      (component as any).handleFingerprintResult(data);
      expect(component.fpImageUrl()).toBeNull();
      expect(toastService.error).toHaveBeenCalledWith('Huella no verificada: Error de captura');
      tick(800);
    }));
  });

  describe('handleFingerprintResult with identify', () => {
    it('should handle matched identify result', fakeAsync(() => {
      fingerprintStore.save({
        userId: 1, userName: 'Juan Pérez', userDisplayId: '123',
        templateBase64: 'x', templateSize: 500,
        referenceTemplateBase64: 'y', referenceTemplateSize: 100,
        encryptedTemplate: null, enrolledAt: '',
      });

      const data = {
        type: 'fingerprint.identify.result',
        success: true,
        message: 'Identificado',
        templateBase64: 'dGVzdA==',
        templateSize: 100,
        quality: null,
        fingerprintImageDataUrl: null,
      } as any;

      (component as any).handleFingerprintResult(data);
      expect(toastService.success).toHaveBeenCalledWith('Asistencia marcada: Juan Pérez');
      tick(800);
    }));

    it('should handle unmatched identify result', fakeAsync(() => {
      fingerprintStore.save({
        userId: 1, userName: 'Juan Pérez', userDisplayId: '123',
        templateBase64: 'x', templateSize: 500,
        referenceTemplateBase64: 'y', referenceTemplateSize: 100,
        encryptedTemplate: null, enrolledAt: '',
      });

      const data = {
        type: 'fingerprint.identify.result',
        success: true,
        message: 'Identificado',
        templateBase64: 'dGVzdA==',
        templateSize: 999,
        quality: null,
        fingerprintImageDataUrl: null,
      } as any;

      (component as any).handleFingerprintResult(data);
      expect(toastService.info).toHaveBeenCalledWith('Huella capturada pero no coincide con ningún usuario enrolado');
      tick(800);
    }));
  });

  describe('agregarEventoAsistencia', () => {
    it('should add attendance event and increment presentesHoy', () => {
      const lenBefore = component.eventos.length;
      (component as any).agregarEventoAsistencia('Juan Pérez', '123');
      expect(component.eventos.length).toBe(lenBefore + 1);
      expect(component.eventos[0].nombre).toBe('Juan Pérez');
      expect(component.metricas.presentesHoy).toBe(1285);
    });

    it('should cap events at 10', () => {
      for (let i = 0; i < 15; i++) {
        (component as any).agregarEventoAsistencia(`User ${i}`, `${i}`);
      }
      expect(component.eventos.length).toBeLessThanOrEqual(10);
    });

    it('should increment tardanzas for late events', () => {
      const originalTardanzas = component.metricas.tardanzas;
      (component as any).agregarEventoAsistencia('Late User', '999');
      if (component.eventos[0].estado === 'TARDE') {
        expect(component.metricas.tardanzas).toBe(originalTardanzas + 1);
      }
    });
  });

  describe('finishOp', () => {
    it('should set status message and reset after timeout', fakeAsync(() => {
      (component as any).finishOp('Operación completada.');
      expect(component.fpStatus()).toBe('Operación completada.');
      tick(800);
      expect(component.operationLabel()).toBe('Inactivo');
      expect(component.operationInProgress()).toBeFalse();
      expect(component.enrollmentInProgress()).toBeFalse();
    }));
  });

  describe('actualizarEscaneresPorMiddleware', () => {
    it('should add physical scanner if not present', () => {
      const initialLen = component.escaneres.length;
      (component as any).actualizarEscaneresPorMiddleware({
        type: 'device.status.changed', success: true, isConnected: true, message: 'Conectado',
      });
      expect(component.escaneres.length).toBe(initialLen + 1);
      expect(component.escaneres[0].fisico).toBeTrue();
      expect(component.escaneres[0].online).toBeTrue();
    });

    it('should update existing physical scanner', () => {
      (component as any).actualizarEscaneresPorMiddleware({
        type: 'device.status.changed', success: true, isConnected: true, message: 'Conectado',
      });
      expect(component.escaneres[0].online).toBeTrue();
      (component as any).actualizarEscaneresPorMiddleware({
        type: 'device.status.changed', success: true, isConnected: false, message: 'Desconectado',
      });
      expect(component.escaneres[0].online).toBeFalse();
    });

    it('should update active device count', () => {
      (component as any).actualizarEscaneresPorMiddleware({
        type: 'device.status.changed', success: true, isConnected: true, message: 'Conectado',
      });
      expect(component.metricas.dispositivosActivos).toBe(component.escaneres.filter(e => e.online).length);
    });
  });

  describe('ngOnInit', () => {
    it('should connect middleware on init', () => {
      const connectSpy = spyOn(middlewareWs, 'connect');
      component.ngOnInit();
      expect(connectSpy).toHaveBeenCalled();
      component.ngOnDestroy();
    });

    it('should set up interval for real-time updates', fakeAsync(() => {
      component.ngOnInit();
      const eventosBefore = component.eventos.length;
      tick(30000);
      expect(component.eventos.length).toBeGreaterThanOrEqual(eventosBefore);
      component.ngOnDestroy();
      discardPeriodicTasks();
    }));
  });

  describe('ngOnDestroy', () => {
    it('should disconnect middleware', () => {
      const disconnectSpy = spyOn(middlewareWs, 'disconnect');
      component.ngOnDestroy();
      expect(disconnectSpy).toHaveBeenCalled();
    });
  });

  describe('actualizarDatosTiempoReal and nivelTardanza', () => {
    it('should set nivelTardanza to MEDIO when tardanzas > 20', () => {
      component.metricas.tardanzas = 21;
      component.metricas.nivelTardanza = 'BAJO';
      spyOn(Monitoreo as any, 'secureRandom').and.returnValue(0.9);
      component.actualizarDatosTiempoReal();
      expect(component.metricas.nivelTardanza).toBe('MEDIO');
    });

    it('should set nivelTardanza to ALTO when tardanzas > 50', () => {
      component.metricas.tardanzas = 55;
      component.actualizarDatosTiempoReal();
      expect(component.metricas.nivelTardanza).toBe('ALTO');
    });
  });

  describe('eliminarEvento metrics update', () => {
    it('should decrease tardanzas when deleting a late event', () => {
      component.eventos.unshift({
        id: 999, nombre: 'Late', rol: '', hora: '', idDispositivo: '',
        estado: 'TARDE', escaner: '', ubicacion: '', online: true,
      });
      component.metricas.tardanzas = 10;
      component.eliminarEvento(999);
      expect(component.metricas.tardanzas).toBe(9);
    });
  });

  describe('actualizarTextoUltimaActualizacion', () => {
    it('should show segundos for < 60s diff', () => {
      (component as any).actualizarTextoUltimaActualizacion();
      expect(component.ultimaActualizacionTexto()).toContain('segundos');
    });

    it('should show minutos for >= 60s diff', () => {
      component.ultimaActualizacion = new Date(Date.now() - 120000);
      (component as any).actualizarTextoUltimaActualizacion();
      expect(component.ultimaActualizacionTexto()).toContain('minutos');
    });

    it('should show singular minuto for exactly 60s', () => {
      component.ultimaActualizacion = new Date(Date.now() - 60000);
      (component as any).actualizarTextoUltimaActualizacion();
      expect(component.ultimaActualizacionTexto()).toContain('minuto');
    });

    it('should show time string for >= 3600s diff', () => {
      component.ultimaActualizacion = new Date(Date.now() - 7200000);
      (component as any).actualizarTextoUltimaActualizacion();
      expect(component.ultimaActualizacionTexto()).not.toContain('hace');
    });
  });

  describe('actualizarMetricasConEvento', () => {
    it('should increment tardanzas for TARDE event', () => {
      const before = component.metricas.tardanzas;
      (component as any).actualizarMetricasConEvento({
        id: 1, nombre: 'Test', rol: '', hora: '', idDispositivo: '',
        estado: 'TARDE', escaner: '', ubicacion: '', online: true,
      });
      expect(component.metricas.tardanzas).toBe(before + 1);
    });

    it('should not increment tardanzas for A TIEMPO event', () => {
      const before = component.metricas.tardanzas;
      (component as any).actualizarMetricasConEvento({
        id: 1, nombre: 'Test', rol: '', hora: '', idDispositivo: '',
        estado: 'A TIEMPO', escaner: '', ubicacion: '', online: true,
      });
      expect(component.metricas.tardanzas).toBe(before);
    });
  });

  describe('agregarEventoALista', () => {
    it('should pop oldest when exceeding 10 events', () => {
      component.eventos = Array.from({ length: 10 }, (_, i) => ({
        id: i, nombre: 'E', rol: '', hora: '', idDispositivo: '',
        estado: 'A TIEMPO' as const, escaner: '', ubicacion: '', online: true,
      }));
      (component as any).agregarEventoALista({
        id: 999, nombre: 'New', rol: '', hora: '', idDispositivo: '',
        estado: 'A TIEMPO', escaner: '', ubicacion: '', online: true,
      });
      expect(component.eventos.length).toBe(10);
      expect(component.eventos[0].id).toBe(999);
      expect(component.eventos.some(e => e.id === 9)).toBeFalse();
    });
  });

  describe('conectarMiddleware event handlers', () => {
    function dispatch(type: string, data: unknown): void {
      const handlers = (middlewareWs as any).messageHandlers as Map<string, Set<(msg: unknown) => void>>;
      handlers.get(type)?.forEach(h => h(data));
    }

    afterEach(() => {
      component.ngOnDestroy();
    });

    it('should handle device.connecting event', () => {
      component.ngOnInit();
      dispatch('device.connecting', {
        type: 'device.connecting', success: true, isConnected: false, message: 'Conectando...',
      });
      expect(component.readerConnected()).toBeFalse();
      expect(component.readerDetail()).toBe('Conectando...');
    });

    it('should handle device.status.result with connected reader', () => {
      component.ngOnInit();
      dispatch('device.status.result', {
        type: 'device.status.result', success: true, isConnected: true, message: 'Conectado', deviceCount: 1,
      });
      expect(component.readerConnected()).toBeTrue();
      expect(component.readerDetail()).toContain('Conectado');
    });

    it('should handle device.status.result with disconnected reader', () => {
      component.ngOnInit();
      dispatch('device.status.result', {
        type: 'device.status.result', success: true, isConnected: false, message: 'Desconectado', deviceCount: 0,
      });
      expect(component.readerConnected()).toBeFalse();
      expect(component.fpStatus()).toBe('Conecte el ZK9500 por USB y vuelva a consultar el estado.');
    });

    it('should handle error event from middleware', fakeAsync(() => {
      component.ngOnInit();
      dispatch('error', { type: 'error', success: false, message: 'Error de prueba' });
      expect(toastService.error).toHaveBeenCalledWith('Error de prueba');
      tick(800);
    }));

    it('should handle health.check.result silently', () => {
      component.ngOnInit();
      dispatch('health.check.result', {});
      expect(component.operationLabel()).toBe('Inactivo');
    });

    it('should handle fingerprint.enroll.progress event', () => {
      component.ngOnInit();
      dispatch('fingerprint.enroll.progress', {
        type: 'fingerprint.enroll.progress', success: true, step: 1, totalSteps: 3, message: 'Captura 1 de 3',
      });
      expect(component.enrollmentProgress()).toBe('1/3');
      expect(component.fpStatus()).toBe('Captura 1 de 3');
      expect(component.operationLabel()).toBe('Enrolando huella…');
      expect(component.operationInProgress()).toBeTrue();
      expect(component.enrollmentInProgress()).toBeTrue();
    });

    it('should handle fingerprint.enroll.result with success', fakeAsync(() => {
      component.ngOnInit();
      dispatch('fingerprint.enroll.result', {
        type: 'fingerprint.enroll.result', success: true, message: 'Enrolamiento exitoso',
        registeredTemplateBase64: 'dGVzdA==', registeredTemplateSize: 500, capturedSamples: 3, encryptedRegisteredTemplate: null,
      });
      expect(component.fpTemplateSize()).toBe(500);
      expect(component.fpStatus()).toBe('Enrolamiento exitoso');
      tick(800);
    }));

    it('should handle fingerprint.enroll.result with failure', fakeAsync(() => {
      component.ngOnInit();
      dispatch('fingerprint.enroll.result', {
        type: 'fingerprint.enroll.result', success: false, message: 'Error en enrolamiento',
        registeredTemplateBase64: null, registeredTemplateSize: 0, capturedSamples: 0, encryptedRegisteredTemplate: null,
      });
      expect(component.fpStatus()).toBe('Error en enrolamiento');
      tick(800);
    }));
  });
});
