import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Sedes } from './sedes';

describe('Sedes', () => {
  let component: Sedes;
  let fixture: ComponentFixture<Sedes>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Sedes],
    }).compileComponents();

    const mockModalInstance = { show: () => {}, hide: () => {} };
    (window as any).bootstrap = {
      Modal: Object.assign(
        function () { return mockModalInstance; },
        { getInstance: () => mockModalInstance }
      ),
    };
    spyOn(window, 'alert').and.stub();

    fixture = TestBed.createComponent(Sedes);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  afterEach(() => {
    delete (window as any).bootstrap;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 3 initial sedes', () => {
    expect(component.sedes.length).toBe(3);
  });

  it('should compute totalSedes', () => {
    expect(component.totalSedes).toBe(3);
  });

  it('should compute totalAreas', () => {
    const expected = component.sedes.reduce((t, s) => t + s.areas.length, 0);
    expect(component.totalAreas).toBe(expected);
  });

  it('should compute totalDepartamentos', () => {
    const expected = component.sedes.reduce((t, s) =>
      t + s.areas.reduce((st, a) => st + a.departamentos.length, 0), 0);
    expect(component.totalDepartamentos).toBe(expected);
  });

  it('should compute totalUsuarios', () => {
    const expected = component.sedes.reduce((t, s) => t + s.usuarios, 0);
    expect(component.totalUsuarios).toBe(expected);
  });

  it('should compute totalDepartamentosPorSede', () => {
    const sede = component.sedes[0];
    const expected = sede.areas.reduce((t, a) => t + a.departamentos.length, 0);
    expect(component.totalDepartamentosPorSede(sede)).toBe(expected);
  });

  it('should get areasDeSedeSeleccionada', () => {
    component.sedeIdParaDepto = 1;
    const areas = component.areasDeSedeSeleccionada;
    expect(areas.length).toBe(2);
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
    expect(component.sedes.length).toBe(4);
    expect(component.sedes.find(s => s.nombre === 'Nueva Sede')).toBeTruthy();
  });

  it('should not guardarSede without nombre', () => {
    const lenBefore = component.sedes.length;
    component.abrirModalSede();
    component.guardarSede();
    expect(component.sedes.length).toBe(lenBefore);
  });

  it('should guardarSede update existing sede', () => {
    component.editarSede(component.sedes[0]);
    component.sedeForm.nombre = 'Sede Editada';
    component.guardarSede();
    expect(component.sedes[0].nombre).toBe('Sede Editada');
    expect(component.sedeEditando).toBeFalse();
  });

  it('should eliminarSede', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    const lenBefore = component.sedes.length;
    component.eliminarSede(1);
    expect(component.sedes.length).toBe(lenBefore - 1);
  });

  it('should not eliminarSede if cancelled', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    const lenBefore = component.sedes.length;
    component.eliminarSede(1);
    expect(component.sedes.length).toBe(lenBefore);
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
    const sede = component.sedes.find(s => s.id === 1)!;
    expect(sede.areas.length).toBe(3);
  });

  it('should guardarArea update existing area', () => {
    component.editarArea(1, 1);
    component.areaForm.nombre = 'Area Editada';
    component.guardarArea();
    const sede = component.sedes.find(s => s.id === 1)!;
    expect(sede.areas.find(a => a.id === 1)!.nombre).toBe('Area Editada');
  });

  it('should not guardarArea without nombre', () => {
    component.abrirModalArea(1);
    component.areaForm.nombre = '';
    component.guardarArea();
    const sede = component.sedes.find(s => s.id === 1)!;
    expect(sede.areas.length).toBe(2);
  });

  it('should eliminarArea', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    component.eliminarArea(1, 1);
    const sede = component.sedes.find(s => s.id === 1)!;
    expect(sede.areas.length).toBe(1);
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
    const area = component.sedes.find(s => s.id === 1)!.areas.find(a => a.id === 1)!;
    expect(area.departamentos.length).toBe(3);
  });

  it('should guardarDepartamento update existing depto', () => {
    component.editarDepartamento(1, 1, 1);
    component.deptoForm.nombre = 'Depto Editado';
    component.guardarDepartamento();
    const area = component.sedes.find(s => s.id === 1)!.areas.find(a => a.id === 1)!;
    expect(area.departamentos.find(d => d.id === 1)!.nombre).toBe('Depto Editado');
  });

  it('should not guardarDepartamento without nombre', () => {
    component.abrirModalDepartamento(1, 1);
    component.deptoForm.nombre = '';
    component.guardarDepartamento();
    const area = component.sedes.find(s => s.id === 1)!.areas.find(a => a.id === 1)!;
    expect(area.departamentos.length).toBe(2);
  });

  it('should eliminarDepartamento', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    component.eliminarDepartamento(1, 1, 1);
    const area = component.sedes.find(s => s.id === 1)!.areas.find(a => a.id === 1)!;
    expect(area.departamentos.length).toBe(1);
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
});
