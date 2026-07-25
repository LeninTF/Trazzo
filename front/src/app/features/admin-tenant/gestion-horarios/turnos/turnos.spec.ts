import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { TurnosComponent } from './turnos';
import { ApiService } from '../../../../api/services/api.service';
import { ToastService } from '../../../../services/toast.service';
import { of } from 'rxjs';

describe('TurnosComponent', () => {
  let component: TurnosComponent;
  let fixture: ComponentFixture<TurnosComponent>;

  const mockShifts = {
    content: [
      { id: 1, name: 'Turno Mañana', description: null, schedules: [
        { id: 1, shift_id: 1, name: '08:00-12:00', entry_time: '08:00:00', departure_time: '12:00:00', tolerancias: [] },
      ], created_at: new Date().toISOString(), updated_at: new Date().toISOString() },
    ],
    page: 0, size: 50, totalElements: 1, totalPages: 1,
  };

  const mockApi = {
    horarios: {
      listShifts: jasmine.createSpy('listShifts').and.returnValue(of(mockShifts)),
      createShift: jasmine.createSpy('createShift').and.returnValue(of({ id: 2, name: 'Test', schedules: [] })),
      patchShift: jasmine.createSpy('patchShift').and.returnValue(of({ id: 1, name: 'Updated', schedules: [] })),
      deleteShift: jasmine.createSpy('deleteShift').and.returnValue(of(undefined)),
      createSchedule: jasmine.createSpy('createSchedule').and.returnValue(of({ id: 3, shift_id: 1, name: '09:00-13:00', entry_time: '09:00', departure_time: '13:00', tolerancias: [] })),
      patchSchedule: jasmine.createSpy('patchSchedule').and.returnValue(of({ id: 1, entry_time: '09:00', departure_time: '13:00' })),
      deleteSchedule: jasmine.createSpy('deleteSchedule').and.returnValue(of(undefined)),
    },
  };

  const mockToast = jasmine.createSpyObj('ToastService', ['success', 'error']);

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TurnosComponent],
      providers: [
        provideHttpClient(),
        { provide: ApiService, useValue: mockApi },
        { provide: ToastService, useValue: mockToast },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TurnosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load shifts on init', () => {
    expect(mockApi.horarios.listShifts).toHaveBeenCalled();
    expect(component.turnos.length).toBe(1);
    expect(component.turnos[0].nombre).toBe('Turno Mañana');
  });

  it('should open and cancel new turno modal', () => {
    component.openNewTurnoModal();
    expect(component.showNewTurnoModal).toBeTrue();
    component.cancelNewTurno();
    expect(component.showNewTurnoModal).toBeFalse();
  });

  it('should add a new turno', async () => {
    component.openNewTurnoModal();
    component.turnoForm.setValue({ nombre: 'Nuevo Turno' });
    await component.addTurno();
    expect(mockApi.horarios.createShift).toHaveBeenCalledWith({ name: 'Nuevo Turno' });
    expect(component.showNewTurnoModal).toBeFalse();
  });

  it('should not add turno if form is invalid', () => {
    component.openNewTurnoModal();
    component.turnoForm.setValue({ nombre: '' });
    expect(component.turnoForm.invalid).toBeTrue();
  });

  it('should show and cancel add horario', () => {
    component.showAddHorario(1);
    expect(component.activeHorarioTurnoId).toBe(1);
    component.cancelAddHorario();
    expect(component.activeHorarioTurnoId).toBeNull();
  });

  it('should add horario', async () => {
    component.showAddHorario(1);
    component.horarioForm.setValue({ inicio: '09:00', fin: '13:00' });
    await component.addHorario(1);
    expect(mockApi.horarios.createSchedule).toHaveBeenCalled();
    expect(component.activeHorarioTurnoId).toBeNull();
  });

  it('should not add horario if form is invalid', () => {
    component.showAddHorario(1);
    component.horarioForm.setValue({ inicio: '', fin: '' });
    expect(component.horarioForm.invalid).toBeTrue();
  });

  it('should start and cancel editing a turno', () => {
    const turno = component.turnos[0];
    component.startEditTurno(turno);
    expect(component.editingTurnoId).toBe(turno.id);
    component.cancelEditTurno();
    expect(component.editingTurnoId).toBeNull();
  });

  it('should save edit turno', async () => {
    const turno = component.turnos[0];
    component.startEditTurno(turno);
    component.editTurnoForm.setValue({ nombre: 'Edited' });
    await component.saveEditTurno(turno);
    expect(mockApi.horarios.patchShift).toHaveBeenCalledWith(turno.id, { name: 'Edited' });
    expect(component.editingTurnoId).toBeNull();
  });

  it('should delete turno', async () => {
    await component.deleteTurno(1);
    expect(mockApi.horarios.deleteShift).toHaveBeenCalledWith(1);
  });

  it('should delete horario', async () => {
    await component.deleteHorario(1, 1);
    expect(mockApi.horarios.deleteSchedule).toHaveBeenCalledWith(1);
  });

  it('should calculate duration correctly', () => {
    expect(component.calcularDuracion('08:00', '12:00')).toBe('4h');
    expect(component.calcularDuracion('09:30', '11:45')).toBe('2h 15m');
    expect(component.calcularDuracion('22:00', '06:00')).toBe('8h');
  });

  it('isEditingHorario should return correct value', () => {
    component.editingHorarioKey = '1-2';
    expect(component.isEditingHorario(1, 2)).toBeTrue();
    expect(component.isEditingHorario(1, 3)).toBeFalse();
  });

  it('should start, cancel, and save edit horario', async () => {
    const horario = { id: 1, inicio: '08:00', fin: '12:00' };
    component.startEditHorario(1, horario);
    expect(component.editingHorarioKey).toBe('1-1');
    component.editHorarioForm.setValue({ inicio: '09:00', fin: '13:00' });
    await component.saveEditHorario(1, horario);
    expect(mockApi.horarios.patchSchedule).toHaveBeenCalledWith(1, { entry_time: '09:00', departure_time: '13:00' });
    expect(component.editingHorarioKey).toBeNull();
  });

  it('should handle cargarTurnos error', async () => {
    mockApi.horarios.listShifts.and.callFake(() => { throw new Error('fail'); });
    await component.cargarTurnos();
    expect(component.error()).toBe('Error al cargar turnos');
    expect(mockToast.error).toHaveBeenCalledWith('Error al cargar turnos');
  });

  it('should not add turno if form is invalid via addTurno call', async () => {
    mockApi.horarios.createShift.calls.reset();
    component.openNewTurnoModal();
    component.turnoForm.setValue({ nombre: '' });
    await component.addTurno();
    expect(mockApi.horarios.createShift).not.toHaveBeenCalled();
  });

  it('should handle addTurno error', async () => {
    mockApi.horarios.createShift.and.callFake(() => { throw new Error('fail'); });
    component.openNewTurnoModal();
    component.turnoForm.setValue({ nombre: 'Fallo' });
    await component.addTurno();
    expect(mockToast.error).toHaveBeenCalledWith('Error al crear turno');
  });

  it('should not save edit turno if form is invalid', async () => {
    mockApi.horarios.listShifts.and.returnValue(of(mockShifts));
    await component.cargarTurnos();
    mockApi.horarios.patchShift.calls.reset();
    const turno = component.turnos[0];
    component.startEditTurno(turno);
    component.editTurnoForm.setValue({ nombre: '' });
    await component.saveEditTurno(turno);
    expect(mockApi.horarios.patchShift).not.toHaveBeenCalled();
  });

  it('should handle saveEditTurno error', async () => {
    mockApi.horarios.listShifts.and.returnValue(of(mockShifts));
    await component.cargarTurnos();
    mockApi.horarios.patchShift.and.callFake(() => { throw new Error('fail'); });
    const turno = component.turnos[0];
    component.startEditTurno(turno);
    component.editTurnoForm.setValue({ nombre: 'Fallo' });
    await component.saveEditTurno(turno);
    expect(mockToast.error).toHaveBeenCalledWith('Error al actualizar turno');
  });

  it('should handle deleteTurno error', async () => {
    mockApi.horarios.deleteShift.and.callFake(() => { throw new Error('fail'); });
    await component.deleteTurno(1);
    expect(mockToast.error).toHaveBeenCalledWith('Error al eliminar turno');
  });

  it('should not add horario if form is invalid via addHorario call', async () => {
    mockApi.horarios.createSchedule.calls.reset();
    component.showAddHorario(1);
    component.horarioForm.setValue({ inicio: '', fin: '' });
    await component.addHorario(1);
    expect(mockApi.horarios.createSchedule).not.toHaveBeenCalled();
  });

  it('should handle addHorario error', async () => {
    mockApi.horarios.createSchedule.and.callFake(() => { throw new Error('fail'); });
    component.showAddHorario(1);
    component.horarioForm.setValue({ inicio: '09:00', fin: '13:00' });
    await component.addHorario(1);
    expect(mockToast.error).toHaveBeenCalledWith('Error al agregar horario');
  });

  it('should not save edit horario if form is invalid', async () => {
    mockApi.horarios.patchSchedule.calls.reset();
    const horario = { id: 1, inicio: '08:00', fin: '12:00' };
    component.startEditHorario(1, horario);
    component.editHorarioForm.setValue({ inicio: '', fin: '' });
    await component.saveEditHorario(1, horario);
    expect(mockApi.horarios.patchSchedule).not.toHaveBeenCalled();
  });

  it('should handle saveEditHorario error', async () => {
    mockApi.horarios.patchSchedule.and.callFake(() => { throw new Error('fail'); });
    const horario = { id: 1, inicio: '08:00', fin: '12:00' };
    component.startEditHorario(1, horario);
    component.editHorarioForm.setValue({ inicio: '09:00', fin: '13:00' });
    await component.saveEditHorario(1, horario);
    expect(mockToast.error).toHaveBeenCalledWith('Error al actualizar horario');
  });

  it('should handle deleteHorario error', async () => {
    mockApi.horarios.deleteSchedule.and.callFake(() => { throw new Error('fail'); });
    await component.deleteHorario(1, 1);
    expect(mockToast.error).toHaveBeenCalledWith('Error al eliminar horario');
  });
});
