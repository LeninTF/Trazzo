import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ReportsService } from '../../../../api/services/reports.service';
import { getMonthName } from '../../../../api/services/helpers';
import type { MonthlyClosureWithDetails } from '../../../../api/types';

@Component({
  selector: 'app-detalle-cierre',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './detalle-cierre.html',
  styleUrl: './detalle-cierre.css',
})
export class DetalleCierre implements OnInit {
  readonly loading = signal(true);
  readonly error = signal('');

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly reportsService = inject(ReportsService);

  report: MonthlyClosureWithDetails | null = null;

  get totalHorasTrabajadas(): number {
    return this.report?.details.reduce((sum, d) => sum + d.totalWorkedHours, 0) ?? 0;
  }

  get totalTardanzas(): number {
    return this.report?.details.reduce((sum, d) => sum + d.totalTardinessMinutes, 0) ?? 0;
  }

  get totalAusencias(): number {
    return this.report?.details.reduce((sum, d) => sum + d.totalAbsences, 0) ?? 0;
  }

  get totalHorasExtras(): number {
    return this.report?.details.reduce((sum, d) => sum + d.totalOvertimeHours, 0) ?? 0;
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set('ID de cierre no proporcionado');
      this.loading.set(false);
      return;
    }
    this.cargarReporte(id);
  }

  cargarReporte(id: string): void {
    this.loading.set(true);
    this.error.set('');
    this.reportsService.getFullReport(id).subscribe({
      next: data => {
        this.report = data;
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar el reporte');
        this.loading.set(false);
      },
    });
  }

  getMonthName(month: number): string {
    return getMonthName(month);
  }

  volver(): void {
    this.router.navigate(['/tenant/cierres-mensuales']);
  }
}
