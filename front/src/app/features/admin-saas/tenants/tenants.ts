import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastService } from '../../../services/toast.service';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { ApiService } from '../../../api/services/api.service';
import type { TenantSaasProfile, TenantMetrics, HoldingProfile, SaasPlanResult, TenantStatus } from '../../../api/types';

const PAGE_SIZE = 10;

const ESTADO_LABELS: Record<TenantStatus, string> = {
  TRIAL: 'Trial',
  ACTIVE: 'Activo',
  SUSPENDED: 'Suspendido',
};

// Same catalog of plan-feature ids used by the SaaS Gestor de Planes / public pricing page —
// duplicated intentionally (matches the existing modulosDisponibles pattern) rather than a
// premature shared abstraction across independently-evolving screens.
const FEATURE_LABELS: Record<string, string> = {
  reportes: 'Reportes Avanzados',
  'api-externa': 'API Externa',
  'facturacion-auto': 'Facturación Auto',
  'control-huella': 'Control por Huella',
  'escaneo-codigo': 'Escaneo de Código',
  'soporte-24-7': 'Soporte 24/7',
  'multi-sede': 'Multi-sede',
  'trial-gratuito': 'Trial Gratuito',
  'facturacion-publica': 'Facturación Pública',
};

interface NuevoTenantForm {
  subDomain: string;
  holdingId: number | null;
  planId: number | null;
  logoUrl: string;
  slogan: string;
  primaryColor: string;
  secondaryColor: string;
}

interface BrandingForm {
  logoUrl: string;
  slogan: string;
  primaryColor: string;
  secondaryColor: string;
}

@Component({
  selector: 'app-tenants',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './tenants.html',
  styleUrl: './tenants.css',
})
export class Tenants implements OnInit {
  readonly loading = signal(false);
  readonly error = signal('');

  private readonly api = inject(ApiService);
  private readonly toastService = inject(ToastService);

  readonly estadoLabels = ESTADO_LABELS;
  readonly estadosDisponibles: TenantStatus[] = ['TRIAL', 'ACTIVE', 'SUSPENDED'];

  tenants = signal<TenantSaasProfile[]>([]);
  totalElements = signal(0);
  totalPaginas = signal(1);
  paginaActual = signal(1);

  metricas = signal<TenantMetrics | null>(null);
  planesDisponibles = signal<SaasPlanResult[]>([]);
  holdingsDisponibles = signal<HoldingProfile[]>([]);

  searchTerm = '';
  filtroPlan: number | null = null;
  filtroEstado: TenantStatus | '' = '';

  modalTenantOpen = signal(false);
  modalDetalleOpen = signal(false);
  modalBrandingOpen = signal(false);
  modalConfirmarSuspensionOpen = signal(false);

  tenantSeleccionado = signal<TenantSaasProfile | null>(null);
  planFeaturesSeleccionado = signal<string[]>([]);

  nuevoTenantForm: NuevoTenantForm = this.formVacio();
  brandingForm: BrandingForm = { logoUrl: '', slogan: '', primaryColor: '', secondaryColor: '' };

  ngOnInit(): void {
    this.api.saas.listActivePlans().subscribe({ next: planes => this.planesDisponibles.set(planes) });
    this.api.saas.listHoldings().subscribe({ next: holdings => this.holdingsDisponibles.set(holdings) });
    this.cargarTenants();
    this.cargarMetricas();
  }

  cargarTenants(): void {
    this.loading.set(true);
    this.api.tenants.list({
      search: this.searchTerm || undefined,
      planId: this.filtroPlan ?? undefined,
      status: this.filtroEstado || undefined,
      page: this.paginaActual() - 1,
      size: PAGE_SIZE,
    }).subscribe({
      next: res => {
        this.tenants.set(res.content);
        this.totalElements.set(res.totalElements);
        this.totalPaginas.set(Math.max(1, res.totalPages));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los tenants.');
        this.loading.set(false);
      },
    });
  }

  cargarMetricas(): void {
    this.api.tenants.getMetrics().subscribe({ next: m => this.metricas.set(m) });
  }

  filtrarTenants(): void {
    this.paginaActual.set(1);
    this.cargarTenants();
  }

  cambiarPagina(pagina: number): void {
    if (pagina < 1 || pagina > this.totalPaginas()) return;
    this.paginaActual.set(pagina);
    this.cargarTenants();
  }

  private formVacio(): NuevoTenantForm {
    return { subDomain: '', holdingId: null, planId: null, logoUrl: '', slogan: '', primaryColor: '#163A96', secondaryColor: '#ffffff' };
  }

  abrirModalAgregar(): void {
    this.nuevoTenantForm = this.formVacio();
    this.modalTenantOpen.set(true);
  }

  cerrarModalTenant(): void {
    this.modalTenantOpen.set(false);
  }

  guardarNuevoTenant(): void {
    const f = this.nuevoTenantForm;
    if (!f.subDomain.trim() || !f.holdingId || !f.planId) {
      this.toastService.error('Complete los campos obligatorios.');
      return;
    }
    this.api.tenants.createTrial({
      subDomain: f.subDomain.trim(),
      holdingId: f.holdingId,
      planId: f.planId,
      logoUrl: f.logoUrl || undefined,
      slogan: f.slogan || undefined,
      primaryColor: f.primaryColor || undefined,
      secondaryColor: f.secondaryColor || undefined,
    }).subscribe({
      next: () => {
        this.toastService.success('Tenant registrado correctamente.');
        this.cerrarModalTenant();
        this.cargarTenants();
        this.cargarMetricas();
      },
      error: () => this.toastService.error('No se pudo registrar el tenant.'),
    });
  }

  abrirModalDetalle(tenant: TenantSaasProfile): void {
    this.tenantSeleccionado.set(tenant);
    this.modalDetalleOpen.set(true);
    this.planFeaturesSeleccionado.set([]);
    this.api.saas.getPlan(tenant.planId).subscribe({
      next: plan => this.planFeaturesSeleccionado.set(this.featureLabels(plan.features)),
    });
  }

  cerrarModalDetalle(): void {
    this.modalDetalleOpen.set(false);
    this.tenantSeleccionado.set(null);
  }

  abrirModalBranding(tenant: TenantSaasProfile): void {
    this.tenantSeleccionado.set(tenant);
    this.brandingForm = { logoUrl: '', slogan: '', primaryColor: '', secondaryColor: '' };
    this.modalBrandingOpen.set(true);
  }

  cerrarModalBranding(): void {
    this.modalBrandingOpen.set(false);
    this.tenantSeleccionado.set(null);
  }

  guardarBranding(): void {
    const tenant = this.tenantSeleccionado();
    if (!tenant) return;
    const f = this.brandingForm;
    this.api.tenants.updateBranding(tenant.id, {
      logoUrl: f.logoUrl || undefined,
      slogan: f.slogan || undefined,
      primaryColor: f.primaryColor || undefined,
      secondaryColor: f.secondaryColor || undefined,
    }).subscribe({
      next: () => {
        this.toastService.success('Branding actualizado.');
        this.cerrarModalBranding();
        this.cargarTenants();
      },
      error: () => this.toastService.error('No se pudo actualizar el branding.'),
    });
  }

  confirmarSuspender(tenant: TenantSaasProfile): void {
    this.tenantSeleccionado.set(tenant);
    this.modalConfirmarSuspensionOpen.set(true);
  }

  cerrarModalConfirmarSuspension(): void {
    this.modalConfirmarSuspensionOpen.set(false);
    this.tenantSeleccionado.set(null);
  }

  ejecutarSuspension(): void {
    const tenant = this.tenantSeleccionado();
    if (!tenant) return;
    this.api.tenants.suspend(tenant.id).subscribe({
      next: () => {
        this.toastService.success(`"${tenant.subDomain}" suspendido correctamente.`);
        this.cerrarModalConfirmarSuspension();
        this.cargarTenants();
        this.cargarMetricas();
      },
      error: () => this.toastService.error('No se pudo suspender el tenant.'),
    });
  }

  reactivarTenant(tenant: TenantSaasProfile): void {
    this.api.tenants.reactivate(tenant.id).subscribe({
      next: () => {
        this.toastService.success(`"${tenant.subDomain}" reactivado correctamente.`);
        this.cargarTenants();
        this.cargarMetricas();
      },
      error: () => this.toastService.error('No se pudo reactivar el tenant.'),
    });
  }

  eliminarTenant(tenant: TenantSaasProfile): void {
    this.api.tenants.deleteById(tenant.id).subscribe({
      next: () => {
        this.toastService.success(`"${tenant.subDomain}" eliminado correctamente.`);
        this.cargarTenants();
        this.cargarMetricas();
      },
      error: () => this.toastService.error('No se pudo eliminar el tenant.'),
    });
  }

  iniciales(tenant: TenantSaasProfile): string {
    const base = tenant.holdingName ?? tenant.subDomain;
    return base.split(/[\s-]+/).filter(Boolean).map(w => w[0]).join('').toUpperCase().substring(0, 2);
  }

  colorFor(tenant: TenantSaasProfile): string {
    let hash = 0;
    for (const ch of tenant.id) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0;
    return `hsl(${hash % 360}, 55%, 45%)`;
  }

  mesesDesde(fechaIso: string): number {
    const meses = (Date.now() - new Date(fechaIso).getTime()) / (1000 * 60 * 60 * 24 * 30);
    return Math.max(0, Math.floor(meses));
  }

  private featureLabels(features: Record<string, number | boolean>): string[] {
    const labels: string[] = [];
    if (features['max_trabajadores'] != null) labels.push(`Hasta ${features['max_trabajadores']} usuarios`);
    if (features['max_sedes'] != null) labels.push(`Hasta ${features['max_sedes']} sedes`);
    if (features['almacenamiento_gb'] != null) labels.push(`${features['almacenamiento_gb']}GB de almacenamiento`);
    for (const [id, label] of Object.entries(FEATURE_LABELS)) {
      if (features[id]) labels.push(label);
    }
    return labels;
  }
}
