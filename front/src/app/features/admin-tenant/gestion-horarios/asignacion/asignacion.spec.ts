import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AsignacionComponent } from './asignacion';
import { ApiService } from '../../../../api/services/api.service';

let shiftsData: any[];
let userSchedulesData: any[];
let nextUserScheduleId = 6;

function initData() {
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

  userSchedulesData = [
    { id: 1, tenant_user_id: 1, schedule_id: 1, schedule: { id: 1, name: 'Horario 8-12', entry_time: '08:00:00', departure_time: '12:00:00' }, description: null, entry_time: '08:00:00', departure_time: '12:00:00', created_at: '2025-01-01T00:00:00Z', updated_at: '2025-01-01T00:00:00Z' },
    { id: 2, tenant_user_id: 2, schedule_id: 1, schedule: { id: 1, name: 'Horario 8-12', entry_time: '08:00:00', departure_time: '12:00:00' }, description: null, entry_time: '08:00:00', departure_time: '12:00:00', created_at: '2025-01-01T00:00:00Z', updated_at: '2025-01-01T00:00:00Z' },
    { id: 3, tenant_user_id: 3, schedule_id: 1, schedule: { id: 1, name: 'Horario 8-12', entry_time: '08:00:00', departure_time: '12:00:00' }, description: null, entry_time: '08:00:00', departure_time: '12:00:00', created_at: '2025-01-01T00:00:00Z', updated_at: '2025-01-01T00:00:00Z' },
    { id: 4, tenant_user_id: 4, schedule_id: 2, schedule: { id: 2, name: 'Horario 7-13', entry_time: '07:00:00', departure_time: '13:00:00' }, description: null, entry_time: '07:00:00', departure_time: '13:00:00', created_at: '2025-01-01T00:00:00Z', updated_at: '2025-01-01T00:00:00Z' },
    { id: 5, tenant_user_id: 5, schedule_id: 2, schedule: { id: 2, name: 'Horario 7-13', entry_time: '07:00:00', departure_time: '13:00:00' }, description: null, entry_time: '07:00:00', departure_time: '13:00:00', created_at: '2025-01-01T00:00:00Z', updated_at: '2025-01-01T00:00:00Z' },
  ];
  nextUserScheduleId = 6;
}

function createMockApiService(): ApiService {
  initData();

  return {
    horarios: {
      listShifts: jasmine.createSpy('listShifts').and.callFake(() => of({
        content: shiftsData,
        page: 0,
        size: 50,
        totalElements: shiftsData.length,
        totalPages: 1,
      })),
      listUserSchedules: jasmine.createSpy('listUserSchedules').and.callFake(() => of({
        content: userSchedulesData,
        page: 0,
        size: 100,
        totalElements: userSchedulesData.length,
        totalPages: 1,
      })),
      createUserSchedule: jasmine.createSpy('createUserSchedule').and.callFake((body: any) => {
        const newSchedule = {
          id: nextUserScheduleId++,
          tenant_user_id: body.tenant_user_id,
          schedule_id: body.schedule_id,
          schedule: { id: body.schedule_id, name: '', entry_time: body.entry_time, departure_time: body.departure_time },
          description: null,
          entry_time: body.entry_time,
          departure_time: body.departure_time,
          created_at: new Date().toISOString(),
          updated_at: new Date().toISOString(),
        };
        userSchedulesData.push(newSchedule);
        return of(newSchedule);
      }),
      deleteUserSchedule: jasmine.createSpy('deleteUserSchedule').and.callFake((id: number) => {
        userSchedulesData = userSchedulesData.filter((us: any) => us.id !== id);
        return of(undefined);
      }),
      createShift: jasmine.createSpy('createShift'),
      patchShift: jasmine.createSpy('patchShift'),
      deleteShift: jasmine.createSpy('deleteShift'),
      getShift: jasmine.createSpy('getShift'),
      listSchedules: jasmine.createSpy('listSchedules'),
      createSchedule: jasmine.createSpy('createSchedule'),
      patchSchedule: jasmine.createSpy('patchSchedule'),
      deleteSchedule: jasmine.createSpy('deleteSchedule'),
      getSchedule: jasmine.createSpy('getSchedule'),
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

describe('AsignacionComponent', () => {
  let component: AsignacionComponent;
  let fixture: ComponentFixture<AsignacionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AsignacionComponent],
      providers: [
        { provide: ApiService, useValue: createMockApiService() },
      ],
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
    component.onSearch('Usuario #1');
    expect(component.filteredAsignaciones().length).toBe(1);
    expect(component.filteredAsignaciones()[0].trabajador).toBe('Usuario #1');
  });

  it('should filter by area', () => {
    component.onAreaFilter('Ventas');
    expect(component.filteredAsignaciones().length).toBe(0);
  });

  it('should filter by both search and area', () => {
    component.onSearch('Usuario #1');
    component.onAreaFilter('');
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

  it('should submitAsignacion with valid form', async () => {
    component.openModal();
    component.asignacionForm.patchValue({
      trabajador: 'Test User',
      area: 'RRHH',
      turnoId: '1',
      horarioId: '1',
    });
    component.onTurnoChange();
    component.asignacionForm.patchValue({ horarioId: '1' });
    await component.submitAsignacion();
    expect(component.asignaciones().length).toBe(6);
    expect(component.showModal).toBeFalse();
  });

  it('should not submitAsignacion with invalid form', () => {
    component.openModal();
    component.submitAsignacion();
    expect(component.asignaciones().length).toBe(5);
  });

  it('should deleteAsignacion', async () => {
    await component.deleteAsignacion(1);
    expect(component.asignaciones().length).toBe(4);
  });
});
