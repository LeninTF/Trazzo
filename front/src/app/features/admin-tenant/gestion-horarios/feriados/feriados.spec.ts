import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { FeriadosComponent } from './feriados';
import { ApiService } from '../../../../api/services/api.service';

let feriadosData: any[];

const mockFeriados = [
  { id: 1, date: '2026-01-01', description: 'Año Nuevo', is_recurring: true, created_at: '2025-01-01T00:00:00Z' },
  { id: 2, date: '2026-05-01', description: 'Día del Trabajo', is_recurring: true, created_at: '2025-01-01T00:00:00Z' },
  { id: 3, date: '2026-07-28', description: 'Fiestas Patrias', is_recurring: true, created_at: '2025-01-01T00:00:00Z' },
  { id: 4, date: '2026-07-29', description: 'Fiestas Patrias', is_recurring: true, created_at: '2025-01-01T00:00:00Z' },
];

function createMockApiService(): ApiService {
  feriadosData = mockFeriados.map(f => ({ ...f }));

  return {
    corehr: {
      listNonWorkingDays: jasmine.createSpy('listNonWorkingDays').and.callFake(() => of({
        content: feriadosData,
        page: 0,
        size: 100,
        totalElements: feriadosData.length,
        totalPages: 1,
      })),
      createNonWorkingDay: jasmine.createSpy('createNonWorkingDay').and.callFake((body: any) => {
        const newDay = { id: Math.max(...feriadosData.map(d => d.id), 0) + 1, ...body, is_recurring: body.is_recurring ?? false, created_at: new Date().toISOString() };
        feriadosData.push(newDay);
        return of(newDay);
      }),
      patchNonWorkingDay: jasmine.createSpy('patchNonWorkingDay').and.callFake((id: number, body: any) => {
        const idx = feriadosData.findIndex((d: any) => d.id === id);
        if (idx >= 0) {
          feriadosData[idx] = { ...feriadosData[idx], ...body };
        }
        return of(feriadosData[idx]);
      }),
      deleteNonWorkingDay: jasmine.createSpy('deleteNonWorkingDay').and.callFake((id: number) => {
        feriadosData = feriadosData.filter((d: any) => d.id !== id);
        return of(undefined);
      }),
    },
    horarios: {},
    users: {},
    incidents: {},
    auth: {},
  } as unknown as ApiService;
}

describe('FeriadosComponent', () => {
  let component: FeriadosComponent;
  let fixture: ComponentFixture<FeriadosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FeriadosComponent],
      providers: [
        { provide: ApiService, useValue: createMockApiService() },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FeriadosComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 4 initial feriados', () => {
    expect(component.feriados.length).toBe(4);
  });

  it('should openNewForm', () => {
    component.openNewForm();
    expect(component.showNewForm).toBeTrue();
    expect(component.feriadoForm.get('tipo')?.value).toBe('nacional');
  });

  it('should cancelNewForm', () => {
    component.openNewForm();
    component.cancelNewForm();
    expect(component.showNewForm).toBeFalse();
  });

  it('should addFeriado with valid form', async () => {
    component.openNewForm();
    component.feriadoForm.patchValue({ fecha: '2026-06-01', nombre: 'Test Feriado', tipo: 'institucional' });
    await component.addFeriado();
    expect(component.feriados.length).toBe(5);
    expect(component.feriados.find(f => f.nombre === 'Test Feriado')).toBeTruthy();
    expect(component.showNewForm).toBeFalse();
  });

  it('should not addFeriado with invalid form', () => {
    component.openNewForm();
    component.addFeriado();
    expect(component.feriados.length).toBe(4);
  });

  it('should startEdit', () => {
    component.startEdit(component.feriados[0]);
    expect(component.editingFeriadoId).toBe(1);
    expect(component.editFeriadoForm.get('nombre')?.value).toBe('Año Nuevo');
  });

  it('should cancelEdit', () => {
    component.startEdit(component.feriados[0]);
    component.cancelEdit();
    expect(component.editingFeriadoId).toBeNull();
  });

  it('should saveEdit with valid form', async () => {
    component.startEdit(component.feriados[0]);
    component.editFeriadoForm.patchValue({ nombre: 'Año Nuevo Editado' });
    await component.saveEdit(component.feriados[0]);
    expect(component.feriados[0].nombre).toBe('Año Nuevo Editado');
    expect(component.editingFeriadoId).toBeNull();
  });

  it('should not saveEdit with invalid form', () => {
    component.startEdit(component.feriados[0]);
    component.editFeriadoForm.patchValue({ nombre: '' });
    component.saveEdit(component.feriados[0]);
    expect(component.feriados[0].nombre).toBe('Año Nuevo');
  });

  it('should deleteFeriado', async () => {
    await component.deleteFeriado(1);
    expect(component.feriados.length).toBe(3);
    expect(component.feriados.find(f => f.id === 1)).toBeUndefined();
  });
});
