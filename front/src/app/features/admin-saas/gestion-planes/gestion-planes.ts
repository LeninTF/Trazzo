import { Component, computed, signal, inject, OnInit } from '@angular/core';
import { FormBuilder, AbstractControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { ToastService } from '../../../services/toast.service';
import { ExportService } from '../../../services/export.service';
import { ModalService } from '../../../services/modal.service';
import { SaasService } from '../../../api/services/saas.service';

interface Modulo {
  id: string;
  label: string;
  icono: string;
}

interface Regla {
  id: string;
  label: string;
}

interface Plan {
  id: number;
  nombre: string;
  sku: string;
  precio: number;
  precioMensual: number;
  precioAnual: number;
  descripcion: string;
  maxTrabajadores: number;
  maxSedes: number;
  almacenamiento: number;
  sincronizacionNube: boolean;
  modulos: string[];
  reglas: string[];
  moneda: string;
  periodo: string;
  activo: boolean;
  creadoEn: string;
  ultimaModificacion: { usuario: string; fecha: string };
}

interface Suscripcion {
  id: number;
  tenant: string;
  plan: string;
  fechaInicio: string;
  fechaFin: string;
  monto: number;
  estado: 'activo' | 'vencido' | 'pendiente';
}

@Component({
  selector: 'app-gestion-planes',
  standalone: true,
  imports: [ReactiveFormsModule, PaginationComponent],
  templateUrl: './gestion-planes.html',
  styleUrl: './gestion-planes.css',
})
export class GestionPlanes implements OnInit {
  readonly loading = signal(false);
  readonly error = signal('');

  private readonly fb = new FormBuilder();
  private readonly toastService = inject(ToastService);
  private readonly exportService = inject(ExportService);
  private readonly modalService = inject(ModalService);
  private readonly saasService = inject(SaasService);

  readonly modulosDisponibles: Modulo[] = [
    { id: 'reportes', label: 'Reportes Avanzados', icono: 'bi-bar-chart' },
    { id: 'api-externa', label: 'API Externa', icono: 'bi-plug' },
    { id: 'facturacion-auto', label: 'Facturación Auto', icono: 'bi-receipt' },
    { id: 'control-huella', label: 'Control por Huella', icono: 'bi-fingerprint' },
    { id: 'escaneo-codigo', label: 'Escaneo de Código', icono: 'bi-upc-scan' },
    { id: 'soporte-24-7', label: 'Soporte 24/7', icono: 'bi-headset' },
  ];

  readonly reglasDisponibles: Regla[] = [
    { id: 'multi-sede', label: 'Multi-sede' },
    { id: 'trial-gratuito', label: 'Trial Gratuito' },
    { id: 'facturacion-publica', label: 'Facturación Pública' },
  ];

  editando = signal(false);
  planSeleccionado = signal<Plan | null>(null);
  editPlanId = signal<number | null>(null);
  mostrarFormNuevo = signal(false);

  suscripcionPagina = signal(1);
  readonly suscripcionPageSize = 5;
  suscripcionSeleccionada = signal<Suscripcion | null>(null);

  readonly planes = signal<Plan[]>([]);

  readonly suscripciones: Suscripcion[] = [];

  readonly planForm = this.fb.group({
    nombre:           ['', [Validators.required, Validators.minLength(3)]],
    sku:              ['', Validators.required],
    precioMensual:    [0, [Validators.required, Validators.min(0.01)]],
    precioAnual:      [0, [Validators.required, Validators.min(0.01)]],
    moneda:           ['SOLES', Validators.required],
    periodo:          ['MONTHLY', Validators.required],
    descripcion:      [''],
    maxTrabajadores:  [100, Validators.min(1)],
    maxSedes:         [1, Validators.min(0)],
    almacenamiento:   [5, Validators.min(1)],
    sincronizacionNube: [false],
    modulos: this.fb.group({
      'reportes':          [false],
      'api-externa':       [false],
      'facturacion-auto':  [false],
      'control-huella':    [false],
      'escaneo-codigo':    [false],
      'soporte-24-7':      [false],
    }),
    reglas: this.fb.group({
      'multi-sede':           [false],
      'trial-gratuito':       [false],
      'facturacion-publica':  [false],
    }),
  });

  get modulosForm(): AbstractControl { return this.planForm.get('modulos')!; }

  readonly precioFinal = computed(() => ({
    mensual: this.planForm.get('precioMensual')?.value ?? 0,
    anual:   this.planForm.get('precioAnual')?.value ?? 0,
  }));

  readonly modulosActivos = computed(() =>
    this.modulosDisponibles.filter(m => this.planForm.get('modulos.' + m.id)?.value)
  );

  readonly reglasActivas = computed(() =>
    this.reglasDisponibles.filter(r => this.planForm.get('reglas.' + r.id)?.value)
  );

  readonly suscripcionesPaginadas = computed(() => {
    const start = (this.suscripcionPagina() - 1) * this.suscripcionPageSize;
    return this.suscripciones.slice(start, start + this.suscripcionPageSize);
  });

  readonly totalSuscripcionPaginas = computed(() =>
    Math.max(1, Math.ceil(this.suscripciones.length / this.suscripcionPageSize))
  );

  readonly suscripcionPaginasNum = computed(() =>
    Array.from({ length: this.totalSuscripcionPaginas() }, (_, i) => i + 1)
  );

  ngOnInit(): void {
    this.cargarPlanes();
  }

  cargarPlanes(): void {
    this.loading.set(true);
    this.saasService.listPlans().subscribe({
      next: plans => {
        this.planes.set(plans.map(p => ({
          id: p.id,
          nombre: p.name,
          sku: `PLAN-${String(p.id).padStart(3, '0')}`,
          precio: Number(p.price),
          precioMensual: p.billingPeriod === 'MONTHLY' ? Number(p.price) : Number(p.price) / 12,
          precioAnual: p.billingPeriod === 'ANNUAL' ? Number(p.price) : Number(p.price) * 12,
          descripcion: '',
          maxTrabajadores: 100,
          maxSedes: 1,
          almacenamiento: 5,
          sincronizacionNube: false,
          modulos: [],
          reglas: [],
          moneda: p.currency,
          periodo: p.billingPeriod,
          activo: p.active,
          creadoEn: p.createdAt,
          ultimaModificacion: { usuario: 'Sistema', fecha: p.createdAt },
        })));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar los planes');
        this.loading.set(false);
      },
    });
  }

  seleccionarPlan(plan: Plan): void {
    this.editPlanId.set(plan.id);
    this.mostrarFormNuevo.set(false);
    this.editando.set(true);
    this.planSeleccionado.set(plan);
    this.planForm.patchValue({
      nombre:          plan.nombre,
      sku:             plan.sku,
      precioMensual:   plan.precioMensual,
      precioAnual:     plan.precioAnual,
      moneda:          plan.moneda,
      periodo:         plan.periodo,
      descripcion:     plan.descripcion,
      maxTrabajadores: plan.maxTrabajadores,
      maxSedes:        plan.maxSedes,
      almacenamiento:  plan.almacenamiento,
      sincronizacionNube: plan.sincronizacionNube,
    });
  }

  nuevoPlan(): void {
    this.editPlanId.set(null);
    this.mostrarFormNuevo.set(true);
    this.editando.set(true);
    this.planSeleccionado.set(null);
    this.planForm.reset({ nombre: '', sku: '', precioMensual: 0, precioAnual: 0, moneda: 'SOLES', periodo: 'MONTHLY', descripcion: '', maxTrabajadores: 100, maxSedes: 1, almacenamiento: 5, sincronizacionNube: false });
  }

  cancelarEdicion(): void {
    this.editando.set(false);
    this.mostrarFormNuevo.set(false);
    this.editPlanId.set(null);
    this.planSeleccionado.set(null);
  }

  guardarPlan(): void {
    if (this.planForm.invalid) {
      this.planForm.markAllAsTouched();
      this.toastService.show('Corrige los errores antes de guardar.', 'error');
      return;
    }
    const v = this.planForm.value;
    const isMonthly = v.periodo === 'MONTHLY';
    const price = isMonthly ? (v.precioMensual ?? 0) : (v.precioAnual ?? 0);
    const body = {
      name: v.nombre!,
      price,
      currency: v.moneda ?? 'SOLES',
      billingPeriod: v.periodo ?? 'MONTHLY',
    };

    this.loading.set(true);
    const id = this.editPlanId();
    const req$ = id
      ? this.saasService.updatePlan(id, { id, ...body })
      : this.saasService.createPlan(body);

    req$.subscribe({
      next: () => {
        this.toastService.show(id ? 'Plan actualizado.' : 'Plan creado.', 'success');
        this.cancelarEdicion();
        this.cargarPlanes();
      },
      error: () => {
        this.toastService.show('Error al guardar el plan.', 'error');
        this.loading.set(false);
      },
    });
  }

  confirmarEliminar(): void {
    if (this.editPlanId()) this.modalService.show('modalEliminarPlan');
  }

  eliminarPlan(): void {
    const id = this.editPlanId();
    if (!id) return;
    this.saasService.deletePlan(id).subscribe({
      next: () => {
        this.cerrarModal('modalEliminarPlan');
        this.toastService.show('Plan eliminado.', 'success');
        this.cancelarEdicion();
        this.cargarPlanes();
      },
      error: () => this.toastService.show('Error al eliminar el plan.', 'error'),
    });
  }

  toggleActivo(plan: Plan): void {
    const req$ = plan.activo
      ? this.saasService.deactivatePlan(plan.id)
      : this.saasService.activatePlan(plan.id);
    req$.subscribe({
      next: () => this.cargarPlanes(),
      error: () => this.toastService.show('Error al cambiar estado del plan.', 'error'),
    });
  }

  irPaginaSuscripcion(p: number): void {
    this.suscripcionPagina.set(p);
  }

  cambiarPaginaSuscripcion(delta: number): void {
    const next = this.suscripcionPagina() + delta;
    if (next >= 1 && next <= this.totalSuscripcionPaginas()) {
      this.suscripcionPagina.set(next);
    }
  }

  verDetalleSuscripcion(s: Suscripcion): void {
    this.suscripcionSeleccionada.set(s);
    this.modalService.show('modalDetalleSuscripcion');
  }

  exportarCSV(): void {
    const headers = ['ID', 'Nombre', 'Precio', 'Moneda', 'Periodo', 'Activo', 'Creado'];
    const rows = this.planes().map(p =>
      [String(p.id), p.nombre, String(p.precio), p.moneda, p.periodo, p.activo ? 'Sí' : 'No', p.creadoEn]
    );
    this.exportService.exportCSV(`planes-${new Date().toISOString().split('T')[0]}.csv`, headers, rows);
    this.toastService.show('CSV exportado.', 'info');
  }

  private cerrarModal(id: string): void {
    this.modalService.hide(id);
  }
}
