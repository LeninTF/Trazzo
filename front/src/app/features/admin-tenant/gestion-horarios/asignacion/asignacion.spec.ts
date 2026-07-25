import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { AsignacionComponent } from './asignacion';
import { ApiService } from '../../../../api/services/api.service';
import { ToastService } from '../../../../services/toast.service';
import { of, throwError } from 'rxjs';

describe('AsignacionComponent', () => {
  let component: AsignacionComponent;
  let fixture: ComponentFixture<AsignacionComponent>;

  const mockApi: any = {
    horarios: {
      listShifts: jasmine.createSpy('listShifts').and.returnValue(of({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 })),
      listUserSchedules: jasmine.createSpy('listUserSchedules').and.returnValue(of({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 })),
      createUserSchedule: jasmine.createSpy('createUserSchedule').and.returnValue(of({ id: 2 })),
      deleteUserSchedule: jasmine.createSpy('deleteUserSchedule').and.returnValue(of(undefined)),
    },
    users: {
      list: jasmine.createSpy('list').and.returnValue(of({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 })),
    },
  };

  const mockToast = jasmine.createSpyObj('ToastService', ['success', 'error']);

  beforeEach(async () => {
    mockApi.horarios.listShifts.calls.reset();
    mockApi.horarios.listUserSchedules.calls.reset();
    mockApi.horarios.createUserSchedule.calls.reset();
    mockApi.horarios.deleteUserSchedule.calls.reset();
    mockApi.users.list.calls.reset();
    mockApi.users.list.and.returnValue(of({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }));

    await TestBed.configureTestingModule({
      imports: [AsignacionComponent],
      providers: [
        provideHttpClient(),
        { provide: ApiService, useValue: mockApi },
        { provide: ToastService, useValue: mockToast },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AsignacionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call API on init', () => {
    expect(mockApi.horarios.listShifts).toHaveBeenCalled();
    expect(mockApi.horarios.listUserSchedules).toHaveBeenCalled();
  });

  it('should filter asignaciones by search term', () => {
    component.asignaciones.set([
      { id: 1, tenant_user_id: 1, trabajador: 'Usuario #1', area: 'Admin', turno: 'Mañana', horario: '08:00 – 12:00' },
      { id: 2, tenant_user_id: 2, trabajador: 'Usuario #2', area: 'Ventas', turno: 'Tarde', horario: '14:00 – 18:00' },
    ]);
    component.searchTerm.set('Usuario #1');
    expect(component.filteredAsignaciones().length).toBe(1);
    component.searchTerm.set('no-existe');
    expect(component.filteredAsignaciones().length).toBe(0);
  });

  it('should filter asignaciones by area', () => {
    component.asignaciones.set([
      { id: 1, tenant_user_id: 1, trabajador: 'Usuario #1', area: 'Admin', turno: 'Mañana', horario: '08:00 – 12:00' },
      { id: 2, tenant_user_id: 2, trabajador: 'Usuario #2', area: 'Ventas', turno: 'Tarde', horario: '14:00 – 18:00' },
    ]);
    component.areaFilter.set('Admin');
    expect(component.filteredAsignaciones().length).toBe(1);
  });

  it('should open and close modal', () => {
    component.openModal();
    expect(component.showModal).toBeTrue();
    component.closeModal();
    expect(component.showModal).toBeFalse();
  });

  it('should update selectedTurnoHorarios on turno change', () => {
    component.turnosDisponibles = [
      { id: 1, nombre: 'Mañana', horarios: [{ id: 1, label: '08:00 – 12:00' }, { id: 2, label: '12:00 – 16:00' }] },
    ];
    component.asignacionForm.patchValue({ turnoId: '1' });
    component.onTurnoChange();
    expect(component.selectedTurnoHorarios.length).toBe(2);
  });

  it('should submit asignacion', async () => {
    component.openModal();
    component.turnosDisponibles = [
      { id: 1, nombre: 'Mañana', horarios: [{ id: 1, label: '08:00 – 12:00' }] },
    ];
    component.asignacionForm.setValue({ trabajadorId: '1', area: 'Admin', turnoId: '1', horarioId: '1' });
    await component.submitAsignacion();
    expect(mockApi.horarios.createUserSchedule).toHaveBeenCalled();
    expect(component.showModal).toBeFalse();
  });

  it('should not submit if form is invalid', () => {
    component.openModal();
    component.asignacionForm.setValue({ trabajadorId: '', area: '', turnoId: '', horarioId: '' });
    expect(component.asignacionForm.invalid).toBeTrue();
  });

  it('should delete asignacion', async () => {
    await component.deleteAsignacion(1);
    expect(mockApi.horarios.deleteUserSchedule).toHaveBeenCalledWith(1);
  });

  it('should update searchTerm onSearch', () => {
    component.onSearch('test');
    expect(component.searchTerm()).toBe('test');
  });

  it('should update areaFilter onAreaFilter', () => {
    component.onAreaFilter('Admin');
    expect(component.areaFilter()).toBe('Admin');
  });

  it('should handle cargarDatos error (Promise.all rejection)', async () => {
    mockApi.horarios.listShifts.and.returnValue(throwError(() => 'fail'));
    mockApi.horarios.listUserSchedules.and.returnValue(throwError(() => 'fail'));

    await component.cargarDatos();

    expect(component.error()).toBe('Error al cargar datos');
    expect(mockToast.error).toHaveBeenCalledWith('Error al cargar datos');
    expect(component.loading()).toBeFalse();
  });

  it('should set empty horarios when turno is not found in onTurnoChange', () => {
    component.turnosDisponibles = [
      { id: 1, nombre: 'Mañana', horarios: [{ id: 1, label: '08:00 – 12:00' }] },
    ];
    component.asignacionForm.patchValue({ turnoId: '999' });
    component.onTurnoChange();
    expect(component.selectedTurnoHorarios).toEqual([]);
    expect(component.asignacionForm.get('horarioId')?.value).toBe('');
  });

  it('should set empty horarios when turno exists but has empty horarios', () => {
    component.turnosDisponibles = [
      { id: 1, nombre: 'Mañana', horarios: [] },
    ];
    component.asignacionForm.patchValue({ turnoId: '1' });
    component.onTurnoChange();
    expect(component.selectedTurnoHorarios).toEqual([]);
  });

  it('should return early from submitAsignacion when form is invalid', async () => {
    await component.submitAsignacion();
    expect(mockApi.horarios.createUserSchedule).not.toHaveBeenCalled();
  });

  it('should return early from submitAsignacion when turno is not found', async () => {
    component.turnosDisponibles = [];
    component.asignacionForm.setValue({ trabajadorId: '1', area: 'Admin', turnoId: '999', horarioId: '1' });
    await component.submitAsignacion();
    expect(mockApi.horarios.createUserSchedule).not.toHaveBeenCalled();
  });

  it('should return early from submitAsignacion when horario is not found', async () => {
    component.turnosDisponibles = [
      { id: 1, nombre: 'Mañana', horarios: [{ id: 1, label: '08:00 – 12:00' }] },
    ];
    component.asignacionForm.setValue({ trabajadorId: '1', area: 'Admin', turnoId: '1', horarioId: '999' });
    await component.submitAsignacion();
    expect(mockApi.horarios.createUserSchedule).not.toHaveBeenCalled();
  });

  it('should handle submitAsignacion API error', async () => {
    mockApi.horarios.createUserSchedule.and.returnValue(throwError(() => 'fail'));
    component.openModal();
    component.turnosDisponibles = [
      { id: 1, nombre: 'Mañana', horarios: [{ id: 1, label: '08:00 – 12:00' }] },
    ];
    component.asignacionForm.setValue({ trabajadorId: '1', area: 'Admin', turnoId: '1', horarioId: '1' });
    await component.submitAsignacion();
    expect(mockToast.error).toHaveBeenCalledWith('Error al crear asignación');
    expect(component.showModal).toBeFalse();
  });

  it('should handle deleteAsignacion success with toast', async () => {
    await component.deleteAsignacion(5);
    expect(mockApi.horarios.deleteUserSchedule).toHaveBeenCalledWith(5);
    expect(mockToast.success).toHaveBeenCalledWith('Asignación eliminada');
  });

  it('should handle deleteAsignacion error with toast', async () => {
    mockApi.horarios.deleteUserSchedule.and.returnValue(throwError(() => 'fail'));
    await component.deleteAsignacion(1);
    expect(mockToast.error).toHaveBeenCalledWith('Error al eliminar asignación');
  });

  it('should filter asignaciones by search term matching turno', () => {
    component.asignaciones.set([
      { id: 1, tenant_user_id: 1, trabajador: 'Juan Pérez', area: 'Admin', turno: 'Mañana', horario: '08:00 – 12:00' },
      { id: 2, tenant_user_id: 2, trabajador: 'Ana López', area: 'Ventas', turno: 'Tarde', horario: '14:00 – 18:00' },
    ]);
    component.searchTerm.set('Tarde');
    expect(component.filteredAsignaciones().length).toBe(1);
    expect(component.filteredAsignaciones()[0].turno).toBe('Tarde');
  });

  it('should filter asignaciones by combined search and area', () => {
    component.asignaciones.set([
      { id: 1, tenant_user_id: 1, trabajador: 'Juan Pérez', area: 'Admin', turno: 'Mañana', horario: '08:00 – 12:00' },
      { id: 2, tenant_user_id: 2, trabajador: 'Ana López', area: 'Ventas', turno: 'Tarde', horario: '14:00 – 18:00' },
      { id: 3, tenant_user_id: 3, trabajador: 'Carlos Ruiz', area: 'Ventas', turno: 'Noche', horario: '20:00 – 00:00' },
    ]);
    component.searchTerm.set('Ana');
    component.areaFilter.set('Ventas');
    expect(component.filteredAsignaciones().length).toBe(1);
    expect(component.filteredAsignaciones()[0].trabajador).toBe('Ana López');

    component.searchTerm.set('Carlos');
    component.areaFilter.set('Admin');
    expect(component.filteredAsignaciones().length).toBe(0);
  });

  it('should build worker name from API data and fallback to Usuario #id', async () => {
    mockApi.horarios.listShifts.and.returnValue(of({
      content: [{
        id: 10, name: 'Turno A', schedules: [{ id: 100, entry_time: '08:00:00', departure_time: '12:00:00' }],
      }],
      page: 0, size: 50, totalElements: 1, totalPages: 1,
    }) as any);
    mockApi.horarios.listUserSchedules.and.returnValue(of({
      content: [
        { id: 1, tenant_user_id: 1, schedule_id: 100 },
        { id: 2, tenant_user_id: 999, schedule_id: 100 },
      ],
      page: 0, size: 100, totalElements: 2, totalPages: 1,
    }) as any);
    mockApi.users.list.and.returnValue(of({
      content: [{
        id: 1,
        persona: { name: 'María', father_surname: 'García', mother_surname: 'López' },
      }],
      page: 0, size: 100, totalElements: 1, totalPages: 1,
    }) as any);

    await component.cargarDatos();

    const asigs = component.asignaciones();
    expect(asigs.length).toBe(2);
    expect(asigs[0].trabajador).toBe('María García López');
    expect(asigs[1].trabajador).toBe('Usuario #999');
  });

  it('should handle cargarDatos success path', async () => {
    mockApi.horarios.listShifts.and.returnValue(of({
      content: [{
        id: 1, name: 'Turno X',
        schedules: [
          { id: 10, entry_time: '06:00:00', departure_time: '14:00:00' },
          { id: 11, entry_time: '14:00:00', departure_time: '22:00:00' },
        ],
      }],
      page: 0, size: 50, totalElements: 1, totalPages: 1,
    }) as any);
    mockApi.horarios.listUserSchedules.and.returnValue(of({
      content: [{ id: 1, tenant_user_id: 1, schedule_id: 10 }],
      page: 0, size: 100, totalElements: 1, totalPages: 1,
    }) as any);
    mockApi.users.list.and.returnValue(of({
      content: [{
        id: 1,
        persona: { name: 'Pedro', father_surname: 'Soto', mother_surname: 'Ríos' },
      }],
      page: 0, size: 100, totalElements: 1, totalPages: 1,
    }) as any);

    await component.cargarDatos();

    expect(component.loading()).toBeFalse();
    expect(component.error()).toBe('');
    expect(component.turnosDisponibles.length).toBe(1);
    expect(component.turnosDisponibles[0].horarios.length).toBe(2);
    expect(component.asignaciones().length).toBe(1);
    expect(component.asignaciones()[0].turno).toBe('Turno X');
    expect(component.asignaciones()[0].horario).toBe('06:00 – 14:00');
  });
});
