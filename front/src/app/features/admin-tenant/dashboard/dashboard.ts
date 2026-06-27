import { Component, signal } from '@angular/core';

interface Alerta {
  icono: string;
  titulo: string;
  descripcion: string;
  fechaHora: string;
  tipo: 'danger' | 'warning';
}

interface RolPuntualidad {
  nombre: string;
  porcentaje: number;
  color: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  readonly loading = signal(false);
  readonly error = signal('');

  usuariosActivos = 882;
  capacidadPlan = 88;

  metricas = [
    {
      titulo: 'Total Inasistencias',
      valor: 124,
      porcentaje: '+12.5%',
      icono: 'bi-person-x-fill',
      color: '#FF5A5A',
    },
    {
      titulo: 'Total Incidencias',
      valor: 47,
      porcentaje: '-3.2%',
      icono: 'bi-exclamation-triangle-fill',
      color: '#FF9B5E',
    },
  ];

  indicePuntualidad = 87;

  rolesPuntualidad: RolPuntualidad[] = [
    { nombre: 'Director', porcentaje: 95, color: '#10B981' },
    { nombre: 'RRHH', porcentaje: 88, color: '#3B82F6' },
    { nombre: 'Docentes', porcentaje: 72, color: '#FF9B5E' },
    { nombre: 'Personal de Servicio', porcentaje: 64, color: '#FF5A5A' },
  ];

  alertas: Alerta[] = [
    {
      icono: 'bi-clock-fill',
      titulo: 'Tardanzas',
      descripcion: '15 empleados registraron ingreso después de las 8:00 AM',
      fechaHora: 'Hoy, 08:30 AM',
      tipo: 'danger',
    },
    {
      icono: 'bi-person-dash-fill',
      titulo: 'Empleado sin horario',
      descripcion: '3 empleados no tienen horario asignado en el sistema',
      fechaHora: 'Ayer, 04:15 PM',
      tipo: 'warning',
    },
    {
      icono: 'bi-clock-fill',
      titulo: 'Tardanzas recurrentes',
      descripcion: '5 empleados acumulan más de 3 tardanzas esta semana',
      fechaHora: 'Hoy, 07:45 AM',
      tipo: 'danger',
    },
    {
      icono: 'bi-person-dash-fill',
      titulo: 'Personal sin registro',
      descripcion: '2 empleados no han marcado ingreso hoy',
      fechaHora: 'Hoy, 09:00 AM',
      tipo: 'warning',
    },
  ];

  fechaActual = new Date().toLocaleDateString('es-PE', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}
