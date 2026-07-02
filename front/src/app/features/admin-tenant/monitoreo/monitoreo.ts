import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastService } from '../../../services/toast.service';
import { ModalService } from '../../../services/modal.service';
import { MiddlewareWebSocketService, type MiddlewareDeviceStatusChanged } from '../../../services/middleware-websocket.service';

// Interfaces
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

  readonly mwConnectionState = this.middlewareWs.connectionState.asReadonly();
  readonly mwDeviceStatus = this.middlewareWs.lastDeviceStatus.asReadonly();

  private unsubscribeMw: (() => void)[] = [];
  
  // ==========================================
  // MÉTRICAS PRINCIPALES
  // ==========================================
  metricas: Metricas = {
    presentesHoy: 1284,
    porcentajePresentes: 82,
    tardanzas: 42,
    nivelTardanza: 'ALTO',
    dispositivosActivos: 2,
    totalDispositivos: 3
  };

  // ==========================================
  // LISTA DE EVENTOS EN VIVO
  // ==========================================
  eventos: Evento[] = [
    {
      id: 1,
      nombre: 'Sara Cárdenas',
      rol: 'Administración',
      hora: '09:15:30 AM',
      idDispositivo: 'AX-9124',
      estado: 'TARDE',
      escaner: 'ESCÁNER 02',
      ubicacion: 'PUERTA NORTE',
      online: true
    },
    {
      id: 2,
      nombre: 'David Velarde',
      rol: 'Docente',
      hora: '07:28:59 AM',
      idDispositivo: 'AX-8802',
      estado: 'A TIEMPO',
      escaner: 'ESCÁNER 01',
      ubicacion: 'ENTRADA PRINCIPAL',
      online: true
    },
    {
      id: 3,
      nombre: 'Juan Peña',
      rol: 'Docente',
      hora: '07:13:01 AM',
      idDispositivo: 'AX-8802',
      estado: 'A TIEMPO',
      escaner: 'ESCÁNER 01',
      ubicacion: 'ENTRADA PRINCIPAL',
      online: true
    },
    {
      id: 4,
      nombre: 'Carlos Ramiro',
      rol: 'Personal de servicio',
      hora: '05:42:15 AM',
      idDispositivo: 'AX-8802',
      estado: 'A TIEMPO',
      escaner: 'ESCÁNER 01',
      ubicacion: 'PUERTA NORTE',
      online: true
    }
  ];

  // ==========================================
  // LISTA DE ESCÁNERES
  // ==========================================
  escaneres: Escaner[] = [
    { id: 1, nombre: 'Escáner 01', ubicacion: 'PRINCIPAL', online: true },
    { id: 2, nombre: 'Escáner 02', ubicacion: 'PUERTA NORTE', online: false },
    { id: 3, nombre: 'Escáner 01', ubicacion: 'ENTRADA PRINCIPAL', online: true }
  ];

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

  // ==========================================
  // GETTERS (cálculos dinámicos)
  // ==========================================
  
  get totalDispositivosTexto(): string {
    return `${this.metricas.dispositivosActivos}/${this.metricas.totalDispositivos}`;
  }

  get eventosATiempo(): number {
    return this.eventos.filter(e => e.estado === 'A TIEMPO').length;
  }

  get eventosTarde(): number {
    return this.eventos.filter(e => e.estado === 'TARDE').length;
  }

  get ultimaActualizacionTexto(): string {
    const ahora = new Date();
    const diffSegundos = Math.floor((ahora.getTime() - this.ultimaActualizacion.getTime()) / 1000);
    
    if (diffSegundos < 60) {
      return `hace ${diffSegundos} segundos`;
    } else if (diffSegundos < 3600) {
      const minutos = Math.floor(diffSegundos / 60);
      return `hace ${minutos} ${minutos === 1 ? 'minuto' : 'minutos'}`;
    } else {
      return this.ultimaActualizacion.toLocaleTimeString();
    }
  }

  // ==========================================
  // CICLO DE VIDA
  // ==========================================
  
  ngOnInit(): void {
    this.conectarMiddleware();

    this.intervalId = setInterval(() => {
      this.actualizarDatosTiempoReal();
    }, 30000);
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
    this.middlewareWs.disconnect();
    this.unsubscribeMw.forEach(fn => fn());
  }

  private conectarMiddleware(): void {
    this.middlewareWs.connect();

    this.unsubscribeMw.push(
      this.middlewareWs.on('device.status.changed', (msg) => {
        const data = msg as MiddlewareDeviceStatusChanged;
        this.actualizarEscaneresPorMiddleware(data);
        this.agregarEventoDeSistema(data.message);
      })
    );

    this.unsubscribeMw.push(
      this.middlewareWs.on('device.connecting', (msg) => {
        const data = msg as MiddlewareDeviceStatusChanged;
        this.actualizarEscaneresPorMiddleware(data);
      })
    );

    const interval = setInterval(() => {
      this.middlewareWs.send('device.status');
    }, 10000);
    this.unsubscribeMw.push(() => clearInterval(interval));
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
  // MÉTODOS DE ACTUALIZACIÓN
  // ==========================================
  
  /**
   * Simula actualización en tiempo real
   */
  actualizarDatosTiempoReal(): void {
    this.ultimaActualizacion = new Date();
    const nuevoEvento = this.crearEventoSimulado();
    this.agregarEventoALista(nuevoEvento);
    this.actualizarMetricasConEvento(nuevoEvento);
  }

  private crearEventoSimulado(): Evento {
    const nombres = ['María López', 'Pedro Sánchez', 'Ana García', 'Luis Fernández'];
    const roles = ['Administración', 'Docente', 'Personal de servicio', 'Invitado'];
    const escaneres = ['ESCÁNER 01', 'ESCÁNER 02', 'ESCÁNER 03'];
    const ubicaciones = ['ENTRADA PRINCIPAL', 'PUERTA NORTE', 'PUERTA SUR'];
    return {
      id: Date.now(),
      nombre: nombres[Monitoreo.secureRandomInt(nombres.length)],
      rol: roles[Monitoreo.secureRandomInt(roles.length)],
      hora: new Date().toLocaleTimeString(),
      idDispositivo: `AX-${Monitoreo.secureRandomInt(9000) + 1000}`,
      estado: Monitoreo.secureRandom() > 0.8 ? 'TARDE' : 'A TIEMPO',
      escaner: escaneres[Monitoreo.secureRandomInt(escaneres.length)],
      ubicacion: ubicaciones[Monitoreo.secureRandomInt(ubicaciones.length)],
      online: true,
    };
  }

  private agregarEventoALista(evento: Evento): void {
    this.eventos.unshift(evento);
    if (this.eventos.length > 10) {
      this.eventos.pop();
    }
  }

  private actualizarMetricasConEvento(evento: Evento): void {
    this.metricas.presentesHoy += 1;
    this.metricas.porcentajePresentes = Math.floor((this.metricas.presentesHoy / 1500) * 100);
    if (evento.estado === 'TARDE') {
      this.metricas.tardanzas += 1;
      if (this.metricas.tardanzas > 50) {
        this.metricas.nivelTardanza = 'ALTO';
      } else if (this.metricas.tardanzas > 20) {
        this.metricas.nivelTardanza = 'MEDIO';
      }
    }
  }

  // ==========================================
  // MÉTODOS PARA ESCÁNERES
  // ==========================================
  
  /**
   * Registrar nuevo escáner
   */
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
  
  /**
   * Abrir modal de confirmación para eliminar escáner
   */
  eliminarEscaner(id: number): void {
    this.escanerAEliminar = this.escaneres.find(e => e.id === id) || null;
    if (this.escanerAEliminar) {
      this.modalService.show('modalConfirmarEliminar');
    }
  }
  
  /**
   * Confirmar y eliminar escáner
   */
  confirmarEliminarEscaner(): void {
    if (this.escanerAEliminar) {
      const escaner = this.escanerAEliminar;
      this.escaneres = this.escaneres.filter(e => e.id !== escaner.id);
      
      // Actualizar métricas
      this.metricas.totalDispositivos = this.escaneres.length;
      this.actualizarContadorDispositivos();
      
      // Agregar evento de sistema
      this.agregarEventoDeSistema(`Escáner eliminado: ${escaner.nombre}`);
      
      this.mostrarToast(`Escáner "${escaner.nombre}" eliminado correctamente`);
      
      // Cerrar modal
      this.modalService.hide('modalConfirmarEliminar');
      this.escanerAEliminar = null;
    }
  }
  
  /**
   * Alternar estado online/offline de un escáner
   */
  toggleEscaner(id: number): void {
    const escaner = this.escaneres.find(e => e.id === id);
    if (escaner) {
      escaner.online = !escaner.online;
      this.actualizarContadorDispositivos();
      const estado = escaner.online ? 'EN LÍNEA' : 'OFFLINE';
      this.mostrarToast(` Escáner ${escaner.nombre} ahora está ${estado}`);
      
      // Agregar evento de sistema por cambio de estado
      this.agregarEventoDeSistema(` Escáner ${escaner.nombre} cambió a ${estado}`);
    }
  }
  
  /**
   * Limpiar formulario del escáner
   */
  limpiarFormularioEscaner(): void {
    this.nuevoEscaner = {
      nombre: '',
      ubicacion: '',
      online: true
    };
  }
  
  /**
   * Actualizar contador de dispositivos activos
   */
  private actualizarContadorDispositivos(): void {
    const activos = this.escaneres.filter(e => e.online).length;
    this.metricas.dispositivosActivos = activos;
    this.metricas.totalDispositivos = this.escaneres.length;
  }

  // ==========================================
  // MÉTODOS PARA EVENTOS
  // ==========================================
  
  /**
   * Agregar evento de sistema a la lista de eventos
   */
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
  
  /**
   * Eliminar evento
   */
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
  
  /**
   * Refrescar todos los datos manualmente
   */
  refrescarDatos(): void {
    this.middlewareWs.send('device.status');
    this.actualizarDatosTiempoReal();
    this.mostrarToast('Datos actualizados correctamente');
  }

  private static secureRandomInt(max: number): number {
    const array = new Uint32Array(1);
    crypto.getRandomValues(array);
    return array[0] % max;
  }

  private static secureRandom(): number {
    const array = new Uint32Array(1);
    crypto.getRandomValues(array);
    return array[0] / 0xFFFFFFFF;
  }

  // ==========================================
  // MÉTODOS UTILITARIOS
  // ==========================================
  
  /**
   * Cerrar modal programáticamente
   */
  private mostrarToast(mensaje: string): void {
    this.toastService.info(mensaje);
  }
}