import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { TurnosComponent } from './turnos';
import { ApiService } from '../../../../api/services/api.service';

let shiftsData: any[];
let nextShiftId = 4;
let nextScheduleId = 5;

function initShifts() {
  shiftsData = [
    {
      id: 1, name: 'Turno Mañana', description: null,
      schedules: [
        { id: 1, name: 'Horario 8-12', entry_time: '08:00:00', departure_time: '12:00:00' },
        { id: 2, name: 'Horario 7-13', entry_time: '07:00:00', departure_time: '13:00:00' },
      ],
      created_at: '2025-01-01T00:00:00Z', updated_at: '2025-01-01T00:00:00Z',
    },
    {
      id: 2, name: 'Turno Tarde', description: null,
      schedules: [
        { id: 3, name: 'Horario 14-18', entry_time: '14:00:00', departure_time: '18:00:00' },
      ],
      created_at: '2025-01-01T00:00:00Z', updated_at: '2025-01-01T00:00:00Z',
    },
    {
      id: 3, name: 'Turno Noche', description: null,
      schedules: [
        { id: 4, name: 'Horario 22-06', entry_time: '22:00:00', departure_time: '06:00:00' },
      ],
      created_at: '2025-01-01T00:00:00Z', updated_at: '2025-01-01T00:00:00Z',
    },
  ];
  nextShiftId = 4;
  nextScheduleId = 5;
}

function createMockApiService(): ApiService {
  initShifts();

  return {
    horarios: {
      listShifts: jasmine.createSpy('listShifts').and.callFake(() => of({
        content: shiftsData,
        page: 0,
        size: 50,
        totalElements: shiftsData.length,
        totalPages: 1,
      })),
      createShift: jasmine.createSpy('createShift').and.callFake((body: any) => {
        const newShift = { id: nextShiftId++, name: body.name, description: body.description ?? null, schedules: [], created_at: new Date().toISOString(), updated_at: new Date().toISOString() };
        shiftsData.push(newShift);
        return of(newShift);
      }),
      patchShift: jasmine.createSpy('patchShift').and.callFake((id: number, body: any) => {
        const idx = shiftsData.findIndex((s: any) => s.id === id);
        if (idx >= 0) {
          shiftsData[idx] = { ...shiftsData[idx], ...body };
        }
        return of(shiftsData[idx]);
      }),
      deleteShift: jasmine.createSpy('deleteShift').and.callFake((id: number) => {
        shiftsData = shiftsData.filter((s: any) => s.id !== id);
        return of(undefined);
      }),
      createSchedule: jasmine.createSpy('createSchedule').and.callFake((body: any) => {
        const newSchedule = { id: nextScheduleId++, name: body.name, entry_time: body.entry_time, departure_time: body.departure_time };
        const shift = shiftsData.find((s: any) => s.id === body.shift_id);
        if (shift) {
          shift.schedules.push(newSchedule);
        }
        return of({ ...newSchedule, shift_id: body.shift_id, shift: { id: shift?.id, name: shift?.name }, description: null, tolerancias: [], created_at: new Date().toISOString(), updated_at: new Date().toISOString() });
      }),
      patchSchedule: jasmine.createSpy('patchSchedule').and.callFake((id: number, body: any) => {
        for (const shift of shiftsData) {
          const idx = shift.schedules.findIndex((s: any) => s.id === id);
          if (idx >= 0) {
            shift.schedules[idx] = { ...shift.schedules[idx], ...body };
          }
        }
        return of({ id, ...body });
      }),
      deleteSchedule: jasmine.createSpy('deleteSchedule').and.callFake((id: number) => {
        for (const shift of shiftsData) {
          shift.schedules = shift.schedules.filter((s: any) => s.id !== id);
        }
        return of(undefined);
      }),
      listSchedules: jasmine.createSpy('listSchedules'),
      getSchedule: jasmine.createSpy('getSchedule'),
      listUserSchedules: jasmine.createSpy('listUserSchedules'),
      createUserSchedule: jasmine.createSpy('createUserSchedule'),
      deleteUserSchedule: jasmine.createSpy('deleteUserSchedule'),
      listTolerancias: jasmine.createSpy('listTolerancias'),
      createTolerancia: jasmine.createSpy('createTolerancia'),
      patchTolerancia: jasmine.createSpy('patchTolerancia'),
      deleteTolerancia: jasmine.createSpy('deleteTolerancia'),
    },
    corehr: {},
    users: {},
    incidents: {},
    auth: {},
  } as unknown as ApiService;
}

describe('TurnosComponent', () => {
  let component: TurnosComponent;
  let fixture: ComponentFixture<TurnosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TurnosComponent],
      providers: [
        { provide: ApiService, useValue: createMockApiService() },
      ],
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

  it('should addTurno with valid form', async () => {
    component.openNewTurnoModal();
    component.turnoForm.patchValue({ nombre: 'Turno Test' });
    await component.addTurno();
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

  it('should saveEditTurno with valid form', async () => {
    component.startEditTurno(component.turnos[0]);
    component.editTurnoForm.patchValue({ nombre: 'Turno Editado' });
    await component.saveEditTurno(component.turnos[0]);
    expect(component.turnos[0].nombre).toBe('Turno Editado');
    expect(component.editingTurnoId).toBeNull();
  });

  it('should not saveEditTurno with invalid form', () => {
    component.startEditTurno(component.turnos[0]);
    component.editTurnoForm.patchValue({ nombre: '' });
    component.saveEditTurno(component.turnos[0]);
    expect(component.turnos[0].nombre).toBe('Turno Mañana');
  });

  it('should deleteTurno', async () => {
    await component.deleteTurno(1);
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

  it('should addHorario with valid form', async () => {
    component.showAddHorario(1);
    component.horarioForm.patchValue({ inicio: '10:00', fin: '11:00' });
    await component.addHorario(1);
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

  it('should saveEditHorario with valid form', async () => {
    component.startEditHorario(1, component.turnos[0].horarios[0]);
    component.editHorarioForm.patchValue({ inicio: '09:00', fin: '13:00' });
    await component.saveEditHorario(1, component.turnos[0].horarios[0]);
    expect(component.turnos[0].horarios[0].inicio).toBe('09:00');
    expect(component.turnos[0].horarios[0].fin).toBe('13:00');
    expect(component.editingHorarioKey).toBeNull();
  });

  it('should not saveEditHorario with invalid form', () => {
    const horario = component.turnos[0].horarios[0];
    component.startEditHorario(1, horario);
    component.editHorarioForm.patchValue({ inicio: '', fin: '' });
    component.saveEditHorario(1, horario);
    expect(horario.inicio).toBe('08:00');
  });

  it('should deleteHorario', async () => {
    await component.deleteHorario(1, 1);
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
