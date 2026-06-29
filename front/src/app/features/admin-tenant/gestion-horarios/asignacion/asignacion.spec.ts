import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { AsignacionComponent } from './asignacion';
import { ApiService } from '../../../../api/services/api.service';
import { ToastService } from '../../../../services/toast.service';
import { of } from 'rxjs';

describe('AsignacionComponent', () => {
  let component: AsignacionComponent;
  let fixture: ComponentFixture<AsignacionComponent>;

  const mockApi = {
    horarios: {
      listShifts: jasmine.createSpy('listShifts').and.returnValue(of({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 })),
      listUserSchedules: jasmine.createSpy('listUserSchedules').and.returnValue(of({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 })),
      createUserSchedule: jasmine.createSpy('createUserSchedule').and.returnValue(of({ id: 2 })),
      deleteUserSchedule: jasmine.createSpy('deleteUserSchedule').and.returnValue(of(undefined)),
    },
  };

  const mockToast = jasmine.createSpyObj('ToastService', ['success', 'error']);

  beforeEach(async () => {
    mockApi.horarios.listShifts.calls.reset();
    mockApi.horarios.listUserSchedules.calls.reset();
    mockApi.horarios.createUserSchedule.calls.reset();
    mockApi.horarios.deleteUserSchedule.calls.reset();

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
});
