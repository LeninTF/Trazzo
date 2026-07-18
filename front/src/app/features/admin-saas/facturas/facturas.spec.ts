import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Facturas } from './facturas';
import { ApiService } from '../../../api/services/api.service';
import { ExportService } from '../../../services/export.service';
import { ToastService } from '../../../services/toast.service';
import { ModalService } from '../../../services/modal.service';
import type { InvoiceProfile } from '../../../api/types';

describe('Facturas', () => {
  let component: Facturas;
  let fixture: ComponentFixture<Facturas>;
  let exportServiceSpy: jasmine.SpyObj<ExportService>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;
  let modalServiceSpy: jasmine.SpyObj<ModalService>;
  let mockSaas: {
    listInvoices: jasmine.Spy;
    getInvoice: jasmine.Spy;
    exportInvoicesExcel: jasmine.Spy;
    exportInvoicesPdf: jasmine.Spy;
  };

  const mockInvoice = (id: string, paymentStatus: string, total: number, createdAt = '2026-05-10T00:00:00'): InvoiceProfile => ({
    id,
    tenantId: 'tenant-1',
    clientName: 'Cliente Test',
    clientTaxId: '20123456789',
    invoiceSeries: 'F001',
    consecutiveNumber: '1',
    voucherType: 'FACTURA',
    subTotal: total / 1.18,
    taxAmount: total - total / 1.18,
    total,
    paymentStatus,
    expirationDate: null,
    createdAt,
  });

  const mockPage = (items: InvoiceProfile[], totalElements = items.length) => ({
    content: items, page: 0, size: 5, totalElements, totalPages: Math.max(1, Math.ceil(totalElements / 5)),
  });

  beforeEach(async () => {
    mockSaas = {
      listInvoices: jasmine.createSpy('listInvoices').and.returnValue(of(mockPage([
        mockInvoice('inv-1', 'PENDIENTE', 100),
        mockInvoice('inv-2', 'COMPLETADO', 200),
      ]))),
      getInvoice: jasmine.createSpy('getInvoice').and.returnValue(of(mockInvoice('inv-1', 'PENDIENTE', 100))),
      exportInvoicesExcel: jasmine.createSpy('exportInvoicesExcel').and.returnValue(of(new Blob(['excel']))),
      exportInvoicesPdf: jasmine.createSpy('exportInvoicesPdf').and.returnValue(of(new Blob(['pdf']))),
    };
    exportServiceSpy = jasmine.createSpyObj('ExportService', ['exportCSV', 'downloadBlob']);
    toastServiceSpy = jasmine.createSpyObj('ToastService', ['info', 'error', 'success']);
    modalServiceSpy = jasmine.createSpyObj('ModalService', ['show', 'hide']);

    await TestBed.configureTestingModule({
      imports: [Facturas],
      providers: [
        { provide: ApiService, useValue: { saas: mockSaas } },
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

  it('loads invoices from the backend on init', () => {
    expect(mockSaas.listInvoices).toHaveBeenCalledWith(jasmine.objectContaining({ page: 0, size: 5 }));
    expect(component.items()).toHaveSize(2);
    expect(component.totalElements()).toBe(2);
    expect(component.loading()).toBeFalse();
  });

  it('sets error when loading invoices fails', () => {
    mockSaas.listInvoices.and.returnValue(throwError(() => new Error('fail')));
    component['cargarFacturas']();
    expect(component.error()).toBe('No se pudieron cargar las facturas.');
  });

  it('shows an empty list gracefully', () => {
    mockSaas.listInvoices.and.returnValue(of(mockPage([], 0)));
    component['cargarFacturas']();
    expect(component.items()).toHaveSize(0);
    expect(component.totalFacturado()).toBe(0);
    expect(component.promedioFactura()).toBe(0);
  });

  describe('metrics', () => {
    it('should compute totalFacturado from loaded items', () => {
      expect(component.totalFacturado()).toBe(300);
    });

    it('should compute totalPendientes and totalCompletadas', () => {
      expect(component.totalPendientes()).toBe(1);
      expect(component.totalCompletadas()).toBe(1);
    });

    it('should compute promedioFactura', () => {
      expect(component.promedioFactura()).toBe(150);
    });
  });

  describe('chartConfig', () => {
    it('should group totals by year-month', () => {
      const config = component.chartConfig();
      expect(config.series[0].data).toEqual([300]);
      expect(config.xaxis.categories).toEqual(['2026-05']);
    });
  });

  describe('totalPaginas', () => {
    it('should calculate total pages', () => {
      expect(component.totalPaginas()).toBeGreaterThanOrEqual(1);
    });
  });

  describe('rangoInfo', () => {
    it('should return formatted range info', () => {
      const info = component.rangoInfo();
      expect(info).toContain('Mostrando');
      expect(info).toContain('facturas');
    });

    it('should handle zero facturas', () => {
      mockSaas.listInvoices.and.returnValue(of(mockPage([], 0)));
      component['cargarFacturas']();
      expect(component.rangoInfo()).toBe('Mostrando 0 facturas');
    });
  });

  describe('irPagina', () => {
    it('should set pagina and reload', () => {
      component.irPagina(3);
      expect(component.pagina()).toBe(3);
      expect(mockSaas.listInvoices).toHaveBeenCalledWith(jasmine.objectContaining({ page: 2 }));
    });
  });

  describe('onFilterChange', () => {
    it('should reset pagina and reload with filters', () => {
      component.irPagina(3);
      component.filterForm.patchValue({ paymentStatus: 'COMPLETADO' });
      component.onFilterChange();
      expect(component.pagina()).toBe(1);
      expect(mockSaas.listInvoices).toHaveBeenCalledWith(jasmine.objectContaining({ paymentStatus: 'COMPLETADO', page: 0 }));
    });

    it('should omit paymentStatus filter when todos', () => {
      component.filterForm.patchValue({ paymentStatus: 'todos' });
      component.onFilterChange();
      const lastCall = mockSaas.listInvoices.calls.mostRecent().args[0];
      expect(lastCall.paymentStatus).toBeUndefined();
    });
  });

  describe('verDetalle', () => {
    it('should load detail and show modal', () => {
      const f = component.items()[0];
      component.verDetalle(f);
      expect(mockSaas.getInvoice).toHaveBeenCalledWith(f.id);
      expect(component.facturaSeleccionada()).toEqual(mockInvoice('inv-1', 'PENDIENTE', 100));
      expect(modalServiceSpy.show).toHaveBeenCalledWith('modalDetalleTransaccion');
    });

    it('should show error toast when detail load fails', () => {
      mockSaas.getInvoice.and.returnValue(throwError(() => new Error('fail')));
      component.verDetalle(component.items()[0]);
      expect(toastServiceSpy.error).toHaveBeenCalledWith('No se pudo cargar el detalle de la factura.');
    });
  });

  describe('exportarCSV', () => {
    it('should call exportService.exportCSV', () => {
      component.exportarCSV();
      expect(exportServiceSpy.exportCSV).toHaveBeenCalledWith(
        jasmine.stringMatching(/^facturas-/),
        ['Cliente', 'RUC', 'Comprobante', 'Fecha', 'Monto', 'Estado'],
        jasmine.any(Array)
      );
      expect(toastServiceSpy.info).toHaveBeenCalledWith('CSV exportado correctamente.');
    });
  });

  describe('exportarExcel', () => {
    it('should download the excel blob', () => {
      component.exportarExcel();
      expect(mockSaas.exportInvoicesExcel).toHaveBeenCalled();
      expect(exportServiceSpy.downloadBlob).toHaveBeenCalledWith(jasmine.stringMatching(/^facturas-.*\.xlsx$/), jasmine.any(Blob));
    });

    it('should show error toast when excel export fails', () => {
      mockSaas.exportInvoicesExcel.and.returnValue(throwError(() => new Error('fail')));
      component.exportarExcel();
      expect(toastServiceSpy.error).toHaveBeenCalledWith('No se pudo exportar a Excel.');
    });
  });

  describe('exportarPDF', () => {
    it('should download the pdf blob', () => {
      component.exportarPDF();
      expect(mockSaas.exportInvoicesPdf).toHaveBeenCalled();
      expect(exportServiceSpy.downloadBlob).toHaveBeenCalledWith(jasmine.stringMatching(/^facturas-.*\.pdf$/), jasmine.any(Blob));
    });

    it('should show error toast when pdf export fails', () => {
      mockSaas.exportInvoicesPdf.and.returnValue(throwError(() => new Error('fail')));
      component.exportarPDF();
      expect(toastServiceSpy.error).toHaveBeenCalledWith('No se pudo exportar a PDF.');
    });
  });
});
