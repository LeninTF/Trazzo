import { Component, inject, signal } from '@angular/core';
import { SaasService } from '../../../api/services/saas.service';
import type { SaasPlanResult } from '../../../api/types';

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

export interface PlanCard {
  plan: SaasPlanResult;
  features: string[];
  destacado: boolean;
}

@Component({
  selector: 'app-index',
  imports: [],
  templateUrl: './index.html',
  styleUrl: './index.css',
})
export class Index {
  private readonly saasService = inject(SaasService);

  readonly loading = signal(false);
  readonly planCards = signal<PlanCard[]>([]);

  constructor() {
    this.cargarPlanes();
  }

  private cargarPlanes(): void {
    this.loading.set(true);
    this.saasService.listPublicPlans().subscribe({
      next: planes => {
        const ordenados = [...planes].sort((a, b) => a.price - b.price);
        const destacadoIndex = ordenados.length >= 2 ? Math.floor((ordenados.length - 1) / 2) : -1;
        this.planCards.set(ordenados.map((plan, i) => ({
          plan,
          features: this.featureLabels(plan),
          destacado: i === destacadoIndex,
        })));
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  private featureLabels(plan: SaasPlanResult): string[] {
    const f = plan.features ?? {};
    const labels: string[] = [];
    if (f['max_trabajadores'] != null) labels.push(`Hasta ${f['max_trabajadores']} usuarios`);
    if (f['max_sedes'] != null) labels.push(`Hasta ${f['max_sedes']} sedes`);
    if (f['almacenamiento_gb'] != null) labels.push(`${f['almacenamiento_gb']}GB de almacenamiento`);
    for (const [id, label] of Object.entries(FEATURE_LABELS)) {
      if (f[id]) labels.push(label);
    }
    return labels;
  }
}
