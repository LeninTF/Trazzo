import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AsignacionComponent } from './asignacion';

describe('AsignacionComponent', () => {
  let component: AsignacionComponent;
  let fixture: ComponentFixture<AsignacionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AsignacionComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AsignacionComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 6 areas', () => {
    expect(component.areas.length).toBe(6);
  });

  it('should have 5 initial asignaciones', () => {
    expect(component.asignaciones().length).toBe(5);
  });

  it('should have 3 turnos disponibles', () => {
    expect(component.turnosDisponibles.length).toBe(3);
  });

  it('should filter by search term', () => {
    component.onSearch('María');
    expect(component.filteredAsignaciones().length).toBe(1);
    expect(component.filteredAsignaciones()[0].trabajador).toBe('María García');
  });

  it('should filter by area', () => {
    component.onAreaFilter('Ventas');
    expect(component.filteredAsignaciones().length).toBe(1);
    expect(component.filteredAsignaciones()[0].area).toBe('Ventas');
  });

  it('should filter by both search and area', () => {
    component.onSearch('Carlos');
    component.onAreaFilter('Ventas');
    expect(component.filteredAsignaciones().length).toBe(1);
  });

  it('should show all when no filters', () => {
    component.onSearch('');
    component.onAreaFilter('');
    expect(component.filteredAsignaciones().length).toBe(5);
  });

  it('should openModal', () => {
    component.openModal();
    expect(component.showModal).toBeTrue();
    expect(component.selectedTurnoHorarios).toEqual([]);
  });

  it('should closeModal', () => {
    component.openModal();
    component.closeModal();
    expect(component.showModal).toBeFalse();
  });

  it('should onTurnoChange update horarios', () => {
    component.openModal();
    component.asignacionForm.patchValue({ turnoId: '1' });
    component.onTurnoChange();
    expect(component.selectedTurnoHorarios.length).toBe(2);
    expect(component.selectedTurnoHorarios[0].label).toBe('08:00 – 12:00');
  });

  it('should submitAsignacion with valid form', () => {
    component.openModal();
    component.asignacionForm.patchValue({
      trabajador: 'Test User',
      area: 'RRHH',
      turnoId: '1',
      horarioId: '1',
    });
    component.onTurnoChange();
    component.asignacionForm.patchValue({ horarioId: '1' });
    component.submitAsignacion();
    expect(component.asignaciones().length).toBe(6);
    expect(component.showModal).toBeFalse();
  });

  it('should not submitAsignacion with invalid form', () => {
    component.openModal();
    component.submitAsignacion();
    expect(component.asignaciones().length).toBe(5);
  });

  it('should deleteAsignacion', () => {
    component.deleteAsignacion(1);
    expect(component.asignaciones().length).toBe(4);
  });
});
