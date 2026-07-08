import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Sedes } from './sedes';
import { OrgService } from '../../../api/services/org.service';
import { ToastService } from '../../../services/toast.service';
import { ModalService } from '../../../services/modal.service';

describe('Sedes', () => {
  let component: Sedes;
  let fixture: ComponentFixture<Sedes>;

  const mockBranches = {
    content: [
      { id: 1, name: 'Sede San Isidro', description: 'Principal', state: true, createdAt: '2025-01-01', updatedAt: '2025-01-01' },
      { id: 2, name: 'Sede Miraflores', description: null, state: true, createdAt: '2025-01-01', updatedAt: '2025-01-01' },
      { id: 3, name: 'Sede Surco', description: null, state: false, createdAt: '2025-01-01', updatedAt: '2025-01-01' },
    ],
    page: 0, size: 200, total: 3, totalPages: 1,
  };

  const mockAreas = {
    content: [
      { id: 1, branchId: 1, name: 'Dirección Académica', description: null, state: true, createdAt: '2025-01-01', updatedAt: '2025-01-01' },
      { id: 2, branchId: 1, name: 'Administración', description: null, state: true, createdAt: '2025-01-01', updatedAt: '2025-01-01' },
    ],
    page: 0, size: 1000, total: 2, totalPages: 1,
  };

  const mockDepts = {
    content: [
      { id: 1, areaId: 1, name: 'Matemáticas', description: null, state: true, createdAt: '2025-01-01', updatedAt: '2025-01-01' },
      { id: 2, areaId: 1, name: 'Comunicación', description: null, state: true, createdAt: '2025-01-01', updatedAt: '2025-01-01' },
    ],
    page: 0, size: 2000, total: 2, totalPages: 1,
  };

  const mockOrg = {
    listBranches: jasmine.createSpy('listBranches').and.returnValue(of(mockBranches)),
    listAreas: jasmine.createSpy('listAreas').and.returnValue(of(mockAreas)),
    listDepartments: jasmine.createSpy('listDepartments').and.returnValue(of(mockDepts)),
    createBranch: jasmine.createSpy('createBranch').and.returnValue(of(mockBranches.content[0])),
    updateBranch: jasmine.createSpy('updateBranch').and.returnValue(of(mockBranches.content[0])),
    deleteBranch: jasmine.createSpy('deleteBranch').and.returnValue(of(undefined)),
    createArea: jasmine.createSpy('createArea').and.returnValue(of(mockAreas.content[0])),
    updateArea: jasmine.createSpy('updateArea').and.returnValue(of(mockAreas.content[0])),
    deleteArea: jasmine.createSpy('deleteArea').and.returnValue(of(undefined)),
    createDepartment: jasmine.createSpy('createDepartment').and.returnValue(of(mockDepts.content[0])),
    updateDepartment: jasmine.createSpy('updateDepartment').and.returnValue(of(mockDepts.content[0])),
    deleteDepartment: jasmine.createSpy('deleteDepartment').and.returnValue(of(undefined)),
  };

  const mockToast = jasmine.createSpyObj('ToastService', ['success', 'error', 'info']);
  const mockModal = jasmine.createSpyObj('ModalService', ['show', 'hide']);

  beforeEach(async () => {
    Object.values(mockOrg).forEach(spy => spy.calls.reset());
    mockOrg.listBranches.and.returnValue(of(mockBranches));
    mockOrg.listAreas.and.returnValue(of(mockAreas));
    mockOrg.listDepartments.and.returnValue(of(mockDepts));

    await TestBed.configureTestingModule({
      imports: [Sedes],
      providers: [
        { provide: OrgService, useValue: mockOrg },
        { provide: ToastService, useValue: mockToast },
        { provide: ModalService, useValue: mockModal },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Sedes);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load sedes on init', () => {
    expect(mockOrg.listBranches).toHaveBeenCalled();
    expect(component.sedes.length).toBe(3);
  });

  it('should build tree with areas and departamentos', () => {
    const sede = component.sedes.find(s => s.id === 1)!;
    expect(sede.areas.length).toBe(2);
    expect(sede.areas.find(a => a.id === 1)!.departamentos.length).toBe(2);
  });

  it('should compute totalSedes', () => {
    expect(component.totalSedes).toBe(3);
  });

  it('should compute totalAreas', () => {
    expect(component.totalAreas).toBe(2);
  });

  it('should compute totalDepartamentos', () => {
    expect(component.totalDepartamentos).toBe(2);
  });

  it('should compute totalDepartamentosPorSede', () => {
    const sede = component.sedes.find(s => s.id === 1)!;
    expect(component.totalDepartamentosPorSede(sede)).toBe(2);
  });

  it('should get areasDeSedeSeleccionada', () => {
    component.sedeIdParaDepto = 1;
    expect(component.areasDeSedeSeleccionada.length).toBe(2);
  });

  it('should return empty areas for non-existent sede', () => {
    component.sedeIdParaDepto = 999;
    expect(component.areasDeSedeSeleccionada).toEqual([]);
  });

  it('should abrirModalSede reset form', () => {
    component.abrirModalSede();
    expect(component.sedeEditando).toBeFalse();
    expect(component.sedeForm.nombre).toBe('');
    expect(component.sedeForm.estado).toBe('activo');
  });

  it('should guardarSede create new sede', () => {
    component.abrirModalSede();
    component.sedeForm.nombre = 'Nueva Sede';
    component.sedeForm.descripcion = 'Descripcion';
    component.guardarSede();
    expect(mockOrg.createBranch).toHaveBeenCalledWith({ name: 'Nueva Sede', description: 'Descripcion' });
  });

  it('should not guardarSede without nombre', () => {
    component.abrirModalSede();
    component.guardarSede();
    expect(mockOrg.createBranch).not.toHaveBeenCalled();
    expect(mockToast.error).toHaveBeenCalled();
  });

  it('should guardarSede update existing sede', () => {
    component.editarSede(component.sedes[0]);
    component.sedeForm.nombre = 'Sede Editada';
    component.guardarSede();
    expect(mockOrg.updateBranch).toHaveBeenCalledWith(1, { name: 'Sede Editada', description: component.sedes[0].descripcion });
  });

  it('should handle guardarSede error', () => {
    mockOrg.createBranch.and.returnValue(throwError(() => new Error('fail')));
    component.abrirModalSede();
    component.sedeForm.nombre = 'Nueva Sede';
    component.guardarSede();
    expect(mockToast.error).toHaveBeenCalledWith('Error al guardar la sede');
  });

  it('should eliminarSede', () => {
    component.eliminarSede(1);
    expect(mockOrg.deleteBranch).toHaveBeenCalledWith(1);
    expect(mockToast.success).toHaveBeenCalled();
  });

  it('should abrirModalArea reset area form', () => {
    component.abrirModalArea(1);
    expect(component.areaEditando).toBeFalse();
    expect(component.areaForm.sedeId).toBe(1);
  });

  it('should guardarArea create new area', () => {
    component.abrirModalArea(1);
    component.areaForm.nombre = 'Nueva Area';
    component.guardarArea();
    expect(mockOrg.createArea).toHaveBeenCalledWith({ branchId: 1, name: 'Nueva Area', description: '' });
  });

  it('should guardarArea update existing area', () => {
    component.editarArea(1, 1);
    component.areaForm.nombre = 'Area Editada';
    component.guardarArea();
    expect(mockOrg.updateArea).toHaveBeenCalledWith(1, { name: 'Area Editada', description: component.areaForm.descripcion });
  });

  it('should not guardarArea without nombre', () => {
    component.abrirModalArea(1);
    component.areaForm.nombre = '';
    component.guardarArea();
    expect(mockOrg.createArea).not.toHaveBeenCalled();
  });

  it('should eliminarArea', () => {
    component.eliminarArea(1, 1);
    expect(mockOrg.deleteArea).toHaveBeenCalledWith(1);
  });

  it('should abrirModalDepartamento reset depto form', () => {
    component.abrirModalDepartamento(1, 1);
    expect(component.deptoEditando).toBeFalse();
    expect(component.deptoForm.areaId).toBe(1);
  });

  it('should guardarDepartamento create new depto', () => {
    component.abrirModalDepartamento(1, 1);
    component.deptoForm.nombre = 'Nuevo Depto';
    component.guardarDepartamento();
    expect(mockOrg.createDepartment).toHaveBeenCalledWith({ areaId: 1, name: 'Nuevo Depto', description: '' });
  });

  it('should guardarDepartamento update existing depto', () => {
    component.editarDepartamento(1, 1, 1);
    component.deptoForm.nombre = 'Depto Editado';
    component.guardarDepartamento();
    expect(mockOrg.updateDepartment).toHaveBeenCalledWith(1, { name: 'Depto Editado', description: component.deptoForm.descripcion });
  });

  it('should not guardarDepartamento without nombre', () => {
    component.abrirModalDepartamento(1, 1);
    component.deptoForm.nombre = '';
    component.guardarDepartamento();
    expect(mockOrg.createDepartment).not.toHaveBeenCalled();
  });

  it('should eliminarDepartamento', () => {
    component.eliminarDepartamento(1, 1, 1);
    expect(mockOrg.deleteDepartment).toHaveBeenCalledWith(1);
  });

  it('should onSedeChangeForArea convert to number', () => {
    component.areaForm.sedeId = '2' as any;
    component.onSedeChangeForArea();
    expect(typeof component.areaForm.sedeId).toBe('number');
  });

  it('should onSedeChangeForDepto set first area', () => {
    component.onSedeChangeForDepto(1);
    expect(component.deptoForm.areaId).toBe(component.sedes[0].areas[0].id);
  });

  it('should onSedeChangeForDepto set 0 if no areas', () => {
    component.onSedeChangeForDepto(999);
    expect(component.deptoForm.areaId).toBe(0);
  });

  it('should handle cargarSedes error', () => {
    mockOrg.listBranches.and.returnValue(throwError(() => new Error('fail')));
    component.cargarSedes();
    expect(component.error()).toBe('Error al cargar las sedes');
  });
});
