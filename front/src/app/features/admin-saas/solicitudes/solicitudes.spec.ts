import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Solicitudes } from './solicitudes';
import { ApiService } from '../../../api/services/api.service';
import { ToastService } from '../../../services/toast.service';
import { ModalService } from '../../../services/modal.service';
import type { RequestDetail, RequestListResponse, RequestSummary } from '../../../api/types';

describe('Solicitudes', () => {
  let component: Solicitudes;
  let fixture: ComponentFixture<Solicitudes>;

  const mockContact = {
    name: 'Ana', lastName: 'Perez', email: 'ana@example.com',
    phoneNumber: '999999999', taxId: '20123456789', companyName: 'Acme SAC',
  };

  const mockSummary = (id: number, status: RequestSummary['status'] = 'PENDING'): RequestSummary => ({
    id, type: 'TRIAL', title: `Solicitud de trial - Acme SAC ${id}`, message: 'Quiero una demo',
    status, createdAt: '2026-01-01T00:00:00', updatedAt: '2026-01-01T00:00:00', contact: mockContact,
  });

  const mockDetail = (id: number): RequestDetail => ({
    ...mockSummary(id), comments: [], history: [],
  });

  const listByStatus: Record<string, RequestSummary[]> = {
    PENDING: [mockSummary(1, 'PENDING'), mockSummary(2, 'PENDING')],
    IN_REVIEW: [mockSummary(3, 'IN_REVIEW')],
    APPROVED: [],
    REJECTED: [],
  };

  const toResponse = (items: RequestSummary[]): RequestListResponse => ({
    content: items, page: 0, size: 5, totalElements: items.length, totalPages: 1,
  });

  let mockRequests: {
    list: jasmine.Spy; getById: jasmine.Spy; changeStatus: jasmine.Spy; addComment: jasmine.Spy; submit: jasmine.Spy;
  };
  let mockToast: jasmine.SpyObj<ToastService>;
  let mockModal: jasmine.SpyObj<ModalService>;

  beforeEach(async () => {
    mockRequests = {
      list: jasmine.createSpy('list').and.callFake((opts?: { status?: string }) => {
        const items = opts?.status ? (listByStatus[opts.status] ?? []) : Object.values(listByStatus).flat();
        return of(toResponse(items));
      }),
      getById: jasmine.createSpy('getById').and.callFake((id: number) => of(mockDetail(id))),
      changeStatus: jasmine.createSpy('changeStatus').and.returnValue(of(mockSummary(1, 'APPROVED'))),
      addComment: jasmine.createSpy('addComment')
        .and.returnValue(of({ id: 1, comment: 'Todo en orden', authorUserId: 'admin-1', createdAt: '2026-01-01T00:00:00' })),
      submit: jasmine.createSpy('submit').and.returnValue(of(mockSummary(1))),
    };
    mockToast = jasmine.createSpyObj('ToastService', ['show', 'success', 'error', 'info']);
    mockModal = jasmine.createSpyObj('ModalService', ['show', 'hide']);

    await TestBed.configureTestingModule({
      imports: [Solicitudes],
      providers: [
        { provide: ApiService, useValue: { requests: mockRequests } },
        { provide: ToastService, useValue: mockToast },
        { provide: ModalService, useValue: mockModal },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Solicitudes);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('creates the solicitudes component', () => {
    expect(component).toBeTruthy();
  });

  it('has default signal state', () => {
    expect(component.loading()).toBeFalse();
    expect(component.error()).toBe('');
    expect(component.activeTab()).toBe('PENDING');
  });

  it('loads the pending items on init', () => {
    expect(mockRequests.list).toHaveBeenCalled();
    expect(component.items()).toHaveSize(2);
    expect(component.totalElements()).toBe(2);
  });

  it('computes counts per status tab', () => {
    expect(component.counts()).toEqual({ PENDING: 2, IN_REVIEW: 1, APPROVED: 0, REJECTED: 0 });
  });

  it('has 4 status tabs', () => {
    expect(component.statusTabs).toHaveSize(4);
  });

  it('has tipoOptions with 3 entries', () => {
    expect(component.tipoOptions).toHaveSize(3);
  });

  describe('toggleDropdown', () => {
    it('should toggle dropdown name', () => {
      const event = new MouseEvent('click');
      spyOn(event, 'stopPropagation');

      component.toggleDropdown('tipo', event);
      expect(component.dropdownOpen()).toBe('tipo');
      expect(event.stopPropagation).toHaveBeenCalled();
    });

    it('should close if already open', () => {
      component.dropdownOpen.set('tipo');
      component.toggleDropdown('tipo', new MouseEvent('click'));
      expect(component.dropdownOpen()).toBeNull();
    });
  });

  describe('selectTipo', () => {
    it('should set filter and close dropdown', () => {
      component.selectTipo('TRIAL');
      expect(component.filterTipo.value).toBe('TRIAL');
      expect(component.dropdownOpen()).toBeNull();
    });
  });

  describe('cerrarDropdowns', () => {
    it('should set dropdownOpen to null', () => {
      component.dropdownOpen.set('tipo');
      component.cerrarDropdowns();
      expect(component.dropdownOpen()).toBeNull();
    });
  });

  describe('setTab', () => {
    it('should set active tab and refetch', () => {
      mockRequests.list.calls.reset();
      component.setTab('IN_REVIEW');
      fixture.detectChanges();
      expect(component.activeTab()).toBe('IN_REVIEW');
      expect(mockRequests.list).toHaveBeenCalledWith(jasmine.objectContaining({ status: 'IN_REVIEW' }));
    });
  });

  describe('goToPage', () => {
    it('should update current page for the active tab', () => {
      component.goToPage(2);
      expect(component.currentPage()['PENDING']).toBe(2);
    });
  });

  describe('limpiarFiltros', () => {
    it('should reset all filters and pagination', () => {
      component.filterSearch.setValue('acme');
      component.filterTipo.setValue('TRIAL');
      component.goToPage(3);

      component.limpiarFiltros();

      expect(component.filterSearch.value).toBe('');
      expect(component.filterTipo.value).toBe('todos');
      expect(component.currentPage()).toEqual({ PENDING: 1, IN_REVIEW: 1, APPROVED: 1, REJECTED: 1 });
    });
  });

  describe('verDetalle', () => {
    it('should load detail and show modal', () => {
      component.verDetalle(1);
      expect(mockRequests.getById).toHaveBeenCalledWith(1);
      expect(component.selectedDetail()?.id).toBe(1);
      expect(mockModal.show).toHaveBeenCalledWith('modalDetalle');
    });

    it('should show a toast on error', () => {
      mockRequests.getById.and.returnValue(throwError(() => new Error('fail')));
      component.verDetalle(1);
      expect(mockToast.error).toHaveBeenCalled();
    });
  });

  describe('confirmation prompts', () => {
    it('confirmarAprobar sets confirm action', () => {
      component.confirmarAprobar(1);
      expect(component.confirmAction()).toEqual({ id: 1, action: 'aprobar', label: jasmine.any(String) });
      expect(mockModal.show).toHaveBeenCalledWith('modalConfirmar');
    });

    it('confirmarRechazar sets confirm action', () => {
      component.confirmarRechazar(2);
      expect(component.confirmAction()?.action).toBe('rechazar');
    });

    it('confirmarObservar sets confirm action', () => {
      component.confirmarObservar(3);
      expect(component.confirmAction()?.action).toBe('observar');
    });

    it('confirmarReconsiderar sets confirm action', () => {
      component.confirmarReconsiderar(4);
      expect(component.confirmAction()?.action).toBe('reconsiderar');
    });
  });

  describe('ejecutarAccion', () => {
    it('should do nothing if no confirmAction', () => {
      component.ejecutarAccion();
      expect(mockRequests.changeStatus).not.toHaveBeenCalled();
    });

    it('should approve and refresh', () => {
      component.confirmAction.set({ id: 1, action: 'aprobar', label: 'Aprobar?' });
      component.ejecutarAccion();
      expect(mockRequests.changeStatus).toHaveBeenCalledWith(1, { status: 'APPROVED' });
      expect(mockModal.hide).toHaveBeenCalledWith('modalConfirmar');
      expect(component.confirmAction()).toBeNull();
      expect(mockToast.show).toHaveBeenCalled();
    });

    it('should reject with REJECTED status', () => {
      component.confirmAction.set({ id: 1, action: 'rechazar', label: 'Rechazar?' });
      component.ejecutarAccion();
      expect(mockRequests.changeStatus).toHaveBeenCalledWith(1, { status: 'REJECTED' });
    });

    it('should observar with IN_REVIEW status', () => {
      component.confirmAction.set({ id: 1, action: 'observar', label: 'Observar?' });
      component.ejecutarAccion();
      expect(mockRequests.changeStatus).toHaveBeenCalledWith(1, { status: 'IN_REVIEW' });
    });

    it('should reconsiderar with PENDING status', () => {
      component.confirmAction.set({ id: 1, action: 'reconsiderar', label: 'Reconsiderar?' });
      component.ejecutarAccion();
      expect(mockRequests.changeStatus).toHaveBeenCalledWith(1, { status: 'PENDING' });
    });

    it('should show error toast when changeStatus fails', () => {
      mockRequests.changeStatus.and.returnValue(throwError(() => new Error('fail')));
      component.confirmAction.set({ id: 1, action: 'aprobar', label: 'Aprobar?' });
      component.ejecutarAccion();
      expect(mockToast.show).toHaveBeenCalledWith('No se pudo actualizar la solicitud.', 'error');
    });
  });

  describe('enviarComentario', () => {
    it('should do nothing without a selected detail', () => {
      component.newComment.set('hola');
      component.enviarComentario();
      expect(mockRequests.addComment).not.toHaveBeenCalled();
    });

    it('should do nothing with a blank comment', () => {
      component.verDetalle(1);
      component.newComment.set('   ');
      component.enviarComentario();
      expect(mockRequests.addComment).not.toHaveBeenCalled();
    });

    it('should add the comment, reset input and reload detail', () => {
      component.verDetalle(1);
      component.newComment.set('Todo en orden');
      component.enviarComentario();
      expect(mockRequests.addComment).toHaveBeenCalledWith(1, 'Todo en orden');
      expect(component.newComment()).toBe('');
      expect(mockToast.show).toHaveBeenCalledWith('Comentario agregado', 'success');
    });

    it('should show error toast when addComment fails', () => {
      component.verDetalle(1);
      mockRequests.addComment.and.returnValue(throwError(() => new Error('fail')));
      component.newComment.set('Todo en orden');
      component.enviarComentario();
      expect(mockToast.show).toHaveBeenCalledWith('No se pudo agregar el comentario.', 'error');
    });
  });

  describe('typeLabel', () => {
    it('should label TRIAL', () => {
      expect(component.typeLabel('TRIAL')).toBe('Trial');
    });

    it('should label INFO', () => {
      expect(component.typeLabel('INFO')).toBe('Más información');
    });
  });

  describe('fetchList error handling', () => {
    it('should set error message when list fails', () => {
      mockRequests.list.and.returnValue(throwError(() => new Error('fail')));
      component.setTab('APPROVED');
      fixture.detectChanges();
      expect(component.error()).toBe('No se pudieron cargar las solicitudes.');
      expect(component.loading()).toBeFalse();
    });
  });
});
