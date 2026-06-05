import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface Incidencia {
  id: string;
  tipo: string;
  descripcion: string;
  fecha: string;
  estado: 'Pendiente' | 'Aprobado' | 'Rechazado';
  archivo: { nombre: string; tamano: string } | null;
}

@Component({
  selector: 'app-incidencias',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './incidencias.html',
  styleUrl: './incidencias.css',
})
export class Incidencias {
  mostrarModal = false;
  tiposDisponibles = ['Permiso Personal', 'Justificación de Falta', 'Cambio de Turno', 'Vacaciones'];

  nuevaIncidencia = {
    tipo: '',
    fecha: '',
    descripcion: '',
    archivo: null as File | null,
  };

  incidencias = signal<Incidencia[]>([
    { id: '#INC-006', tipo: 'Permiso Personal', descripcion: 'Trámite bancario - medio día', fecha: '05/06/2026', estado: 'Pendiente', archivo: null },
    { id: '#INC-005', tipo: 'Permiso Personal', descripcion: 'Cita médica - 4 horas', fecha: '01/06/2026', estado: 'Aprobado', archivo: null },
    { id: '#INC-004', tipo: 'Justificación de Falta', descripcion: 'Emergencia familiar - 27/05', fecha: '27/05/2026', estado: 'Aprobado', archivo: null },
    { id: '#INC-003', tipo: 'Cambio de Turno', descripcion: 'Solicitud de intercambio con J. Pérez para el 10/06', fecha: '25/05/2026', estado: 'Aprobado', archivo: null },
    { id: '#INC-002', tipo: 'Permiso Personal', descripcion: 'Día personal solicitado para el 15/06', fecha: '20/05/2026', estado: 'Rechazado', archivo: null },
    { id: '#INC-001', tipo: 'Justificación de Falta', descripcion: 'Problema de salud - 15/05', fecha: '16/05/2026', estado: 'Aprobado', archivo: null },
  ]);

  get resumen() {
    const p = this.incidencias().filter(i => i.estado === 'Pendiente').length;
    const a = this.incidencias().filter(i => i.estado === 'Aprobado').length;
    const r = this.incidencias().filter(i => i.estado === 'Rechazado').length;
    return [
      { label: 'Pendientes', valor: p, color: '#FF9B5E' },
      { label: 'Aprobados', valor: a, color: '#10B981' },
      { label: 'Rechazados', valor: r, color: '#FF5A5A' },
    ];
  }

  abrirModal(): void {
    this.nuevaIncidencia = { tipo: '', fecha: '', descripcion: '', archivo: null };
    this.mostrarModal = true;
    document.body.style.overflow = 'hidden';
  }

  cerrarModal(): void {
    this.mostrarModal = false;
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
      estado: 'Pendiente',
      archivo: this.nuevaIncidencia.archivo
        ? { nombre: this.nuevaIncidencia.archivo.name, tamano: this.formatSize(this.nuevaIncidencia.archivo.size) }
        : null,
    };

    this.incidencias.update(list => [nueva, ...list]);
    this.cerrarModal();
  }

  private formatSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }
}
