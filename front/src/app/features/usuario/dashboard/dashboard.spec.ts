import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dashboard],
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize signals with default values', () => {
    expect(component.loading()).toBeFalse();
    expect(component.error()).toBe('');
  });

  it('should have usuario data', () => {
    expect(component.usuario.nombre).toBe('Roberto');
    expect(component.usuario.rol).toBe('Docente');
  });

  it('should have turno data', () => {
    expect(component.turno.horaInicio).toBe('08:00');
    expect(component.turno.horaFin).toBe('13:00');
  });

  it('should have metricas data', () => {
    expect(component.metricas.asistenciasPuntuales).toBe(22);
    expect(component.metricas.puntualidad).toBe(98);
  });

  it('should have 3 actividadesRecientes', () => {
    expect(component.actividadesRecientes.length).toBe(3);
  });

  it('should have 3 ultimosRegistros', () => {
    expect(component.ultimosRegistros.length).toBe(3);
  });

  it('should have 8 todosLosRegistros', () => {
    expect(component.todosLosRegistros.length).toBe(8);
  });

  describe('verHistorialCompleto', () => {
    it('should set modalHistorialOpen to true', () => {
      component.verHistorialCompleto();
      expect(component.modalHistorialOpen).toBeTrue();
    });
  });

  describe('cerrarModalHistorial', () => {
    it('should set modalHistorialOpen to false', () => {
      component.modalHistorialOpen = true;
      component.cerrarModalHistorial();
      expect(component.modalHistorialOpen).toBeFalse();
    });
  });
});
