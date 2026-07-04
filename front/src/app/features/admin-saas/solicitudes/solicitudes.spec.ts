import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Solicitudes } from './solicitudes';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { ToastService } from '../../../services/toast.service';
import { ModalService } from '../../../services/modal.service';

describe('Solicitudes', () => {
  let component: Solicitudes;
  let fixture: ComponentFixture<Solicitudes>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;
  let modalServiceSpy: jasmine.SpyObj<ModalService>;

  beforeEach(async () => {
    toastServiceSpy = jasmine.createSpyObj('ToastService', ['show']);
    modalServiceSpy = jasmine.createSpyObj('ModalService', ['show', 'hide']);

    await TestBed.configureTestingModule({
      imports: [Solicitudes, ReactiveFormsModule, PaginationComponent],
      providers: [
        { provide: ToastService, useValue: toastServiceSpy },
        { provide: ModalService, useValue: modalServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Solicitudes);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('creates the solicitudes component', () => {
    expect(component).toBeTruthy();
  });

  it('has default signal state for solicitudes', () => {
    expect(component.loading()).toBeFalse();
    expect(component.error()).toBe('');
    expect(component.activeTab()).toBe('pendiente');
  });

  it('should have 26 allSolicitudes', () => {
    expect(component.allSolicitudes.length).toBe(26);
  });

  it('should have estadoOptions with 4 entries', () => {
    expect(component.estadoOptions.length).toBe(4);
  });

  describe('toggleDropdown', () => {
    it('should toggle dropdown name', () => {
      const event = new MouseEvent('click');
      spyOn(event, 'stopPropagation');

      component.toggleDropdown('estado', event);
      expect(component.dropdownOpen()).toBe('estado');
      expect(event.stopPropagation).toHaveBeenCalled();
    });

    it('should close if already open', () => {
      component.dropdownOpen.set('estado');
      component.toggleDropdown('estado', new MouseEvent('click'));
      expect(component.dropdownOpen()).toBeNull();
    });
  });

  describe('selectEstado', () => {
    it('should set filter and close dropdown', () => {
      component.selectEstado('aprobado');
      expect(component.filterEstado.value).toBe('aprobado');
      expect(component.dropdownOpen()).toBeNull();
    });
  });

  describe('selectPeriodo', () => {
    it('should set filter and close dropdown', () => {
      component.selectPeriodo('7');
      expect(component.filterPeriodo.value).toBe('7');
      expect(component.dropdownOpen()).toBeNull();
    });
  });

  describe('cerrarDropdowns', () => {
    it('should set dropdownOpen to null', () => {
      component.dropdownOpen.set('estado');
      component.cerrarDropdowns();
      expect(component.dropdownOpen()).toBeNull();
    });
  });

  describe('filtered', () => {
    it('should return all when no filters applied', () => {
      component.filterPeriodo.setValue('personalizado');
      component.filterFechaDesde.setValue('2026-01-01');
      component.filterFechaHasta.setValue('2026-12-31');
      fixture.detectChanges();
      expect(component.filtered().length).toBeGreaterThanOrEqual(1);
    });

    it('should filter by estado', () => {
      component.filterPeriodo.setValue('personalizado');
      component.filterFechaDesde.setValue('2026-01-01');
      component.filterFechaHasta.setValue('2026-12-31');
      component.filterEstado.setValue('aprobado');
      fixture.detectChanges();
      expect(component.filtered().every(s => s.estado === 'aprobado')).toBeTrue();
    });

    it('should filter by search text', () => {
      component.filterPeriodo.setValue('personalizado');
      component.filterFechaDesde.setValue('2026-01-01');
      component.filterFechaHasta.setValue('2026-12-31');
      component.filterSearch.setValue('universidad');
      fixture.detectChanges();
      expect(component.filtered().length).toBeGreaterThan(0);
      expect(component.filtered().every(s =>
        s.institucion.toLowerCase().includes('universidad') ||
        s.contacto.toLowerCase().includes('universidad')
      )).toBeTrue();
    });

    it('should filter by custom date range without default period restriction', () => {
      component.filterPeriodo.setValue('personalizado');
      component.filterFechaDesde.setValue('2026-06-01');
      component.filterFechaHasta.setValue('2026-06-30');
      fixture.detectChanges();
      expect(component.filtered().length).toBe(8);
    });
  });

  describe('computed totals', () => {
    it('should compute totalPendientes', () => {
      component.filterPeriodo.setValue('personalizado');
      component.filterFechaDesde.setValue('2026-01-01');
      component.filterFechaHasta.setValue('2026-12-31');
      fixture.detectChanges();
      expect(component.totalPendientes()).toBeGreaterThan(0);
    });

    it('should compute totalAprobadas', () => {
      component.filterPeriodo.setValue('personalizado');
      component.filterFechaDesde.setValue('2026-01-01');
      component.filterFechaHasta.setValue('2026-12-31');
      fixture.detectChanges();
      expect(component.totalAprobadas()).toBeGreaterThan(0);
    });

    it('should compute totalRechazadas', () => {
      component.filterPeriodo.setValue('personalizado');
      component.filterFechaDesde.setValue('2026-01-01');
      component.filterFechaHasta.setValue('2026-12-31');
      fixture.detectChanges();
      expect(component.totalRechazadas()).toBeGreaterThan(0);
    });
  });

  describe('filtroCountText', () => {
    it('should return formatted count text', () => {
      const text = component.filtroCountText();
      expect(text).toContain('Mostrando');
      expect(text).toContain('solicitudes');
    });
  });

  describe('paginated', () => {
    it('should return first page of pendientes', () => {
      const page = component.paginated('pendiente');
      expect(page.length).toBeLessThanOrEqual(component.pageSize);
    });
  });

  describe('totalPages', () => {
    it('should return at least 1', () => {
      expect(component.totalPages('pendiente')).toBeGreaterThanOrEqual(1);
    });
  });

  describe('pageNumbers', () => {
    it('should return array of page numbers', () => {
      const pages = component.pageNumbers('pendiente');
      expect(pages[0]).toBe(1);
    });
  });

  describe('goToPage', () => {
    it('should update current page', () => {
      component.goToPage('pendiente', 2);
      expect(component.currentPage()['pendiente']).toBe(2);
    });
  });

  describe('prevPage', () => {
    it('should go to previous page', () => {
      component.goToPage('pendiente', 2);
      component.prevPage('pendiente');
      expect(component.currentPage()['pendiente']).toBe(1);
    });

    it('should not go below 1', () => {
      component.prevPage('pendiente');
      expect(component.currentPage()['pendiente']).toBe(1);
    });
  });

  describe('nextPage', () => {
    it('should go to next page', () => {
      component.filterPeriodo.setValue('personalizado');
      fixture.detectChanges();
      component.nextPage('pendiente');
      expect(component.currentPage()['pendiente']).toBe(2);
    });
  });

  describe('setTab', () => {
    it('should set active tab', () => {
      component.setTab('aprobado');
      expect(component.activeTab()).toBe('aprobado');
    });
  });

  describe('limpiarFiltros', () => {
    it('should reset all filters', () => {
      component.filterSearch.setValue('test');
      component.filterEstado.setValue('aprobado');
      component.filterPeriodo.setValue('7');
      component.filterFechaDesde.setValue('2026-01-01');
      component.filterFechaHasta.setValue('2026-06-01');
      component.currentPage.set({ pendiente: 3, aprobado: 2, rechazado: 1 });

      component.limpiarFiltros();

      expect(component.filterSearch.value).toBe('');
      expect(component.filterEstado.value).toBe('todos');
      expect(component.filterPeriodo.value).toBe('30');
      expect(component.filterFechaDesde.value).toBe('');
      expect(component.filterFechaHasta.value).toBe('');
      expect(component.currentPage()).toEqual({ pendiente: 1, aprobado: 1, rechazado: 1 });
    });
  });

  describe('verDetalle', () => {
    it('should set selected and show modal', () => {
      const s = component.allSolicitudes[0];
      component.verDetalle(s);
      expect(component.selectedSolicitud()).toBe(s);
      expect(modalServiceSpy.show).toHaveBeenCalledWith('modalDetalle');
    });
  });

  describe('confirmarAprobar', () => {
    it('should set confirm action', () => {
      component.confirmarAprobar(1);
      expect(component.confirmAction()?.action).toBe('aprobar');
      expect(modalServiceSpy.show).toHaveBeenCalledWith('modalConfirmar');
    });

    it('should do nothing for invalid id', () => {
      component.confirmarAprobar(999);
      expect(component.confirmAction()).toBeNull();
    });
  });

  describe('confirmarRechazar', () => {
    it('should set confirm action to rechazar', () => {
      component.confirmarRechazar(2);
      expect(component.confirmAction()?.action).toBe('rechazar');
    });
  });

  describe('confirmarReconsiderar', () => {
    it('should set confirm action to reconsiderar', () => {
      component.confirmarReconsiderar(3);
      expect(component.confirmAction()?.action).toBe('reconsiderar');
    });
  });

  describe('ejecutarAccion', () => {
    it('should do nothing if no confirmAction', () => {
      component.ejecutarAccion();
    });

    it('should aprobar', () => {
      component.confirmAction.set({ id: 1, action: 'aprobar', label: 'Aprobar?' });
      component.ejecutarAccion();
      const sol = component.allSolicitudes.find(s => s.id === 1);
      expect(sol?.estado).toBe('aprobado');
      expect(toastServiceSpy.show).toHaveBeenCalled();
      expect(modalServiceSpy.hide).toHaveBeenCalledWith('modalConfirmar');
    });

    it('should rechazar', () => {
      component.confirmAction.set({ id: 1, action: 'rechazar', label: 'Rechazar?' });
      component.ejecutarAccion();
      const sol = component.allSolicitudes.find(s => s.id === 1);
      expect(sol?.estado).toBe('rechazado');
    });

    it('should reconsiderar', () => {
      component.confirmAction.set({ id: 2, action: 'reconsiderar', label: 'Reconsiderar?' });
      component.ejecutarAccion();
      const sol = component.allSolicitudes.find(s => s.id === 2);
      expect(sol?.estado).toBe('pendiente');
    });
  });
});
