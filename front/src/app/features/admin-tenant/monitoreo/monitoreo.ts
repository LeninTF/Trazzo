import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

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
  private intervalId: any;
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
    // Iniciar actualización automática cada 30 segundos
    this.intervalId = setInterval(() => {
      this.actualizarDatosTiempoReal();
    }, 30000);
  }

  ngOnDestroy(): void {
    // Limpiar intervalo al destruir el componente
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  // ==========================================
  // MÉTODOS DE ACTUALIZACIÓN
  // ==========================================
  
  /**
   * Simula actualización en tiempo real
   */
  actualizarDatosTiempoReal(): void {
    console.log(' Actualizando datos en tiempo real...');
    this.ultimaActualizacion = new Date();
    
    // Simular un nuevo evento aleatorio
    const nombres = ['María López', 'Pedro Sánchez', 'Ana García', 'Luis Fernández'];
    const roles = ['Administración', 'Docente', 'Personal de servicio', 'Invitado'];
    const escaneres = ['ESCÁNER 01', 'ESCÁNER 02', 'ESCÁNER 03'];
    const ubicaciones = ['ENTRADA PRINCIPAL', 'PUERTA NORTE', 'PUERTA SUR'];
    
    const nuevoEvento: Evento = {
      id: Date.now(),
      nombre: nombres[Math.floor(Math.random() * nombres.length)],
      rol: roles[Math.floor(Math.random() * roles.length)],
      hora: new Date().toLocaleTimeString(),
      idDispositivo: `AX-${Math.floor(Math.random() * 9000 + 1000)}`,
      estado: Math.random() > 0.8 ? 'TARDE' : 'A TIEMPO',
      escaner: escaneres[Math.floor(Math.random() * escaneres.length)],
      ubicacion: ubicaciones[Math.floor(Math.random() * ubicaciones.length)],
      online: true
    };
    
    // Agregar al inicio (más reciente primero)
    this.eventos.unshift(nuevoEvento);
    
    // Mantener solo últimos 10 eventos
    if (this.eventos.length > 10) {
      this.eventos.pop();
    }
    
    // Actualizar métricas
    this.metricas.presentesHoy += 1;
    this.metricas.porcentajePresentes = Math.floor((this.metricas.presentesHoy / 1500) * 100);
    
    if (nuevoEvento.estado === 'TARDE') {
      this.metricas.tardanzas += 1;
      if (this.metricas.tardanzas > 50) {
        this.metricas.nivelTardanza = 'ALTO';
      } else if (this.metricas.tardanzas > 20) {
        this.metricas.nivelTardanza = 'MEDIO';
      }
    }
    
    console.log(`📢 Nuevo evento: ${nuevoEvento.nombre} - ${nuevoEvento.estado}`);
  }

  // ==========================================
  // MÉTODOS PARA ESCÁNERES
  // ==========================================
  
  /**
   * Registrar nuevo escáner
   */
  registrarEscaner(): void {
    if (!this.nuevoEscaner.nombre || !this.nuevoEscaner.ubicacion) {
      this.mostrarToast('⚠️ Complete los campos obligatorios: Nombre y Ubicación');
      return;
    }
    
    const nuevoId = Math.max(...this.escaneres.map(e => e.id), 0) + 1;
    
    const nuevoEscanerObj: Escaner = {
      id: nuevoId,
      nombre: this.nuevoEscaner.nombre,
      ubicacion: this.nuevoEscaner.ubicacion,
      online: this.nuevoEscaner.online
    };
    
    this.escaneres.push(nuevoEscanerObj);
    
    // Actualizar métricas
    this.metricas.totalDispositivos = this.escaneres.length;
    this.actualizarContadorDispositivos();
    
    // Agregar evento de sistema
    this.agregarEventoDeSistema(`📡 Nuevo escáner registrado: ${nuevoEscanerObj.nombre} - ${nuevoEscanerObj.ubicacion}`);
    
    this.mostrarToast(` Escáner "${this.nuevoEscaner.nombre}" registrado correctamente`);
    
    // Limpiar formulario
    this.limpiarFormularioEscaner();
    
    // Cerrar modal
    this.cerrarModal('modalRegistrarEscaner');
  }
  
  /**
   * Abrir modal de confirmación para eliminar escáner
   */
  eliminarEscaner(id: number): void {
    this.escanerAEliminar = this.escaneres.find(e => e.id === id) || null;
    if (this.escanerAEliminar) {
      const modalElement = document.getElementById('modalConfirmarEliminar');
      if (modalElement) {
        // @ts-ignore
        const modal = new bootstrap.Modal(modalElement);
        modal.show();
      }
    }
  }
  
  /**
   * Confirmar y eliminar escáner
   */
  confirmarEliminarEscaner(): void {
    if (this.escanerAEliminar) {
      const nombreEscaner = this.escanerAEliminar.nombre;
      this.escaneres = this.escaneres.filter(e => e.id !== this.escanerAEliminar!.id);
      
      // Actualizar métricas
      this.metricas.totalDispositivos = this.escaneres.length;
      this.actualizarContadorDispositivos();
      
      // Agregar evento de sistema
      this.agregarEventoDeSistema(`🗑️ Escáner eliminado: ${nombreEscaner}`);
      
      this.mostrarToast(`🗑️ Escáner "${nombreEscaner}" eliminado correctamente`);
      
      // Cerrar modal
      this.cerrarModal('modalConfirmarEliminar');
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
    if (evento && confirm(`¿Eliminar evento de ${evento.nombre}?`)) {
      this.eventos = this.eventos.filter(e => e.id !== id);
      console.log(` Evento ${id} eliminado`);
      
      // Actualizar métricas (opcional)
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
    console.log('🔄 Refrescando datos manualmente...');
    this.actualizarDatosTiempoReal();
    this.mostrarToast('🔄 Datos actualizados correctamente');
  }

  // ==========================================
  // MÉTODOS UTILITARIOS
  // ==========================================
  
  /**
   * Cerrar modal programáticamente
   */
  cerrarModal(modalId: string): void {
    const modalElement = document.getElementById(modalId);
    if (modalElement) {
      // @ts-ignore
      const modal = bootstrap.Modal.getInstance(modalElement);
      modal?.hide();
    }
  }
  
  /**
   * Mostrar toast notification
   */
  private mostrarToast(mensaje: string): void {
    // Crear elemento toast
    const toast = document.createElement('div');
    toast.className = 'toast-notification';
    toast.innerHTML = `
      <div class="toast-notification__content">
        <i class="bi bi-info-circle-fill me-2"></i>
        <span>${mensaje}</span>
      </div>
    `;
    document.body.appendChild(toast);
    
    // Animar y eliminar
    setTimeout(() => {
      toast.classList.add('toast-notification--show');
      setTimeout(() => {
        toast.classList.remove('toast-notification--show');
        setTimeout(() => toast.remove(), 300);
      }, 2000);
    }, 10);
  }
}