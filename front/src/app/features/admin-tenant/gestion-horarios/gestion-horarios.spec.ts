import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GestionHorarios } from './gestion-horarios';

describe('GestionHorarios', () => {
  let component: GestionHorarios;
  let fixture: ComponentFixture<GestionHorarios>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GestionHorarios],
    }).compileComponents();

    fixture = TestBed.createComponent(GestionHorarios);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have default active section turnos', () => {
    expect(component.activeSection).toBe('turnos');
  });

  it('should have section meta for all sections', () => {
    expect(component.sectionMeta['turnos']).toBeDefined();
    expect(component.sectionMeta['asignacion']).toBeDefined();
    expect(component.sectionMeta['feriados']).toBeDefined();
  });

  it('should get currentSection meta', () => {
    expect(component.currentSection.title).toBe('Turnos');
    component.activeSection = 'asignacion';
    expect(component.currentSection.title).toBe('Asignación');
  });

  it('should change section', () => {
    component.activeSection = 'feriados';
    expect(component.activeSection).toBe('feriados');
  });

  it('should call nuevoTurno', () => {
    component.nuevoTurno();
    expect(component.activeSection).toBe('turnos');
  });

});
