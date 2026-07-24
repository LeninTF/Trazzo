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
      { id: 1, incidencia_id: 4, file_name: 'justificacion.pdf', file_key: 'k/1', download_url: '/api/v1/incidentes/4/evidencias/1/descarga', mime_type: 'application/pdf', file_size: 512000, created_at: '', updated_at: '' },
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
      { id: 2, incidencia_id: 1, file_name: 'certificado-medico.pdf', file_key: 'k/2', download_url: '/api/v1/incidentes/1/evidencias/2/descarga', mime_type: 'application/pdf', file_size: 1228800, created_at: '', updated_at: '' },
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
  create: (_body: any) => of({ id: 99 } as IncidentProfile),
  getPresignedUrl: (_name: string, _ct: string, _incId?: number) => of({ presigned_url: 'https://r2/presigned', object_key: 'evidences/42/9/uuid/f.pdf' }),
  uploadToR2: (_url: string, _file: File, _ct: string) => of({} as any),
  createEvidence: (_id: number, _body: any) => of({} as any),
};

function makePdfFile(): File {
  const pdfMagic = new Uint8Array([0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e, 0x35, 0x0a, 0x25, 0xe2, 0xe3, 0xcf, 0xd3, 0x0a]);
  return new File([pdfMagic], 'cert.pdf', { type: 'application/pdf' });
}

function makeInvalidFile(): File {
  const exe = new Uint8Array([0x4d, 0x5a, 0x90, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0xff, 0xff]);
  return new File([exe], 'malware.exe', { type: 'application/x-msdownload' });
}

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
    spyOn(mockIncidentsService, 'getPresignedUrl').and.callThrough();
    spyOn(mockIncidentsService, 'uploadToR2').and.callThrough();
    spyOn(mockIncidentsService, 'createEvidence').and.callThrough();

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
    it('should accept a valid PDF and set archivo', async () => {
      const file = makePdfFile();
      const input = document.createElement('input');
      Object.defineProperty(input, 'files', { value: [file] });

      await component.onFileChange({ target: input } as unknown as Event);
      expect(component.nuevaIncidencia.archivo).toBe(file);
      expect(component.fileError()).toBe('');
    });

    it('should reject an invalid file (spoofed mime / disallowed type)', async () => {
      const file = makeInvalidFile();
      const input = document.createElement('input');
      Object.defineProperty(input, 'files', { value: [file] });

      await component.onFileChange({ target: input } as unknown as Event);
      expect(component.nuevaIncidencia.archivo).toBeNull();
      expect(component.fileError().length).toBeGreaterThan(0);
    });

    it('should clear archivo when no file', async () => {
      const input = document.createElement('input');
      Object.defineProperty(input, 'files', { value: [] });

      await component.onFileChange({ target: input } as unknown as Event);
      expect(component.nuevaIncidencia.archivo).toBeNull();
    });
  });

  describe('removeFile', () => {
    it('should clear archivo and stop propagation', async () => {
      const file = makePdfFile();
      const input = document.createElement('input');
      Object.defineProperty(input, 'files', { value: [file] });
      await component.onFileChange({ target: input } as unknown as Event);
      expect(component.nuevaIncidencia.archivo).not.toBeNull();

      const event = { stopPropagation: jasmine.createSpy() } as unknown as Event;
      component.removeFile(event);
      expect(component.nuevaIncidencia.archivo).toBeNull();
      expect(event.stopPropagation).toHaveBeenCalled();
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

    it('should run full upload flow when an archivo is selected', async () => {
      const file = makePdfFile();
      const input = document.createElement('input');
      Object.defineProperty(input, 'files', { value: [file] });
      await component.onFileChange({ target: input } as unknown as Event);

      component.nuevaIncidencia.tipo = 'Permiso Personal';
      component.nuevaIncidencia.descripcion = 'Con evidencia';

      await component.enviar();

      expect(mockIncidentsService.getPresignedUrl).toHaveBeenCalledWith(file.name, 'application/pdf', 99);
      expect(mockIncidentsService.uploadToR2).toHaveBeenCalledWith('https://r2/presigned', file, 'application/pdf');
      expect(mockIncidentsService.createEvidence).toHaveBeenCalledWith(99, jasmine.objectContaining({
        file_name: file.name,
        mime_type: 'application/pdf',
      }));
    });

    it('should handle create error', async () => {
      (mockIncidentsService.create as jasmine.Spy).and.returnValue(throwError(() => new Error('fail')));
      component.nuevaIncidencia.tipo = 'Permiso Personal';
      component.nuevaIncidencia.descripcion = 'Test';

      await component.enviar();

      expect(component.error()).toContain('Error al crear');
    });

    it('should set error when upload fails after create', async () => {
      (mockIncidentsService.getPresignedUrl as jasmine.Spy).and.returnValue(throwError(() => new Error('r2 fail')));
      component.nuevaIncidencia.tipo = 'Permiso Personal';
      component.nuevaIncidencia.descripcion = 'Test';
      const file = makePdfFile();
      const input = document.createElement('input');
      Object.defineProperty(input, 'files', { value: [file] });
      await component.onFileChange({ target: input } as unknown as Event);

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
    it('should do nothing when no archivo', async () => {
      const inc = component.incidencias()[1];
      inc.archivo = null;
      await component.descargarArchivo(inc);
    });

    it('should fetch the download_url, build a blob link and click', async () => {
      localStorage.setItem('trazzo_token', 'my-token');
      const inc = component.incidencias().find(i => i.archivo !== null)!;
      const blob = new Blob(['pdf-bytes'], { type: 'application/pdf' });
      const fetchSpy = spyOn(window, 'fetch').and.resolveTo(new Response(blob, { status: 200, headers: { 'Content-Type': 'application/pdf' } }) as any);
      const link = { href: '', download: '', click: jasmine.createSpy('click') } as unknown as HTMLAnchorElement;
      const realCreateElement = document.createElement.bind(document);
      spyOn(document, 'createElement').and.callFake((tag: string) => {
        if (tag === 'a') return link;
        return realCreateElement(tag as keyof HTMLElementTagNameMap);
      });
      const createUrlSpy = spyOn(URL, 'createObjectURL').and.returnValue('blob:');
      spyOn(URL, 'revokeObjectURL').and.callFake(() => {});

      await component.descargarArchivo(inc);

      expect(fetchSpy).toHaveBeenCalled();
      const [url, init] = fetchSpy.calls.mostRecent().args;
      expect(url).toBe(inc.archivo!.downloadUrl);
      expect((init as any).credentials).toBe('include');
      expect((init as any).headers['Authorization']).toBe('Bearer my-token');
      expect(link.download).toBe(inc.archivo!.nombre);
      expect(createUrlSpy).toHaveBeenCalled();
      expect(link.click).toHaveBeenCalled();
    });

    it('should toast on fetch failure', async () => {
      const inc = component.incidencias().find(i => i.archivo !== null)!;
      spyOn(window, 'fetch').and.resolveTo(new Response(null, { status: 500 }) as any);

      await component.descargarArchivo(inc);
    });
  });
});
