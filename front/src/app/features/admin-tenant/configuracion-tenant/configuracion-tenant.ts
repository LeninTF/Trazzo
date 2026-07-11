import { Component, signal, WritableSignal, effect, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastService } from '../../../services/toast.service';

interface Modulo {
  id: string;
  nombre: string;
  activo: boolean;
  descripcion: string;
}

interface TenantInfo {
  nombre: string;
  email: string;
  logoUrl: string | null;
}

interface Limites {
  maxUsuarios: number;
  maxSedes: number;
  cicloFacturacion: string;
}

interface Branding {
  colorPrimario: string;
  colorSecundario: string;
  colorAcento: string;
}

interface LogEntry {
  id: number;
  accion: string;
  fecha: Date;
  usuario: string;
  tipo: 'exito' | 'error' | 'info';
}

@Component({
  selector: 'app-configuracion-tenant',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './configuracion-tenant.html',
  styleUrl: './configuracion-tenant.css',
})
export class ConfiguracionTenant {

  readonly loading = signal(false);
  readonly error = signal('');

  private readonly toastService = inject(ToastService);
  
  // ==========================================
  // SIGNALS PARA DATOS REACTIVOS
  // ==========================================
  
  tenantInfo: WritableSignal<TenantInfo> = signal<TenantInfo>({
    nombre: 'Universidad Nacional',
    email: 'admin@unacional.edu',
    logoUrl: null
  });
  
  limites: WritableSignal<Limites> = signal<Limites>({
    maxUsuarios: 1000,
    maxSedes: 5,
    cicloFacturacion: '2023-10-24'
  });
  
  modulos: WritableSignal<Modulo[]> = signal<Modulo[]>([
    { id: 'geolocalizacion', nombre: 'Geolocalización', activo: true, descripcion: 'Seguimiento de ubicación en tiempo real' },
    { id: 'biometria', nombre: 'Biometría', activo: false, descripcion: 'Validación por huella dactilar' },
    { id: 'api', nombre: 'API Externa', activo: true, descripcion: 'Integración con sistemas externos' },
    { id: 'analitica', nombre: 'Analítica Predictiva', activo: false, descripcion: 'Reportes y predicciones avanzadas' },
    { id: 'notificaciones', nombre: 'Notificaciones Push', activo: true, descripcion: 'Alertas y recordatorios' },
    { id: 'reportes', nombre: 'Reportes Avanzados', activo: true, descripcion: 'Generación de informes personalizados' }
  ]);
  
  branding: WritableSignal<Branding> = signal<Branding>({
    colorPrimario: '#163A96',
    colorSecundario: '#60283F',
    colorAcento: '#10B981'
  });
  
  logs: WritableSignal<LogEntry[]> = signal<LogEntry[]>([
    { id: 1, accion: 'Configuración inicial del tenant', fecha: new Date('2023-10-20T10:00:00'), usuario: 'admin@unacional.edu', tipo: 'exito' },
    { id: 2, accion: 'Actualización de límites', fecha: new Date('2023-10-15T14:30:00'), usuario: 'admin@unacional.edu', tipo: 'info' },
    { id: 3, accion: 'Activación de módulo Geolocalización', fecha: new Date('2023-10-10T09:15:00'), usuario: 'admin@unacional.edu', tipo: 'exito' }
  ]);
  
  // ==========================================
  // ESTADO DE MODALES
  // ==========================================
  modalConfirmacionOpen: boolean = false;
  modalSuspenderOpen: boolean = false;
  modalLogsOpen: boolean = false;
  
  // Estado temporal
  motivoSuspension: string = '';
  cambiosPendientes: boolean = false;
  
  // ==========================================
  // CONSTRUCTOR Y EFECTOS
  // ==========================================
  
  constructor() {
    this.cargarDatosGuardados();
    
    effect(() => {
      if (this.cambiosPendientes) {
        this.guardarEnLocalStorage();
      }
    });
  }
  
  // ==========================================
  // MÉTODOS PRINCIPALES
  // ==========================================
  
  marcarCambiosPendientes(): void {
    this.cambiosPendientes = true;
  }
  
  guardarCambios(): void {
    this.modalConfirmacionOpen = true;
  }
  
  cerrarModalConfirmacion(): void {
    this.modalConfirmacionOpen = false;
  }
  
  confirmarGuardar(): void {
    this.guardarEnLocalStorage();
    this.cambiosPendientes = false;
    this.agregarLog('Configuración del tenant actualizada', 'exito');
    this.cerrarModalConfirmacion();
    this.mostrarToast(' Cambios guardados exitosamente');
  }
  
  private guardarEnLocalStorage(): void {
    const datos = {
      tenantInfo: this.tenantInfo(),
      limites: this.limites(),
      modulos: this.modulos(),
      branding: this.branding()
    };
    localStorage.setItem('configuracion_tenant', JSON.stringify(datos));
  }
  
  private cargarDatosGuardados(): void {
    const guardado = localStorage.getItem('configuracion_tenant');
    if (guardado) {
      try {
        const datos = JSON.parse(guardado);
        this.tenantInfo.set(datos.tenantInfo);
        this.limites.set(datos.limites);
        this.modulos.set(datos.modulos);
        this.branding.set(datos.branding);
        this.aplicarColoresGlobales();
      } catch {
      }
    }
  }
  
  // ==========================================
  // MÉTODOS DE LOGS
  // ==========================================
  
  verLogs(): void {
    this.modalLogsOpen = true;
  }
  
  cerrarModalLogs(): void {
    this.modalLogsOpen = false;
  }
  
  private agregarLog(accion: string, tipo: 'exito' | 'error' | 'info'): void {
    const nuevoLog: LogEntry = {
      id: Date.now(),
      accion,
      fecha: new Date(),
      usuario: this.tenantInfo().email,
      tipo
    };
    this.logs.update(logs => [nuevoLog, ...logs].slice(0, 50));
  }
  
  // ==========================================
  // MÉTODOS DE MÓDULOS
  // ==========================================
  
  toggleModulo(modulo: Modulo): void {
    this.cambiosPendientes = true;
    const estado = modulo.activo ? 'Activado' : 'Desactivado';
    this.agregarLog(`${estado} módulo: ${modulo.nombre}`, 'info');
    this.mostrarToast(` ${estado} módulo ${modulo.nombre}`);
  }
  
  // ==========================================
  // MÉTODOS DE TENANT
  // ==========================================
  
  suspenderTenant(): void {
    this.modalSuspenderOpen = true;
  }
  
  cerrarModalSuspender(): void {
    this.modalSuspenderOpen = false;
    this.motivoSuspension = '';
  }
  
  confirmarSuspender(): void {
    const motivo = this.motivoSuspension || 'No especificado';
    this.agregarLog(`Tenant suspendido - Motivo: ${motivo}`, 'error');
    this.cerrarModalSuspender();
    this.mostrarToast(' Tenant suspendido correctamente');
  }
  
  // ==========================================
  // MÉTODOS DE LOGO
  // ==========================================
  
  abrirSelectorLogo(): void {
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    input?.click();
  }
  
  cambiarLogo(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      
      if (file.size > 2 * 1024 * 1024) {
        this.mostrarToast(' El archivo no puede superar los 2MB');
        return;
      }
      
      const reader = new FileReader();
      reader.onload = (e) => {
        this.tenantInfo.update(info => ({
          ...info,
          logoUrl: e.target?.result as string
        }));
        this.cambiosPendientes = true;
        this.agregarLog('Logo institucional actualizado', 'exito');
        this.mostrarToast(' Logo actualizado correctamente');
      };
      reader.readAsDataURL(file);
    }
  }
  
  eliminarLogo(event: Event): void {
    event.stopPropagation();
    this.tenantInfo.update(info => ({
      ...info,
      logoUrl: null
    }));
    this.cambiosPendientes = true;
    this.agregarLog('Logo institucional eliminado', 'info');
    this.mostrarToast(' Logo eliminado');
  }
  
  // ==========================================
  // MÉTODOS DE BRANDING
  // ==========================================
  
  actualizarColorPrimario(color: string): void {
    this.branding.update(b => ({ ...b, colorPrimario: color }));
    this.cambiosPendientes = true;
    document.documentElement.style.setProperty('--primary', color);
    this.agregarLog('Color primario actualizado', 'info');
  }
  
  actualizarColorSecundario(color: string): void {
    this.branding.update(b => ({ ...b, colorSecundario: color }));
    this.cambiosPendientes = true;
    document.documentElement.style.setProperty('--primary-dark', color);
    this.agregarLog('Color secundario actualizado', 'info');
  }
  
  actualizarColorAcento(color: string): void {
    this.branding.update(b => ({ ...b, colorAcento: color }));
    this.cambiosPendientes = true;
    document.documentElement.style.setProperty('--success', color);
    this.agregarLog('Color de acento actualizado', 'info');
  }
  
  private aplicarColoresGlobales(): void {
    document.documentElement.style.setProperty('--primary', this.branding().colorPrimario);
    document.documentElement.style.setProperty('--primary-dark', this.branding().colorSecundario);
    document.documentElement.style.setProperty('--success', this.branding().colorAcento);
  }
  
  resetearColores(): void {
    this.branding.set({
      colorPrimario: '#163A96',
      colorSecundario: '#60283F',
      colorAcento: '#10B981'
    });
    this.aplicarColoresGlobales();
    this.cambiosPendientes = true;
    this.agregarLog('Colores restaurados a valores por defecto', 'info');
    this.mostrarToast(' Colores restaurados');
  }
  
  // ==========================================
  // MÉTODOS DE VALIDACIÓN
  // ==========================================
  
  validarLimiteMaxUsuarios(): void {
    const valor = this.limites().maxUsuarios;
    if (valor < 1) {
      this.limites.update(l => ({ ...l, maxUsuarios: 1 }));
      this.mostrarToast(' El mínimo de usuarios es 1');
    }
    if (valor > 100000) {
      this.limites.update(l => ({ ...l, maxUsuarios: 100000 }));
      this.mostrarToast(' El máximo de usuarios es 100,000');
    }
    this.cambiosPendientes = true;
  }
  
  validarLimiteSedes(): void {
    const valor = this.limites().maxSedes;
    if (valor < 1) {
      this.limites.update(l => ({ ...l, maxSedes: 1 }));
      this.mostrarToast(' El mínimo de sedes es 1');
    }
    if (valor > 50) {
      this.limites.update(l => ({ ...l, maxSedes: 50 }));
      this.mostrarToast(' El máximo de sedes es 50');
    }
    this.cambiosPendientes = true;
  }
  
  // ==========================================
  // MÉTODOS UTILITARIOS
  // ==========================================
  
  private mostrarToast(mensaje: string): void {
    this.toastService.info(mensaje);
  }
}