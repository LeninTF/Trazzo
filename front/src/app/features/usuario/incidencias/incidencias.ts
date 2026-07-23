import { Component, signal, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { IncidentsService } from '../../../api/services/incidents.service';
import type { IncidentProfile } from '../../../api/types';

interface Incidencia {
  id: string;
  tipo: string;
  descripcion: string;
  fecha: string;
  dias: number;
  estado: 'Pendiente' | 'Aprobado' | 'Rechazado';
  archivo: { nombre: string; tamano: string } | null;
}

type EstadoFilter = 'Todos' | 'Pendiente' | 'Aprobado' | 'Rechazado';

const STATE_MAP: Record<string, 'Pendiente' | 'Aprobado' | 'Rechazado'> = {
  PENDIENTE: 'Pendiente', APROBADO: 'Aprobado', DENEGADO: 'Rechazado',
};

function formatDate(iso: string): string {
  const d = new Date(iso);
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function toIncidencia(inc: IncidentProfile): Incidencia {
  const e = inc.evidencias?.[0];
  return {
    id: `#INC-${inc.id}`,
    tipo: inc.tipo.nombre,
    descripcion: inc.comment ?? '',
    fecha: inc.created_at ? formatDate(inc.created_at) : '',
    dias: inc.permiso?.days_granted ?? 1,
    estado: STATE_MAP[inc.state] ?? 'Pendiente',
    archivo: e ? { nombre: e.file_name, tamano: formatSize(e.file_size) } : null,
  };
}

@Component({
  selector: 'app-incidencias',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './incidencias.html',
  styleUrl: './incidencias.css',
})
export class Incidencias implements OnInit {
  private readonly incidentsService = inject(IncidentsService);

  readonly loading = signal(false);
  readonly error = signal('');
  mostrarModalCrear = false;
  mostrarModalDetalle = false;
  selectedIncidencia: Incidencia | null = null;
  filterEstado = signal<EstadoFilter>('Todos');

  tiposDisponibles: string[] = [];
  private tipoNameToId = new Map<string, number>();

  nuevaIncidencia = {
    tipo: '',
    fecha: '',
    dias: 1,
    descripcion: '',
    archivo: null as File | null,
  };

  incidencias = signal<Incidencia[]>([]);

  ngOnInit(): void {
    this.cargarDatos();
  }

  async cargarDatos(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      const [tiposRes, incRes] = await Promise.all([
        firstValueFrom(this.incidentsService.listTypes({ activo: true, size: 10 })),
        firstValueFrom(this.incidentsService.list({ size: 10, scope: 'SELF' })),
      ]);
      this.tiposDisponibles = tiposRes.content.map(t => t.nombre);
      this.tipoNameToId.clear();
      for (const t of tiposRes.content) {
        this.tipoNameToId.set(t.nombre, t.id);
      }
      this.incidencias.set(incRes.content.map(toIncidencia));
    } catch {
      this.error.set('Error al cargar las incidencias. Intenta de nuevo.');
    } finally {
      this.loading.set(false);
    }
  }

  get filtradas() {
    return this.incidencias().filter(i =>
      this.filterEstado() === 'Todos' || i.estado === this.filterEstado()
    );
  }

  get pendientes() {
    return this.incidencias().filter(i => i.estado === 'Pendiente').length;
  }

  get aprobados() {
    return this.incidencias().filter(i => i.estado === 'Aprobado').length;
  }

  get rechazados() {
    return this.incidencias().filter(i => i.estado === 'Rechazado').length;
  }

  get resumen() {
    return [
      { label: 'Pendientes', valor: this.pendientes, icono: 'bi-clock-history', color: '#F59E0B', bg: '#FFFBEB' },
      { label: 'Aprobados', valor: this.aprobados, icono: 'bi-check-circle', color: '#10B981', bg: '#F0FDF4' },
      { label: 'Rechazados', valor: this.rechazados, icono: 'bi-x-circle', color: '#EF4444', bg: '#FEF2F2' },
    ];
  }

  setFilterEstado(e: string): void {
    this.filterEstado.set(e as EstadoFilter);
  }

  abrirModalCrear(): void {
    this.nuevaIncidencia = { tipo: '', fecha: '', dias: 1, descripcion: '', archivo: null };
    this.mostrarModalCrear = true;
    document.body.style.overflow = 'hidden';
  }

  cerrarModalCrear(): void {
    this.mostrarModalCrear = false;
    document.body.style.overflow = '';
  }

  abrirDetalle(inc: Incidencia): void {
    this.selectedIncidencia = inc;
    this.mostrarModalDetalle = true;
    document.body.style.overflow = 'hidden';
  }

  cerrarDetalle(): void {
    this.mostrarModalDetalle = false;
    this.selectedIncidencia = null;
    document.body.style.overflow = '';
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.nuevaIncidencia.archivo = input.files[0];
    }
  }

  async enviar(): Promise<void> {
    if (!this.nuevaIncidencia.tipo || !this.nuevaIncidencia.descripcion) return;

    const typeId = this.tipoNameToId.get(this.nuevaIncidencia.tipo);
    if (!typeId) return;

    this.loading.set(true);
    try {
      await firstValueFrom(this.incidentsService.create({
        incidencia_type_id: typeId,
        comment: this.nuevaIncidencia.descripcion,
      }));
      await this.cargarDatos();
      this.cerrarModalCrear();
    } catch {
      this.error.set('Error al crear la incidencia.');
    } finally {
      this.loading.set(false);
    }
  }

  descargarArchivo(inc: Incidencia): void {
    if (!inc.archivo) return;
    const link = document.createElement('a');
    link.href = '#';
    link.download = inc.archivo.nombre;
    link.click();
  }
}
