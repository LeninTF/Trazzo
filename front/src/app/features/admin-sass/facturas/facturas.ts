import { Component, computed, signal, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ChartComponent } from 'ng-apexcharts';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { ToastService } from '../../../services/toast.service';
import { ExportService } from '../../../services/export.service';
import { ModalService } from '../../../services/modal.service';

interface Transaccion {
	id: number;
	tenant: string;
	plan: string;
	idTransaccion: string;
	fecha: string;
	fechaObj: Date;
	monto: number;
	estado: 'pagado' | 'pendiente' | 'vencido';
}

@Component({
	selector: 'app-facturas',
	standalone: true,
	imports: [ReactiveFormsModule, ChartComponent, PaginationComponent],
	templateUrl: './facturas.html',
	styleUrl: './facturas.css',
})
export class Facturas {
  private readonly fb = new FormBuilder();
  private readonly toastService = inject(ToastService);
  private readonly exportService = inject(ExportService);
  private readonly modalService = inject(ModalService);

	readonly filterForm = this.fb.group({
		estado: ['todos'],
		rango: ['ultimos-30'],
		fechaDesde: [''],
		fechaHasta: [''],
	});

	private readonly filterValues = toSignal(this.filterForm.valueChanges, {
		initialValue: this.filterForm.value,
	});

	readonly transacciones: Transaccion[] = [
		{ id: 1, tenant: 'Global Tech Corp', plan: 'Enterprise', idTransaccion: 'TN-9821', fecha: '05 Jun 2026', fechaObj: new Date('2026-06-05'), monto: 1200, estado: 'pagado' },
		{ id: 2, tenant: 'Studio Creative S.A.', plan: 'Pro Pack', idTransaccion: 'TN-4450', fecha: '03 Jun 2026', fechaObj: new Date('2026-06-03'), monto: 450, estado: 'pendiente' },
		{ id: 3, tenant: 'Logix Solutions', plan: 'Básico', idTransaccion: 'TN-2901', fecha: '01 Jun 2026', fechaObj: new Date('2026-06-01'), monto: 120, estado: 'vencido' },
		{ id: 4, tenant: 'Nebula Corp', plan: 'Enterprise', idTransaccion: 'TN-8765', fecha: '30 May 2026', fechaObj: new Date('2026-05-30'), monto: 1200, estado: 'pagado' },
		{ id: 5, tenant: 'Blue Ocean Ltd', plan: 'Pro Pack', idTransaccion: 'TN-3321', fecha: '28 May 2026', fechaObj: new Date('2026-05-28'), monto: 450, estado: 'pagado' },
		{ id: 6, tenant: 'Solaris Tech', plan: 'Básico', idTransaccion: 'TN-2109', fecha: '26 May 2026', fechaObj: new Date('2026-05-26'), monto: 120, estado: 'pendiente' },
		{ id: 7, tenant: 'Quantum Labs', plan: 'Enterprise', idTransaccion: 'TN-7654', fecha: '24 May 2026', fechaObj: new Date('2026-05-24'), monto: 1200, estado: 'pagado' },
		{ id: 8, tenant: 'Apex Digital', plan: 'Pro Pack', idTransaccion: 'TN-6543', fecha: '22 May 2026', fechaObj: new Date('2026-05-22'), monto: 450, estado: 'vencido' },
		{ id: 9, tenant: 'Vertex Systems', plan: 'Básico', idTransaccion: 'TN-5432', fecha: '20 May 2026', fechaObj: new Date('2026-05-20'), monto: 120, estado: 'pagado' },
		{ id: 10, tenant: 'Crimson Soft', plan: 'Enterprise', idTransaccion: 'TN-4321', fecha: '18 May 2026', fechaObj: new Date('2026-05-18'), monto: 1200, estado: 'pendiente' },
		{ id: 11, tenant: 'Ironclad Inc', plan: 'Pro Pack', idTransaccion: 'TN-3210', fecha: '16 May 2026', fechaObj: new Date('2026-05-16'), monto: 450, estado: 'pagado' },
		{ id: 12, tenant: 'Fusion Tech', plan: 'Básico', idTransaccion: 'TN-2108', fecha: '14 May 2026', fechaObj: new Date('2026-05-14'), monto: 120, estado: 'pagado' },
		{ id: 13, tenant: 'Pinnacle Group', plan: 'Enterprise', idTransaccion: 'TN-1098', fecha: '12 May 2026', fechaObj: new Date('2026-05-12'), monto: 1200, estado: 'vencido' },
		{ id: 14, tenant: 'NorthStar SA', plan: 'Pro Pack', idTransaccion: 'TN-0987', fecha: '10 May 2026', fechaObj: new Date('2026-05-10'), monto: 450, estado: 'pagado' },
		{ id: 15, tenant: 'Echo Dynamics', plan: 'Básico', idTransaccion: 'TN-0876', fecha: '08 May 2026', fechaObj: new Date('2026-05-08'), monto: 120, estado: 'pendiente' },
		{ id: 16, tenant: 'Titan Healthcare', plan: 'Enterprise', idTransaccion: 'TN-7722', fecha: '06 May 2026', fechaObj: new Date('2026-05-06'), monto: 1200, estado: 'pagado' },
		{ id: 17, tenant: 'OmniStore Peru', plan: 'Pro Pack', idTransaccion: 'TN-6611', fecha: '04 May 2026', fechaObj: new Date('2026-05-04'), monto: 450, estado: 'pagado' },
		{ id: 18, tenant: 'DataVault SAC', plan: 'Básico', idTransaccion: 'TN-5500', fecha: '02 May 2026', fechaObj: new Date('2026-05-02'), monto: 120, estado: 'vencido' },
		{ id: 19, tenant: 'ClearBridge Corp', plan: 'Enterprise', idTransaccion: 'TN-4499', fecha: '29 Abr 2026', fechaObj: new Date('2026-04-29'), monto: 1200, estado: 'pagado' },
		{ id: 20, tenant: 'Andean Tech', plan: 'Pro Pack', idTransaccion: 'TN-3388', fecha: '26 Abr 2026', fechaObj: new Date('2026-04-26'), monto: 450, estado: 'pendiente' },
		{ id: 21, tenant: 'Pacific Solutions', plan: 'Básico', idTransaccion: 'TN-2277', fecha: '23 Abr 2026', fechaObj: new Date('2026-04-23'), monto: 120, estado: 'pagado' },
		{ id: 22, tenant: 'Sigma Consulting', plan: 'Enterprise', idTransaccion: 'TN-1166', fecha: '20 Abr 2026', fechaObj: new Date('2026-04-20'), monto: 1200, estado: 'vencido' },
		{ id: 23, tenant: 'GreenField SAC', plan: 'Pro Pack', idTransaccion: 'TN-0055', fecha: '17 Abr 2026', fechaObj: new Date('2026-04-17'), monto: 450, estado: 'pagado' },
		{ id: 24, tenant: 'Nexus Group', plan: 'Básico', idTransaccion: 'TN-9944', fecha: '14 Abr 2026', fechaObj: new Date('2026-04-14'), monto: 120, estado: 'pagado' },
		{ id: 25, tenant: 'HighCloud Labs', plan: 'Enterprise', idTransaccion: 'TN-8833', fecha: '11 Abr 2026', fechaObj: new Date('2026-04-11'), monto: 1200, estado: 'pendiente' },
		{ id: 26, tenant: 'Zenit Systems', plan: 'Pro Pack', idTransaccion: 'TN-7721', fecha: '08 Abr 2026', fechaObj: new Date('2026-04-08'), monto: 450, estado: 'pagado' },
		{ id: 27, tenant: 'Lima Digital SAC', plan: 'Básico', idTransaccion: 'TN-6610', fecha: '05 Abr 2026', fechaObj: new Date('2026-04-05'), monto: 120, estado: 'vencido' },
		{ id: 28, tenant: 'Alpha Consulting', plan: 'Enterprise', idTransaccion: 'TN-5509', fecha: '02 Abr 2026', fechaObj: new Date('2026-04-02'), monto: 1200, estado: 'pagado' },
		{ id: 29, tenant: 'RedWood Corp', plan: 'Pro Pack', idTransaccion: 'TN-4408', fecha: '30 Mar 2026', fechaObj: new Date('2026-03-30'), monto: 450, estado: 'pagado' },
		{ id: 30, tenant: 'StarLight SAC', plan: 'Básico', idTransaccion: 'TN-3307', fecha: '27 Mar 2026', fechaObj: new Date('2026-03-27'), monto: 120, estado: 'pendiente' },
		{ id: 31, tenant: 'Costa Verde Tech', plan: 'Enterprise', idTransaccion: 'TN-2206', fecha: '24 Mar 2026', fechaObj: new Date('2026-03-24'), monto: 1200, estado: 'pagado' },
		{ id: 32, tenant: 'MegaCloud Peru', plan: 'Pro Pack', idTransaccion: 'TN-1105', fecha: '21 Mar 2026', fechaObj: new Date('2026-03-21'), monto: 450, estado: 'vencido' },
	];

	readonly pagina = signal(1);
	readonly pageSize = 5;
	transaccionSeleccionada = signal<Transaccion | null>(null);

	get mrr(): string { return 'S/. 245,820.00'; }
	get cambioMrr(): string { return '+12.4%'; }
	get churn(): string { return '1.8%'; }
	get cambioChurn(): string { return '+0.2%'; }
	get pendienteCobro(): string { return 'S/. 12,450.00'; }
	get facturasVencidas(): number { return 14; }
	get arpu(): string { return 'S/. 158.50'; }

	readonly meses = ['ENE', 'FEB', 'MAR', 'ABR', 'MAY', 'JUN'];
	readonly ingresosMensuales = [185400, 198200, 210500, 225800, 238100, 245820];

	readonly chartConfig = {
		chart: {
			type: 'bar' as const,
			height: 320,
			toolbar: { show: false },
			fontFamily: 'Inter, system-ui, sans-serif',
			animations: { enabled: true, speed: 500, animateGradually: { enabled: true, delay: 100 } },
			parentHeightOffset: 0,
		},
		series: [{
			name: 'Facturación',
			data: this.ingresosMensuales,
		}],
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
			style: {
				fontSize: '12px',
				fontWeight: 700,
				colors: ['#1e293b'],
				fontFamily: 'Inter, sans-serif',
			},
		},
		xaxis: {
			categories: this.meses,
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
			y: {
				formatter: (v: number) => `S/${v.toLocaleString('es-PE')}`,
			},
			style: { fontFamily: 'Inter, sans-serif', fontSize: '13px' },
		},
		fill: { opacity: 0.85 },
		stroke: { show: false },
	};

	readonly transaccionesFiltradas = computed(() => {
		const fv = this.filterValues();
		let list = [...this.transacciones];

		if (fv.estado && fv.estado !== 'todos') {
			list = list.filter(t => t.estado === fv.estado);
		}

		if (fv.rango === 'ultimos-30') {
			const treintaDiasAtras = new Date();
			treintaDiasAtras.setDate(treintaDiasAtras.getDate() - 30);
			list = list.filter(t => t.fechaObj >= treintaDiasAtras);
		} else if (fv.rango === 'personalizado' && fv.fechaDesde && fv.fechaHasta) {
			const desde = new Date(fv.fechaDesde);
			const hasta = new Date(fv.fechaHasta);
			hasta.setHours(23, 59, 59, 999);
			list = list.filter(t => t.fechaObj >= desde && t.fechaObj <= hasta);
		}

		return list;
	});

	readonly transaccionesPaginadas = computed(() => {
		const start = (this.pagina() - 1) * this.pageSize;
		return this.transaccionesFiltradas().slice(start, start + this.pageSize);
	});

	readonly totalPaginas = computed(() =>
		Math.max(1, Math.ceil(this.transaccionesFiltradas().length / this.pageSize))
	);

	readonly paginasNum = computed(() =>
		Array.from({ length: this.totalPaginas() }, (_, i) => i + 1)
	);

	readonly totalTransacciones = computed(() => this.transaccionesFiltradas().length);

	readonly rangoInfo = computed(() => {
		if (this.totalTransacciones() === 0) return 'Mostrando 0 transacciones';
		const start = (this.pagina() - 1) * this.pageSize + 1;
		const end = Math.min(this.pagina() * this.pageSize, this.totalTransacciones());
		return `Mostrando ${start}\u2013${end} de ${this.totalTransacciones()} transacciones`;
	});

	irPagina(p: number): void { this.pagina.set(p); }

	cambiarPagina(delta: number): void {
		const next = this.pagina() + delta;
		if (next >= 1 && next <= this.totalPaginas()) this.pagina.set(next);
	}

	onRangoChange(): void {
		this.pagina.set(1);
		if (this.filterForm.value.rango !== 'personalizado') {
			this.filterForm.patchValue({ fechaDesde: '', fechaHasta: '' });
		}
	}

	onEstadoChange(): void { this.pagina.set(1); }

  verDetalle(t: Transaccion): void {
    this.transaccionSeleccionada.set(t);
    this.modalService.show('modalDetalleTransaccion');
  }

  exportarCSV(): void {
    const headers = ['Tenant', 'Plan', 'ID Transacción', 'Fecha', 'Monto', 'Estado'];
    const rows = this.transaccionesFiltradas().map(t =>
      [t.tenant, t.plan, t.idTransaccion, t.fecha, `S/${t.monto.toFixed(2)}`, t.estado]
    );
    this.exportService.exportCSV(`transacciones-${new Date().toISOString().split('T')[0]}.csv`, headers, rows);
    this.mostrarToast('CSV exportado correctamente.');
  }

  private mostrarToast(message: string): void {
    this.toastService.info(message);
  }
}
