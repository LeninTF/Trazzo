import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { FeriadosComponent } from './feriados';
import { ApiService } from '../../../../api/services/api.service';
import { ToastService } from '../../../../services/toast.service';
import { of } from 'rxjs';

describe('FeriadosComponent', () => {
  let component: FeriadosComponent;
  let fixture: ComponentFixture<FeriadosComponent>;

  const mockNonWorkingDays = {
    content: [
      { id: 1, date: '2025-01-01', description: 'Año Nuevo', is_recurring: true, created_at: new Date().toISOString() },
      { id: 2, date: '2025-12-25', description: 'Navidad', is_recurring: true, created_at: new Date().toISOString() },
    ],
    page: 0, size: 100, totalElements: 2, totalPages: 1,
  };

  const mockApi = {
    corehr: {
      listNonWorkingDays: jasmine.createSpy('listNonWorkingDays').and.returnValue(of(mockNonWorkingDays)),
      createNonWorkingDay: jasmine.createSpy('createNonWorkingDay').and.returnValue(of({ id: 3, date: '2025-07-28', description: 'Fiestas Patrias' })),
      patchNonWorkingDay: jasmine.createSpy('patchNonWorkingDay').and.returnValue(of({ id: 1, date: '2025-01-01', description: 'Año Nuevo Editado' })),
      deleteNonWorkingDay: jasmine.createSpy('deleteNonWorkingDay').and.returnValue(of(undefined)),
    },
  };

  const mockToast = jasmine.createSpyObj('ToastService', ['success', 'error']);

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FeriadosComponent],
      providers: [
        provideHttpClient(),
        { provide: ApiService, useValue: mockApi },
        { provide: ToastService, useValue: mockToast },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FeriadosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load feriados on init', () => {
    expect(mockApi.corehr.listNonWorkingDays).toHaveBeenCalled();
    expect(component.feriados.length).toBe(2);
    expect(component.feriados[0].nombre).toBe('Año Nuevo');
  });

  it('should open and cancel new form', () => {
    component.openNewForm();
    expect(component.showNewForm).toBeTrue();
    component.cancelNewForm();
    expect(component.showNewForm).toBeFalse();
  });

  it('should add a new feriado', async () => {
    component.openNewForm();
    component.feriadoForm.setValue({ fecha: '2025-07-28', nombre: 'Fiestas Patrias', tipo: 'nacional' });
    await component.addFeriado();
    expect(mockApi.corehr.createNonWorkingDay).toHaveBeenCalledWith({ date: '2025-07-28', description: 'Fiestas Patrias' });
    expect(component.showNewForm).toBeFalse();
  });

  it('should not add feriado if form is invalid', () => {
    component.openNewForm();
    component.feriadoForm.setValue({ fecha: '', nombre: '', tipo: '' });
    expect(component.feriadoForm.invalid).toBeTrue();
  });

  it('should start and cancel editing', () => {
    const feriado = component.feriados[0];
    component.startEdit(feriado);
    expect(component.editingFeriadoId).toBe(feriado.id);
    component.cancelEdit();
    expect(component.editingFeriadoId).toBeNull();
  });

  it('should save edit', async () => {
    const feriado = component.feriados[0];
    component.startEdit(feriado);
    component.editFeriadoForm.setValue({ fecha: '2025-01-01', nombre: 'Año Nuevo Editado', tipo: 'nacional' });
    await component.saveEdit(feriado);
    expect(mockApi.corehr.patchNonWorkingDay).toHaveBeenCalledWith(feriado.id, { date: '2025-01-01', description: 'Año Nuevo Editado' });
    expect(component.editingFeriadoId).toBeNull();
  });

  it('should delete feriado', async () => {
    await component.deleteFeriado(1);
    expect(mockApi.corehr.deleteNonWorkingDay).toHaveBeenCalledWith(1);
  });
});
