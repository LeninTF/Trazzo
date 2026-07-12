import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Incidencias } from './incidencias';

describe('Incidencias', () => {
  let component: Incidencias;
  let fixture: ComponentFixture<Incidencias>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Incidencias],
    }).compileComponents();

    fixture = TestBed.createComponent(Incidencias);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('creates the incidencias component', () => {
    expect(component).toBeTruthy();
  });

  it('defaults incidencia signals', () => {
    expect(component.loading()).toBeFalse();
    expect(component.error()).toBe('');
    expect(component.filterEstado()).toBe('Todos');
  });

  it('should have 6 incidencias', () => {
    expect(component.incidencias().length).toBe(6);
  });

  it('should have 4 tiposDisponibles', () => {
    expect(component.tiposDisponibles.length).toBe(4);
  });

  describe('filtradas', () => {
    it('should return all when filter is Todos', () => {
      expect(component.filtradas.length).toBe(6);
    });

    it('should filter by estado', () => {
      component.setFilterEstado('Pendiente');
      expect(component.filtradas.length).toBe(1);
    });

    it('should filter approved incidencias', () => {
      component.setFilterEstado('Aprobado');
      expect(component.filtradas.length).toBe(4);
    });
  });

  describe('getters', () => {
    it('should count pendientes', () => {
      expect(component.pendientes).toBe(1);
    });

    it('should count aprobados', () => {
      expect(component.aprobados).toBe(4);
    });

    it('should count rechazados', () => {
      expect(component.rechazados).toBe(1);
    });

    it('should return resumen with 3 entries', () => {
      expect(component.resumen.length).toBe(3);
      expect(component.resumen[0].label).toBe('Pendientes');
      expect(component.resumen[0].valor).toBe(1);
    });
  });

  describe('setFilterEstado', () => {
    it('should update filterEstado signal', () => {
      component.setFilterEstado('Aprobado');
      expect(component.filterEstado()).toBe('Aprobado');
    });
  });

  describe('abrirModalCrear', () => {
    it('should show modal and reset form', () => {
      component.nuevaIncidencia.tipo = 'old';
      component.abrirModalCrear();
      expect(component.mostrarModalCrear).toBeTrue();
      expect(component.nuevaIncidencia.tipo).toBe('');
      expect(component.nuevaIncidencia.dias).toBe(1);
    });
  });

  describe('cerrarModalCrear', () => {
    it('should hide modal', () => {
      component.mostrarModalCrear = true;
      component.cerrarModalCrear();
      expect(component.mostrarModalCrear).toBeFalse();
    });
  });

  describe('abrirDetalle', () => {
    it('should set selected incidencia and show modal', () => {
      const inc = component.incidencias()[0];
      component.abrirDetalle(inc);
      expect(component.selectedIncidencia).toBe(inc);
      expect(component.mostrarModalDetalle).toBeTrue();
    });
  });

  describe('cerrarDetalle', () => {
    it('should clear selected and hide modal', () => {
      component.mostrarModalDetalle = true;
      component.selectedIncidencia = component.incidencias()[0];
      component.cerrarDetalle();
      expect(component.mostrarModalDetalle).toBeFalse();
      expect(component.selectedIncidencia).toBeNull();
    });
  });

  describe('onFileChange', () => {
    it('should set archivo when file selected', () => {
      const file = new File(['test'], 'test.pdf', { type: 'application/pdf' });
      const input = document.createElement('input');
      Object.defineProperty(input, 'files', { value: [file] });

      component.onFileChange({ target: input } as unknown as Event);
      expect(component.nuevaIncidencia.archivo).toBe(file);
    });

    it('should not change archivo when no file', () => {
      const input = document.createElement('input');
      Object.defineProperty(input, 'files', { value: [] });

      component.onFileChange({ target: input } as unknown as Event);
      expect(component.nuevaIncidencia.archivo).toBeNull();
    });
  });

  describe('enviar', () => {
    it('should not create incidencia without tipo', () => {
      component.nuevaIncidencia.tipo = '';
      component.enviar();
      expect(component.incidencias().length).toBe(6);
    });

    it('should not create incidencia without descripcion', () => {
      component.nuevaIncidencia.tipo = 'Permiso Personal';
      component.nuevaIncidencia.descripcion = '';
      component.enviar();
      expect(component.incidencias().length).toBe(6);
    });

    it('should create new incidencia and prepend to list', () => {
      component.nuevaIncidencia.tipo = 'Permiso Personal';
      component.nuevaIncidencia.fecha = '10/06/2026';
      component.nuevaIncidencia.descripcion = 'Test incidencia';

      component.enviar();

      expect(component.incidencias().length).toBe(7);
      expect(component.incidencias()[0].id).toBe('#INC-007');
      expect(component.incidencias()[0].estado).toBe('Pendiente');
      expect(component.mostrarModalCrear).toBeFalse();
    });

    it('should create incidencia with archivo metadata', () => {
      component.nuevaIncidencia.tipo = 'Vacaciones';
      component.nuevaIncidencia.descripcion = 'Test with file';
      component.nuevaIncidencia.archivo = new File(['x'], 'doc.pdf', { type: 'application/pdf' });

      component.enviar();

      expect(component.incidencias()[0].archivo).not.toBeNull();
      expect(component.incidencias()[0].archivo!.nombre).toBe('doc.pdf');
    });
  });

  describe('descargarArchivo', () => {
    it('should do nothing when no archivo', () => {
      const inc = component.incidencias()[1];
      inc.archivo = null;
      const lengthBefore = component.incidencias().length;
      component.descargarArchivo(inc);
      expect(component.incidencias().length).toBe(lengthBefore);
    });

    it('should create a download link', () => {
      const inc = component.incidencias().find(i => i.archivo !== null)!;
      const link = document.createElement('a');
      const clickSpy = spyOn(link, 'click');
      spyOn(document, 'createElement').and.returnValue(link);

      component.descargarArchivo(inc);

      expect(link.download).toBe(inc.archivo!.nombre);
      expect(clickSpy).toHaveBeenCalled();
    });
  });
});
