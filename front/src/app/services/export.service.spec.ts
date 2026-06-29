import { TestBed } from '@angular/core/testing';
import { ExportService } from './export.service';

describe('ExportService', () => {
  let service: ExportService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ExportService);
  });

  it('creates the export service', () => {
    expect(service).toBeTruthy();
  });

  describe('escCSV', () => {
    it('should wrap value in double quotes', () => {
      expect(service.escCSV('hello')).toBe('"hello"');
    });

    it('should escape internal double quotes', () => {
      expect(service.escCSV('he"llo')).toBe('"he""llo"');
    });

    it('should handle null or undefined', () => {
      expect(service.escCSV('')).toBe('""');
    });
  });

  describe('dateSuffix', () => {
    it('should return YYYYMMDD format', () => {
      const result = service.dateSuffix();
      expect(result).toMatch(/^\d{8}$/);
    });
  });

  describe('exportCSV', () => {
    it('should create and click a download link', () => {
      const link = document.createElement('a');
      const clickSpy = spyOn(link, 'click');
      spyOn(document, 'createElement').and.returnValue(link);
      spyOn(URL, 'createObjectURL').and.returnValue('blob:test');
      spyOn(URL, 'revokeObjectURL');

      service.exportCSV('test.csv', ['Name', 'Age'], [['Alice', '30'], ['Bob', '25']]);

      expect(link.download).toBe('test.csv');
      expect(link.href).toBe('blob:test');
      expect(clickSpy).toHaveBeenCalled();
      expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:test');
    });
  });
});
