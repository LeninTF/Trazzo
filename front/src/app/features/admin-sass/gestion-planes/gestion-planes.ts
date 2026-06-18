import { Component, computed, signal, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { ToastService } from '../../../services/toast.service';
import { ExportService } from '../../../services/export.service';
import { ModalService } from '../../../services/modal.service';

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
  precioMensual: number;
  precioAnual: number;
  descripcion: string;
  maxTrabajadores: number;
  maxSedes: number;
  almacenamiento: number;
  sincronizacionNube: boolean;
  modulos: string[];
  reglas: string[];
  activo: boolean;
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
export class GestionPlanes {
  private readonly fb = new FormBuilder();
  private readonly toastService = inject(ToastService);
  private readonly exportService = inject(ExportService);
  private readonly modalService = inject(ModalService);

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

  readonly planes = signal<Plan[]>([
    {
      id: 1, nombre: 'Básico', sku: 'PLAN-BAS-001',
      precioMensual: 29, precioAnual: 290,
      descripcion: 'Perfecto para pequeñas instituciones que inician su transformación digital.',
      maxTrabajadores: 50, maxSedes: 1, almacenamiento: 10, sincronizacionNube: false,
      modulos: [], reglas: ['trial-gratuito'],
      activo: true, ultimaModificacion: { usuario: 'Jose Alata', fecha: '2026-05-28' },
    },
    {
      id: 2, nombre: 'Profesional', sku: 'PLAN-PRO-001',
      precioMensual: 79, precioAnual: 790,
      descripcion: 'Para instituciones en crecimiento que necesitan más control y reportes.',
      maxTrabajadores: 200, maxSedes: 5, almacenamiento: 50, sincronizacionNube: true,
      modulos: ['reportes', 'soporte-24-7'], reglas: ['multi-sede', 'trial-gratuito'],
      activo: true, ultimaModificacion: { usuario: 'Jose Alata', fecha: '2026-06-01' },
    },
    {
      id: 3, nombre: 'Enterprise', sku: 'PLAN-ENT-001',
      precioMensual: 199, precioAnual: 1990,
      descripcion: 'Solución completa para grandes instituciones con requisitos avanzados.',
      maxTrabajadores: 9999, maxSedes: 50, almacenamiento: 500, sincronizacionNube: true,
      modulos: ['reportes', 'api-externa', 'facturacion-auto', 'control-huella', 'escaneo-codigo', 'soporte-24-7'],
      reglas: ['multi-sede', 'trial-gratuito', 'facturacion-publica'],
      activo: true, ultimaModificacion: { usuario: 'Jose Alata', fecha: '2026-06-03' },
    },
  ]);

  readonly suscripciones: Suscripcion[] = [
    { id: 1, tenant: 'Vortex Analytics', plan: 'Enterprise', fechaInicio: '2026-01-15', fechaFin: '2027-01-15', monto: 1990, estado: 'activo' },
    { id: 2, tenant: 'Skyline Global', plan: 'Profesional', fechaInicio: '2026-02-01', fechaFin: '2027-02-01', monto: 790, estado: 'activo' },
    { id: 3, tenant: 'Nova Retail', plan: 'Básico', fechaInicio: '2025-11-20', fechaFin: '2026-11-20', monto: 290, estado: 'activo' },
    { id: 4, tenant: 'TechSolutions', plan: 'Enterprise', fechaInicio: '2025-06-01', fechaFin: '2026-06-01', monto: 1990, estado: 'vencido' },
    { id: 5, tenant: 'DataCore', plan: 'Profesional', fechaInicio: '2025-09-10', fechaFin: '2026-09-10', monto: 790, estado: 'pendiente' },
    { id: 6, tenant: 'InnovaTech', plan: 'Básico', fechaInicio: '2026-03-05', fechaFin: '2027-03-05', monto: 290, estado: 'activo' },
    { id: 7, tenant: 'Global Metrics', plan: 'Enterprise', fechaInicio: '2025-05-15', fechaFin: '2026-05-15', monto: 1990, estado: 'vencido' },
    { id: 8, tenant: 'CloudSync', plan: 'Profesional', fechaInicio: '2026-04-01', fechaFin: '2027-04-01', monto: 790, estado: 'activo' },
  ];

  readonly planForm = this.fb.group({
    nombre: ['', [Validators.required, Validators.minLength(3)]],
    sku: ['', Validators.required],
    precioMensual: [0, [Validators.required, Validators.min(1)]],
    precioAnual: [0, [Validators.required, Validators.min(1)]],
    descripcion: [''],
    maxTrabajadores: [0, [Validators.required, Validators.min(1)]],
    maxSedes: [0, [Validators.required, Validators.min(0)]],
    almacenamiento: [0, [Validators.required, Validators.min(1)]],
    sincronizacionNube: [false],
    modulos: this.fb.group(
      Object.fromEntries(this.modulosDisponibles.map(m => [m.id, [false]]))
    ),
    reglas: this.fb.group(
      Object.fromEntries(this.reglasDisponibles.map(r => [r.id, [false]]))
    ),
  });

  get modulosForm(): FormGroup {
    return this.planForm.get('modulos') as FormGroup;
  }

  get reglasForm(): FormGroup {
    return this.planForm.get('reglas') as FormGroup;
  }

  readonly modulosActivos = computed(() => {
    if (!this.modulosForm) return [];
    return this.modulosDisponibles.filter(m => this.modulosForm.get(m.id)?.value);
  });

  readonly reglasActivas = computed(() => {
    if (!this.reglasForm) return [];
    return this.reglasDisponibles.filter(r => this.reglasForm.get(r.id)?.value);
  });

  readonly precioFinal = computed(() => {
    const mensual = this.planForm.get('precioMensual')?.value || 0;
    const anual = this.planForm.get('precioAnual')?.value || 0;
    return { mensual, anual };
  });

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

  seleccionarPlan(plan: Plan): void {
    this.editPlanId.set(plan.id);
    this.mostrarFormNuevo.set(false);
    this.editando.set(true);
    this.planSeleccionado.set(plan);

    this.planForm.patchValue({
      nombre: plan.nombre,
      sku: plan.sku,
      precioMensual: plan.precioMensual,
      precioAnual: plan.precioAnual,
      descripcion: plan.descripcion,
      maxTrabajadores: plan.maxTrabajadores,
      maxSedes: plan.maxSedes,
      almacenamiento: plan.almacenamiento,
      sincronizacionNube: plan.sincronizacionNube,
    });

    const modulosGroup = this.planForm.get('modulos') as FormGroup;
    this.modulosDisponibles.forEach(m => {
      modulosGroup.get(m.id)?.setValue(plan.modulos.includes(m.id));
    });

    const reglasGroup = this.planForm.get('reglas') as FormGroup;
    this.reglasDisponibles.forEach(r => {
      reglasGroup.get(r.id)?.setValue(plan.reglas.includes(r.id));
    });
  }

  nuevoPlan(): void {
    this.editPlanId.set(null);
    this.mostrarFormNuevo.set(true);
    this.editando.set(true);
    this.planSeleccionado.set(null);

    this.planForm.reset({
      nombre: '', sku: '', precioMensual: 0, precioAnual: 0,
      descripcion: '', maxTrabajadores: 0, maxSedes: 0,
      almacenamiento: 0, sincronizacionNube: false,
    });

    const modulosGroup = this.planForm.get('modulos') as FormGroup;
    this.modulosDisponibles.forEach(m => modulosGroup.get(m.id)?.setValue(false));

    const reglasGroup = this.planForm.get('reglas') as FormGroup;
    this.reglasDisponibles.forEach(r => reglasGroup.get(r.id)?.setValue(false));
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
      this.mostrarToast('Corrige los errores antes de guardar.', 'error');
      return;
    }

    const { modulosSeleccionados, reglasSeleccionadas } = this.obtenerModulosYReglasSeleccionados();

    if (this.mostrarFormNuevo()) {
      this.crearNuevoPlan(modulosSeleccionados, reglasSeleccionadas);
    } else {
      this.actualizarPlanExistente(modulosSeleccionados, reglasSeleccionadas);
    }

    this.cancelarEdicion();
  }

  private obtenerModulosYReglasSeleccionados(): { modulosSeleccionados: string[]; reglasSeleccionadas: string[] } {
    const v = this.planForm.value;
    const modulosSeleccionados = this.modulosDisponibles
      .filter(m => (v.modulos as Record<string, boolean>)[m.id])
      .map(m => m.id);
    const reglasSeleccionadas = this.reglasDisponibles
      .filter(r => (v.reglas as Record<string, boolean>)[r.id])
      .map(r => r.id);
    return { modulosSeleccionados, reglasSeleccionadas };
  }

  private crearNuevoPlan(modulos: string[], reglas: string[]): void {
    const v = this.planForm.value;
    const nuevoId = Math.max(...this.planes().map(p => p.id)) + 1;
    this.planes.update(list => [...list, {
      id: nuevoId,
      nombre: v.nombre!,
      sku: v.sku!,
      precioMensual: v.precioMensual!,
      precioAnual: v.precioAnual!,
      descripcion: v.descripcion || '',
      maxTrabajadores: v.maxTrabajadores!,
      maxSedes: v.maxSedes!,
      almacenamiento: v.almacenamiento!,
      sincronizacionNube: v.sincronizacionNube || false,
      modulos,
      reglas,
      activo: true,
      ultimaModificacion: { usuario: 'Jose Alata', fecha: new Date().toISOString().split('T')[0] },
    }]);
    this.mostrarToast('Plan creado correctamente.', 'success');
  }

  private actualizarPlanExistente(modulos: string[], reglas: string[]): void {
    const v = this.planForm.value;
    const id = this.editPlanId();
    if (!id) return;
    this.planes.update(list => list.map(p =>
      p.id === id ? {
        ...p,
        nombre: v.nombre!,
        sku: v.sku!,
        precioMensual: v.precioMensual!,
        precioAnual: v.precioAnual!,
        descripcion: v.descripcion || '',
        maxTrabajadores: v.maxTrabajadores!,
        maxSedes: v.maxSedes!,
        almacenamiento: v.almacenamiento!,
        sincronizacionNube: v.sincronizacionNube || false,
        modulos,
        reglas,
        activo: true,
        ultimaModificacion: { usuario: 'Jose Alata', fecha: new Date().toISOString().split('T')[0] },
      } : p
    ));
    this.mostrarToast('Plan actualizado correctamente.', 'success');
  }

  confirmarEliminar(): void {
    const id = this.editPlanId();
    const plan = this.planes().find(p => p.id === id);
    if (!plan) return;
    this.modalService.show('modalEliminarPlan');
  }

  eliminarPlan(): void {
    const id = this.editPlanId();
    if (!id) return;
    this.planes.update(list => list.filter(p => p.id !== id));
    this.cerrarModal('modalEliminarPlan');
    this.mostrarToast('Plan eliminado correctamente.', 'success');
    this.cancelarEdicion();
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
    const headers = ['Tenant', 'Plan', 'Fecha Inicio', 'Fecha Fin', 'Monto', 'Estado'];
    const rows = this.suscripciones.map(s =>
      [s.tenant, s.plan, s.fechaInicio, s.fechaFin, s.monto.toString(), s.estado]
    );
    this.exportService.exportCSV(`historial-suscripciones-${new Date().toISOString().split('T')[0]}.csv`, headers, rows);
    this.mostrarToast('CSV exportado correctamente.');
  }

  private cerrarModal(id: string): void {
    this.modalService.hide(id);
  }

  private mostrarToast(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
    this.toastService.show(message, type);
  }

}
