import { Component, inject, signal, OnInit } from '@angular/core';
import { DashboardService } from '../../../api/services/dashboard.service';
import type {
  DashboardQueryParams, DashboardSummaryResponse,
  PuntualidadRolResponse, AlertaDashboard,
} from '../../../api/types';

const ROLE_COLORS: Record<string, string> = {};

function getRoleColor(nombre: string): string {
  if (ROLE_COLORS[nombre]) return ROLE_COLORS[nombre];
  const palette = ['#10B981', '#3B82F6', '#FF9B5E', '#FF5A5A', '#8B5CF6', '#06B6D4'];
  const idx = Object.keys(ROLE_COLORS).length % palette.length;
  const color = palette[idx];
  ROLE_COLORS[nombre] = color;
  return color;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  private readonly dashboardService = inject(DashboardService);

  readonly loading = signal(true);
  readonly error = signal('');

  readonly periodo = signal<'dia' | 'semana' | 'mes' | 'anio'>('mes');

  usuariosActivos = signal(0);
  capacidadPlan = signal(0);

  metricas = signal<{ titulo: string; valor: number; porcentaje: string; icono: string; color: string }[]>([]);

  indicePuntualidad = signal(0);

  rolesPuntualidad = signal<{ nombre: string; porcentaje: number; color: string }[]>([]);

  alertas = signal<AlertaDashboard[]>([]);

  fechaActual = new Date().toLocaleDateString('es-PE', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  ngOnInit(): void {
    this.loadDashboard();
  }

  cambiarPeriodo(periodo: 'dia' | 'semana' | 'mes' | 'anio'): void {
    this.periodo.set(periodo);
    this.loadDashboard();
  }

  private loadDashboard(): void {
    this.loading.set(true);
    this.error.set('');

    const query: DashboardQueryParams = { periodo: this.periodo() };

    this.dashboardService.getSummary(query).subscribe({
      next: (data) => {
        this.usuariosActivos.set(data.usuarios_activos);
        this.capacidadPlan.set(data.capacidad_plan);
        this.indicePuntualidad.set(data.indice_puntualidad_anual);
        this.metricas.set([
          {
            titulo: 'Total Inasistencias',
            valor: data.metricas.total_inasistencias,
            porcentaje: '',
            icono: 'bi-person-x-fill',
            color: '#FF5A5A',
          },
          {
            titulo: 'Total Incidencias',
            valor: data.metricas.total_incidencias,
            porcentaje: '',
            icono: 'bi-exclamation-triangle-fill',
            color: '#FF9B5E',
          },
        ]);
      },
      error: (err) => {
        this.error.set('Error al cargar el resumen del dashboard');
        console.error(err);
      },
    });

    this.dashboardService.getPuntualidadPorRol(query).subscribe({
      next: (data) => {
        this.rolesPuntualidad.set(
          data.roles.map((r) => ({
            nombre: r.nombre,
            porcentaje: r.porcentaje,
            color: getRoleColor(r.nombre),
          }))
        );
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Error al cargar la puntualidad por rol');
        this.loading.set(false);
        console.error(err);
      },
    });

    this.dashboardService.getAlertas().subscribe({
      next: (data) => {
        this.alertas.set(data.alertas);
      },
      error: (err) => {
        console.error(err);
      },
    });
  }
}
