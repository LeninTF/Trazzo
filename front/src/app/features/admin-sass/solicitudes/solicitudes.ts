import { Component, computed, signal, effect, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';

interface Solicitud {
  id: number;
  institucion: string;
  contacto: string;
  plan: string;
  fecha: string;
  estado: 'pendiente' | 'aprobado' | 'rechazado';
}

type ToastType = 'success' | 'error' | 'info';

@Component({
  selector: 'app-solicitudes',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './solicitudes.html',
  styleUrl: './solicitudes.css',
})
export class Solicitudes {
  private readonly router = inject(Router);

  readonly filterSearch = new FormControl('');
  readonly filterEstado = new FormControl('todos');
  readonly filterPeriodo = new FormControl('30');
  readonly filterFechaDesde = new FormControl('');
  readonly filterFechaHasta = new FormControl('');

  readonly filterSearchSig = toSignal(this.filterSearch.valueChanges, { initialValue: '' });
  readonly filterEstadoSig = toSignal(this.filterEstado.valueChanges, { initialValue: 'todos' });
  readonly filterPeriodoSig = toSignal(this.filterPeriodo.valueChanges, { initialValue: '30' });
  readonly filterFechaDesdeSig = toSignal(this.filterFechaDesde.valueChanges, { initialValue: '' });
  readonly filterFechaHastaSig = toSignal(this.filterFechaHasta.valueChanges, { initialValue: '' });

  dropdownOpen = signal<string | null>(null);

  readonly estadoOptions = [
    { value: 'todos', label: 'Todos los estados' },
    { value: 'pendiente', label: 'Pendiente' },
    { value: 'aprobado', label: 'Aprobado' },
    { value: 'rechazado', label: 'Rechazado' },
  ];

  readonly periodoOptions = [
    { value: '7', label: 'Últimos 7 días' },
    { value: '30', label: 'Últimos 30 días' },
    { value: 'personalizado', label: 'Personalizado' },
  ];

  readonly estadoLabel = computed(() => this.estadoOptions.find(o => o.value === this.filterEstadoSig())?.label ?? 'Todos los estados');
  readonly periodoLabel = computed(() => this.periodoOptions.find(o => o.value === this.filterPeriodoSig())?.label ?? 'Últimos 30 días');

  toggleDropdown(name: string, event: MouseEvent): void {
    event.stopPropagation();
    this.dropdownOpen.update(v => v === name ? null : name);
  }

  selectEstado(value: string): void {
    this.filterEstado.setValue(value);
    this.dropdownOpen.set(null);
  }

  selectPeriodo(value: string): void {
    this.filterPeriodo.setValue(value);
    this.dropdownOpen.set(null);
  }

  cerrarDropdowns(): void {
    this.dropdownOpen.set(null);
  }

  private refreshTrigger = signal(0);

  currentPage = signal<Record<string, number>>({ pendiente: 1, aprobado: 1, rechazado: 1 });
  pageSize = 5;
  activeTab = signal<'pendiente' | 'aprobado' | 'rechazado'>('pendiente');

  selectedSolicitud = signal<Solicitud | null>(null);
  confirmAction = signal<{ id: number; action: 'aprobar' | 'rechazar' | 'reconsiderar'; label: string } | null>(null);
  toast = signal<{ message: string; type: ToastType } | null>(null);
  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  readonly allSolicitudes: Solicitud[] = [
    { id: 1, institucion: 'Universidad Nacional de Ingeniería', contacto: 'Carlos Mendoza', plan: 'Plan Premium', fecha: '2026-06-04', estado: 'pendiente' },
    { id: 2, institucion: 'Colegio San Agustín', contacto: 'María López', plan: 'Plan Básico', fecha: '2026-06-04', estado: 'aprobado' },
    { id: 3, institucion: 'Instituto Tecnológico del Sur', contacto: 'Pedro García', plan: 'Plan Empresarial', fecha: '2026-06-03', estado: 'rechazado' },
    { id: 4, institucion: 'Universidad Católica de Santa María', contacto: 'Ana Torres', plan: 'Plan Premium', fecha: '2026-06-03', estado: 'pendiente' },
    { id: 5, institucion: 'Colegio La Salle', contacto: 'Luis Fernández', plan: 'Plan Básico', fecha: '2026-06-02', estado: 'aprobado' },
    { id: 6, institucion: 'Universidad San Martín', contacto: 'Rosa Díaz', plan: 'Plan Empresarial', fecha: '2026-06-02', estado: 'pendiente' },
    { id: 7, institucion: 'Colegio Santa María', contacto: 'José Ramírez', plan: 'Plan Premium', fecha: '2026-06-01', estado: 'rechazado' },
    { id: 8, institucion: 'Instituto Peruano de Marketing', contacto: 'Patricia Vega', plan: 'Plan Básico', fecha: '2026-06-01', estado: 'pendiente' },
    { id: 9, institucion: 'Universidad del Pacífico', contacto: 'Diego Castillo', plan: 'Plan Empresarial', fecha: '2026-05-30', estado: 'aprobado' },
    { id: 10, institucion: 'Colegio San José', contacto: 'Gabriela Ríos', plan: 'Plan Premium', fecha: '2026-05-30', estado: 'pendiente' },
    { id: 11, institucion: 'Universidad de Lima', contacto: 'Fernando Paredes', plan: 'Plan Básico', fecha: '2026-05-29', estado: 'rechazado' },
    { id: 12, institucion: 'Colegio Santo Domingo', contacto: 'Carmen Flores', plan: 'Plan Empresarial', fecha: '2026-05-29', estado: 'aprobado' },
    { id: 13, institucion: 'Instituto Gauss', contacto: 'Miguel Ángel', plan: 'Plan Premium', fecha: '2026-05-28', estado: 'pendiente' },
    { id: 14, institucion: 'Universidad Científica del Sur', contacto: 'Valeria Castro', plan: 'Plan Básico', fecha: '2026-05-28', estado: 'aprobado' },
    { id: 15, institucion: 'Colegio Champagnat', contacto: 'Ricardo Guerra', plan: 'Plan Empresarial', fecha: '2026-05-27', estado: 'pendiente' },
    { id: 16, institucion: 'Universidad Tecnológica del Perú', contacto: 'Jose Alata', plan: 'Plan Premium', fecha: '2026-05-27', estado: 'aprobado' },
    { id: 17, institucion: 'Colegio San Ignacio', contacto: 'Sofía Delgado', plan: 'Plan Básico', fecha: '2026-05-26', estado: 'pendiente' },
    { id: 18, institucion: 'Instituto Peruano de Negocios', contacto: 'Andrés Núñez', plan: 'Plan Empresarial', fecha: '2026-05-26', estado: 'rechazado' },
    { id: 19, institucion: 'Universidad Ricardo Palma', contacto: 'Mónica Salazar', plan: 'Plan Premium', fecha: '2026-05-25', estado: 'pendiente' },
    { id: 20, institucion: 'Colegio San Antonio', contacto: 'Jorge Campos', plan: 'Plan Básico', fecha: '2026-05-25', estado: 'aprobado' },
    { id: 21, institucion: 'Universidad Nacional Mayor de San Marcos', contacto: 'Luisa Huertas', plan: 'Plan Empresarial', fecha: '2026-05-24', estado: 'aprobado' },
    { id: 22, institucion: 'Colegio Mater Admirabilis', contacto: 'Raúl Zúñiga', plan: 'Plan Premium', fecha: '2026-05-24', estado: 'rechazado' },
    { id: 23, institucion: 'Instituto Tecnológico del Norte', contacto: 'Cecilia Maldonado', plan: 'Plan Básico', fecha: '2026-05-23', estado: 'pendiente' },
    { id: 24, institucion: 'Universidad Nacional de Trujillo', contacto: 'Héctor Bravo', plan: 'Plan Empresarial', fecha: '2026-05-23', estado: 'pendiente' },
    { id: 25, institucion: 'Colegio San Pablo', contacto: 'Diana Quispe', plan: 'Plan Premium', fecha: '2026-05-22', estado: 'rechazado' },
    { id: 26, institucion: 'Universidad Nacional del Altiplano', contacto: 'Pablo Huamán', plan: 'Plan Básico', fecha: '2026-05-22', estado: 'aprobado' },
  ];

  readonly pendientes = computed(() => this.filtered().filter(s => s.estado === 'pendiente'));
  readonly aprobadas = computed(() => this.filtered().filter(s => s.estado === 'aprobado'));
  readonly rechazadas = computed(() => this.filtered().filter(s => s.estado === 'rechazado'));

  readonly totalPendientes = computed(() => this.pendientes().length);
  readonly totalAprobadas = computed(() => this.aprobadas().length);
  readonly totalRechazadas = computed(() => this.rechazadas().length);

  readonly filtered = computed(() => {
    this.refreshTrigger();
    const search = (this.filterSearchSig() || '').toLowerCase().trim();
    const estado = this.filterEstadoSig();
    const periodo = this.filterPeriodoSig();
    const fechaDesde = this.filterFechaDesdeSig();
    const fechaHasta = this.filterFechaHastaSig();

    return this.allSolicitudes.filter(s => {
      if (search && !s.institucion.toLowerCase().includes(search) && !s.contacto.toLowerCase().includes(search)) return false;
      if (estado !== 'todos' && s.estado !== estado) return false;

      const fecha = new Date(s.fecha);
      if (periodo === 'personalizado') {
        if (fechaDesde && fecha < new Date(fechaDesde)) return false;
        if (fechaHasta && fecha > new Date(fechaHasta + 'T23:59:59')) return false;
      } else {
        const days = Number(periodo);
        if (!isNaN(days) && days > 0) {
          const cutoff = new Date(Date.now() - days * 86400000);
          if (fecha < cutoff) return false;
        }
      }
      return true;
    });
  });

  readonly filtroCountText = computed(() => {
    const total = this.filtered().length;
    return `Mostrando ${total} solicitud${total !== 1 ? 'es' : ''}`;
  });

  paginated(estado: 'pendiente' | 'aprobado' | 'rechazado'): Solicitud[] {
    const list = this.listFor(estado);
    const page = this.clampPage(estado);
    return list.slice((page - 1) * this.pageSize, page * this.pageSize);
  }

  totalPages(estado: 'pendiente' | 'aprobado' | 'rechazado'): number {
    return Math.max(1, Math.ceil(this.listFor(estado).length / this.pageSize));
  }

  pageNumbers(estado: 'pendiente' | 'aprobado' | 'rechazado'): number[] {
    return Array.from({ length: this.totalPages(estado) }, (_, i) => i + 1);
  }

  goToPage(estado: 'pendiente' | 'aprobado' | 'rechazado', page: number): void {
    this.currentPage.update(p => ({ ...p, [estado]: page }));
  }

  prevPage(estado: 'pendiente' | 'aprobado' | 'rechazado'): void {
    const page = this.clampPage(estado);
    if (page > 1) this.goToPage(estado, page - 1);
  }

  nextPage(estado: 'pendiente' | 'aprobado' | 'rechazado'): void {
    const page = this.clampPage(estado);
    if (page < this.totalPages(estado)) this.goToPage(estado, page + 1);
  }

  private listFor(estado: 'pendiente' | 'aprobado' | 'rechazado'): Solicitud[] {
    return estado === 'pendiente' ? this.pendientes() : estado === 'aprobado' ? this.aprobadas() : this.rechazadas();
  }

  private clampPage(estado: 'pendiente' | 'aprobado' | 'rechazado'): number {
    const p = this.currentPage()[estado] || 1;
    const max = this.totalPages(estado);
    if (p > max) {
      this.currentPage.update(pages => ({ ...pages, [estado]: max }));
      return max;
    }
    return p;
  }

  setTab(tab: 'pendiente' | 'aprobado' | 'rechazado'): void {
    this.activeTab.set(tab);
  }

  limpiarFiltros(): void {
    this.filterSearch.setValue('');
    this.filterEstado.setValue('todos');
    this.filterPeriodo.setValue('30');
    this.filterFechaDesde.setValue('');
    this.filterFechaHasta.setValue('');
    this.currentPage.set({ pendiente: 1, aprobado: 1, rechazado: 1 });
  }

  verDetalle(s: Solicitud): void {
    this.selectedSolicitud.set(s);
    const el = document.getElementById('modalDetalle');
    if (el) new (window as any).bootstrap.Modal(el).show();
  }

  confirmarAprobar(id: number): void {
    const s = this.allSolicitudes.find(x => x.id === id);
    if (!s) return;
    this.confirmAction.set({ id, action: 'aprobar', label: `¿Aprobar solicitud de "${s.institucion}"?` });
    const el = document.getElementById('modalConfirmar');
    if (el) new (window as any).bootstrap.Modal(el).show();
  }

  confirmarRechazar(id: number): void {
    const s = this.allSolicitudes.find(x => x.id === id);
    if (!s) return;
    this.confirmAction.set({ id, action: 'rechazar', label: `¿Rechazar solicitud de "${s.institucion}"?` });
    const el = document.getElementById('modalConfirmar');
    if (el) new (window as any).bootstrap.Modal(el).show();
  }

  confirmarReconsiderar(id: number): void {
    const s = this.allSolicitudes.find(x => x.id === id);
    if (!s) return;
    this.confirmAction.set({ id, action: 'reconsiderar', label: `¿Reconsiderar solicitud de "${s.institucion}"?` });
    const el = document.getElementById('modalConfirmar');
    if (el) new (window as any).bootstrap.Modal(el).show();
  }

  ejecutarAccion(): void {
    const action = this.confirmAction();
    if (!action) return;

    if (action.action === 'aprobar') {
      this.aprobar(action.id);
    } else if (action.action === 'rechazar') {
      this.rechazar(action.id);
    } else if (action.action === 'reconsiderar') {
      this.reconsiderar(action.id);
    }

    this.refreshTrigger.update(v => v + 1);
    this.cerrarModal('modalConfirmar');
    this.confirmAction.set(null);

    const msgs: Record<string, string> = {
      aprobar: 'Solicitud aprobada correctamente',
      rechazar: 'Solicitud rechazada correctamente',
      reconsiderar: 'Solicitud reconsiderada correctamente',
    };
    this.mostrarToast(msgs[action.action], 'success');
  }

  private aprobar(id: number): void {
    const s = this.allSolicitudes.find(x => x.id === id);
    if (s) s.estado = 'aprobado';
  }

  private rechazar(id: number): void {
    const s = this.allSolicitudes.find(x => x.id === id);
    if (s) s.estado = 'rechazado';
  }

  private reconsiderar(id: number): void {
    const s = this.allSolicitudes.find(x => x.id === id);
    if (s) s.estado = 'pendiente';
  }

  private cerrarModal(id: string): void {
    const el = document.getElementById(id);
    if (el) {
      const modal = (window as any).bootstrap.Modal.getInstance(el);
      modal?.hide();
    }
  }

  private mostrarToast(message: string, type: ToastType): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toast.set({ message, type });
    this.toastTimer = setTimeout(() => this.toast.set(null), 3500);
  }
}
