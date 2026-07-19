import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { ToastService } from '../../../services/toast.service';
import { ExportService } from '../../../services/export.service';
import { ReportsService } from '../../../api/services/reports.service';
import type { MonthlyClosure } from '../../../api/types';

const MONTH_NAMES = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre',
];

@Component({
  selector: 'app-cierres-mensuales',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './cierres-mensuales.html',
  styleUrl: './cierres-mensuales.css',
})
export class CierresMensuales implements OnInit {
  readonly loading = signal(false);
  readonly error = signal('');
  readonly showCreateModal = signal(false);

  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);
  private readonly exportService = inject(ExportService);
  private readonly reportsService = inject(ReportsService);

  closures: MonthlyClosure[] = [];

  filtroYear = '';
  filtroMonth = '';
  paginaActual = 1;
  itemsPerPage = 10;

  nuevoMes = new Date().getMonth() + 1;
  nuevoAnio = new Date().getFullYear();
  creando = false;

  get closuresPaginado(): MonthlyClosure[] {
    const start = (this.paginaActual - 1) * this.itemsPerPage;
    return this.closures.slice(start, start + this.itemsPerPage);
  }

  get totalPaginas(): number {
    return Math.max(1, Math.ceil(this.closures.length / this.itemsPerPage));
  }

  get inicioRegistro(): number {
    return this.closures.length === 0 ? 0 : (this.paginaActual - 1) * this.itemsPerPage + 1;
  }

  get finRegistro(): number {
    return Math.min(this.paginaActual * this.itemsPerPage, this.closures.length);
  }

  ngOnInit(): void {
    this.cargarCierres();
  }

  cargarCierres(): void {
    this.loading.set(true);
    this.error.set('');
    const opts: { year?: number; month?: number } = {};
    if (this.filtroYear) {
      const year = Number.parseInt(this.filtroYear, 10);
      if (Number.isFinite(year)) opts.year = year;
    }
    if (this.filtroMonth) {
      const month = Number.parseInt(this.filtroMonth, 10);
      if (Number.isFinite(month) && month >= 1 && month <= 12) opts.month = month;
    }

    this.reportsService.listClosures(opts).subscribe({
      next: data => {
        this.closures = data;
        this.error.set('');
        this.paginaActual = Math.min(this.paginaActual, this.totalPaginas);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar los cierres mensuales');
        this.loading.set(false);
      },
    });
  }

  getMonthName(month: number): string {
    return MONTH_NAMES[month - 1] ?? '';
  }

  filtrar(): void {
    this.paginaActual = 1;
    this.cargarCierres();
  }

  limpiarFiltros(): void {
    this.filtroYear = '';
    this.filtroMonth = '';
    this.filtrar();
  }

  cambiarPagina(pagina: number): void {
    if (pagina >= 1 && pagina <= this.totalPaginas) {
      this.paginaActual = pagina;
    }
  }

  abrirCrear(): void {
    this.nuevoMes = new Date().getMonth() + 1;
    this.nuevoAnio = new Date().getFullYear();
    this.showCreateModal.set(true);
  }

  cerrarCrear(): void {
    this.showCreateModal.set(false);
  }

  crearCierre(): void {
    if (this.creando) return;
    this.creando = true;
    this.reportsService.createClosure({ month: this.nuevoMes, year: this.nuevoAnio }).subscribe({
      next: () => {
        this.toastService.info('Cierre mensual creado exitosamente');
        this.showCreateModal.set(false);
        this.cargarCierres();
        this.creando = false;
      },
      error: (err) => {
        const msg = err?.error?.message ?? 'Error al crear el cierre';
        this.toastService.error(msg);
        this.creando = false;
      },
    });
  }

  verDetalle(id: string): void {
    this.router.navigate(['/tenant/cierres-mensuales', id]);
  }

  exportarCSV(): void {
    const headers = ['MES', 'AÑO', 'EMPLEADOS', 'REPORTE EXCEL', 'REPORTE PDF', 'FECHA CREACION'];
    const rows = this.closures.map(c => [
      this.getMonthName(c.month),
      String(c.year),
      String(c.totalEmployees),
      c.excelReportUrl ?? 'N/A',
      c.pdfReportUrl ?? 'N/A',
      new Date(c.createdAt).toLocaleDateString('es-PE'),
    ]);
    this.exportService.exportCSV(`cierres-mensuales-${new Date().toISOString().split('T')[0]}.csv`, headers, rows);
    this.toastService.info('Exportando CSV...');
  }

  get monthOptions(): number[] {
    return Array.from({ length: 12 }, (_, i) => i + 1);
  }
}
