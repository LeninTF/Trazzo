import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastService } from '../../../services/toast.service';
import { ModalService } from '../../../services/modal.service';
import {
  MiddlewareWebSocketService,
  type MiddlewareDeviceStatusChanged,
  type MiddlewareFingerprintCaptureResult,
  type MiddlewareFingerprintIdentifyResult,
  type MiddlewareFingerprintEnrollResult,
  type MiddlewareFingerprintEnrollProgress,
  type MiddlewareFingerprintQuality,
} from '../../../services/middleware-websocket.service';
import { FingerprintStoreService } from '../../../services/fingerprint-store.service';

interface Evento {
  id: number;
  nombre: string;
  rol: string;
  hora: string;
  idDispositivo: string;
  estado: 'A TIEMPO' | 'TARDE';
  escaner: string;
  ubicacion: string;
  online: boolean;
}

interface Escaner {
  id: number;
  nombre: string;
  ubicacion: string;
  online: boolean;
  fisico?: boolean;
}

interface Metricas {
  presentesHoy: number;
  porcentajePresentes: number;
  tardanzas: number;
  nivelTardanza: 'ALTO' | 'MEDIO' | 'BAJO';
  dispositivosActivos: number;
  totalDispositivos: number;
}

interface LogEntry {
  timestamp: string;
  message: string;
  type: 'info' | 'success' | 'error' | 'warn';
}

@Component({
  selector: 'app-monitoreo',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './monitoreo.html',
  styleUrl: './monitoreo.css',
})
export class Monitoreo implements OnInit, OnDestroy {

  readonly loading = signal(false);
  readonly error = signal('');

  private readonly toastService = inject(ToastService);
  private readonly modalService = inject(ModalService);
  private readonly middlewareWs = inject(MiddlewareWebSocketService);
  readonly fingerprintStore = inject(FingerprintStoreService);

  readonly mwConnectionState = this.middlewareWs.connectionState.asReadonly();
  readonly mwDeviceStatus = this.middlewareWs.lastDeviceStatus.asReadonly();

  private unsubscribeMw: (() => void)[] = [];

  // ==========================================
  // MÉTRICAS PRINCIPALES
  // ==========================================
  metricas: Metricas = {
    presentesHoy: 0,
    porcentajePresentes: 0,
    tardanzas: 0,
    nivelTardanza: 'BAJO',
    dispositivosActivos: 0,
    totalDispositivos: 0
  };

  // ==========================================
  // LISTA DE EVENTOS EN VIVO
  // ==========================================
  eventos: Evento[] = [];

  // ==========================================
  // LISTA DE ESCÁNERES
  // ==========================================
  escaneres: Escaner[] = [];

  // ==========================================
  // BIOMETRIC STATE
  // ==========================================
  readonly operationLabel = signal('Inactivo');
  readonly operationInProgress = signal(false);
  readonly enrollmentInProgress = signal(false);
  readonly enrollmentProgress = signal('');
  readonly readerConnected = signal(false);
  readonly readerDetail = signal('Sin consultar');

  readonly fpImageUrl = signal<string | null>(null);
  readonly fpStatus = signal('Sin captura todavía');
  readonly fpQuality = signal<MiddlewareFingerprintQuality | null>(null);
  readonly fpTemplateSize = signal(0);
  readonly fpTemplatePreview = signal('');

  readonly logs = signal<LogEntry[]>([]);

  // ==========================================
  // DATOS PARA EL MODAL (Nuevo Escáner)
  // ==========================================
  nuevoEscaner = {
    nombre: '',
    ubicacion: '',
    online: true
  };

  // ==========================================
  // DATOS PARA ELIMINAR ESCÁNER
  // ==========================================
  escanerAEliminar: Escaner | null = null;

  // ==========================================
  // TIMER PARA ACTUALIZACIÓN EN TIEMPO REAL
  // ==========================================
  private intervalId: ReturnType<typeof setInterval> | null = null;
  ultimaActualizacion: Date = new Date();
  readonly ultimaActualizacionTexto = signal('');

  // ==========================================
  // GETTERS (cálculos dinámicos)

  get totalDispositivosTexto(): string {
    return `${this.metricas.dispositivosActivos}/${this.metricas.totalDispositivos}`;
  }

  get eventosATiempo(): number {
    return this.eventos.filter(e => e.estado === 'A TIEMPO').length;
  }

  get eventosTarde(): number {
    return this.eventos.filter(e => e.estado === 'TARDE').length;
  }

  get calidadEstimada(): string {
    const q = this.fpQuality();
    if (!q) return '—';
    return `${q.foregroundCoveragePercent.toFixed(1)}%`;
  }

  get areaCruda(): string {
    const q = this.fpQuality();
    if (!q) return '—';
    return `${q.foregroundCoveragePercent.toFixed(1)}%`;
  }

  get contraste(): string {
    const q = this.fpQuality();
    if (!q) return '—';
    return q.contrastScore.toFixed(1);
  }

  get centrada(): string {
    const q = this.fpQuality();
    if (!q) return '—';
    return q.isCentered ? 'Sí' : 'No';
  }

  get calidadMensaje(): string {
    const q = this.fpQuality();
    if (!q) return '—';
    return q.message || '—';
  }

  // ==========================================
  // CICLO DE VIDA
  // ==========================================

  ngOnInit(): void {
    this.conectarMiddleware();
    this.actualizarTextoUltimaActualizacion();

    this.intervalId = setInterval(() => {
      this.actualizarTextoUltimaActualizacion();
    }, 30000);

    // update the "time ago" text every second
    const textTimer = setInterval(() => this.actualizarTextoUltimaActualizacion(), 1000);
    this.unsubscribeMw.push(() => clearInterval(textTimer));
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
    this.middlewareWs.disconnect();
    this.unsubscribeMw.forEach(fn => fn());
  }

  private actualizarTextoUltimaActualizacion(): void {
    const ahora = new Date();
    const diffSegundos = Math.floor((ahora.getTime() - this.ultimaActualizacion.getTime()) / 1000);

    if (diffSegundos < 60) {
      this.ultimaActualizacionTexto.set(`hace ${diffSegundos} segundos`);
    } else if (diffSegundos < 3600) {
      const minutos = Math.floor(diffSegundos / 60);
      this.ultimaActualizacionTexto.set(`hace ${minutos} ${minutos === 1 ? 'minuto' : 'minutos'}`);
    } else {
      this.ultimaActualizacionTexto.set(this.ultimaActualizacion.toLocaleTimeString());
    }
  }

  private conectarMiddleware(): void {
    this.middlewareWs.connect();

    this.unsubscribeMw.push(
      this.middlewareWs.on('device.status.changed', (msg) => {
        const data = msg as MiddlewareDeviceStatusChanged;
        this.actualizarEscaneresPorMiddleware(data);
        this.agregarEventoDeSistema(data.message);
        this.readerConnected.set(data.isConnected);
        this.readerDetail.set(data.isConnected ? 'Conectado' : 'Desconectado');
      })
    );

    this.unsubscribeMw.push(
      this.middlewareWs.on('device.connecting', (msg) => {
        const data = msg as MiddlewareDeviceStatusChanged;
        this.actualizarEscaneresPorMiddleware(data);
        this.readerConnected.set(false);
        this.readerDetail.set('Conectando...');
      })
    );

    this.unsubscribeMw.push(
      this.middlewareWs.on('device.status.result', (msg) => {
        const data = msg as any;
        const ok = Boolean(data.isConnected);
        this.readerConnected.set(ok);
        this.readerDetail.set(ok
          ? `Conectado · ${data.deviceCount ?? 0} lector(es)`
          : `Desconectado · ${data.deviceCount ?? 0} lector(es)`);
        if (!ok) {
          this.fpStatus.set('Conecte el ZK9500 por USB y vuelva a consultar el estado.');
          this.fpImageUrl.set(null);
        }
      })
    );

    this.unsubscribeMw.push(
      this.middlewareWs.on('fingerprint.capture.result', (msg) => {
        this.handleFingerprintResult(msg as MiddlewareFingerprintCaptureResult);
      })
    );

    this.unsubscribeMw.push(
      this.middlewareWs.on('fingerprint.identify.result', (msg) => {
        this.handleFingerprintResult(msg as MiddlewareFingerprintIdentifyResult);
      })
    );

    this.unsubscribeMw.push(
      this.middlewareWs.on('fingerprint.enroll.result', (msg) => {
        const data = msg as MiddlewareFingerprintEnrollResult;
        this.fpQuality.set(null);
        this.fpTemplateSize.set(data.registeredTemplateSize);
        this.fpTemplatePreview.set(data.registeredTemplateBase64
          ? `${data.registeredTemplateBase64.slice(0, 18)}…` : '(cifrado)');
        this.enrollmentProgress.set(data.success ? `${data.capturedSamples ?? 0}/3` : '—');
        this.finishOp(data.message || (data.success ? 'Enrolamiento completado.' : 'Enrolamiento fallido.'));
      })
    );

    this.unsubscribeMw.push(
      this.middlewareWs.on('fingerprint.enroll.progress', (msg) => {
        const data = msg as MiddlewareFingerprintEnrollProgress;
        this.enrollmentProgress.set(`${data.step}/${data.totalSteps}`);
        this.fpStatus.set(data.message || 'Enrolamiento en progreso.');
        this.operationLabel.set('Enrolando huella…');
        this.operationInProgress.set(true);
        this.enrollmentInProgress.set(true);
      })
    );

    this.unsubscribeMw.push(
      this.middlewareWs.on('error', (msg) => {
        const err = msg as { type: string; success: boolean; message: string };
        this.toastService.error(err.message || 'Error del middleware');
        this.addLog(`Error: ${err.message}`, 'error');
        this.finishOp(err.message || 'Error');
      })
    );

    this.unsubscribeMw.push(
      this.middlewareWs.on('health.check.result', () => {
        // silently handled
      })
    );

    const interval = setInterval(() => {
      this.middlewareWs.send('device.status');
    }, 10000);
    this.unsubscribeMw.push(() => clearInterval(interval));
  }

  private handleFingerprintResult(data: MiddlewareFingerprintCaptureResult | MiddlewareFingerprintIdentifyResult): void {
    this.fpQuality.set(data.quality);
    const size = 'templateSize' in data ? data.templateSize : 0;
    this.fpTemplateSize.set(size);
    this.fpTemplatePreview.set(size > 0
      ? (data.templateBase64 ? `${data.templateBase64.slice(0, 18)}…` : '(cifrado)')
      : '—');

    if (!data.success) {
      this.fpImageUrl.set(null);
      this.toastService.error(`Huella no verificada: ${data.message}`);
      this.addLog(`Fallo: ${data.message}`, 'error');
      this.finishOp(data.message || 'Operación fallida.');
      return;
    }

    if (data.fingerprintImageDataUrl) {
      this.fpImageUrl.set(data.fingerprintImageDataUrl);
    } else {
      this.fpImageUrl.set(null);
    }

    if (data.type === 'fingerprint.identify.result') {
      const size = 'templateSize' in data ? data.templateSize : 0;
      const { match, reason } = this.fingerprintStore.findByTemplate(size);
      this.addLog(`Tamaño capturado: ${size} bytes — ${reason}`, 'info');
      if (match) {
        this.toastService.success(`Asistencia marcada: ${match.userName}`);
        this.addLog(`Coincidencia: ${match.userName} (${match.userDisplayId})`, 'success');
        this.agregarEventoAsistencia(match.userName, match.userDisplayId);
      } else {
        this.toastService.info('Huella capturada pero no coincide con ningún usuario enrolado');
        this.addLog('No se encontró coincidencia', 'warn');
      }
    } else {
      this.toastService.success('Huella verificada correctamente');
      this.addLog('Huella capturada correctamente', 'success');
    }

    this.finishOp(data.message || 'Operación completada.');
  }

  private agregarEventoAsistencia(nombre: string, idPersonal: string): void {
    const ahora = new Date();
    const evento: Evento = {
      id: ahora.getTime(),
      nombre,
      rol: 'Personal',
      hora: ahora.toLocaleTimeString(),
      idDispositivo: idPersonal,
      estado: ahora.getHours() < 9 ? 'A TIEMPO' : 'TARDE',
      escaner: 'Lector Biométrico',
      ubicacion: 'MIDDLEWARE',
      online: true,
    };
    this.eventos.unshift(evento);
    if (this.eventos.length > 10) {
      this.eventos.pop();
    }
    this.metricas.presentesHoy += 1;
    this.metricas.porcentajePresentes = Math.floor((this.metricas.presentesHoy / 1500) * 100);
    if (evento.estado === 'TARDE') {
      this.metricas.tardanzas += 1;
    }
  }

  private finishOp(message: string): void {
    this.fpStatus.set(message);
    setTimeout(() => {
      this.operationLabel.set('Inactivo');
      this.operationInProgress.set(false);
      this.enrollmentInProgress.set(false);
    }, 800);
  }

  private actualizarEscaneresPorMiddleware(data: MiddlewareDeviceStatusChanged): void {
    const idxFisico = this.escaneres.findIndex(e => e.id === 0);
    if (idxFisico >= 0) {
      this.escaneres[idxFisico].online = data.isConnected;
      this.escaneres[idxFisico].nombre = 'Lector Biométrico';
    } else {
      this.escaneres.unshift({
        id: 0,
        nombre: 'Lector Biométrico',
        ubicacion: 'MIDDLEWARE',
        online: data.isConnected,
        fisico: true,
      });
    }
    this.actualizarContadorDispositivos();
  }

  // ==========================================
  // WEB SOCKET ACTIONS
  // ==========================================

  conectarWs(): void {
    this.addLog('Conectando al middleware...', 'info');
    this.middlewareWs.connect();
  }

  desconectarWs(): void {
    this.addLog('Desconectando del middleware...', 'info');
    this.middlewareWs.disconnect();
    this.readerConnected.set(false);
    this.readerDetail.set('Sin consultar');
    this.operationLabel.set('Inactivo');
    this.operationInProgress.set(false);
    this.enrollmentInProgress.set(false);
    this.fpImageUrl.set(null);
    this.fpStatus.set('Sin captura todavía');
    this.fpQuality.set(null);
    this.fpTemplateSize.set(0);
    this.fpTemplatePreview.set('');
    this.enrollmentProgress.set('');
  }

  consultarEstadoLector(): void {
    this.addLog('Consultando estado del lector...', 'info');
    this.middlewareWs.send('device.status');
  }

  capturarHuella(): void {
    if (this.operationInProgress()) return;
    this.operationLabel.set('Esperando huella…');
    this.operationInProgress.set(true);
    this.fpStatus.set('Coloca el dedo en el lector…');
    this.addLog('Solicitando captura de huella...', 'info');
    this.middlewareWs.send('fingerprint.capture');
  }

  identificarHuella(): void {
    if (this.operationInProgress()) return;

    const enrolled = this.fingerprintStore.getAll()();
    if (enrolled.length === 0) {
      this.toastService.info('No hay usuarios enrolados. Enrole al menos una huella primero.');
      this.addLog('No hay templates enrolados para comparar', 'warn');
      return;
    }

    const refs = enrolled.map(e => `${e.userName}:${e.referenceTemplateSize}B`).join(', ');
    this.operationLabel.set('Identificando…');
    this.operationInProgress.set(true);
    this.fpStatus.set('Coloca el dedo en el lector…');
    this.addLog(`Templates de referencia disponibles: ${refs}`, 'info');
    this.middlewareWs.send('fingerprint.identify');
  }

  enrolarHuella(): void {
    if (this.operationInProgress()) return;
    this.enrollmentProgress.set('0/3');
    this.operationLabel.set('Enrolando huella…');
    this.operationInProgress.set(true);
    this.enrollmentInProgress.set(true);
    this.fpStatus.set('Iniciando enrolamiento…');
    this.addLog('Iniciando enrolamiento (3 capturas)...', 'info');
    this.middlewareWs.send('fingerprint.enroll.start');
  }

  cancelarEnrolamiento(): void {
    if (!this.enrollmentInProgress()) return;
    this.addLog('Cancelando enrolamiento...', 'warn');
    this.middlewareWs.send('fingerprint.enroll.cancel');
  }

  // ==========================================
  // MÉTODOS PARA ESCÁNERES
  // ==========================================

  abrirModalRegistrarEscaner(): void {
    this.limpiarFormularioEscaner();
    this.modalService.show('modalRegistrarEscaner');
  }

  cerrarModalRegistrarEscaner(): void {
    this.modalService.hide('modalRegistrarEscaner');
  }

  registrarEscaner(): void {
    if (!this.validarFormularioEscaner()) return;
    const nuevoEscanerObj = this.crearObjetoEscaner();
    this.escaneres.push(nuevoEscanerObj);
    this.actualizarMetricasEscaner();
    this.agregarEventoDeSistema(`Nuevo escáner registrado: ${nuevoEscanerObj.nombre} - ${nuevoEscanerObj.ubicacion}`);
    this.mostrarToast(`Escáner "${this.nuevoEscaner.nombre}" registrado correctamente`);
    this.limpiarFormularioEscaner();
    this.modalService.hide('modalRegistrarEscaner');
  }

  private validarFormularioEscaner(): boolean {
    if (!this.nuevoEscaner.nombre || !this.nuevoEscaner.ubicacion) {
      this.mostrarToast('Complete los campos obligatorios: Nombre y Ubicación');
      return false;
    }
    return true;
  }

  private crearObjetoEscaner(): Escaner {
    const nuevoId = Math.max(...this.escaneres.map(e => e.id), 0) + 1;
    return {
      id: nuevoId,
      nombre: this.nuevoEscaner.nombre,
      ubicacion: this.nuevoEscaner.ubicacion,
      online: this.nuevoEscaner.online,
    };
  }

  private actualizarMetricasEscaner(): void {
    this.metricas.totalDispositivos = this.escaneres.length;
    this.actualizarContadorDispositivos();
  }

  eliminarEscaner(id: number): void {
    this.escanerAEliminar = this.escaneres.find(e => e.id === id) || null;
    if (this.escanerAEliminar) {
      this.modalService.show('modalConfirmarEliminar');
    }
  }

  confirmarEliminarEscaner(): void {
    if (this.escanerAEliminar) {
      const escaner = this.escanerAEliminar;
      this.escaneres = this.escaneres.filter(e => e.id !== escaner.id);
      this.metricas.totalDispositivos = this.escaneres.length;
      this.actualizarContadorDispositivos();
      this.agregarEventoDeSistema(`Escáner eliminado: ${escaner.nombre}`);
      this.mostrarToast(`Escáner "${escaner.nombre}" eliminado correctamente`);
      this.modalService.hide('modalConfirmarEliminar');
      this.escanerAEliminar = null;
    }
  }

  cancelarEliminarEscaner(): void {
    this.escanerAEliminar = null;
    this.modalService.hide('modalConfirmarEliminar');
  }

  toggleEscaner(id: number): void {
    const escaner = this.escaneres.find(e => e.id === id);
    if (escaner) {
      escaner.online = !escaner.online;
      this.actualizarContadorDispositivos();
      const estado = escaner.online ? 'EN LÍNEA' : 'OFFLINE';
      this.mostrarToast(`Escáner ${escaner.nombre} ahora está ${estado}`);
      this.agregarEventoDeSistema(`Escáner ${escaner.nombre} cambió a ${estado}`);
    }
  }

  limpiarFormularioEscaner(): void {
    this.nuevoEscaner = {
      nombre: '',
      ubicacion: '',
      online: true
    };
  }

  private actualizarContadorDispositivos(): void {
    const activos = this.escaneres.filter(e => e.online).length;
    this.metricas.dispositivosActivos = activos;
    this.metricas.totalDispositivos = this.escaneres.length;
  }

  // ==========================================
  // MÉTODOS PARA EVENTOS
  // ==========================================

  agregarEventoDeSistema(mensaje: string): void {
    const nuevoEvento: Evento = {
      id: Date.now(),
      nombre: 'SISTEMA',
      rol: 'Notificación',
      hora: new Date().toLocaleTimeString(),
      idDispositivo: 'SYS-0000',
      estado: 'A TIEMPO',
      escaner: 'SISTEMA',
      ubicacion: 'CENTRAL',
      online: true
    };
    this.eventos.unshift(nuevoEvento);

    if (this.eventos.length > 10) {
      this.eventos.pop();
    }
  }

  eliminarEvento(id: number): void {
    const evento = this.eventos.find(e => e.id === id);
    if (evento) {
      this.eventos = this.eventos.filter(e => e.id !== id);
      if (evento.estado === 'TARDE') {
        this.metricas.tardanzas -= 1;
      }
      this.metricas.presentesHoy -= 1;
      this.metricas.porcentajePresentes = Math.floor((this.metricas.presentesHoy / 1500) * 100);
    }
  }

  // ==========================================
  // MÉTODOS DE ACTUALIZACIÓN MANUAL
  // ==========================================

  refrescarDatos(): void {
    this.middlewareWs.send('device.status');
    this.ultimaActualizacion = new Date();
    this.mostrarToast('Datos actualizados correctamente');
  }

  // ==========================================
  // LOG
  // ==========================================

  private addLog(message: string, type: LogEntry['type'] = 'info'): void {
    const now = new Date();
    const ts = now.toLocaleTimeString('es-PE', { hour12: false });
    this.logs.update(list => [...list.slice(-99), { timestamp: ts, message, type }]);
  }

  limpiarLogs(): void {
    this.logs.set([]);
  }

  // ==========================================
  // UTILITARIOS
  // ==========================================

  private mostrarToast(mensaje: string): void {
    this.toastService.info(mensaje);
  }
}
