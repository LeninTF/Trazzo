import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface LogEvento {
  id: number;
  fecha: Date;
  hora: string;
  tenant: string;
  tenantId: string;
  userInitials: string;
  userName: string;
  userEmail: string;
  userColor: string;
  accion: string;
  tipo: 'exito' | 'advertencia' | 'error';
  entidad: string;
  entidadId: string;
  eventId: string;
  ipAddress: string;
  userAgent: string;
  oldValue: any;
  newValue: any;
}

interface Metricas {
  totalEventos: number;
  errores: number;
  sesionesActivas: number;
  crecimiento: number;
  porcentajeSesiones: number;
}

@Component({
  selector: 'app-log-auditoria',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './log-auditoria.html',
  styleUrl: './log-auditoria.css',
})
export class LogAuditoria {
  
  // ==========================================
  // DATOS PRINCIPALES
  // ==========================================
  
  logs: LogEvento[] = [
    {
      id: 1,
      fecha: new Date(),
      hora: '14:23:45',
      tenant: 'I.E Andres Avelino',
      tenantId: 'tenant-001',
      userInitials: 'JD',
      userName: 'J. Doe',
      userEmail: 'j.doe@ieaa.edu',
      userColor: '#163A96',
      accion: 'Login',
      tipo: 'exito',
      entidad: 'Auth Service',
      entidadId: 'auth/login',
      eventId: 'EVT-98234-X',
      ipAddress: '192.168.1.144',
      userAgent: 'Chrome / MacOS',
      oldValue: null,
      newValue: { status: 'success', userId: 'user-001' }
    },
    {
      id: 2,
      fecha: new Date(),
      hora: '13:45:12',
      tenant: 'Trazzo',
      tenantId: 'tenant-002',
      userInitials: 'SA',
      userName: 'S. Admin',
      userEmail: 'admin@trazzo.com',
      userColor: '#10B981',
      accion: 'Update User',
      tipo: 'exito',
      entidad: 'Users/1204',
      entidadId: 'user-1204',
      eventId: 'EVT-98235-Y',
      ipAddress: '10.0.0.45',
      userAgent: 'Firefox / Windows',
      oldValue: { id: '1204', role: 'editor', status: 'active', mfa_enabled: false },
      newValue: { id: '1204', role: 'admin', status: 'active', mfa_enabled: true }
    },
    {
      id: 3,
      fecha: new Date(),
      hora: '12:10:05',
      tenant: 'Trazzo',
      tenantId: 'tenant-002',
      userInitials: 'RM',
      userName: 'R. Miller',
      userEmail: 'r.miller@trazzo.com',
      userColor: '#F59E0B',
      accion: 'Delete Tenant',
      tipo: 'error',
      entidad: 'Organization/09',
      entidadId: 'org-09',
      eventId: 'EVT-98236-Z',
      ipAddress: '192.168.1.200',
      userAgent: 'Safari / iOS',
      oldValue: { name: 'Test Org', status: 'active' },
      newValue: null
    },
    {
      id: 4,
      fecha: new Date(Date.now() - 86400000),
      hora: '23:58:10',
      tenant: 'I.E Cristo Moreno',
      tenantId: 'tenant-003',
      userInitials: 'BT',
      userName: 'B. Taylor',
      userEmail: 'b.taylor@iecm.edu',
      userColor: '#6366F1',
      accion: 'Config Change',
      tipo: 'exito',
      entidad: 'App/Settings',
      entidadId: 'settings-001',
      eventId: 'EVT-98237-A',
      ipAddress: '172.16.0.10',
      userAgent: 'Edge / Windows',
      oldValue: { theme: 'light', notifications: true },
      newValue: { theme: 'dark', notifications: true }
    },
    {
      id: 5,
      fecha: new Date(Date.now() - 86400000),
      hora: '22:15:44',
      tenant: 'Trazzo',
      tenantId: 'tenant-002',
      userInitials: 'SA',
      userName: 'S. Admin',
      userEmail: 'admin@trazzo.com',
      userColor: '#10B981',
      accion: 'Policy Update',
      tipo: 'advertencia',
      entidad: 'IAM/Policies',
      entidadId: 'policy-789',
      eventId: 'EVT-98238-B',
      ipAddress: '10.0.0.45',
      userAgent: 'Firefox / Windows',
      oldValue: { mfa_required: false, session_timeout: 30 },
      newValue: { mfa_required: true, session_timeout: 15 }
    }
  ];
  
  // ==========================================
  // ESTADO DE FILTROS Y PAGINACIÓN
  // ==========================================
  searchTerm: string = '';
  filtroFecha: string = '';
  
  paginaActual: number = 1;
  itemsPerPage: number = 5;
  
  // ==========================================
  // ESTADO DE DETALLE
  // ==========================================
  logSeleccionado: LogEvento | null = null;
  
  // ==========================================
  // MÉTRICAS
  // ==========================================
  metricas: Metricas = {
    totalEventos: 1240,
    errores: 12,
    sesionesActivas: 184,
    crecimiento: 12,
    porcentajeSesiones: 15
  };
  
  // ==========================================
  // GETTERS
  // ==========================================
  
  get logsFiltrado(): LogEvento[] {
    let resultado = this.logs;
    
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      resultado = resultado.filter(l => 
        l.accion.toLowerCase().includes(term) || 
        l.entidad.toLowerCase().includes(term) ||
        l.tenant.toLowerCase().includes(term)
      );
    }
    
    if (this.filtroFecha) {
      const fechaFiltro = new Date(this.filtroFecha);
      resultado = resultado.filter(l => 
        l.fecha.toDateString() === fechaFiltro.toDateString()
      );
    }
    
    return resultado;
  }
  
  get logsPaginado(): LogEvento[] {
    const start = (this.paginaActual - 1) * this.itemsPerPage;
    const end = start + this.itemsPerPage;
    return this.logsFiltrado.slice(start, end);
  }
  
  get totalPaginas(): number {
    return Math.ceil(this.logsFiltrado.length / this.itemsPerPage);
  }
  
  get inicioRegistro(): number {
    return this.logsFiltrado.length === 0 ? 0 : (this.paginaActual - 1) * this.itemsPerPage + 1;
  }
  
  get finRegistro(): number {
    return Math.min(this.paginaActual * this.itemsPerPage, this.logsFiltrado.length);
  }
  
  // ==========================================
  // MÉTODOS DE FILTRADO
  // ==========================================
  
  filtrarLogs(): void {
    this.paginaActual = 1;
    this.logSeleccionado = null;
  }
  
  aplicarFiltros(): void {
    this.filtrarLogs();
    this.mostrarToast(' Filtros aplicados correctamente');
  }
  
  limpiarFiltros(): void {
    this.searchTerm = '';
    this.filtroFecha = '';
    this.filtrarLogs();
    this.mostrarToast(' Filtros limpiados');
  }
  
  cambiarPagina(pagina: number): void {
    if (pagina >= 1 && pagina <= this.totalPaginas) {
      this.paginaActual = pagina;
      this.logSeleccionado = null;
    }
  }
  
  // ==========================================
  // MÉTODOS DE DETALLE
  // ==========================================
  
  seleccionarLog(log: LogEvento): void {
    if (this.logSeleccionado?.id === log.id) {
      this.logSeleccionado = null;
    } else {
      this.logSeleccionado = log;
    }
  }
  
  cerrarDetalle(): void {
    this.logSeleccionado = null;
  }
  
  // ==========================================
  // MÉTODOS DE ACCIÓN
  // ==========================================
  
  exportarCSV(): void {
    const headers = ['FECHA/HORA', 'TENANT', 'TENANT ID', 'USUARIO', 'ACCION', 'TIPO', 'ENTIDAD', 'ENTIDAD ID', 'IP ADDRESS', 'USER AGENT'];
    const csvData = this.logsFiltrado.map(log => [
      `${log.fecha.toLocaleDateString()} ${log.hora}`,
      log.tenant,
      log.tenantId,
      `${log.userName} (${log.userEmail})`,
      log.accion,
      log.tipo,
      log.entidad,
      log.entidadId,
      log.ipAddress,
      log.userAgent
    ]);
    
    const csvContent = [headers, ...csvData].map(row => row.join(',')).join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.href = url;
    link.setAttribute('download', `log-auditoria-${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    
    this.mostrarToast(' Exportando CSV...');
  }
  
  // ==========================================
  // MÉTODOS UTILITARIOS
  // ==========================================
  
  private mostrarToast(mensaje: string): void {
    const toast = document.createElement('div');
    toast.className = 'toast-notification';
    toast.innerHTML = `
      <div class="toast-notification__content">
        <i class="bi bi-info-circle-fill me-2"></i>
        <span>${mensaje}</span>
      </div>
    `;
    document.body.appendChild(toast);
    
    setTimeout(() => {
      toast.classList.add('toast-notification--show');
      setTimeout(() => {
        toast.classList.remove('toast-notification--show');
        setTimeout(() => toast.remove(), 300);
      }, 2000);
    }, 10);
  }
}