import { Component, signal } from '@angular/core';

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
export class HistorialAsistencia {
  readonly loading = signal(false);
  readonly error = signal('');
  mesActual = 'Junio 2026';

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

  registros: RegistroAsistencia[] = [
    { fecha: '05/06/2026', ingreso: '06:02', salida: '14:05', turno: 'Mañana', estado: 'A tiempo' },
    { fecha: '04/06/2026', ingreso: '06:15', salida: '14:10', turno: 'Mañana', estado: 'Tardanza' },
    { fecha: '03/06/2026', ingreso: '05:58', salida: '14:02', turno: 'Mañana', estado: 'A tiempo' },
    { fecha: '02/06/2026', ingreso: '06:00', salida: '14:00', turno: 'Mañana', estado: 'A tiempo' },
    { fecha: '01/06/2026', ingreso: '—', salida: '—', turno: 'Mañana', estado: 'Justificado' },
    { fecha: '30/05/2026', ingreso: '14:05', salida: '22:08', turno: 'Tarde', estado: 'A tiempo' },
    { fecha: '29/05/2026', ingreso: '14:00', salida: '22:00', turno: 'Tarde', estado: 'A tiempo' },
    { fecha: '28/05/2026', ingreso: '14:30', salida: '22:15', turno: 'Tarde', estado: 'Tardanza' },
    { fecha: '27/05/2026', ingreso: '13:58', salida: '22:02', turno: 'Tarde', estado: 'A tiempo' },
    { fecha: '26/05/2026', ingreso: '06:00', salida: '14:00', turno: 'Mañana', estado: 'A tiempo' },
  ];

  get completos(): number {
    return this.registros.filter(r => r.estado === 'A tiempo').length;
  }

  get tardanzas(): number {
    return this.registros.filter(r => r.estado === 'Tardanza').length;
  }

  get faltas(): number {
    return this.registros.filter(r => r.estado === 'Falta').length;
  }

  get justificados(): number {
    return this.registros.filter(r => r.estado === 'Justificado').length;
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
    const total = this.registros.filter(r => r.estado !== 'Falta').length;
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

    for (const r of this.registros) {
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
