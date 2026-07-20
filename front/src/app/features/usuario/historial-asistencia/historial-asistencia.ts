import { Component, OnInit, inject, signal } from '@angular/core';
import { CorehrService } from '../../../api/services/corehr.service';
import type { AttendanceProfile } from '../../../api/types';

interface RegistroAsistencia {
  fecha: string;
  ingreso: string;
  salida: string;
  turno: string;
  estado: 'A tiempo' | 'Tardanza' | 'Falta' | 'Justificado';
}

@Component({
  selector: 'app-historial-asistencia',
  standalone: true,
  imports: [],
  templateUrl: './historial-asistencia.html',
  styleUrl: './historial-asistencia.css',
})
export class HistorialAsistencia implements OnInit {
  private readonly corehrService = inject(CorehrService);
  readonly loading = signal(false);
  readonly error = signal('');
  mesActual = 'Junio 2026';
  readonly registros = signal<RegistroAsistencia[]>([]);

  ngOnInit(): void {
    this.cargarHistorial();
  }

  private cargarHistorial(): void {
    this.loading.set(true);
    this.error.set('');

    this.corehrService.listAttendance({ scope: 'SELF', page: 0, size: 50 }).subscribe({
      next: response => {
        const registros = response.content.map(attendance => this.toRegistro(attendance));
        this.registros.set(registros);
        this.mesActual = this.formatearMes(response.content[0]?.attendance_date ?? new Date().toISOString());
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No fue posible cargar el historial de asistencia.');
        this.loading.set(false);
      },
    });
  }

  cambiarMes(delta: number): void {
    const meses = ['Enero','Febrero','Marzo','Abril','Mayo','Junio','Julio','Agosto','Setiembre','Octubre','Noviembre','Diciembre'];
    const partes = this.mesActual.split(' ');
    const mesIdx = meses.indexOf(partes[0]);
    const anio = parseInt(partes[1], 10);
    let nuevoMes = mesIdx + delta;
    let nuevoAnio = anio;
    if (nuevoMes < 0) { nuevoMes = 11; nuevoAnio--; }
    if (nuevoMes > 11) { nuevoMes = 0; nuevoAnio++; }
    this.mesActual = `${meses[nuevoMes]} ${nuevoAnio}`;
  }

  private toRegistro(attendance: AttendanceProfile): RegistroAsistencia {
    const fecha = new Date(attendance.attendance_date);
    const estado = this.mapEstado(attendance.state);
    return {
      fecha: `${String(fecha.getUTCDate()).padStart(2, '0')}/${String(fecha.getUTCMonth() + 1).padStart(2, '0')}/${fecha.getUTCFullYear()}`,
      ingreso: attendance.check_in?.slice(11, 16) ?? '—',
      salida: attendance.check_out?.slice(11, 16) ?? '—',
      turno: attendance.schedule?.name ?? '—',
      estado,
    };
  }

  private mapEstado(state: AttendanceProfile['state']): RegistroAsistencia['estado'] {
    if (state === 'PUNTUAL') {
      return 'A tiempo';
    }
    if (state === 'TARDANZA') {
      return 'Tardanza';
    }
    return 'Falta';
  }

  private formatearMes(fechaIso: string): string {
    const fecha = new Date(fechaIso);
    const meses = ['Enero','Febrero','Marzo','Abril','Mayo','Junio','Julio','Agosto','Setiembre','Octubre','Noviembre','Diciembre'];
    return `${meses[fecha.getUTCMonth()]} ${fecha.getUTCFullYear()}`;
  }

  get completos(): number {
    return this.registros().filter(r => r.estado === 'A tiempo').length;
  }

  get tardanzas(): number {
    return this.registros().filter(r => r.estado === 'Tardanza').length;
  }

  get faltas(): number {
    return this.registros().filter(r => r.estado === 'Falta').length;
  }

  get justificados(): number {
    return this.registros().filter(r => r.estado === 'Justificado').length;
  }

  get resumen() {
    return [
      { label: 'A tiempo', valor: this.completos, color: '#10B981' },
      { label: 'Tardanzas', valor: this.tardanzas, color: '#FF9B5E' },
      { label: 'Faltas', valor: this.faltas, color: '#FF5A5A' },
      { label: 'Justificados', valor: this.justificados, color: '#3B82F6' },
    ];
  }

  get eficiencia(): number {
    const total = this.registros().filter(r => r.estado !== 'Falta').length;
    const onTime = this.completos;
    return total > 0 ? Math.round((onTime / total) * 100) : 0;
  }

  exportarCSV(): void {
    const hoy = new Date();
    const sufijo = `${hoy.getFullYear()}${String(hoy.getMonth() + 1).padStart(2, '0')}${String(hoy.getDate()).padStart(2, '0')}`;

    const esc = (v: string) => `"${(v ?? '').replace(/"/g, '""')}"`;
    const lineas: string[] = [
      `"Reporte de Asistencia - ${this.mesActual}"`,
      '',
      ['Fecha', 'Turno', 'Ingreso', 'Salida', 'Estado'].join(','),
    ];

    for (const r of this.registros()) {
      lineas.push([esc(r.fecha), esc(r.turno), esc(r.ingreso), esc(r.salida), esc(r.estado)].join(','));
    }

    const csv = lineas.join('\r\n');
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `asistencia_${sufijo}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
  }
}
