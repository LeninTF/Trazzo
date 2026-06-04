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
  // DATOS PARA EL MODAL (Nuevo Dispositivo)
  // ==========================================
  nuevoDispositivo = {
    serie: '',
    nombre: '',
    descripcion: '',
    autoEnrolamiento: true
  };

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
    console.log('🔄 Actualizando datos en tiempo real...');
    this.ultimaActualizacion = new Date();
    
    // Simular un nuevo evento aleatorio
    const nombres = ['María López', 'Pedro Sánchez', 'Ana García', 'Luis Fernández'];
    const roles = ['Administración', 'Docente', 'Personal de servicio', 'Invitado'];
    const escaneres = ['ESCÁNER 01', 'ESCÁNER 02'];
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
    
    // Mostrar notificación visual (opcional)
    console.log(`📢 Nuevo evento: ${nuevoEvento.nombre} - ${nuevoEvento.estado}`);
  }

  /**
   * Registrar nuevo dispositivo
   */
  registrarDispositivo(): void {
    if (!this.nuevoDispositivo.serie || !this.nuevoDispositivo.nombre) {
      alert('⚠️ Por favor complete los campos obligatorios: Número de serie y Nombre del dispositivo');
      return;
    }
    
    console.log('✅ Dispositivo registrado:', this.nuevoDispositivo);
    
    // Aquí iría la llamada a tu API
    // this.http.post('/api/dispositivos', this.nuevoDispositivo).subscribe(...)
    
    // Simular éxito
    alert(`✅ Dispositivo "${this.nuevoDispositivo.nombre}" registrado correctamente`);
    
    // Limpiar formulario
    this.limpiarFormularioDispositivo();
    
    // Cerrar modal
    this.cerrarModal();
  }
  
  /**
   * Limpiar formulario del modal
   */
  limpiarFormularioDispositivo(): void {
    this.nuevoDispositivo = {
      serie: '',
      nombre: '',
      descripcion: '',
      autoEnrolamiento: true
    };
  }
  
  /**
   * Cerrar modal programáticamente
   */
  cerrarModal(): void {
    const modalElement = document.getElementById('modalRegistrarDispositivo');
    if (modalElement) {
      // @ts-ignore
      const modal = bootstrap.Modal.getInstance(modalElement);
      modal?.hide();
    }
  }
  
  /**
   * Eliminar evento
   */
  eliminarEvento(id: number): void {
    const evento = this.eventos.find(e => e.id === id);
    if (evento && confirm(`¿Eliminar evento de ${evento.nombre}?`)) {
      this.eventos = this.eventos.filter(e => e.id !== id);
      console.log(`🗑️ Evento ${id} eliminado`);
      
      // Actualizar métricas (opcional)
      if (evento.estado === 'TARDE') {
        this.metricas.tardanzas -= 1;
      }
      this.metricas.presentesHoy -= 1;
      this.metricas.porcentajePresentes = Math.floor((this.metricas.presentesHoy / 1500) * 100);
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
      console.log(`🔄 Escáner ${escaner.nombre} ahora está ${escaner.online ? 'EN LÍNEA' : 'OFFLINE'}`);
    }
  }
  
  /**
   * Actualizar contador de dispositivos activos
   */
  private actualizarContadorDispositivos(): void {
    const activos = this.escaneres.filter(e => e.online).length;
    this.metricas.dispositivosActivos = activos;
  }
  
  /**
   * Refrescar todos los datos manualmente
   */
  refrescarDatos(): void {
    console.log('🔄 Refrescando datos manualmente...');
    this.actualizarDatosTiempoReal();
  }
  
  /**
   * Simular cambio de estado en un evento (ejemplo adicional)
   */
  cambiarEstadoEvento(id: number): void {
    const evento = this.eventos.find(e => e.id === id);
    if (evento) {
      evento.estado = evento.estado === 'A TIEMPO' ? 'TARDE' : 'A TIEMPO';
      console.log(`📝 Evento ${evento.nombre} cambió a ${evento.estado}`);
      
      // Actualizar métricas de tardanzas
      this.metricas.tardanzas = this.eventos.filter(e => e.estado === 'TARDE').length;
      
      if (this.metricas.tardanzas > 50) {
        this.metricas.nivelTardanza = 'ALTO';
      } else if (this.metricas.tardanzas > 20) {
        this.metricas.nivelTardanza = 'MEDIO';
      } else {
        this.metricas.nivelTardanza = 'BAJO';
      }
    }
  }
}