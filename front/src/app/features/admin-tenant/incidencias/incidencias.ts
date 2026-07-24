import { Component, computed, signal, inject, OnInit } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../../../api/services/api.service';
import type { IncidentProfile } from '../../../api/types';
import { ToastService } from '../../../services/toast.service';

interface IncidenciaSolicitud {
  id: number;
  colaborador: string;
  rol: string;
  tipo: string;
  periodo: string;
  detalle: string;
  estado: 'Pendiente' | 'Aprobado' | 'Rechazado';
  descripcion: string;
  fechaCreacion: string;
  archivo: {
    nombre: string;
    tipo: string;
    tamano: string;
    url: string;
  } | null;
}

type EstadoFilter = 'Todos' | 'Pendiente' | 'Aprobado' | 'Rechazado';
const STATE_MAP: Record<string, 'Pendiente' | 'Aprobado' | 'Rechazado'> = {
  PENDIENTE: 'Pendiente', APROBADO: 'Aprobado', DENEGADO: 'Rechazado',
};

function toSolicitud(inc: IncidentProfile): IncidenciaSolicitud {
  const e = inc.evidencias?.[0];
  return {
    id: inc.id,
    colaborador: `${inc.tenant_user.nombre} ${inc.tenant_user.apellido_paterno}`,
    rol: '',
    tipo: inc.tipo.nombre,
    periodo: inc.created_at?.slice(0, 10) ?? '',
    detalle: inc.comment?.slice(0, 40) ?? '',
    estado: STATE_MAP[inc.state] ?? 'Pendiente',
    descripcion: inc.comment ?? '',
    fechaCreacion: inc.created_at ? new Date(inc.created_at).toLocaleDateString('es-PE') : '',
    archivo: e ? { nombre: e.file_name ?? 'archivo', tipo: e.mime_type ?? 'application/octet-stream', tamano: e.file_size ? `${(e.file_size / 1024).toFixed(1)} KB` : '—', url: e.file_url } : null,
  };
}

@Component({
  selector: 'app-incidencias',
  standalone: true,
  imports: [],
  templateUrl: './incidencias.html',
  styleUrl: './incidencias.css',
})
export class Incidencias implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toastService = inject(ToastService);
  readonly loading = signal(false);

  modalOpen = false;
  filterOpen = false;
  selectedSolicitud: IncidenciaSolicitud | null = null;

  rejectionReason = signal('');
  showRejectionError = signal(false);
  rejecting = signal(false);
  showRejectionForm = signal(false);

  filterEstado = signal<EstadoFilter>('Todos');
  filterTipo = signal<string>('');

  tiposDisponibles: string[] = [];

  solicitudes = signal<IncidenciaSolicitud[]>([]);

  filtradas = computed(() => {
    return this.solicitudes().filter(s => {
      const porEstado = this.filterEstado() === 'Todos' || s.estado === this.filterEstado();
      const porTipo = !this.filterTipo() || s.tipo === this.filterTipo();
      return porEstado && porTipo;
    });
  });

  pendientes = computed(() => this.filtradas().filter(s => s.estado === 'Pendiente'));
  aprobadas = computed(() => this.filtradas().filter(s => s.estado === 'Aprobado'));
  rechazadas = computed(() => this.filtradas().filter(s => s.estado === 'Rechazado'));

  get metricas() {
    const p = this.pendientes().length;
    const a = this.aprobadas().length;
    const r = this.rechazadas().length;
    return [
      {
        titulo: 'Pendientes', valor: p, subtitulo: '+4 esta semana',
        icono: 'bi-clock-history', color: '#F59E0B', bg: '#FFFBEB',
      },
      {
        titulo: 'Aprobadas', valor: a, subtitulo: 'Mes de Junio',
        icono: 'bi-check-circle', color: '#10B981', bg: '#F0FDF4',
      },
      {
        titulo: 'Rechazadas', valor: r < 10 ? '0' + r : r,
        subtitulo: r === 0 ? 'Sin incidencias críticas' : 'Requieren atención',
        icono: 'bi-x-circle', color: '#EF4444', bg: '#FEF2F2',
      },
    ];
  }

  ngOnInit(): void {
    this.cargarIncidencias();
  }

  async cargarIncidencias(): Promise<void> {
    this.loading.set(true);
    try {
      const incRes = await firstValueFrom(this.api.incidents.list({ size: 100 }));
      this.solicitudes.set(incRes.content.map(inc => toSolicitud(inc)));
      const tipos = [...new Set(incRes.content.map(inc => inc.tipo.nombre))];
      this.tiposDisponibles = tipos;
    } catch {
      this.toastService.error('Error al cargar incidencias');
    } finally {
      this.loading.set(false);
    }
  }

  toggleFilter(): void {
    this.filterOpen = !this.filterOpen;
  }

  setFilterEstado(estado: string): void {
    this.filterEstado.set(estado as EstadoFilter);
  }

  setFilterTipo(tipo: string): void {
    this.filterTipo.set(tipo === this.filterTipo() ? '' : tipo);
  }

  limpiarFiltros(): void {
    this.filterEstado.set('Todos');
    this.filterTipo.set('');
  }

  get hayFiltrosActivos(): boolean {
    return this.filterEstado() !== 'Todos' || !!this.filterTipo();
  }

  openModal(solicitud: IncidenciaSolicitud): void {
    this.selectedSolicitud = solicitud;
    this.modalOpen = true;
    this.rejectionReason.set('');
    this.showRejectionError.set(false);
    this.rejecting.set(false);
    this.showRejectionForm.set(false);
    document.body.style.overflow = 'hidden';
  }

  closeModal(): void {
    this.modalOpen = false;
    this.selectedSolicitud = null;
    this.rejectionReason.set('');
    this.showRejectionError.set(false);
    this.rejecting.set(false);
    this.showRejectionForm.set(false);
    document.body.style.overflow = '';
  }

  toggleRejectionForm(): void {
    this.showRejectionForm.set(!this.showRejectionForm());
    if (!this.showRejectionForm()) {
      this.rejectionReason.set('');
      this.showRejectionError.set(false);
    }
  }

  onRejectionReasonInput(event: Event): void {
    const value = (event.target as HTMLTextAreaElement).value;
    this.rejectionReason.set(value);
    if (value.trim() && this.showRejectionError()) {
      this.showRejectionError.set(false);
    }
  }

  async aprobar(solicitud: IncidenciaSolicitud): Promise<void> {
    try {
      await firstValueFrom(this.api.incidents.changeState(solicitud.id, { state: 'APROBADO' }));
      await this.cargarIncidencias();
      this.toastService.success('Incidencia aprobada');
    } catch {
      this.toastService.error('Error al aprobar');
    }
    this.closeModal();
  }

  async rechazar(solicitud: IncidenciaSolicitud): Promise<void> {
    if (!this.showRejectionForm()) {
      this.showRejectionForm.set(true);
      return;
    }
    const motivo = this.rejectionReason().trim();
    if (!motivo) {
      this.showRejectionError.set(true);
      return;
    }
    this.rejecting.set(true);
    try {
      await firstValueFrom(
        this.api.incidents.changeState(solicitud.id, {
          state: 'DENEGADO',
          motivo_rechazo: motivo,
        }),
      );
      await this.cargarIncidencias();
      this.toastService.success('Incidencia rechazada');
      this.closeModal();
    } catch (err) {
      this.handleRejectionError(err as { error?: { details?: Array<{ field?: string; message?: string }> } });
    } finally {
      this.rejecting.set(false);
    }
  }

  private handleRejectionError(err: { error?: { details?: Array<{ field?: string; message?: string }>; message?: string } }): void {
    const details = err?.error?.details;
    const motivoDetail = Array.isArray(details) ? details.find(d => d?.field === 'motivo_rechazo') : null;
    if (motivoDetail) {
      this.showRejectionError.set(true);
      this.toastService.error(motivoDetail.message ?? 'Debes ingresar un motivo para rechazar');
    } else {
      this.toastService.error(err?.error?.message ?? 'Error al rechazar');
    }
  }

  async descargarArchivo(solicitud: IncidenciaSolicitud): Promise<void> {
    if (!solicitud.archivo) return;
    try {
      const response = await fetch(solicitud.archivo.url);
      const blob = await response.blob();
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = solicitud.archivo.nombre;
      link.click();
      URL.revokeObjectURL(link.href);
    } catch {
      this.toastService.error('Error al descargar el archivo');
    }
  }

  exportarCSV(): void {
    const csv = this.generarContenidoCSV();
    this.descargarArchivoCSV(csv);
  }

  private generarContenidoCSV(): string {
    const totales = this.solicitudes().length;
    const p = this.solicitudes().filter(s => s.estado === 'Pendiente').length;
    const a = this.solicitudes().filter(s => s.estado === 'Aprobado').length;
    const r = this.solicitudes().filter(s => s.estado === 'Rechazado').length;
    const fecha = new Date().toLocaleDateString('es-PE');
    const esc = (v: string) => `"${(v ?? '').replace(/"/g, '""')}"`;
    const lineas: string[] = [];
    lineas.push(`"Reporte de Incidencias - ${fecha}"`);
    lineas.push(`"Total: ${totales} | Pendientes: ${p} | Aprobadas: ${a} | Rechazadas: ${r}"`);
    lineas.push('');
    lineas.push(['Colaborador', 'Rol', 'Tipo', 'Periodo', 'Detalle', 'Estado', 'Descripción', 'Fecha Creación', 'Archivo Adjunto'].join(','));
    for (const s of this.solicitudes()) {
      lineas.push([
        esc(s.colaborador), esc(s.rol), esc(s.tipo), esc(s.periodo), esc(s.detalle),
        esc(s.estado), esc(s.descripcion), esc(s.fechaCreacion), esc(s.archivo ? s.archivo.nombre : '—'),
      ].join(','));
    }
    return lineas.join('\r\n');
  }

  private descargarArchivoCSV(csv: string): void {
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    const hoy = new Date();
    const sufijo = `${hoy.getFullYear()}${String(hoy.getMonth()+1).padStart(2,'0')}${String(hoy.getDate()).padStart(2,'0')}`;
    link.download = `incidencias_${sufijo}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
  }
}
