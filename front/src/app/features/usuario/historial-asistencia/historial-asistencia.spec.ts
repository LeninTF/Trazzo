import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HistorialAsistencia } from './historial-asistencia';

describe('HistorialAsistencia', () => {
  let component: HistorialAsistencia;
  let fixture: ComponentFixture<HistorialAsistencia>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HistorialAsistencia],
    }).compileComponents();

    fixture = TestBed.createComponent(HistorialAsistencia);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('creates the historial-asistencia component', () => {
    expect(component).toBeTruthy();
  });

  it('has default historial signal state', () => {
    expect(component.loading()).toBeFalse();
    expect(component.error()).toBe('');
  });

  it('should have default mesActual', () => {
    expect(component.mesActual).toBe('Junio 2026');
  });

  it('should have 10 registros', () => {
    expect(component.registros.length).toBe(10);
  });

  describe('cambiarMes', () => {
    it('should advance to next month', () => {
      component.cambiarMes(1);
      expect(component.mesActual).toBe('Julio 2026');
    });

    it('should go to previous month', () => {
      component.cambiarMes(-1);
      expect(component.mesActual).toBe('Mayo 2026');
    });

    it('should wrap to next year when advancing past December', () => {
      component.mesActual = 'Diciembre 2026';
      component.cambiarMes(1);
      expect(component.mesActual).toBe('Enero 2027');
    });

    it('should wrap to previous year when going before January', () => {
      component.mesActual = 'Enero 2026';
      component.cambiarMes(-1);
      expect(component.mesActual).toBe('Diciembre 2025');
    });
  });

  describe('getters', () => {
    it('should count completos', () => {
      expect(component.completos).toBe(7);
    });

    it('should count tardanzas', () => {
      expect(component.tardanzas).toBe(2);
    });

    it('should count faltas', () => {
      expect(component.faltas).toBe(0);
    });

    it('should count justificados', () => {
      expect(component.justificados).toBe(1);
    });

    it('should return resumen with 4 entries', () => {
      expect(component.resumen.length).toBe(4);
      expect(component.resumen[0].label).toBe('A tiempo');
      expect(component.resumen[0].valor).toBe(7);
    });

    it('should calculate eficiencia', () => {
      expect(component.eficiencia).toBe(70);
    });
  });

  describe('exportarCSV', () => {
    it('should create and click a download link', () => {
      const link = document.createElement('a');
      const clickSpy = spyOn(link, 'click');
      spyOn(document, 'createElement').and.returnValue(link);
      spyOn(URL, 'createObjectURL').and.returnValue('blob:csv');
      spyOn(URL, 'revokeObjectURL');

      component.exportarCSV();

      expect(clickSpy).toHaveBeenCalled();
      expect(link.download).toContain('asistencia_');
      expect(link.download).toMatch(/asistencia_\d{8}\.csv/);
    });
  });
});
