import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { ToastService } from '../../../services/toast.service';
import { ExportService } from '../../../services/export.service';
import { AuditService } from '../../../api/services/audit.service';

interface LogEvento {
  id: string;
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
  oldValue: Record<string, unknown> | null;
  newValue: Record<string, unknown> | null;
}

interface Metricas {
  totalEventos: number;
  errores: number;
  sesionesActivas: number;
  crecimiento: number;
  porcentajeSesiones: number;
}

const TIPO_MAP: Record<string, 'exito' | 'advertencia' | 'error'> = {
  exito: 'exito', success: 'exito',
  advertencia: 'advertencia', warning: 'advertencia',
  error: 'error',
};

const COLORS = ['#163A96', '#10B981', '#F59E0B', '#6366F1', '#EF4444', '#8B5CF6'];

function colorForUser(email: string): string {
  let hash = 0;
  for (const ch of email) hash = (ch.codePointAt(0) ?? 0) + ((hash << 5) - hash);
  return COLORS[Math.abs(hash) % COLORS.length];
}

function initialsFor(name: string): string {
  return name.split(' ').slice(0, 2).map(p => p[0] ?? '').join('').toUpperCase();
}

@Component({
  selector: 'app-log-auditoria',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './log-auditoria.html',
  styleUrl: './log-auditoria.css',
})
export class LogAuditoria implements OnInit {
  readonly loading = signal(false);
  readonly error = signal('');

  private readonly toastService = inject(ToastService);
  private readonly exportService = inject(ExportService);
  private readonly auditService = inject(AuditService);

  logs: LogEvento[] = [];

  searchTerm = '';
  filtroFecha = '';
  paginaActual = 1;
  itemsPerPage = 5;
  totalElementos = 0;
  totalPaginasServidor = 1;

  logSeleccionado: LogEvento | null = null;

  metricas: Metricas = {
    totalEventos: 0,
    errores: 0,
    sesionesActivas: 0,
    crecimiento: 0,
    porcentajeSesiones: 0,
  };

  ngOnInit(): void {
    this.cargarLogs();
    this.cargarMetricas();
  }

  cargarLogs(): void {
    this.loading.set(true);
    this.auditService.listLogs({
      searchTerm: this.searchTerm || undefined,
      fecha_desde: this.filtroFecha || undefined,
      fecha_hasta: this.filtroFecha || undefined,
      page: this.paginaActual - 1,
      size: this.itemsPerPage,
    }).subscribe({
      next: resp => {
        this.totalElementos = resp.totalElements;
        this.totalPaginasServidor = resp.totalPages;
        this.logs = resp.content.map(e => {
          const fecha = new Date(e.fecha);
          return {
            id: e.id,
            fecha,
            hora: fecha.toLocaleTimeString('es-PE', { hour12: false }),
            tenant: e.tenant,
            tenantId: e.tenantId,
            userInitials: initialsFor(e.userName),
            userName: e.userName,
            userEmail: e.userEmail,
            userColor: colorForUser(e.userEmail),
            accion: e.accion,
            tipo: TIPO_MAP[e.tipo?.toLowerCase()] ?? 'exito',
            entidad: e.entidad,
            entidadId: e.entidadId,
            eventId: e.eventId,
            ipAddress: e.ipAddress,
            userAgent: e.userAgent,
            oldValue: e.oldValue,
            newValue: e.newValue,
          };
        });
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar los logs');
        this.loading.set(false);
      },
    });
  }

  cargarMetricas(): void {
    this.auditService.getMetrics().subscribe({
      next: m => {
        this.metricas = {
          totalEventos: m.total_eventos,
          errores: m.errores,
          sesionesActivas: m.sesiones_activas,
          crecimiento: m.crecimiento,
          porcentajeSesiones: m.porcentaje_sesiones,
        };
      },
      error: () => {},
    });
  }

  get logsFiltrado(): LogEvento[] {
    return this.logs;
  }

  get logsPaginado(): LogEvento[] {
    return this.logs;
  }

  get totalPaginas(): number {
    return this.totalPaginasServidor;
  }

  get inicioRegistro(): number {
    return this.totalElementos === 0 ? 0 : (this.paginaActual - 1) * this.itemsPerPage + 1;
  }

  get finRegistro(): number {
    return Math.min(this.paginaActual * this.itemsPerPage, this.totalElementos);
  }

  filtrarLogs(): void {
    this.paginaActual = 1;
    this.logSeleccionado = null;
    this.cargarLogs();
  }

  aplicarFiltros(): void {
    this.filtrarLogs();
    this.mostrarToast('Filtros aplicados');
  }

  limpiarFiltros(): void {
    this.searchTerm = '';
    this.filtroFecha = '';
    this.filtrarLogs();
    this.mostrarToast('Filtros limpiados');
  }

  cambiarPagina(pagina: number): void {
    if (pagina >= 1 && pagina <= this.totalPaginas) {
      this.paginaActual = pagina;
      this.logSeleccionado = null;
      this.cargarLogs();
    }
  }

  seleccionarLog(log: LogEvento): void {
    this.logSeleccionado = this.logSeleccionado?.id === log.id ? null : log;
  }

  cerrarDetalle(): void {
    this.logSeleccionado = null;
  }

  exportarCSV(): void {
    const headers = ['FECHA/HORA', 'TENANT', 'TENANT ID', 'USUARIO', 'ACCION', 'TIPO', 'ENTIDAD', 'ENTIDAD ID', 'IP ADDRESS', 'USER AGENT'];
    const rows = this.logs.map(log => [
      `${log.fecha.toLocaleDateString()} ${log.hora}`,
      log.tenant, log.tenantId,
      `${log.userName} (${log.userEmail})`,
      log.accion, log.tipo, log.entidad, log.entidadId, log.ipAddress, log.userAgent,
    ]);
    this.exportService.exportCSV(`log-auditoria-${new Date().toISOString().split('T')[0]}.csv`, headers, rows);
    this.mostrarToast('Exportando CSV...');
  }

  private mostrarToast(mensaje: string): void {
    this.toastService.info(mensaje);
  }
}
