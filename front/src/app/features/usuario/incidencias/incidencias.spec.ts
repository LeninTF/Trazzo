import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { IncidentsService } from '../../../api/services/incidents.service';
import type { IncidentTypeProfile, IncidentProfile, IncidentListResponse, IncidentTypeListResponse } from '../../../api/types';
import { Incidencias } from './incidencias';

const mockTypes: IncidentTypeProfile[] = [
  { id: 1, nombre: 'Permiso Personal', descripcion: 'Permiso por asuntos personales', activo: true, created_at: '', updated_at: '' },
  { id: 2, nombre: 'Justificación de Falta', descripcion: 'Justificación por inasistencia', activo: true, created_at: '', updated_at: '' },
  { id: 3, nombre: 'Cambio de Turno', descripcion: 'Solicitud de intercambio de turno', activo: true, created_at: '', updated_at: '' },
];

const mockIncidents: IncidentProfile[] = [
  {
    id: 6, tenant_user_id: 1, incidencia_type_id: 1, state: 'PENDIENTE',
    comment: 'Trámite bancario - medio día',
    tipo: mockTypes[0], permiso: null, evidencias: [],
    tenant_user: { id: 1, nombre: 'Josselin', apellido_paterno: 'Rojas', apellido_materno: 'Luque', email: 'joss@colegio.edu.pe' },
    created_at: '2026-06-05T10:00:00Z', updated_at: '2026-06-05T10:00:00Z',
  },
  {
    id: 5, tenant_user_id: 1, incidencia_type_id: 1, state: 'APROBADO',
    comment: 'Cita médica - 4 horas',
    tipo: mockTypes[0], permiso: null, evidencias: [],
    tenant_user: { id: 1, nombre: 'Josselin', apellido_paterno: 'Rojas', apellido_materno: 'Luque', email: 'joss@colegio.edu.pe' },
    created_at: '2026-06-01T08:00:00Z', updated_at: '2026-06-01T08:00:00Z',
  },
  {
    id: 4, tenant_user_id: 1, incidencia_type_id: 2, state: 'APROBADO',
    comment: 'Emergencia familiar - 27/05',
    tipo: mockTypes[1], permiso: { id: 1, incidencia_id: 4, start_date: '2026-05-27', end_date: '2026-05-28', days_granted: 2, created_at: '', updated_at: '' }, evidencias: [
      { id: 1, incidencia_id: 4, file_name: 'justificacion.pdf', file_url: '#', mime_type: 'application/pdf', file_size: 512000, created_at: '', updated_at: '' },
    ],
    tenant_user: { id: 1, nombre: 'Josselin', apellido_paterno: 'Rojas', apellido_materno: 'Luque', email: 'joss@colegio.edu.pe' },
    created_at: '2026-05-27T09:00:00Z', updated_at: '2026-05-27T09:00:00Z',
  },
  {
    id: 3, tenant_user_id: 1, incidencia_type_id: 3, state: 'APROBADO',
    comment: 'Solicitud de intercambio con J. Pérez para el 10/06',
    tipo: mockTypes[2], permiso: null, evidencias: [],
    tenant_user: { id: 1, nombre: 'Josselin', apellido_paterno: 'Rojas', apellido_materno: 'Luque', email: 'joss@colegio.edu.pe' },
    created_at: '2026-05-25T07:00:00Z', updated_at: '2026-05-25T07:00:00Z',
  },
  {
    id: 2, tenant_user_id: 1, incidencia_type_id: 1, state: 'DENEGADO',
    comment: 'Día personal solicitado para el 15/06',
    tipo: mockTypes[0], permiso: null, evidencias: [],
    tenant_user: { id: 1, nombre: 'Josselin', apellido_paterno: 'Rojas', apellido_materno: 'Luque', email: 'joss@colegio.edu.pe' },
    created_at: '2026-05-20T11:00:00Z', updated_at: '2026-05-20T11:00:00Z',
  },
  {
    id: 1, tenant_user_id: 1, incidencia_type_id: 2, state: 'APROBADO',
    comment: 'Problema de salud - 15/05',
    tipo: mockTypes[1], permiso: { id: 2, incidencia_id: 1, start_date: '2026-05-15', end_date: '2026-05-17', days_granted: 3, created_at: '', updated_at: '' }, evidencias: [
      { id: 2, incidencia_id: 1, file_name: 'certificado-medico.pdf', file_url: '#', mime_type: 'application/pdf', file_size: 1228800, created_at: '', updated_at: '' },
    ],
    tenant_user: { id: 1, nombre: 'Josselin', apellido_paterno: 'Rojas', apellido_materno: 'Luque', email: 'joss@colegio.edu.pe' },
    created_at: '2026-05-16T06:00:00Z', updated_at: '2026-05-16T06:00:00Z',
  },
];

const typesResponse: IncidentTypeListResponse = {
  content: mockTypes, page: 0, size: 100, totalElements: mockTypes.length, totalPages: 1,
};

let incidentsResponse: IncidentListResponse;

const mockIncidentsService = {
  listTypes: () => of(typesResponse),
  list: () => of(incidentsResponse),
  create: (_body: any) => of({} as IncidentProfile),
};

describe('Incidencias (usuario)', () => {
  let component: Incidencias;
  let fixture: ComponentFixture<Incidencias>;

  beforeEach(async () => {
    incidentsResponse = {
      content: [...mockIncidents], page: 0, size: 100, totalElements: mockIncidents.length, totalPages: 1,
    };
    spyOn(mockIncidentsService, 'listTypes').and.callThrough();
    spyOn(mockIncidentsService, 'list').and.callThrough();
    spyOn(mockIncidentsService, 'create').and.callThrough();

    await TestBed.configureTestingModule({
      imports: [Incidencias],
      providers: [
        provideHttpClient(),
        { provide: IncidentsService, useValue: mockIncidentsService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Incidencias);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('creates the incidencias component', () => {
    expect(component).toBeTruthy();
  });

  it('loads incidencias from API on init', () => {
    expect(mockIncidentsService.list).toHaveBeenCalled();
    expect(component.incidencias().length).toBe(6);
  });

  it('loads tipos from API on init', () => {
    expect(mockIncidentsService.listTypes).toHaveBeenCalled();
    expect(component.tiposDisponibles.length).toBe(3);
    expect(component.tiposDisponibles).toContain('Permiso Personal');
  });

  it('defaults incidencia signals', () => {
    expect(component.loading()).toBeFalse();
    expect(component.error()).toBe('');
    expect(component.filterEstado()).toBe('Todos');
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
    it('should not create incidencia without tipo', async () => {
      component.nuevaIncidencia.tipo = '';
      component.nuevaIncidencia.descripcion = 'Test';
      await component.enviar();
      expect(mockIncidentsService.create).not.toHaveBeenCalled();
    });

    it('should not create incidencia without descripcion', async () => {
      component.nuevaIncidencia.tipo = 'Permiso Personal';
      component.nuevaIncidencia.descripcion = '';
      await component.enviar();
      expect(mockIncidentsService.create).not.toHaveBeenCalled();
    });

    it('should create new incidencia via API and reload list', async () => {
      component.nuevaIncidencia.tipo = 'Permiso Personal';
      component.nuevaIncidencia.descripcion = 'Nueva solicitud';

      await component.enviar();

      expect(mockIncidentsService.create).toHaveBeenCalledWith({
        incidencia_type_id: 1,
        comment: 'Nueva solicitud',
      });
      expect(component.mostrarModalCrear).toBeFalse();
    });

    it('should handle create error', async () => {
      (mockIncidentsService.create as jasmine.Spy).and.returnValue(throwError(() => new Error('fail')));
      component.nuevaIncidencia.tipo = 'Permiso Personal';
      component.nuevaIncidencia.descripcion = 'Test';

      await component.enviar();

      expect(component.error()).toContain('Error al crear');
    });
  });

  describe('cargarDatos', () => {
    it('should set error on load failure', async () => {
      (mockIncidentsService.list as jasmine.Spy).and.returnValue(throwError(() => new Error('fail')));
      await component.cargarDatos();
      expect(component.error()).toContain('Error al cargar');
    });
  });

  describe('descargarArchivo', () => {
    it('should do nothing when no archivo', () => {
      const inc = component.incidencias()[1];
      inc.archivo = null;
      component.descargarArchivo(inc);
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
