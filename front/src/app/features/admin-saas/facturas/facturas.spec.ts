import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Facturas } from './facturas';
import { ExportService } from '../../../services/export.service';
import { ToastService } from '../../../services/toast.service';
import { ModalService } from '../../../services/modal.service';

describe('Facturas', () => {
  let component: Facturas;
  let fixture: ComponentFixture<Facturas>;
  let exportServiceSpy: jasmine.SpyObj<ExportService>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;
  let modalServiceSpy: jasmine.SpyObj<ModalService>;

  beforeEach(async () => {
    exportServiceSpy = jasmine.createSpyObj('ExportService', ['exportCSV']);
    toastServiceSpy = jasmine.createSpyObj('ToastService', ['info']);
    modalServiceSpy = jasmine.createSpyObj('ModalService', ['show', 'hide']);

    await TestBed.configureTestingModule({
      imports: [Facturas],
      providers: [
        { provide: ExportService, useValue: exportServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
        { provide: ModalService, useValue: modalServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Facturas);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('creates the facturas component', () => {
    expect(component).toBeTruthy();
  });

  it('initializes signals to defaults', () => {
    expect(component.loading()).toBeFalse();
    expect(component.error()).toBe('');
    expect(component.pagina()).toBe(1);
  });

  it('should have 32 transacciones', () => {
    expect(component.transacciones.length).toBe(32);
  });

  describe('getters', () => {
    it('should return mrr', () => {
      expect(component.mrr).toBe('S/. 245,820.00');
    });

    it('should return facturasVencidas', () => {
      expect(component.facturasVencidas).toBe(14);
    });

    it('should return arpu', () => {
      expect(component.arpu).toBe('S/. 158.50');
    });
  });

  describe('transaccionesFiltradas', () => {
    it('should filter by estado', () => {
      component.filterForm.patchValue({ estado: 'pagado' });
      fixture.detectChanges();
      const filtered = component.transaccionesFiltradas();
      expect(filtered.every(t => t.estado === 'pagado')).toBeTrue();
    });
  });

  describe('totalPaginas', () => {
    it('should calculate total pages', () => {
      expect(component.totalPaginas()).toBeGreaterThanOrEqual(1);
    });
  });

  describe('totalTransacciones', () => {
    it('should return filtered count', () => {
      expect(component.totalTransacciones()).toBe(component.transaccionesFiltradas().length);
    });
  });

  describe('rangoInfo', () => {
    it('should return formatted range info', () => {
      const info = component.rangoInfo();
      expect(info).toContain('Mostrando');
      expect(info).toContain('transacciones');
    });

    it('should handle zero transactions', () => {
      component.filterForm.patchValue({ estado: 'nonexistent' });
      fixture.detectChanges();
      expect(component.rangoInfo()).toBe('Mostrando 0 transacciones');
    });
  });

  describe('irPagina', () => {
    it('should set pagina', () => {
      component.irPagina(3);
      expect(component.pagina()).toBe(3);
    });
  });

  describe('cambiarPagina', () => {
    it('should increment page when totalPages allows', () => {
      component.irPagina(1);
      component.cambiarPagina(1);
      expect(component.pagina()).toBeGreaterThanOrEqual(1);
    });

    it('should decrement page', () => {
      component.irPagina(2);
      component.cambiarPagina(-1);
      expect(component.pagina()).toBe(1);
    });

    it('should not go below 1', () => {
      component.cambiarPagina(-1);
      expect(component.pagina()).toBe(1);
    });
  });

  describe('onRangoChange', () => {
    it('should reset pagina on range change', () => {
      component.irPagina(3);
      component.onRangoChange();
      expect(component.pagina()).toBe(1);
    });

    it('should clear date fields when not personalizado', () => {
      component.filterForm.patchValue({ fechaDesde: '2026-01-01', fechaHasta: '2026-06-01' });
      component.onRangoChange();
      expect(component.filterForm.value.fechaDesde).toBe('');
      expect(component.filterForm.value.fechaHasta).toBe('');
    });

    it('should keep date fields when personalizado', () => {
      component.filterForm.patchValue({ rango: 'personalizado', fechaDesde: '2026-01-01', fechaHasta: '2026-06-01' });
      component.onRangoChange();
      expect(component.filterForm.value.fechaDesde).toBe('2026-01-01');
    });
  });

  describe('onEstadoChange', () => {
    it('should reset pagina on estado change', () => {
      component.irPagina(3);
      component.onEstadoChange();
      expect(component.pagina()).toBe(1);
    });
  });

  describe('verDetalle', () => {
    it('should set selected transaccion and show modal', () => {
      const t = component.transacciones[0];
      component.verDetalle(t);
      expect(component.transaccionSeleccionada()).toBe(t);
      expect(modalServiceSpy.show).toHaveBeenCalledWith('modalDetalleTransaccion');
    });
  });

  describe('exportarCSV', () => {
    it('should call exportService.exportCSV', () => {
      component.exportarCSV();
      expect(exportServiceSpy.exportCSV).toHaveBeenCalled();
      expect(exportServiceSpy.exportCSV).toHaveBeenCalledWith(
        jasmine.stringMatching(/^transacciones-/),
        ['Tenant', 'Plan', 'ID Transacción', 'Fecha', 'Monto', 'Estado'],
        jasmine.any(Array)
      );
      expect(toastServiceSpy.info).toHaveBeenCalledWith('CSV exportado correctamente.');
    });
  });
});
