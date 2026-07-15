import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { TiposIncidencia } from './tipos-incidencia';
import { ApiService } from '../../../api/services/api.service';
import { ToastService } from '../../../services/toast.service';
import type { IncidentTypeProfile } from '../../../api/types';

const makeTipo = (id: number, nombre: string, activo = true): IncidentTypeProfile => ({
  id, nombre, descripcion: `Descripción de ${nombre}`, activo,
  created_at: '2025-06-01T00:00:00Z', updated_at: '2025-06-01T00:00:00Z',
});

let tiposData: IncidentTypeProfile[];

const mockApi = {
  incidents: {
    listTypes: () => of({ content: tiposData, page: 0, size: 100, totalElements: tiposData.length, totalPages: 1 }),
    createType: (body: { nombre: string; descripcion?: string | null }) => {
      const nuevo = makeTipo(tiposData.length + 1, body.nombre);
      tiposData.push(nuevo);
      return of(nuevo);
    },
    patchType: (id: number, body: { nombre?: string; descripcion?: string | null; activo?: boolean }) => {
      const tipo = tiposData.find(t => t.id === id);
      if (tipo) {
        if (body.nombre !== undefined) tipo.nombre = body.nombre;
        if (body.descripcion !== undefined) tipo.descripcion = body.descripcion;
        if (body.activo !== undefined) tipo.activo = body.activo;
      }
      return of(tipo!);
    },
  },
};

const originalListTypes = mockApi.incidents.listTypes;
const originalCreateType = mockApi.incidents.createType;
const originalPatchType = mockApi.incidents.patchType;

const mockToast = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
};

describe('TiposIncidencia', () => {
  let component: TiposIncidencia;
  let fixture: ComponentFixture<TiposIncidencia>;

  beforeEach(async () => {
    tiposData = [
      makeTipo(1, 'Vacaciones'),
      makeTipo(2, 'Permiso Médico'),
      makeTipo(3, 'Capacitación'),
    ];

    mockToast.success.calls.reset();
    mockToast.error.calls.reset();

    mockApi.incidents.listTypes = originalListTypes;
    mockApi.incidents.createType = originalCreateType;
    mockApi.incidents.patchType = originalPatchType;

    await TestBed.configureTestingModule({
      imports: [TiposIncidencia],
      providers: [
        provideHttpClient(),
        { provide: ApiService, useValue: mockApi },
        { provide: ToastService, useValue: mockToast },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TiposIncidencia);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await component.cargarTipos();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load types on init', () => {
    expect(component.loading()).toBeFalse();
    expect(component.tipos.length).toBe(3);
    expect(component.tipos[0].nombre).toBe('Vacaciones');
  });

  it('should set error on load failure', async () => {
    mockApi.incidents.listTypes = () => throwError(() => new Error('fail'));
    await component.cargarTipos();
    expect(component.error()).toContain('Error');
    expect(mockToast.error).toHaveBeenCalledWith('Error al cargar tipos de incidencia');
  });

  it('should reload types via cargarTipos', async () => {
    expect(component.tipos.length).toBe(3);
    tiposData = [makeTipo(1, 'Solo uno')];
    await component.cargarTipos();
    fixture.detectChanges();
    expect(component.tipos.length).toBe(1);
  });

  it('should open and close create modal', () => {
    component.openCreateModal();
    expect(component.showCreateModal).toBeTrue();
    expect(component.createForm.value.nombre).toBe('');
    component.closeCreateModal();
    expect(component.showCreateModal).toBeFalse();
  });

  it('should create tipo successfully', async () => {
    component.openCreateModal();
    component.createForm.setValue({ nombre: 'Nuevo Tipo', descripcion: 'Desc del tipo' });

    await component.createTipo();
    expect(component.showCreateModal).toBeFalse();
    expect(mockToast.success).toHaveBeenCalledWith('Tipo de incidencia creado');
  });

  it('should show toast on create error', async () => {
    mockApi.incidents.createType = () => throwError(() => new Error('fail'));
    component.openCreateModal();
    component.createForm.setValue({ nombre: 'Fail', descripcion: '' });

    await component.createTipo();
    expect(mockToast.error).toHaveBeenCalledWith('Error al crear tipo de incidencia');
  });

  it('should not create if form invalid', async () => {
    component.openCreateModal();
    component.createForm.setValue({ nombre: '', descripcion: '' });
    await component.createTipo();
    expect(mockToast.success).not.toHaveBeenCalled();
  });

  it('should start edit mode', () => {
    component.startEdit(component.tipos[0]);
    expect(component.isEditing(component.tipos[0])).toBeTrue();
    expect(component.editForm.value.nombre).toBe('Vacaciones');
  });

  it('should cancel edit', () => {
    component.startEdit(component.tipos[0]);
    expect(component.editingId).toBe(1);
    component.cancelEdit();
    expect(component.editingId).toBeNull();
  });

  it('should save edit successfully', async () => {
    component.startEdit(component.tipos[0]);
    component.editForm.setValue({ nombre: 'Vacaciones Editado', descripcion: 'Nueva desc', activo: true });

    await component.saveEdit(component.tipos[0]);
    expect(component.editingId).toBeNull();
    expect(mockToast.success).toHaveBeenCalledWith('Tipo de incidencia actualizado');
  });

  it('should show toast on save edit error', async () => {
    mockApi.incidents.patchType = () => throwError(() => new Error('fail'));
    component.startEdit(component.tipos[0]);
    component.editForm.setValue({ nombre: 'Fail', descripcion: '', activo: true });

    await component.saveEdit(component.tipos[0]);
    expect(mockToast.error).toHaveBeenCalledWith('Error al actualizar tipo de incidencia');
  });

  it('should not save edit if form invalid', async () => {
    component.startEdit(component.tipos[0]);
    component.editForm.setValue({ nombre: '', descripcion: '', activo: true });
    await component.saveEdit(component.tipos[0]);
    expect(mockToast.success).not.toHaveBeenCalled();
  });

  it('should toggle activo successfully', async () => {
    expect(component.tipos[0].activo).toBeTrue();
    await component.toggleActivo(component.tipos[0]);
    expect(mockToast.success).toHaveBeenCalledWith('Tipo desactivado');
  });

  it('should show toast on toggle error', async () => {
    mockApi.incidents.patchType = () => throwError(() => new Error('fail'));
    await component.toggleActivo(component.tipos[0]);
    expect(mockToast.error).toHaveBeenCalledWith('Error al cambiar estado del tipo');
  });

  it('should format fecha correctly', () => {
    const result = component.formatFecha('2025-06-15T10:30:00Z');
    expect(result).toContain('jun');
    expect(result).toContain('2025');
  });

  it('should return false for isEditing when no tipo selected', () => {
    expect(component.isEditing(makeTipo(99, 'X'))).toBeFalse();
  });
});
