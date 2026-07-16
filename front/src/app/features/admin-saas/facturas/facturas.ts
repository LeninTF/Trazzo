import { Component, computed, signal, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ChartComponent } from 'ng-apexcharts';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { ToastService } from '../../../services/toast.service';
import { ExportService } from '../../../services/export.service';
import { ModalService } from '../../../services/modal.service';
import { ApiService } from '../../../api/services/api.service';
import type { InvoiceProfile } from '../../../api/types';

const ESTADO_LABELS: Record<string, string> = {
  PENDIENTE: 'Pendiente',
  COMPLETADO: 'Completado',
  FALLIDO: 'Fallido',
  REEMBOLSADO: 'Reembolsado',
};

@Component({
  selector: 'app-facturas',
  standalone: true,
  imports: [ReactiveFormsModule, ChartComponent, PaginationComponent],
  templateUrl: './facturas.html',
  styleUrl: './facturas.css',
})
export class Facturas {
  readonly loading = signal(false);
  readonly error = signal('');

  private readonly fb = new FormBuilder();
  private readonly toastService = inject(ToastService);
  private readonly exportService = inject(ExportService);
  private readonly modalService = inject(ModalService);
  private readonly api = inject(ApiService);

  readonly filterForm = this.fb.group({
    paymentStatus: ['todos'],
    dateFrom: [''],
    dateTo: [''],
  });

  private readonly filterValues = toSignal(this.filterForm.valueChanges, {
    initialValue: this.filterForm.value,
  });

  readonly pagina = signal(1);
  readonly pageSize = 5;
  readonly items = signal<InvoiceProfile[]>([]);
  readonly totalElements = signal(0);
  readonly totalPaginas = computed(() => Math.max(1, Math.ceil(this.totalElements() / this.pageSize)));

  facturaSeleccionada = signal<InvoiceProfile | null>(null);

  readonly estadoLabels = ESTADO_LABELS;

  constructor() {
    this.filterValues();
    this.cargarFacturas();
  }

  private currentFilters(): { paymentStatus?: string; dateFrom?: string; dateTo?: string } {
    const fv = this.filterForm.value;
    return {
      paymentStatus: fv.paymentStatus && fv.paymentStatus !== 'todos' ? fv.paymentStatus : undefined,
      dateFrom: fv.dateFrom || undefined,
      dateTo: fv.dateTo || undefined,
    };
  }

  private cargarFacturas(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.saas.listInvoices({
      ...this.currentFilters(),
      page: this.pagina() - 1,
      size: this.pageSize,
    }).subscribe({
      next: res => {
        this.items.set(res.content);
        this.totalElements.set(res.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las facturas.');
        this.loading.set(false);
      },
    });
  }

  // ── Métricas: derivadas de la página actualmente cargada. Con la lista vacía
  // (todavía no hay pagos reales sin Mercado Pago) muestran honestamente 0/S.0.00
  // en vez de cifras inventadas; se autopoblarán cuando existan facturas reales. ──
  readonly totalFacturado = computed(() => this.items().reduce((sum, f) => sum + f.total, 0));
  readonly totalPendientes = computed(() => this.items().filter(f => f.paymentStatus === 'PENDIENTE').length);
  readonly totalCompletadas = computed(() => this.items().filter(f => f.paymentStatus === 'COMPLETADO').length);
  readonly promedioFactura = computed(() => {
    const n = this.items().length;
    return n > 0 ? this.totalFacturado() / n : 0;
  });

  readonly chartConfig = computed(() => {
    const porFecha = new Map<string, number>();
    for (const f of this.items()) {
      const mes = f.createdAt.slice(0, 7);
      porFecha.set(mes, (porFecha.get(mes) ?? 0) + f.total);
    }
    const categorias = [...porFecha.keys()].sort((a, b) => a.localeCompare(b));
    const data = categorias.map(k => porFecha.get(k) ?? 0);
    return {
      chart: {
        type: 'bar' as const,
        height: 320,
        toolbar: { show: false },
        fontFamily: 'Inter, system-ui, sans-serif',
        animations: { enabled: true, speed: 500, animateGradually: { enabled: true, delay: 100 } },
        parentHeightOffset: 0,
      },
      series: [{ name: 'Facturación', data }],
      colors: ['#6366f1', '#06b6d4', '#f59e0b', '#ef4444', '#8b5cf6', '#10b981'],
      plotOptions: {
        bar: {
          borderRadius: 6,
          borderRadiusApplication: 'end' as const,
          columnWidth: '55%',
          distributed: true,
          dataLabels: { position: 'top' as const },
        },
      },
      dataLabels: {
        enabled: true,
        formatter: (v: number) => `S/${(v / 1000).toFixed(1)}k`,
        offsetY: -10,
        style: { fontSize: '12px', fontWeight: 700, colors: ['#1e293b'], fontFamily: 'Inter, sans-serif' },
      },
      xaxis: {
        categories: categorias,
        labels: { style: { colors: '#64748b', fontWeight: 600, fontSize: '13px' } },
        axisBorder: { show: false },
        axisTicks: { show: false },
      },
      yaxis: {
        labels: {
          formatter: (v: number) => `S/${(v / 1000).toFixed(0)}k`,
          style: { colors: '#94a3b8', fontSize: '12px', fontWeight: 500 },
        },
        axisBorder: { show: false },
        axisTicks: { show: false },
        min: 0,
      },
      grid: {
        borderColor: 'rgba(100,116,139,0.12)',
        strokeDashArray: 4,
        xaxis: { lines: { show: false } },
        padding: { top: 8 },
      },
      tooltip: {
        y: { formatter: (v: number) => `S/${v.toLocaleString('es-PE')}` },
        style: { fontFamily: 'Inter, sans-serif', fontSize: '13px' },
      },
      fill: { opacity: 0.85 },
      stroke: { show: false },
    };
  });

  readonly rangoInfo = computed(() => {
    if (this.totalElements() === 0) return 'Mostrando 0 facturas';
    const start = (this.pagina() - 1) * this.pageSize + 1;
    const end = Math.min(this.pagina() * this.pageSize, this.totalElements());
    return `Mostrando ${start}–${end} de ${this.totalElements()} facturas`;
  });

  irPagina(p: number): void {
    this.pagina.set(p);
    this.cargarFacturas();
  }

  onFilterChange(): void {
    this.pagina.set(1);
    this.cargarFacturas();
  }

  verDetalle(f: InvoiceProfile): void {
    this.api.saas.getInvoice(f.id).subscribe({
      next: detalle => {
        this.facturaSeleccionada.set(detalle);
        this.modalService.show('modalDetalleTransaccion');
      },
      error: () => this.toastService.error('No se pudo cargar el detalle de la factura.'),
    });
  }

  cerrarDetalle(): void {
    this.modalService.hide('modalDetalleTransaccion');
  }

  exportarCSV(): void {
    const headers = ['Cliente', 'RUC', 'Comprobante', 'Fecha', 'Monto', 'Estado'];
    const rows = this.items().map(f =>
      [f.clientName, f.clientTaxId, `${f.invoiceSeries}-${f.consecutiveNumber}`,
        f.createdAt.slice(0, 10), `S/${f.total.toFixed(2)}`, this.estadoLabels[f.paymentStatus] ?? f.paymentStatus]
    );
    this.exportService.exportCSV(`facturas-${new Date().toISOString().split('T')[0]}.csv`, headers, rows);
    this.toastService.info('CSV exportado correctamente.');
  }

  exportarExcel(): void {
    this.api.saas.exportInvoicesExcel(this.currentFilters()).subscribe({
      next: blob => this.exportService.downloadBlob(`facturas-${new Date().toISOString().split('T')[0]}.xlsx`, blob),
      error: () => this.toastService.error('No se pudo exportar a Excel.'),
    });
  }

  exportarPDF(): void {
    this.api.saas.exportInvoicesPdf(this.currentFilters()).subscribe({
      next: blob => this.exportService.downloadBlob(`facturas-${new Date().toISOString().split('T')[0]}.pdf`, blob),
      error: () => this.toastService.error('No se pudo exportar a PDF.'),
    });
  }
}
