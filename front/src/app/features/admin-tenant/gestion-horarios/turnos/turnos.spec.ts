import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TurnosComponent } from './turnos';

describe('TurnosComponent', () => {
  let component: TurnosComponent;
  let fixture: ComponentFixture<TurnosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TurnosComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TurnosComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 3 initial turnos', () => {
    expect(component.turnos.length).toBe(3);
  });

  it('should openNewTurnoModal', () => {
    component.openNewTurnoModal();
    expect(component.showNewTurnoModal).toBeTrue();
  });

  it('should cancelNewTurno', () => {
    component.openNewTurnoModal();
    component.cancelNewTurno();
    expect(component.showNewTurnoModal).toBeFalse();
  });

  it('should addTurno with valid form', () => {
    component.openNewTurnoModal();
    component.turnoForm.patchValue({ nombre: 'Turno Test' });
    component.addTurno();
    expect(component.turnos.length).toBe(4);
    expect(component.turnos.find(t => t.nombre === 'Turno Test')).toBeTruthy();
    expect(component.showNewTurnoModal).toBeFalse();
  });

  it('should not addTurno with invalid form', () => {
    component.openNewTurnoModal();
    component.turnoForm.patchValue({ nombre: '' });
    component.addTurno();
    expect(component.turnos.length).toBe(3);
  });

  it('should startEditTurno', () => {
    component.startEditTurno(component.turnos[0]);
    expect(component.editingTurnoId).toBe(1);
    expect(component.editTurnoForm.get('nombre')?.value).toBe('Turno Mañana');
  });

  it('should cancelEditTurno', () => {
    component.startEditTurno(component.turnos[0]);
    component.cancelEditTurno();
    expect(component.editingTurnoId).toBeNull();
  });

  it('should saveEditTurno with valid form', () => {
    component.startEditTurno(component.turnos[0]);
    component.editTurnoForm.patchValue({ nombre: 'Turno Editado' });
    component.saveEditTurno(component.turnos[0]);
    expect(component.turnos[0].nombre).toBe('Turno Editado');
    expect(component.editingTurnoId).toBeNull();
  });

  it('should not saveEditTurno with invalid form', () => {
    component.startEditTurno(component.turnos[0]);
    component.editTurnoForm.patchValue({ nombre: '' });
    component.saveEditTurno(component.turnos[0]);
    expect(component.turnos[0].nombre).toBe('Turno Mañana');
  });

  it('should deleteTurno', () => {
    component.deleteTurno(1);
    expect(component.turnos.length).toBe(2);
  });

  it('should showAddHorario', () => {
    component.showAddHorario(1);
    expect(component.activeHorarioTurnoId).toBe(1);
  });

  it('should cancelAddHorario', () => {
    component.showAddHorario(1);
    component.cancelAddHorario();
    expect(component.activeHorarioTurnoId).toBeNull();
  });

  it('should addHorario with valid form', () => {
    component.showAddHorario(1);
    component.horarioForm.patchValue({ inicio: '10:00', fin: '11:00' });
    component.addHorario(1);
    const turno = component.turnos.find(t => t.id === 1)!;
    expect(turno.horarios.length).toBe(3);
    expect(component.activeHorarioTurnoId).toBeNull();
  });

  it('should not addHorario with invalid form', () => {
    component.showAddHorario(1);
    component.addHorario(1);
    const turno = component.turnos.find(t => t.id === 1)!;
    expect(turno.horarios.length).toBe(2);
  });

  it('should startEditHorario', () => {
    component.startEditHorario(1, component.turnos[0].horarios[0]);
    expect(component.editingHorarioKey).toBe('1-1');
    expect(component.editHorarioForm.get('inicio')?.value).toBe('08:00');
  });

  it('should cancelEditHorario', () => {
    component.startEditHorario(1, component.turnos[0].horarios[0]);
    component.cancelEditHorario();
    expect(component.editingHorarioKey).toBeNull();
  });

  it('should saveEditHorario with valid form', () => {
    const horario = component.turnos[0].horarios[0];
    component.startEditHorario(1, horario);
    component.editHorarioForm.patchValue({ inicio: '09:00', fin: '13:00' });
    component.saveEditHorario(1, horario);
    expect(horario.inicio).toBe('09:00');
    expect(horario.fin).toBe('13:00');
    expect(component.editingHorarioKey).toBeNull();
  });

  it('should not saveEditHorario with invalid form', () => {
    const horario = component.turnos[0].horarios[0];
    component.startEditHorario(1, horario);
    component.editHorarioForm.patchValue({ inicio: '', fin: '' });
    component.saveEditHorario(1, horario);
    expect(horario.inicio).toBe('08:00');
  });

  it('should deleteHorario', () => {
    component.deleteHorario(1, 1);
    const turno = component.turnos.find(t => t.id === 1)!;
    expect(turno.horarios.length).toBe(1);
  });

  it('should isEditingHorario', () => {
    component.editingHorarioKey = '1-1';
    expect(component.isEditingHorario(1, 1)).toBeTrue();
    expect(component.isEditingHorario(1, 2)).toBeFalse();
  });

  it('should calcularDuracion normal', () => {
    expect(component.calcularDuracion('08:00', '12:00')).toBe('4h');
  });

  it('should calcularDuracion overnight', () => {
    expect(component.calcularDuracion('22:00', '02:00')).toBe('4h');
  });

  it('should calcularDuracion with minutes', () => {
    expect(component.calcularDuracion('08:30', '10:45')).toBe('2h 15m');
  });
});
