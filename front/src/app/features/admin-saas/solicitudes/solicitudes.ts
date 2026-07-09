import { Component, computed, signal, inject, effect } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { SlicePipe } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { ToastService } from '../../../services/toast.service';
import { ModalService } from '../../../services/modal.service';
import { ApiService } from '../../../api/services/api.service';
import type { RequestDetail, RequestStatus, RequestType } from '../../../api/types';

type ToastType = 'success' | 'error' | 'info';
type ActionType = 'aprobar' | 'rechazar' | 'observar' | 'reconsiderar';

const STATUS_TABS: { value: RequestStatus; label: string }[] = [
  { value: 'PENDING', label: 'Pendientes' },
  { value: 'IN_REVIEW', label: 'Observadas' },
  { value: 'APPROVED', label: 'Aprobadas' },
  { value: 'REJECTED', label: 'Rechazadas' },
];

@Component({
  selector: 'app-solicitudes',
  standalone: true,
  imports: [ReactiveFormsModule, PaginationComponent, SlicePipe],
  templateUrl: './solicitudes.html',
  styleUrl: './solicitudes.css',
})
export class Solicitudes {
  readonly loading = signal(false);
  readonly error = signal('');

  private readonly toastService = inject(ToastService);
  private readonly modalService = inject(ModalService);
  private readonly api = inject(ApiService);

  readonly statusTabs = STATUS_TABS;

  readonly filterSearch = new FormControl('');
  readonly filterTipo = new FormControl('todos');
  readonly filterSearchSig = toSignal(this.filterSearch.valueChanges, { initialValue: '' });
  readonly filterTipoSig = toSignal(this.filterTipo.valueChanges, { initialValue: 'todos' });

  dropdownOpen = signal<string | null>(null);

  readonly tipoOptions = [
    { value: 'todos', label: 'Todos los tipos' },
    { value: 'TRIAL', label: 'Trial' },
    { value: 'INFO', label: 'Más información' },
  ];

  readonly tipoLabel = computed(() => this.tipoOptions.find(o => o.value === this.filterTipoSig())?.label ?? 'Todos los tipos');

  toggleDropdown(name: string, event: Event): void {
    event.stopPropagation();
    this.dropdownOpen.update(v => v === name ? null : name);
  }

  selectTipo(value: string): void {
    this.filterTipo.setValue(value);
    this.dropdownOpen.set(null);
  }

  cerrarDropdowns(): void {
    this.dropdownOpen.set(null);
  }

  readonly pageSize = 5;
  activeTab = signal<RequestStatus>('PENDING');
  currentPage = signal<Record<RequestStatus, number>>({ PENDING: 1, IN_REVIEW: 1, APPROVED: 1, REJECTED: 1 });

  readonly items = signal<import('../../../api/types').RequestSummary[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.totalElements() / this.pageSize)));

  readonly counts = signal<Record<RequestStatus, number>>({ PENDING: 0, IN_REVIEW: 0, APPROVED: 0, REJECTED: 0 });

  selectedDetail = signal<RequestDetail | null>(null);
  confirmAction = signal<{ id: number; action: ActionType; label: string } | null>(null);
  newComment = signal('');

  constructor() {
    effect(() => this.fetchList());
    this.fetchCounts();
  }

  private fetchList(): void {
    this.loading.set(true);
    this.error.set('');
    const status = this.activeTab();
    const type = this.filterTipoSig();

    this.api.requests.list({
      status,
      type: type && type !== 'todos' ? type : undefined,
      search: this.filterSearchSig() || undefined,
      page: (this.currentPage()[status] || 1) - 1,
      size: this.pageSize,
    }).subscribe({
      next: (res) => {
        this.items.set(res.content);
        this.totalElements.set(res.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las solicitudes.');
        this.loading.set(false);
      },
    });
  }

  private fetchCounts(): void {
    forkJoin({
      PENDING: this.api.requests.list({ status: 'PENDING', page: 0, size: 1 }),
      IN_REVIEW: this.api.requests.list({ status: 'IN_REVIEW', page: 0, size: 1 }),
      APPROVED: this.api.requests.list({ status: 'APPROVED', page: 0, size: 1 }),
      REJECTED: this.api.requests.list({ status: 'REJECTED', page: 0, size: 1 }),
    }).subscribe(res => {
      this.counts.set({
        PENDING: res.PENDING.totalElements,
        IN_REVIEW: res.IN_REVIEW.totalElements,
        APPROVED: res.APPROVED.totalElements,
        REJECTED: res.REJECTED.totalElements,
      });
    });
  }

  private refresh(): void {
    this.fetchList();
    this.fetchCounts();
  }

  goToPage(page: number): void {
    this.currentPage.update(p => ({ ...p, [this.activeTab()]: page }));
  }

  setTab(tab: RequestStatus): void {
    this.activeTab.set(tab);
  }

  limpiarFiltros(): void {
    this.filterSearch.setValue('');
    this.filterTipo.setValue('todos');
    this.currentPage.set({ PENDING: 1, IN_REVIEW: 1, APPROVED: 1, REJECTED: 1 });
  }

  verDetalle(id: number): void {
    this.api.requests.getById(id).subscribe({
      next: (detail) => {
        this.selectedDetail.set(detail);
        this.modalService.show('modalDetalle');
      },
      error: () => this.toastService.error('No se pudo cargar el detalle de la solicitud.'),
    });
  }

  confirmarAprobar(id: number): void {
    this.confirmAction.set({ id, action: 'aprobar', label: '¿Aprobar esta solicitud?' });
    this.modalService.show('modalConfirmar');
  }

  confirmarRechazar(id: number): void {
    this.confirmAction.set({ id, action: 'rechazar', label: '¿Rechazar esta solicitud?' });
    this.modalService.show('modalConfirmar');
  }

  confirmarObservar(id: number): void {
    this.confirmAction.set({ id, action: 'observar', label: '¿Marcar esta solicitud como observada?' });
    this.modalService.show('modalConfirmar');
  }

  confirmarReconsiderar(id: number): void {
    this.confirmAction.set({ id, action: 'reconsiderar', label: '¿Reconsiderar esta solicitud?' });
    this.modalService.show('modalConfirmar');
  }

  private static targetStatus(action: ActionType): RequestStatus {
    switch (action) {
      case 'aprobar': return 'APPROVED';
      case 'rechazar': return 'REJECTED';
      case 'observar': return 'IN_REVIEW';
      case 'reconsiderar': return 'PENDING';
    }
  }

  ejecutarAccion(): void {
    const action = this.confirmAction();
    if (!action) return;

    this.api.requests.changeStatus(action.id, { status: Solicitudes.targetStatus(action.action) }).subscribe({
      next: () => {
        this.modalService.hide('modalConfirmar');
        this.confirmAction.set(null);
        this.refresh();

        const msgs: Record<ActionType, string> = {
          aprobar: 'Solicitud aprobada correctamente',
          rechazar: 'Solicitud rechazada correctamente',
          observar: 'Solicitud marcada como observada',
          reconsiderar: 'Solicitud reconsiderada correctamente',
        };
        this.mostrarToast(msgs[action.action], 'success');
      },
      error: () => this.mostrarToast('No se pudo actualizar la solicitud.', 'error'),
    });
  }

  enviarComentario(): void {
    const detail = this.selectedDetail();
    const comment = this.newComment().trim();
    if (!detail || !comment) return;

    this.api.requests.addComment(detail.id, comment).subscribe({
      next: () => {
        this.newComment.set('');
        this.api.requests.getById(detail.id).subscribe(updated => this.selectedDetail.set(updated));
        this.mostrarToast('Comentario agregado', 'success');
      },
      error: () => this.mostrarToast('No se pudo agregar el comentario.', 'error'),
    });
  }

  typeLabel(type: RequestType): string {
    return type === 'TRIAL' ? 'Trial' : 'Más información';
  }

  private mostrarToast(message: string, type: ToastType): void {
    this.toastService.show(message, type);
  }
}
