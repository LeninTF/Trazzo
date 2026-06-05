import { Component } from '@angular/core';

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
  mesActual = 'Junio 2026';

  resumen = [
    { label: 'A tiempo', valor: 16, color: '#10B981' },
    { label: 'Tardanzas', valor: 2, color: '#FF9B5E' },
    { label: 'Faltas', valor: 0, color: '#FF5A5A' },
    { label: 'Justificados', valor: 1, color: '#3B82F6' },
  ];

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

  cambiarMes(delta: number): void {}
}
