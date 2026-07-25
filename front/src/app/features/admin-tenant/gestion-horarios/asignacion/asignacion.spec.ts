import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { AsignacionComponent } from './asignacion';
import { ApiService } from '../../../../api/services/api.service';
import { ToastService } from '../../../../services/toast.service';
import { of, throwError } from 'rxjs';
import type { BulkAssignUserSchedulesResponse, UserScheduleProfile, ScheduleSummary, TenantUserProfile } from '../../../../api/types';

function makeSchedule(id: number, entry = '08:00:00', dep = '12:00:00'): ScheduleSummary {
  return { id, name: `Horario ${id}`, entry_time: entry, departure_time: dep, days_of_week: [] };
}

function makeUserSchedule(id: number, userId: number, sched: ScheduleSummary, tu?: any): UserScheduleProfile {
  return {
    id, tenant_user_id: userId, schedule_id: sched.id, schedule: sched,
    description: null, entry_time: sched.entry_time, departure_time: sched.departure_time,
    created_at: '2026-01-01T00:00:00Z', updated_at: '2026-01-01T00:00:00Z',
    tenant_user: tu,
  };
}

function makeTenantUser(id: number, name = 'María', father = 'García', mother = 'López'): TenantUserProfile {
  return {
    id,
    email: `user${id}@test.com`,
    phone: null,
    estado: 'ACTIVO',
    must_change_password: false,
    created_at: '2026-01-01T00:00:00Z',
    updated_at: '2026-01-01T00:00:00Z',
    persona: { id, img_url: null, document_type: 'DNI', document_value: '00000000', name, father_surname: father, mother_surname: mother, birth_date: null },
    MetodoRecuperacion: [],
    rol: { id: 1, name: 'Admin', descripcion: '', permissions: [] },
    sedes: [{ id: 1, nombre: 'Sede Principal' }],
    areas: [{ id: 1, nombre: 'Admin' }],
    departamentos: [{ id: 1, nombre: 'Sistemas' }],
  };
}

describe('AsignacionComponent', () => {
  let component: AsignacionComponent;
  let fixture: ComponentFixture<AsignacionComponent>;

  const mockApi: any = {
    horarios: {
      listShifts: jasmine.createSpy('listShifts').and.returnValue(of({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 })),
      listUserSchedules: jasmine.createSpy('listUserSchedules').and.returnValue(of({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 })),
      createUserSchedule: jasmine.createSpy('createUserSchedule').and.returnValue(of({ id: 2 })),
      deleteUserSchedule: jasmine.createSpy('deleteUserSchedule').and.returnValue(of(undefined)),
      bulkAssignUserSchedules: jasmine.createSpy('bulkAssignUserSchedules').and.returnValue(of({
        schedule_id: 1, items: [], total: 0, created: 0, skipped: 0, errors: 0,
      } as BulkAssignUserSchedulesResponse)),
    },
    users: {
      list: jasmine.createSpy('list').and.returnValue(of({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 })),
    },
  };

  const mockToast = jasmine.createSpyObj('ToastService', ['success', 'error', 'info']);

  beforeEach(async () => {
    mockApi.horarios.listShifts.calls.reset();
    mockApi.horarios.listUserSchedules.calls.reset();
    mockApi.horarios.createUserSchedule.calls.reset();
    mockApi.horarios.deleteUserSchedule.calls.reset();
    mockApi.horarios.bulkAssignUserSchedules.calls.reset();
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
    mockApi.horarios.bulkAssignUserSchedules.and.returnValue(of({
      schedule_id: 1,
      items: [
        { tenant_user_id: 1, status: 'CREATED', message: 'Creada' },
        { tenant_user_id: 2, status: 'SKIPPED', message: 'Ya asignada' },
      ],
      total: 2, created: 1, skipped: 1, errors: 0,
    } as BulkAssignUserSchedulesResponse));
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
      { id: 1, tenant_user_id: 1, trabajador: 'Usuario #1', sede: '', area: '', departamento: '', turno: 'Mañana', horario: '08:00 – 12:00' },
      { id: 2, tenant_user_id: 2, trabajador: 'Usuario #2', sede: '', area: '', departamento: '', turno: 'Tarde', horario: '14:00 – 18:00' },
    ]);
    component.searchTerm.set('Usuario #1');
    expect(component.filteredAsignaciones().length).toBe(1);
    component.searchTerm.set('no-existe');
    expect(component.filteredAsignaciones().length).toBe(0);
  });

  it('should filter asignaciones by sede', () => {
    component.asignaciones.set([
      { id: 1, tenant_user_id: 1, trabajador: 'Ana', sede: 'Norte', area: 'Admin', departamento: '', turno: 'Mañana', horario: '08:00 – 12:00' },
      { id: 2, tenant_user_id: 2, trabajador: 'Beto', sede: 'Sur', area: 'Admin', departamento: '', turno: 'Tarde', horario: '14:00 – 18:00' },
    ]);
    component.sedeFilter.set('Norte');
    expect(component.filteredAsignaciones().length).toBe(1);
    expect(component.filteredAsignaciones()[0].sede).toBe('Norte');
  });

  it('should filter asignaciones by area', () => {
    component.asignaciones.set([
      { id: 1, tenant_user_id: 1, trabajador: 'Usuario #1', sede: '', area: 'Admin', departamento: '', turno: 'Mañana', horario: '08:00 – 12:00' },
      { id: 2, tenant_user_id: 2, trabajador: 'Usuario #2', sede: '', area: 'Ventas', departamento: '', turno: 'Tarde', horario: '14:00 – 18:00' },
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

  it('should open and close bulk modal', () => {
    component.openBulkModal();
    expect(component.showBulkModal).toBeTrue();
    expect(component.candidates().length).toBe(0);
    component.closeBulkModal();
    expect(component.showBulkModal).toBeFalse();
    expect(component.candidates().length).toBe(0);
  });

  it('should update selectedTurnoHorarios on turno change', () => {
    component.turnosDisponibles = [
      { id: 1, nombre: 'Mañana', horarios: [{ id: 1, label: '08:00 – 12:00' }, { id: 2, label: '12:00 – 16:00' }] },
    ];
    component.asignacionForm.patchValue({ turnoId: '1' });
    component.onTurnoChange(component.asignacionForm);
    expect(component.selectedTurnoHorarios.length).toBe(2);
  });

  it('should submit asignacion', async () => {
    component.openModal();
    component.turnosDisponibles = [
      { id: 1, nombre: 'Mañana', horarios: [{ id: 1, label: '08:00 – 12:00' }] },
    ];
    component.asignacionForm.setValue({ trabajadorId: '1', turnoId: '1', horarioId: '1' });
    await component.submitAsignacion();
    expect(mockApi.horarios.createUserSchedule).toHaveBeenCalled();
    expect(component.showModal).toBeFalse();
  });

  it('should not submit if form is invalid', () => {
    component.openModal();
    component.asignacionForm.setValue({ trabajadorId: '', turnoId: '', horarioId: '' });
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

  it('should update sedeFilter onSedeFilter', () => {
    component.onSedeFilter('Sede N');
    expect(component.sedeFilter()).toBe('Sede N');
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
    component.onTurnoChange(component.asignacionForm);
    expect(component.selectedTurnoHorarios).toEqual([]);
    expect(component.asignacionForm.get('horarioId')?.value).toBe('');
  });

  it('should set empty horarios when turno exists but has empty horarios', () => {
    component.turnosDisponibles = [
      { id: 1, nombre: 'Mañana', horarios: [] },
    ];
    component.asignacionForm.patchValue({ turnoId: '1' });
    component.onTurnoChange(component.asignacionForm);
    expect(component.selectedTurnoHorarios).toEqual([]);
  });

  it('should return early from submitAsignacion when form is invalid', async () => {
    await component.submitAsignacion();
    expect(mockApi.horarios.createUserSchedule).not.toHaveBeenCalled();
  });

  it('should return early from submitAsignacion when turno is not found', async () => {
    component.turnosDisponibles = [];
    component.asignacionForm.setValue({ trabajadorId: '1', turnoId: '999', horarioId: '1' });
    await component.submitAsignacion();
    expect(mockApi.horarios.createUserSchedule).not.toHaveBeenCalled();
  });

  it('should return early from submitAsignacion when horario is not found', async () => {
    component.turnosDisponibles = [
      { id: 1, nombre: 'Mañana', horarios: [{ id: 1, label: '08:00 – 12:00' }] },
    ];
    component.asignacionForm.setValue({ trabajadorId: '1', turnoId: '1', horarioId: '999' });
    await component.submitAsignacion();
    expect(mockApi.horarios.createUserSchedule).not.toHaveBeenCalled();
  });

  it('should handle submitAsignacion API error', async () => {
    mockApi.horarios.createUserSchedule.and.returnValue(throwError(() => 'fail'));
    component.openModal();
    component.turnosDisponibles = [
      { id: 1, nombre: 'Mañana', horarios: [{ id: 1, label: '08:00 – 12:00' }] },
    ];
    component.asignacionForm.setValue({ trabajadorId: '1', turnoId: '1', horarioId: '1' });
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
      { id: 1, tenant_user_id: 1, trabajador: 'Juan Pérez', sede: '', area: 'Admin', departamento: '', turno: 'Mañana', horario: '08:00 – 12:00' },
      { id: 2, tenant_user_id: 2, trabajador: 'Ana López', sede: '', area: 'Ventas', departamento: '', turno: 'Tarde', horario: '14:00 – 18:00' },
    ]);
    component.searchTerm.set('Tarde');
    expect(component.filteredAsignaciones().length).toBe(1);
    expect(component.filteredAsignaciones()[0].turno).toBe('Tarde');
  });

  it('should filter asignaciones by combined search and area', () => {
    component.asignaciones.set([
      { id: 1, tenant_user_id: 1, trabajador: 'Juan Pérez', sede: '', area: 'Admin', departamento: '', turno: 'Mañana', horario: '08:00 – 12:00' },
      { id: 2, tenant_user_id: 2, trabajador: 'Ana López', sede: '', area: 'Ventas', departamento: '', turno: 'Tarde', horario: '14:00 – 18:00' },
      { id: 3, tenant_user_id: 3, trabajador: 'Carlos Ruiz', sede: '', area: 'Ventas', departamento: '', turno: 'Noche', horario: '20:00 – 00:00' },
    ]);
    component.searchTerm.set('Ana');
    component.areaFilter.set('Ventas');
    expect(component.filteredAsignaciones().length).toBe(1);
    expect(component.filteredAsignaciones()[0].trabajador).toBe('Ana López');

    component.searchTerm.set('Carlos');
    component.areaFilter.set('Admin');
    expect(component.filteredAsignaciones().length).toBe(0);
  });

  it('should build asignacion with sede/area/departamento from tenant_user summary', async () => {
    const sched = makeSchedule(100, '08:00:00', '12:00:00');
    mockApi.horarios.listShifts.and.returnValue(of({
      content: [{ id: 10, name: 'Turno A', schedules: [sched] }],
      page: 0, size: 50, totalElements: 1, totalPages: 1,
    }) as any);
    mockApi.horarios.listUserSchedules.and.returnValue(of({
      content: [makeUserSchedule(1, 7, sched, {
        id: 7, name: 'María', father_surname: 'García', mother_surname: 'López',
        sede: 'Sede Lima', area: 'Ventas', department: 'Comercial',
      })],
      page: 0, size: 100, totalElements: 1, totalPages: 1,
    }) as any);
    mockApi.users.list.and.returnValue(of({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }));
    mockApi.horarios.bulkAssignUserSchedules.and.returnValue(of({
      schedule_id: 1, items: [], total: 0, created: 0, skipped: 0, errors: 0,
    } as BulkAssignUserSchedulesResponse));

    await component.cargarDatos();

    const asigs = component.asignaciones();
    expect(asigs.length).toBe(1);
    expect(asigs[0].trabajador).toBe('María García López');
    expect(asigs[0].sede).toBe('Sede Lima');
    expect(asigs[0].area).toBe('Ventas');
    expect(asigs[0].departamento).toBe('Comercial');
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
      content: [makeUserSchedule(1, 1, makeSchedule(10, '06:00:00', '14:00:00'), {
        id: 1, name: 'Pedro', father_surname: 'Soto', mother_surname: 'Ríos',
        sede: 'Lima', area: 'Logística', department: 'Operaciones',
      })],
      page: 0, size: 100, totalElements: 1, totalPages: 1,
    }) as any);
    mockApi.users.list.and.returnValue(of({
      content: [makeTenantUser(1, 'Pedro', 'Soto', 'Ríos')],
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

  it('should load candidates from workers on openBulkModal', () => {
    component.workers = [makeTenantUser(1), makeTenantUser(2, 'Juan', 'Pérez', 'Díaz')];
    component.openBulkModal();
    expect(component.candidates().length).toBe(2);
    expect(component.candidates()[0].selected).toBeFalse();
  });

  it('should toggle candidate selection', () => {
    component.workers = [makeTenantUser(1)];
    component.openBulkModal();
    expect(component.candidates()[0].selected).toBeFalse();
    component.toggleCandidate(component.candidates()[0]);
    expect(component.candidates()[0].selected).toBeTrue();
    expect(component.selectedCount()).toBe(1);
    component.toggleCandidate(component.candidates()[0]);
    expect(component.candidates()[0].selected).toBeFalse();
    expect(component.selectedCount()).toBe(0);
  });

  it('should select all visible and clear selection', () => {
    component.workers = [makeTenantUser(1), makeTenantUser(2, 'Juan', 'Pérez', 'Díaz')];
    component.openBulkModal();
    component.selectAllCandidates();
    expect(component.selectedCount()).toBe(2);
    component.clearCandidateSelection();
    expect(component.selectedCount()).toBe(0);
  });

  it('should filter candidates by search', () => {
    component.workers = [
      makeTenantUser(1, 'María', 'García', 'López'),
      makeTenantUser(2, 'Juan', 'Pérez', 'Díaz'),
    ];
    component.openBulkModal();
    component.candidatesSearch.set('maría');
    expect(component.filteredCandidates().length).toBe(1);
    expect(component.filteredCandidates()[0].persona.name).toBe('María');
  });

  it('should not submit bulk if no candidates selected', async () => {
    component.workers = [makeTenantUser(1)];
    component.openBulkModal();
    component.bulkForm.setValue({ turnoId: '1', horarioId: '1', description: '' });
    await component.submitBulk();
    expect(mockApi.horarios.bulkAssignUserSchedules).not.toHaveBeenCalled();
    expect(mockToast.error).toHaveBeenCalledWith('Seleccione al menos un trabajador');
  });

  it('should submit bulk and show summary toast with created/skipped/errors', async () => {
    component.workers = [makeTenantUser(1), makeTenantUser(2, 'Juan', 'Pérez', 'Díaz')];
    component.turnosDisponibles = [
      { id: 1, nombre: 'Turno A', horarios: [{ id: 5, label: '08:00 – 12:00' }] },
    ];
    component.openBulkModal();
    component.bulkForm.setValue({ turnoId: '1', horarioId: '5', description: 'Horario 2026' });
    component.candidates().forEach(c => c.selected = true);
    await component.submitBulk();
    expect(mockApi.horarios.bulkAssignUserSchedules).toHaveBeenCalled();
    expect(mockToast.success).toHaveBeenCalled();
  });

  it('should show error toast on bulk API error', async () => {
    component.workers = [makeTenantUser(1)];
    component.turnosDisponibles = [
      { id: 1, nombre: 'Turno A', horarios: [{ id: 5, label: '08:00 – 12:00' }] },
    ];
    component.openBulkModal();
    component.bulkForm.setValue({ turnoId: '1', horarioId: '5', description: '' });
    component.candidates()[0].selected = true;
    mockApi.horarios.bulkAssignUserSchedules.and.returnValue(throwError(() => 'fail'));
    await component.submitBulk();
    expect(mockToast.error).toHaveBeenCalledWith('Error al asignar masivamente');
  });
});
