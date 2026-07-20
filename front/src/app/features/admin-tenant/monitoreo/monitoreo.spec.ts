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
    expect(component.metricas.presentesHoy).toBe(0);
    expect(component.metricas.tardanzas).toBe(0);
    expect(component.metricas.dispositivosActivos).toBe(0);
  });

  it('should have no initial events', () => {
    expect(component.eventos.length).toBe(0);
  });

  it('should have no escaneres', () => {
    expect(component.escaneres.length).toBe(0);
  });

  it('should compute totalDispositivosTexto', () => {
    expect(component.totalDispositivosTexto).toBe('0/0');
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



  it('should registrarEscaner', () => {
    component.nuevoEscaner = { nombre: 'Test Escaner', ubicacion: 'Test Ubicacion', online: true };
    component.registrarEscaner();
    expect(component.escaneres.length).toBe(1);
    expect(component.metricas.totalDispositivos).toBe(1);
  });

  it('should not registrarEscaner with empty nombre', () => {
    component.nuevoEscaner = { nombre: '', ubicacion: 'Ubicacion', online: true };
    component.registrarEscaner();
    expect(component.escaneres.length).toBe(0);
  });

  it('should not registrarEscaner with empty ubicacion', () => {
    component.nuevoEscaner = { nombre: 'Nombre', ubicacion: '', online: true };
    component.registrarEscaner();
    expect(component.escaneres.length).toBe(0);
  });

  it('should eliminarEscaner find and set', () => {
    component.escaneres.push({ id: 1, nombre: 'Test', ubicacion: 'Ubi', online: true });
    component.eliminarEscaner(1);
    expect(component.escanerAEliminar).toBeTruthy();
    expect(component.escanerAEliminar!.id).toBe(1);
  });

  it('should confirmarEliminarEscaner', () => {
    component.escaneres.push({ id: 1, nombre: 'Test', ubicacion: 'Ubi', online: true });
    component.escanerAEliminar = component.escaneres[0];
    component.confirmarEliminarEscaner();
    expect(component.escaneres.length).toBe(0);
    expect(component.escanerAEliminar).toBeNull();
  });

  it('should toggleEscaner online/offline', () => {
    component.escaneres.push({ id: 1, nombre: 'Test', ubicacion: 'Ubi', online: true });
    const escaner = component.escaneres[0];
    const wasOnline = escaner.online;
    component.toggleEscaner(escaner.id);
    expect(escaner.online).toBe(!wasOnline);
  });

  it('should toggleEscaner update metricas', () => {
    component.escaneres.push({ id: 1, nombre: 'Test', ubicacion: 'Ubi', online: true });
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
    component.eventos.push({ id: 1, nombre: 'Test', rol: '', hora: '', idDispositivo: '', estado: 'A TIEMPO', escaner: '', ubicacion: '', online: true });
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
    const sendSpy = spyOn(middlewareWs, 'send');
    component.refrescarDatos();
    expect(sendSpy).toHaveBeenCalledWith('device.status');
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
      expect(component.metricas.presentesHoy).toBe(1);
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
      (component as any).actualizarEscaneresPorMiddleware({
        type: 'device.status.changed', success: true, isConnected: true, message: 'Conectado',
      });
      expect(component.escaneres.length).toBe(1);
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

    it('should set up interval for ultimaActualizacion update', fakeAsync(() => {
      component.ngOnInit();
      const textBefore = component.ultimaActualizacionTexto();
      tick(30000);
      expect(component.ultimaActualizacionTexto()).toBeDefined();
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

  // =============== BRANCH COVERAGE TESTS ===============

  describe('actualizarTextoUltimaActualizacion – branch coverage', () => {
    it('should display seconds when diff < 60', () => {
      component.ultimaActualizacion = new Date(Date.now() - 15000);
      (component as any).actualizarTextoUltimaActualizacion();
      expect(component.ultimaActualizacionTexto()).toContain('segundos');
    });

    it('should display singular minuto when minutos === 1', () => {
      component.ultimaActualizacion = new Date(Date.now() - 60000);
      (component as any).actualizarTextoUltimaActualizacion();
      expect(component.ultimaActualizacionTexto()).toBe('hace 1 minuto');
    });

    it('should display plural minutos when minutos > 1', () => {
      component.ultimaActualizacion = new Date(Date.now() - 180000);
      (component as any).actualizarTextoUltimaActualizacion();
      expect(component.ultimaActualizacionTexto()).toBe('hace 3 minutos');
    });

    it('should display localized time when diff >= 3600', () => {
      component.ultimaActualizacion = new Date(Date.now() - 3600000);
      (component as any).actualizarTextoUltimaActualizacion();
      expect(component.ultimaActualizacionTexto()).not.toContain('hace');
    });
  });

  describe('actualizarEscaneresPorMiddleware – branch coverage', () => {
    it('should unshift new physical scanner when id=0 not present (idxFisico < 0)', () => {
      component.escaneres = [];
      (component as any).actualizarEscaneresPorMiddleware({
        type: 'device.status.changed', success: true, isConnected: true, message: 'ok',
      });
      expect(component.escaneres.length).toBe(1);
      expect(component.escaneres[0].id).toBe(0);
      expect(component.escaneres[0].online).toBeTrue();
      expect(component.escaneres[0].fisico).toBeTrue();
    });

    it('should update existing scanner when id=0 already present (idxFisico >= 0)', () => {
      component.escaneres = [{ id: 0, nombre: 'Old', ubicacion: 'Old', online: false, fisico: true }];
      (component as any).actualizarEscaneresPorMiddleware({
        type: 'device.status.changed', success: true, isConnected: true, message: 'ok',
      });
      expect(component.escaneres.length).toBe(1);
      expect(component.escaneres[0].online).toBeTrue();
      expect(component.escaneres[0].nombre).toBe('Lector Biométrico');
    });
  });

  describe('capturarHuella – operationInProgress guard', () => {
    it('should return early when operationInProgress is true', () => {
      component.operationInProgress.set(true);
      const sendSpy = spyOn(middlewareWs, 'send');
      component.capturarHuella();
      expect(sendSpy).not.toHaveBeenCalled();
    });
  });

  describe('identificarHuella – operationInProgress guard and enrolled.length === 0', () => {
    it('should return early when operationInProgress is true', () => {
      component.operationInProgress.set(true);
      const sendSpy = spyOn(middlewareWs, 'send');
      component.identificarHuella();
      expect(sendSpy).not.toHaveBeenCalled();
    });

    it('should show info and return early when enrolled.length === 0', () => {
      const sendSpy = spyOn(middlewareWs, 'send');
      component.identificarHuella();
      expect(toastService.info).toHaveBeenCalledWith(
        'No hay usuarios enrolados. Enrole al menos una huella primero.'
      );
      expect(sendSpy).not.toHaveBeenCalled();
    });
  });

  describe('enrolarHuella – operationInProgress guard', () => {
    it('should return early when operationInProgress is true', () => {
      component.operationInProgress.set(true);
      const sendSpy = spyOn(middlewareWs, 'send');
      component.enrolarHuella();
      expect(sendSpy).not.toHaveBeenCalled();
    });
  });

  describe('registrarEscaner – valid vs invalid form branches', () => {
    it('should not register when both nombre and ubicacion are empty', () => {
      component.nuevoEscaner = { nombre: '', ubicacion: '', online: true };
      component.registrarEscaner();
      expect(component.escaneres.length).toBe(0);
      expect(toastService.info).toHaveBeenCalledWith(
        'Complete los campos obligatorios: Nombre y Ubicación'
      );
    });

    it('should not register when nombre is empty', () => {
      component.nuevoEscaner = { nombre: '', ubicacion: 'Lobby', online: true };
      component.registrarEscaner();
      expect(component.escaneres.length).toBe(0);
    });

    it('should not register when ubicacion is empty', () => {
      component.nuevoEscaner = { nombre: 'Scanner', ubicacion: '', online: true };
      component.registrarEscaner();
      expect(component.escaneres.length).toBe(0);
    });

    it('should register and reset form when both fields valid', () => {
      component.nuevoEscaner = { nombre: 'Scanner', ubicacion: 'Lobby', online: true };
      component.registrarEscaner();
      expect(component.escaneres.length).toBe(1);
      expect(component.escaneres[0].nombre).toBe('Scanner');
      expect(component.nuevoEscaner.nombre).toBe('');
    });
  });

  describe('eliminarEscaner – null escanerAEliminar branch', () => {
    it('should keep escanerAEliminar null when id not found', () => {
      component.escaneres = [{ id: 1, nombre: 'Test', ubicacion: 'Ubi', online: true }];
      component.eliminarEscaner(999);
      expect(component.escanerAEliminar).toBeNull();
    });
  });

  describe('confirmarEliminarEscaner – null escanerAEliminar branch', () => {
    it('should do nothing when escanerAEliminar is null', () => {
      component.escaneres = [
        { id: 1, nombre: 'A', ubicacion: 'X', online: true },
        { id: 2, nombre: 'B', ubicacion: 'Y', online: true },
      ];
      component.escanerAEliminar = null;
      component.confirmarEliminarEscaner();
      expect(component.escaneres.length).toBe(2);
    });
  });

  describe('agregarEventoAsistencia – time-based branch coverage', () => {
    const _OrigDate = globalThis.Date;

    afterEach(() => {
      (globalThis as any).Date = _OrigDate;
    });

    function freezeTimeAt(hours: number): void {
      const base = new _OrigDate(2025, 5, 15, hours, 30, 0);
      const FakeDate = function () {
        return new _OrigDate(base.getTime());
      } as any;
      FakeDate.now = () => base.getTime();
      FakeDate.parse = _OrigDate.parse;
      FakeDate.UTC = _OrigDate.UTC;
      (globalThis as any).Date = FakeDate;
    }

    it('should set A TIEMPO when hour < 9', () => {
      freezeTimeAt(8);
      (component as any).agregarEventoAsistencia('Early', '001');
      expect(component.eventos[0].estado).toBe('A TIEMPO');
      expect(component.metricas.tardanzas).toBe(0);
    });

    it('should set TARDE when hour >= 9', () => {
      freezeTimeAt(10);
      (component as any).agregarEventoAsistencia('Late', '002');
      expect(component.eventos[0].estado).toBe('TARDE');
    });

    it('should increment tardanzas when estado is TARDE', () => {
      freezeTimeAt(14);
      component.metricas.tardanzas = 0;
      (component as any).agregarEventoAsistencia('Late', '003');
      expect(component.metricas.tardanzas).toBe(1);
    });

    it('should not increment tardanzas when estado is A TIEMPO', () => {
      freezeTimeAt(7);
      component.metricas.tardanzas = 5;
      (component as any).agregarEventoAsistencia('Early', '004');
      expect(component.metricas.tardanzas).toBe(5);
    });
  });

  describe('toggleEscaner – escaner not found guard', () => {
    it('should do nothing when escaner with given id does not exist', () => {
      component.escaneres = [{ id: 1, nombre: 'Test', ubicacion: 'Ubi', online: true }];
      component.toggleEscaner(999);
      expect(component.escaneres[0].online).toBeTrue();
    });
  });

  describe('eliminarEvento – TARDE decrement branch', () => {
    it('should decrement tardanzas when eliminating TARDE event', () => {
      component.eventos.unshift({
        id: 100, nombre: 'Late', rol: '', hora: '', idDispositivo: '',
        estado: 'TARDE', escaner: '', ubicacion: '', online: true,
      });
      component.metricas.tardanzas = 10;
      component.eliminarEvento(100);
      expect(component.metricas.tardanzas).toBe(9);
    });

    it('should not decrement tardanzas when eliminating A TIEMPO event', () => {
      component.eventos.unshift({
        id: 101, nombre: 'OnTime', rol: '', hora: '', idDispositivo: '',
        estado: 'A TIEMPO', escaner: '', ubicacion: '', online: true,
      });
      component.metricas.tardanzas = 10;
      component.eliminarEvento(101);
      expect(component.metricas.tardanzas).toBe(10);
    });
  });

  describe('handleFingerprintResult – uncovered inner branches', () => {
    it('should default templateSize to 0 when property not in data', fakeAsync(() => {
      const data = {
        type: 'fingerprint.capture.result',
        success: true,
        message: 'OK',
        quality: null,
        fingerprintImageDataUrl: 'data:image/png;base64,img',
        templateBase64: 'dGVzdA==',
      } as any;
      (component as any).handleFingerprintResult(data);
      expect(component.fpTemplateSize()).toBe(0);
      expect(component.fpTemplatePreview()).toBe('—');
      tick(800);
    }));

    it('should show (cifrado) when templateBase64 is falsy and templateSize > 0', fakeAsync(() => {
      const data = {
        type: 'fingerprint.capture.result',
        success: true,
        message: 'OK',
        templateSize: 200,
        quality: null,
        fingerprintImageDataUrl: null,
        templateBase64: null,
      } as any;
      (component as any).handleFingerprintResult(data);
      expect(component.fpTemplatePreview()).toBe('(cifrado)');
      tick(800);
    }));

    it('should handle identify result without templateSize property', fakeAsync(() => {
      fingerprintStore.save({
        userId: 1, userName: 'Test', userDisplayId: '123',
        templateBase64: 'x', templateSize: 500,
        referenceTemplateBase64: 'y', referenceTemplateSize: 100,
        encryptedTemplate: null, enrolledAt: '',
      });

      const data = {
        type: 'fingerprint.identify.result',
        success: true,
        message: 'Identified',
        quality: null,
        fingerprintImageDataUrl: null,
        templateBase64: 'dGVzdA==',
      } as any;
      (component as any).handleFingerprintResult(data);
      expect(component.fpTemplateSize()).toBe(0);
      tick(800);
    }));
  });

  describe('deep branch coverage – 17 targeted branches', () => {
    function dispatch(type: string, data: unknown): void {
      const handlers = (middlewareWs as any).messageHandlers as Map<string, Set<(msg: unknown) => void>>;
      handlers.get(type)?.forEach(h => h(data));
    }

    afterEach(() => {
      component.ngOnDestroy();
    });

    it('should cover device.status.changed handler isConnected=true with readerDetail Conectado', () => {
      component.ngOnInit();
      dispatch('device.status.changed', {
        type: 'device.status.changed', success: true, isConnected: true, message: 'Conectado',
      });
      expect(component.readerConnected()).toBeTrue();
      expect(component.readerDetail()).toBe('Conectado');
    });

    it('should cover device.status.changed handler isConnected=false with readerDetail Desconectado', () => {
      component.ngOnInit();
      dispatch('device.status.changed', {
        type: 'device.status.changed', success: true, isConnected: false, message: 'Desconectado',
      });
      expect(component.readerConnected()).toBeFalse();
      expect(component.readerDetail()).toBe('Desconectado');
    });

    it('should set fpImageUrl null when fingerprintImageDataUrl is null on success', fakeAsync(() => {
      (component as any).handleFingerprintResult({
        type: 'fingerprint.capture.result', success: true, message: 'Capturada',
        templateBase64: 'dGVzdA==', templateSize: 200, quality: null,
        fingerprintImageDataUrl: null,
      } as any);
      expect(component.fpImageUrl()).toBeNull();
      expect(toastService.success).toHaveBeenCalledWith('Huella verificada correctamente');
      tick(800);
    }));

    it('should default capturedSamples to 0/3 when undefined on enroll success', fakeAsync(() => {
      component.ngOnInit();
      dispatch('fingerprint.enroll.result', {
        type: 'fingerprint.enroll.result', success: true, message: 'Enrolled',
        registeredTemplateBase64: 'abc', registeredTemplateSize: 500,
      });
      expect(component.enrollmentProgress()).toBe('0/3');
      tick(800);
    }));

    it('should use Enrolamiento completado when enroll message is empty on success', fakeAsync(() => {
      component.ngOnInit();
      dispatch('fingerprint.enroll.result', {
        type: 'fingerprint.enroll.result', success: true, message: '',
        registeredTemplateBase64: 'abc', registeredTemplateSize: 500, capturedSamples: 2,
      });
      expect(component.fpStatus()).toBe('Enrolamiento completado.');
      tick(800);
    }));

    it('should use Enrolamiento fallido when enroll message is empty on failure', fakeAsync(() => {
      component.ngOnInit();
      dispatch('fingerprint.enroll.result', {
        type: 'fingerprint.enroll.result', success: false, message: '',
        registeredTemplateBase64: null, registeredTemplateSize: 0,
      });
      expect(component.fpStatus()).toBe('Enrolamiento fallido.');
      tick(800);
    }));

    it('should show (cifrado) when registeredTemplateBase64 is null on enroll success', fakeAsync(() => {
      component.ngOnInit();
      dispatch('fingerprint.enroll.result', {
        type: 'fingerprint.enroll.result', success: true, message: 'OK',
        registeredTemplateBase64: null, registeredTemplateSize: 300, capturedSamples: 3,
      });
      expect(component.fpTemplatePreview()).toBe('(cifrado)');
      expect(component.fpTemplateSize()).toBe(300);
      tick(800);
    }));

    it('should use Enrolamiento en progreso when enroll progress message is empty', () => {
      component.ngOnInit();
      dispatch('fingerprint.enroll.progress', {
        type: 'fingerprint.enroll.progress', step: 2, totalSteps: 3, message: '',
      });
      expect(component.fpStatus()).toBe('Enrolamiento en progreso.');
    });

    it('should use Error del middleware when error event message is empty', fakeAsync(() => {
      component.ngOnInit();
      dispatch('error', { type: 'error', success: false, message: '' });
      expect(toastService.error).toHaveBeenCalledWith('Error del middleware');
      tick(800);
    }));

    it('should reset escanerAEliminar in cancelarEliminarEscaner', () => {
      component.escanerAEliminar = { id: 1, nombre: 'X', ubicacion: 'Y', online: true };
      component.cancelarEliminarEscaner();
      expect(component.escanerAEliminar).toBeNull();
    });

    it('should cap system events at 10 in agregarEventoDeSistema', () => {
      for (let i = 0; i < 15; i++) {
        component.agregarEventoDeSistema(`evt-${i}`);
      }
      expect(component.eventos.length).toBe(10);
    });

    it('should calculate correct next id when registering scanner with existing scanners', () => {
      component.escaneres.push(
        { id: 3, nombre: 'A', ubicacion: 'X', online: true },
        { id: 7, nombre: 'B', ubicacion: 'Y', online: true },
      );
      component.nuevoEscaner = { nombre: 'C', ubicacion: 'Z', online: true };
      component.registrarEscaner();
      expect(component.escaneres[2].id).toBe(8);
      expect(component.escaneres[2].nombre).toBe('C');
    });

    it('should return No for centrada when isCentered is false', () => {
      component.fpQuality.set({
        isAcceptable: false, foregroundPixelCount: 50,
        foregroundCoveragePercent: 30.0, contrastScore: 0.3,
        isCentered: false, message: 'Baja calidad',
      });
      expect(component.centrada).toBe('No');
      expect(component.calidadMensaje).toBe('Baja calidad');
    });

    it('should return — for calidadMensaje when message is empty string', () => {
      component.fpQuality.set({
        isAcceptable: true, foregroundPixelCount: 200,
        foregroundCoveragePercent: 90.0, contrastScore: 0.9,
        isCentered: true, message: '',
      });
      expect(component.calidadMensaje).toBe('—');
    });

    it('should handle ngOnDestroy when intervalId is null', () => {
      (component as any).intervalId = null;
      const disconnectSpy = spyOn(middlewareWs, 'disconnect');
      component.ngOnDestroy();
      expect(disconnectSpy).toHaveBeenCalled();
    });

    it('should update existing physical scanner via device.status.changed event', () => {
      component.ngOnInit();
      dispatch('device.status.changed', {
        type: 'device.status.changed', success: true, isConnected: true, message: 'Connected',
      });
      expect(component.escaneres.length).toBe(1);
      expect(component.escaneres[0].online).toBeTrue();
      dispatch('device.status.changed', {
        type: 'device.status.changed', success: true, isConnected: false, message: 'Disconnected',
      });
      expect(component.escaneres[0].online).toBeFalse();
      expect(component.escaneres.length).toBe(1);
    });

    it('should add system event via device.status.changed handler', () => {
      spyOn(middlewareWs, 'connect');
      component.ngOnInit();
      const lenBefore = component.eventos.length;
      dispatch('device.status.changed', {
        type: 'device.status.changed', success: true, isConnected: true, message: 'Device online',
      });
      expect(component.eventos.length).toBeGreaterThanOrEqual(lenBefore + 1);
      expect(component.eventos[0].nombre).toBe('SISTEMA');
    });
  });
});
