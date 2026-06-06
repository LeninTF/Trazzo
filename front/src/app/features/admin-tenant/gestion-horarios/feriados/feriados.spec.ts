import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FeriadosComponent } from './feriados';

describe('FeriadosComponent', () => {
  let component: FeriadosComponent;
  let fixture: ComponentFixture<FeriadosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FeriadosComponent],
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

  it('should addFeriado with valid form', () => {
    component.openNewForm();
    component.feriadoForm.patchValue({ fecha: '2026-06-01', nombre: 'Test Feriado', tipo: 'institucional' });
    component.addFeriado();
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

  it('should saveEdit with valid form', () => {
    component.startEdit(component.feriados[0]);
    component.editFeriadoForm.patchValue({ nombre: 'Año Nuevo Editado' });
    component.saveEdit(component.feriados[0]);
    expect(component.feriados[0].nombre).toBe('Año Nuevo Editado');
    expect(component.editingFeriadoId).toBeNull();
  });

  it('should not saveEdit with invalid form', () => {
    component.startEdit(component.feriados[0]);
    component.editFeriadoForm.patchValue({ nombre: '' });
    component.saveEdit(component.feriados[0]);
    expect(component.feriados[0].nombre).toBe('Año Nuevo');
  });

  it('should deleteFeriado', () => {
    component.deleteFeriado(1);
    expect(component.feriados.length).toBe(3);
    expect(component.feriados.find(f => f.id === 1)).toBeUndefined();
  });
});
