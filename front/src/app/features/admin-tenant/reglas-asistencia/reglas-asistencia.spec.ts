import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReglasAsistencia } from './reglas-asistencia';

describe('ReglasAsistencia', () => {
  let component: ReglasAsistencia;
  let fixture: ComponentFixture<ReglasAsistencia>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReglasAsistencia],
    }).compileComponents();

    fixture = TestBed.createComponent(ReglasAsistencia);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('signal initial values', () => {
    it('should have loading as false', () => {
      expect(component.loading()).toBeFalse();
    });

    it('should have error as empty string', () => {
      expect(component.error()).toBe('');
    });

    it('should have tolerancia as 15', () => {
      expect(component.tolerancia()).toBe(15);
    });

    it('should have redondeo as estandar', () => {
      expect(component.redondeo()).toBe('estandar');
    });

    it('should have huellaActivada as true', () => {
      expect(component.huellaActivada()).toBeTrue();
    });

    it('should have autorizacionPrevia as false', () => {
      expect(component.autorizacionPrevia()).toBeFalse();
    });

    it('should have topeSemanal as true', () => {
      expect(component.topeSemanal()).toBeTrue();
    });

    it('should have recargoNocturno as false', () => {
      expect(component.recargoNocturno()).toBeFalse();
    });
  });

  describe('toleranciaTexto', () => {
    it('should return sin tolerancia for 0', () => {
      component.tolerancia.set(0);
      expect(component.toleranciaTexto).toBe('Sin tolerancia — el registro debe ser exacto a la hora de entrada.');
    });

    it('should return estricta for 1-5', () => {
      component.tolerancia.set(3);
      expect(component.toleranciaTexto).toContain('Tolerancia muy estricta de 3 minutos');
    });

    it('should use singular for 1 minute', () => {
      component.tolerancia.set(1);
      expect(component.toleranciaTexto).toContain('1 minuto');
    });

    it('should return moderada for 6-15', () => {
      component.tolerancia.set(10);
      expect(component.toleranciaTexto).toContain('Tolerancia moderada de 10 minutos');
    });

    it('should return amplia for 16-30', () => {
      component.tolerancia.set(20);
      expect(component.toleranciaTexto).toContain('Tolerancia amplia de 20 minutos');
    });

    it('should return muy amplia for >30', () => {
      component.tolerancia.set(45);
      expect(component.toleranciaTexto).toContain('Tolerancia muy amplia de 45 minutos');
    });
  });

  describe('alternarHuella', () => {
    it('should toggle huellaActivada', () => {
      component.alternarHuella();
      expect(component.huellaActivada()).toBeFalse();
      component.alternarHuella();
      expect(component.huellaActivada()).toBeTrue();
    });
  });

  describe('alternarAutorizacion', () => {
    it('should toggle autorizacionPrevia', () => {
      component.alternarAutorizacion();
      expect(component.autorizacionPrevia()).toBeTrue();
      component.alternarAutorizacion();
      expect(component.autorizacionPrevia()).toBeFalse();
    });
  });

  describe('alternarTope', () => {
    it('should toggle topeSemanal', () => {
      component.alternarTope();
      expect(component.topeSemanal()).toBeFalse();
      component.alternarTope();
      expect(component.topeSemanal()).toBeTrue();
    });
  });

  describe('alternarRecargo', () => {
    it('should toggle recargoNocturno', () => {
      component.alternarRecargo();
      expect(component.recargoNocturno()).toBeTrue();
      component.alternarRecargo();
      expect(component.recargoNocturno()).toBeFalse();
    });
  });
});
