import { Component, computed, signal } from '@angular/core';

interface IncidenciaSolicitud {
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
  } | null;
}

type EstadoFilter = 'Todos' | 'Pendiente' | 'Aprobado' | 'Rechazado';

@Component({
  selector: 'app-incidencias',
  standalone: true,
  imports: [],
  templateUrl: './incidencias.html',
  styleUrl: './incidencias.css',
})
export class Incidencias {
  modalOpen = false;
  filterOpen = false;
  selectedSolicitud: IncidenciaSolicitud | null = null;

  filterEstado = signal<EstadoFilter>('Todos');
  filterTipo = signal<string>('');

  tiposDisponibles = ['Permiso Médico', 'Vacaciones', 'Falta Justificada'];

  solicitudes = signal<IncidenciaSolicitud[]>([
    {
      colaborador: 'Mariana Rodríguez',
      rol: 'Docente de Matemáticas',
      tipo: 'Permiso Médico',
      periodo: '12–14 Jun',
      detalle: '3 días hábiles',
      estado: 'Pendiente',
      descripcion: 'Solicito permiso médico por diagnóstico de influenza. Adjunto certificado médico emitido por la Clínica San Pablo.',
      fechaCreacion: '02/06/2026',
      archivo: {
        nombre: 'certificado-medico-mariana.pdf',
        tipo: 'application/pdf',
        tamano: '1.2 MB',
      },
    },
    {
      colaborador: 'Jorge Salazar',
      rol: 'Servicios Generales',
      tipo: 'Vacaciones',
      periodo: '15–30 Jul',
      detalle: '15 días naturales',
      estado: 'Aprobado',
      descripcion: 'Solicitud de vacaciones aprobadas por el área de RRHH. Periodo solicitado desde el 15 de julio al 30 de julio.',
      fechaCreacion: '20/05/2026',
      archivo: {
        nombre: 'solicitud-vacaciones-jorge.pdf',
        tipo: 'application/pdf',
        tamano: '0.8 MB',
      },
    },
    {
      colaborador: 'Carla Méndez',
      rol: 'Secretaria Académica',
      tipo: 'Falta Justificada',
      periodo: '05 Jun',
      detalle: '1 día',
      estado: 'Pendiente',
      descripcion: 'Inasistencia por emergencia familiar. Adjunto documento de justificación firmado por el coordinador académico.',
      fechaCreacion: '05/06/2026',
      archivo: {
        nombre: 'justificacion-carla.pdf',
        tipo: 'application/pdf',
        tamano: '0.5 MB',
      },
    },
  ]);

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

  get metricas(): { titulo: string; valor: number | string; subtitulo: string; icono: string; color: string; bg: string }[] {
    const p = this.pendientes().length;
    const a = this.aprobadas().length;
    const r = this.rechazadas().length;
    return [
      {
        titulo: 'Pendientes',
        valor: p,
        subtitulo: '+4 esta semana',
        icono: 'bi-clock-history',
        color: '#F59E0B',
        bg: '#FFFBEB',
      },
      {
        titulo: 'Aprobadas',
        valor: a,
        subtitulo: 'Mes de Junio',
        icono: 'bi-check-circle',
        color: '#10B981',
        bg: '#F0FDF4',
      },
      {
        titulo: 'Rechazadas',
        valor: r < 10 ? '0' + r : r,
        subtitulo: r === 0 ? 'Sin incidencias críticas' : 'Requieren atención',
        icono: 'bi-x-circle',
        color: '#EF4444',
        bg: '#FEF2F2',
      },
    ];
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
    document.body.style.overflow = 'hidden';
  }

  closeModal(): void {
    this.modalOpen = false;
    this.selectedSolicitud = null;
    document.body.style.overflow = '';
  }

  aprobar(solicitud: IncidenciaSolicitud): void {
    this.solicitudes.update(list =>
      list.map(s => s === solicitud ? { ...s, estado: 'Aprobado' } : s)
    );
    this.closeModal();
  }

  rechazar(solicitud: IncidenciaSolicitud): void {
    this.solicitudes.update(list =>
      list.map(s => s === solicitud ? { ...s, estado: 'Rechazado' } : s)
    );
    this.closeModal();
  }

  descargarArchivo(solicitud: IncidenciaSolicitud): void {
    if (!solicitud.archivo) return;
    const link = document.createElement('a');
    link.href = '#';
    link.download = solicitud.archivo.nombre;
    link.click();
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
