import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

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

@Component({
  selector: 'app-incidencias',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './incidencias.html',
  styleUrl: './incidencias.css',
})
export class Incidencias {
  mostrarModalCrear = false;
  mostrarModalDetalle = false;
  selectedIncidencia: Incidencia | null = null;
  filterEstado = signal<EstadoFilter>('Todos');

  tiposDisponibles = ['Permiso Personal', 'Justificación de Falta', 'Cambio de Turno', 'Vacaciones'];

  nuevaIncidencia = {
    tipo: '',
    fecha: '',
    dias: 1,
    descripcion: '',
    archivo: null as File | null,
  };

  incidencias = signal<Incidencia[]>([
    { id: '#INC-006', tipo: 'Permiso Personal', descripcion: 'Trámite bancario - medio día', fecha: '05/06/2026', dias: 1, estado: 'Pendiente', archivo: null },
    { id: '#INC-005', tipo: 'Permiso Personal', descripcion: 'Cita médica - 4 horas', fecha: '01/06/2026', dias: 1, estado: 'Aprobado', archivo: null },
    { id: '#INC-004', tipo: 'Justificación de Falta', descripcion: 'Emergencia familiar - 27/05', fecha: '27/05/2026', dias: 2, estado: 'Aprobado', archivo: { nombre: 'justificacion.pdf', tamano: '0.5 MB' } },
    { id: '#INC-003', tipo: 'Cambio de Turno', descripcion: 'Solicitud de intercambio con J. Pérez para el 10/06', fecha: '25/05/2026', dias: 1, estado: 'Aprobado', archivo: null },
    { id: '#INC-002', tipo: 'Permiso Personal', descripcion: 'Día personal solicitado para el 15/06', fecha: '20/05/2026', dias: 1, estado: 'Rechazado', archivo: null },
    { id: '#INC-001', tipo: 'Justificación de Falta', descripcion: 'Problema de salud - 15/05', fecha: '16/05/2026', dias: 3, estado: 'Aprobado', archivo: { nombre: 'certificado-medico.pdf', tamano: '1.2 MB' } },
  ]);

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

  enviar(): void {
    if (!this.nuevaIncidencia.tipo || !this.nuevaIncidencia.descripcion) return;

    const ahora = new Date();
    const fechaLocal = `${String(ahora.getDate()).padStart(2, '0')}/${String(ahora.getMonth() + 1).padStart(2, '0')}/${ahora.getFullYear()}`;

    const ids = this.incidencias().map(i => parseInt(i.id.replace('#INC-', ''), 10));
    const nextId = Math.max(0, ...ids) + 1;

    const nueva: Incidencia = {
      id: `#INC-${String(nextId).padStart(3, '0')}`,
      tipo: this.nuevaIncidencia.tipo,
      descripcion: this.nuevaIncidencia.descripcion,
      fecha: this.nuevaIncidencia.fecha || fechaLocal,
      dias: this.nuevaIncidencia.dias,
      estado: 'Pendiente',
      archivo: this.nuevaIncidencia.archivo
        ? { nombre: this.nuevaIncidencia.archivo.name, tamano: this.formatSize(this.nuevaIncidencia.archivo.size) }
        : null,
    };

    this.incidencias.update(list => [nueva, ...list]);
    this.cerrarModalCrear();
  }

  descargarArchivo(inc: Incidencia): void {
    if (!inc.archivo) return;
    const link = document.createElement('a');
    link.href = '#';
    link.download = inc.archivo.nombre;
    link.click();
  }

  private formatSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }
}
