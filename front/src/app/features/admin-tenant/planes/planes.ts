import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { ToastService } from '../../../services/toast.service';
import { ApiService } from '../../../api/services/api.service';
import type { SaasPlanResult, InvoiceProfile } from '../../../api/types';

const ESTADO_LABELS: Record<string, string> = {
  PENDIENTE: 'Pendiente',
  COMPLETADO: 'Pagada',
  FALLIDO: 'Fallida',
  REEMBOLSADO: 'Reembolsada',
};

@Component({
  selector: 'app-planes',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './planes.html',
  styleUrl: './planes.css',
})
export class Planes {

  readonly loading = signal(false);
  readonly error = signal('');

  private readonly toastService = inject(ToastService);
  private readonly api = inject(ApiService);

  planActual = signal<SaasPlanResult | null>(null);
  planes = signal<SaasPlanResult[]>([]);
  facturas = signal<InvoiceProfile[]>([]);
  usuariosActivos = signal(0);

  planSeleccionadoId: number | null = null;
  facturaSeleccionadaId: string | null = null;

  modalActualizarOpen = false;
  modalFacturaOpen = false;

  readonly estadoLabels = ESTADO_LABELS;

  constructor() {
    this.cargarDatos();
  }

  private cargarDatos(): void {
    this.loading.set(true);
    this.error.set('');

    forkJoin({
      planActual: this.api.org.getMyPlan(),
      planes: this.api.org.listAvailablePlans(),
      facturas: this.api.org.listMyInvoices({ size: 10 }),
      usuarios: this.api.users.list({ page: 1, size: 1 }),
    }).subscribe({
      next: ({ planActual, planes, facturas, usuarios }) => {
        this.planActual.set(planActual);
        this.planes.set(planes);
        this.facturas.set(facturas.content);
        this.usuariosActivos.set(usuarios.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la información de planes y facturación.');
        this.loading.set(false);
      },
    });
  }

  get planSeleccionado(): SaasPlanResult | undefined {
    if (this.planSeleccionadoId === null) return undefined;
    return this.planes().find(p => p.id === this.planSeleccionadoId);
  }

  get facturaSeleccionada(): InvoiceProfile | undefined {
    if (!this.facturaSeleccionadaId) return undefined;
    return this.facturas().find(f => f.id === this.facturaSeleccionadaId);
  }

  get limiteUsuarios(): number | null {
    const max = this.planActual()?.features?.['max_trabajadores'];
    return typeof max === 'number' ? max : null;
  }

  get porcentajeUsuarios(): number | null {
    const limite = this.limiteUsuarios;
    if (!limite) return null;
    return Math.round((this.usuariosActivos() / limite) * 100);
  }

  featureLabel(plan: SaasPlanResult): string[] {
    const f = plan.features ?? {};
    const labels: string[] = [];
    if (f['max_trabajadores'] != null) labels.push(`Hasta ${f['max_trabajadores']} empleados`);
    if (f['max_sedes'] != null) labels.push(`Hasta ${f['max_sedes']} sedes`);
    if (f['almacenamiento_gb'] != null) labels.push(`${f['almacenamiento_gb']} GB de almacenamiento`);
    return labels;
  }

  seleccionarPlan(plan: SaasPlanResult): void {
    this.planSeleccionadoId = plan.id;
  }

  seleccionarFactura(factura: InvoiceProfile): void {
    this.facturaSeleccionadaId = factura.id;
    this.modalFacturaOpen = true;
  }

  abrirModalActualizarPlan(): void {
    if (this.planSeleccionadoId === null) {
      const alternativo = this.planes().find(p => p.id !== this.planActual()?.id);
      this.planSeleccionadoId = alternativo?.id ?? this.planActual()?.id ?? null;
    }
    this.modalActualizarOpen = true;
  }

  cerrarModalActualizar(): void {
    this.modalActualizarOpen = false;
  }

  confirmarActualizarPlan(): void {
    this.toastService.info('El cambio de plan aún no está disponible en autoservicio. Contacta a soporte para actualizar tu suscripción.');
    this.cerrarModalActualizar();
  }

  cerrarModalFactura(): void {
    this.modalFacturaOpen = false;
    this.facturaSeleccionadaId = null;
  }

  exportarFacturas(): void {
    this.toastService.info('La exportación de facturas aún no está disponible.');
  }

  descargarFactura(): void {
    this.toastService.info('La descarga de facturas aún no está disponible.');
  }
}
