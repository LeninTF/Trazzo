import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { ApiService } from '../../../api/services/api.service';
import type { IncidentProfile } from '../../../api/types';
import { ToastService } from '../../../services/toast.service';
import { Incidencias } from './incidencias';

const makeIncident = (
  id: number, state: 'PENDIENTE' | 'APROBADO' | 'DENEGADO',
  nombre: string, apellido: string, tipoNombre: string,
): IncidentProfile => ({
  id, tenant_user_id: id, incidencia_type_id: id, state,
  comment: `Comentario ${id}`,
  tipo: { id, nombre: tipoNombre, descripcion: null, activo: true, created_at: '', updated_at: '' },
  permiso: null, evidencias: [],
  tenant_user: { id, nombre, apellido_paterno: apellido, apellido_materno: '', email: `${nombre.toLocaleLowerCase()}.${apellido.toLocaleLowerCase()}@colegio.edu.pe` },
  created_at: '2025-06-01T00:00:00Z', updated_at: '2025-06-01T00:00:00Z',
});

let incidentsData: IncidentProfile[];

const mockApi = {
  incidents: {
    list: () => of({ content: incidentsData, page: 0, size: 100, totalElements: incidentsData.length, totalPages: 1 }),
    changeState: (id: number, body: { state: 'APROBADO' | 'DENEGADO'; days_granted?: number; motivo_rechazo?: string }) => {
      if (body.state === 'DENEGADO' && (!body.motivo_rechazo || !body.motivo_rechazo.trim())) {
        return throwError(() => ({
          error: {
            status: 400,
            error: 'Validation Error',
            message: 'motivo_rechazo is required when state is DENEGADO',
            details: [{ field: 'motivo_rechazo', message: 'Debes ingresar un motivo para rechazar la incidencia.' }],
          },
        }));
      }
      const inc = incidentsData.find(x => x.id === id);
      if (inc) inc.state = body.state;
      return of({} as any);
    },
  },
};

const mockToast = {
  success: jasmine.createSpy('success'),
  error: jasmine.createSpy('error'),
};

describe('Incidencias (admin-tenant)', () => {
  let component: Incidencias;
  let fixture: ComponentFixture<Incidencias>;

  beforeEach(async () => {
    incidentsData = [
      makeIncident(1, 'PENDIENTE', 'Mariana', 'Rodríguez', 'Vacaciones'),
      makeIncident(2, 'APROBADO', 'Juan', 'Pérez', 'Permiso Médico'),
      makeIncident(3, 'DENEGADO', 'Carlos', 'López', 'Capacitación'),
    ];
    incidentsData[0].evidencias.push({ id: 1, incidencia_id: 1, file_name: 'doc.pdf', file_url: '#', mime_type: 'application/pdf', file_size: 2048, created_at: '', updated_at: '' });

    mockToast.success.calls.reset();
    mockToast.error.calls.reset();

    await TestBed.configureTestingModule({
      imports: [Incidencias],
      providers: [
        provideHttpClient(),
        { provide: ApiService, useValue: mockApi },
        { provide: ToastService, useValue: mockToast },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Incidencias);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 3 initial solicitudes', () => {
    expect(component.solicitudes().length).toBe(3);
  });

  it('should have tipos disponibles', () => {
    expect(component.tiposDisponibles.length).toBe(3);
  });

  it('should compute filtradas by estado', () => {
    component.setFilterEstado('Pendiente');
    expect(component.filtradas().every(s => s.estado === 'Pendiente')).toBeTrue();
  });

  it('should compute filtradas by tipo', () => {
    component.setFilterTipo('Vacaciones');
    expect(component.filtradas().every(s => s.tipo === 'Vacaciones')).toBeTrue();
  });

  it('should show all with Todos and empty tipo', () => {
    component.setFilterEstado('Todos');
    component.setFilterTipo('');
    expect(component.filtradas().length).toBe(3);
  });

  it('should compute pendientes/aprobadas/rechazadas', () => {
    expect(component.pendientes().every(s => s.estado === 'Pendiente')).toBeTrue();
    expect(component.aprobadas().every(s => s.estado === 'Aprobado')).toBeTrue();
    expect(component.rechazadas().every(s => s.estado === 'Rechazado')).toBeTrue();
  });

  it('should compute metricas', () => {
    expect(component.metricas.length).toBe(3);
    expect(component.metricas[0].titulo).toBe('Pendientes');
  });

  it('should toggleFilter', () => {
    component.toggleFilter();
    expect(component.filterOpen).toBeTrue();
    component.toggleFilter();
    expect(component.filterOpen).toBeFalse();
  });

  it('should setFilterEstado', () => {
    component.setFilterEstado('Aprobado');
    expect(component.filterEstado()).toBe('Aprobado');
  });

  it('should setFilterTipo toggle', () => {
    component.setFilterTipo('Permiso Médico');
    expect(component.filterTipo()).toBe('Permiso Médico');
    component.setFilterTipo('Permiso Médico');
    expect(component.filterTipo()).toBe('');
  });

  it('should limpiarFiltros', () => {
    component.setFilterEstado('Pendiente');
    component.setFilterTipo('Vacaciones');
    component.limpiarFiltros();
    expect(component.filterEstado()).toBe('Todos');
    expect(component.filterTipo()).toBe('');
  });

  it('should detect hayFiltrosActivos', () => {
    expect(component.hayFiltrosActivos).toBeFalse();
    component.setFilterEstado('Pendiente');
    expect(component.hayFiltrosActivos).toBeTrue();
  });

  it('should openModal and closeModal', () => {
    component.openModal(component.solicitudes()[0]);
    expect(component.modalOpen).toBeTrue();
    expect(component.selectedSolicitud?.colaborador).toBe('Mariana Rodríguez');
    component.closeModal();
    expect(component.modalOpen).toBeFalse();
    expect(component.selectedSolicitud).toBeNull();
  });

  it('should aprobar solicitud', async () => {
    const sol = component.solicitudes()[0];
    expect(sol.estado).toBe('Pendiente');
    await component.aprobar(sol);
    expect(component.solicitudes()[0].estado).toBe('Aprobado');
    expect(component.modalOpen).toBeFalse();
  });

  it('should open rejection form on first rechazar click and not call api', async () => {
    const spy = spyOn(mockApi.incidents, 'changeState').and.callThrough();
    component.openModal(component.solicitudes()[0]);
    await component.rechazar(component.solicitudes()[0]);
    expect(component.showRejectionForm()).toBeTrue();
    expect(spy).not.toHaveBeenCalled();
  });

  it('should show rejection error and not call api when motivo is empty', async () => {
    const spy = spyOn(mockApi.incidents, 'changeState').and.callThrough();
    component.openModal(component.solicitudes()[0]);
    await component.rechazar(component.solicitudes()[0]); // abre form
    await component.rechazar(component.solicitudes()[0]); // intenta sin motivo
    expect(component.showRejectionError()).toBeTrue();
    expect(spy).not.toHaveBeenCalled();
    expect(component.modalOpen).toBeTrue();
  });

  it('should rechazar solicitud when motivo provided', async () => {
    component.openModal(component.solicitudes()[0]);
    await component.rechazar(component.solicitudes()[0]); // abre form
    component.rejectionReason.set('Documentación insuficiente');
    await component.rechazar(component.solicitudes()[0]); // confirma
    expect(component.solicitudes()[0].estado).toBe('Rechazado');
    expect(component.modalOpen).toBeFalse();
  });

  it('should show toast on aprobar error', async () => {
    mockApi.incidents.changeState = () => throwError(() => new Error('fail'));
    const sol = component.solicitudes()[0];
    await component.aprobar(sol);
    expect(mockToast.error).toHaveBeenCalledWith('Error al aprobar');
  });

  it('should show toast on rechazar error from api with other message', async () => {
    mockApi.incidents.changeState = () => throwError(() => ({ error: { message: 'Server boom' } }));
    component.openModal(component.solicitudes()[0]);
    await component.rechazar(component.solicitudes()[0]);
    component.rejectionReason.set('algo');
    await component.rechazar(component.solicitudes()[0]);
    expect(mockToast.error).toHaveBeenCalledWith('Server boom');
  });

  it('should set rejection error flag when api returns motivo_rechazo detail', async () => {
    mockApi.incidents.changeState = () => throwError(() => ({
      error: { details: [{ field: 'motivo_rechazo', message: 'Inválido' }] },
    }));
    component.openModal(component.solicitudes()[0]);
    await component.rechazar(component.solicitudes()[0]);
    component.rejectionReason.set('x');
    await component.rechazar(component.solicitudes()[0]);
    expect(component.showRejectionError()).toBeTrue();
    expect(mockToast.error).toHaveBeenCalledWith('Inválido');
  });

  it('should clear showRejectionError when typing non-empty value', () => {
    component.showRejectionError.set(true);
    component.onRejectionReasonInput({ target: { value: 'texto' } } as unknown as Event);
    expect(component.showRejectionError()).toBeFalse();
  });

  it('should toggleRejectionForm and clear state on cancel', () => {
    component.showRejectionForm.set(true);
    component.rejectionReason.set('x');
    component.showRejectionError.set(true);
    component.toggleRejectionForm();
    expect(component.showRejectionForm()).toBeFalse();
    expect(component.rejectionReason()).toBe('');
    expect(component.showRejectionError()).toBeFalse();
  });

  it('should descargarArchivo', async () => {
    const createSpy = spyOn(document, 'createElement').and.returnValue({ href: '', download: '', click: () => {} } as unknown as HTMLAnchorElement);
    spyOn(globalThis, 'fetch').and.returnValue(Promise.resolve(new Response(new Blob(['test']))));
    spyOn(URL, 'createObjectURL').and.returnValue('blob:test');
    spyOn(URL, 'revokeObjectURL');
    await component.descargarArchivo(component.solicitudes()[0]);
    expect(createSpy).toHaveBeenCalledWith('a');
  });

  it('should not descargar when no archivo', async () => {
    const sol = component.solicitudes()[2];
    sol.archivo = null;
    await component.descargarArchivo(sol);
  });

  it('should exportarCSV', () => {
    spyOn(URL, 'createObjectURL').and.returnValue('blob:test');
    spyOn(URL, 'revokeObjectURL');
    component.exportarCSV();
    expect(URL.createObjectURL).toHaveBeenCalled();
  });

  it('should compute filtradas with combined estado and tipo filters', () => {
    component.setFilterEstado('Pendiente');
    component.setFilterTipo('Vacaciones');
    expect(component.filtradas().length).toBe(1);
    expect(component.filtradas()[0].tipo).toBe('Vacaciones');
    expect(component.filtradas()[0].estado).toBe('Pendiente');
  });

  it('should clear filterTipo when toggling the same type', () => {
    component.setFilterTipo('Vacaciones');
    expect(component.filterTipo()).toBe('Vacaciones');
    component.setFilterTipo('Vacaciones');
    expect(component.filterTipo()).toBe('');
  });

  it('should return early from descargarArchivo when archivo is null', async () => {
    const spy = spyOn(HttpClient.prototype, 'get');
    await component.descargarArchivo({ ...component.solicitudes()[1], archivo: null });
    expect(spy).not.toHaveBeenCalled();
  });

  it('should zero-pad metricas rechazadas value when count < 10', () => {
    expect(component.metricas[2].valor).toBe('01');
  });

  it('should show Requieren atención when rechazadas > 0', () => {
    expect(component.metricas[2].subtitulo).toBe('Requieren atención');
  });

  it('should show Sin incidencias críticas and zero-padded 00 when rechazadas is zero', () => {
    component.solicitudes.set(component.solicitudes().filter(s => s.estado !== 'Rechazado'));
    expect(component.metricas[2].valor).toBe('00');
    expect(component.metricas[2].subtitulo).toBe('Sin incidencias críticas');
  });

  it('should not zero-pad metricas rechazadas when count >= 10', () => {
    const rechazadas = Array.from({ length: 10 }, (_, i) => ({
      id: 100 + i, colaborador: `User ${i}`, rol: '', tipo: 'Vacaciones',
      periodo: '', detalle: '', estado: 'Rechazado' as const,
      descripcion: '', fechaCreacion: '', archivo: null,
    }));
    component.solicitudes.set([...component.solicitudes().filter(s => s.estado !== 'Rechazado'), ...rechazadas]);
    expect(component.metricas[2].valor).toBe(10);
  });

  it('should map evidence data to archivo in toSolicitud', () => {
    const sol = component.solicitudes()[0];
    expect(sol.archivo).not.toBeNull();
    expect(sol.archivo?.nombre).toBe('doc.pdf');
    expect(sol.archivo?.tipo).toBe('application/pdf');
  });

  it('should set archivo to null in toSolicitud when no evidence exists', () => {
    const sol = component.solicitudes()[1];
    expect(sol.archivo).toBeNull();
  });

  describe('deep branch coverage - 9 targeted branches', () => {
    it('should handle cargarIncidencias error gracefully', async () => {
      spyOn(mockApi.incidents, 'list').and.returnValue(throwError(() => new Error('Network fail')));
      await component.cargarIncidencias();
      expect(component.loading()).toBeFalse();
    });

    it('should handle aprobar error gracefully', async () => {
      spyOn(mockApi.incidents, 'changeState').and.returnValue(throwError(() => new Error('fail')));
      component.openModal(component.solicitudes()[0]);
      await component.aprobar(component.solicitudes()[0]);
      expect(component.modalOpen).toBeFalse();
    });

    it('should handle rechazar error gracefully', async () => {
      spyOn(mockApi.incidents, 'changeState').and.returnValue(throwError(() => new Error('fail')));
      component.openModal(component.solicitudes()[0]);
      await component.rechazar(component.solicitudes()[0]); // abre form
      component.rejectionReason.set('motivo');
      await component.rechazar(component.solicitudes()[0]); // intenta confirmar
      expect(component.modalOpen).toBeTrue();
      expect(mockToast.error).toHaveBeenCalled();
    });

    it('should handle descargarArchivo HTTP error gracefully', async () => {
      spyOn(HttpClient.prototype, 'get').and.returnValue(throwError(() => new Error('Download fail')));
      await component.descargarArchivo(component.solicitudes()[0]);
      expect(component).toBeTruthy();
    });

    it('should handle toSolicitud with null created_at and null comment', async () => {
      incidentsData = [{
        id: 50, tenant_user_id: 50, incidencia_type_id: 50, state: 'PENDIENTE',
        comment: null as any,
        tipo: { id: 50, nombre: 'Vacaciones', descripcion: null, activo: true, created_at: '', updated_at: '' },
        permiso: null, evidencias: [],
        tenant_user: { id: 50, nombre: 'Ana', apellido_paterno: 'García', apellido_materno: 'López', email: 'ana@test.com' },
        created_at: null as any, updated_at: '',
      }];
      await component.cargarIncidencias();
      const sol = component.solicitudes()[0];
      expect(sol.periodo).toBe('');
      expect(sol.detalle).toBe('');
      expect(sol.descripcion).toBe('');
      expect(sol.fechaCreacion).toBe('');
    });

    it('should handle toSolicitud with unknown state mapping to Pendiente', async () => {
      incidentsData = [{
        id: 60, tenant_user_id: 60, incidencia_type_id: 60, state: 'UNKNOWN' as any,
        comment: 'Test comment that is long enough to be sliced by the detail field',
        tipo: { id: 60, nombre: 'Permiso', descripcion: null, activo: true, created_at: '', updated_at: '' },
        permiso: null, evidencias: [],
        tenant_user: { id: 60, nombre: 'Bob', apellido_paterno: 'Smith', apellido_materno: 'Jones', email: 'bob@test.com' },
        created_at: '2025-07-01T10:00:00Z', updated_at: '2025-07-01T10:00:00Z',
      }];
      await component.cargarIncidencias();
      const sol = component.solicitudes()[0];
      expect(sol.estado).toBe('Pendiente');
      expect(sol.detalle).toBe('Test comment that is long enough to be s');
    });

    it('should handle toSolicitud with evidence file_size null', async () => {
      incidentsData = [{
        id: 70, tenant_user_id: 70, incidencia_type_id: 70, state: 'APROBADO',
        comment: 'Test',
        tipo: { id: 70, nombre: 'Permiso', descripcion: null, activo: true, created_at: '', updated_at: '' },
        permiso: null,
        evidencias: [{ id: 70, incidencia_id: 70, file_name: null as any, file_url: '#', mime_type: null as any, file_size: null as any, created_at: '', updated_at: '' }],
        tenant_user: { id: 70, nombre: 'Eve', apellido_paterno: 'Lee', apellido_materno: '', email: 'eve@test.com' },
        created_at: '2025-07-01T10:00:00Z', updated_at: '2025-07-01T10:00:00Z',
      }];
      await component.cargarIncidencias();
      const sol = component.solicitudes()[0];
      expect(sol.archivo).not.toBeNull();
      expect(sol.archivo?.nombre).toBe('archivo');
      expect(sol.archivo?.tipo).toBe('application/octet-stream');
      expect(sol.archivo?.tamano).toBe('—');
    });

    it('should set document.body.style.overflow on openModal and closeModal', () => {
      component.openModal(component.solicitudes()[0]);
      expect(document.body.style.overflow).toBe('hidden');
      component.closeModal();
      expect(document.body.style.overflow).toBe('');
    });

    it('should handle toSolicitud with file_size of 0 showing —', async () => {
      incidentsData = [{
        id: 80, tenant_user_id: 80, incidencia_type_id: 80, state: 'PENDIENTE',
        comment: 'Zero size',
        tipo: { id: 80, nombre: 'Otro', descripcion: null, activo: true, created_at: '', updated_at: '' },
        permiso: null,
        evidencias: [{ id: 80, incidencia_id: 80, file_name: 'empty.txt', file_url: '#', mime_type: 'text/plain', file_size: 0, created_at: '', updated_at: '' }],
        tenant_user: { id: 80, nombre: 'Zoe', apellido_paterno: 'Kim', apellido_materno: '', email: 'zoe@test.com' },
        created_at: '2025-07-01T10:00:00Z', updated_at: '2025-07-01T10:00:00Z',
      }];
      await component.cargarIncidencias();
      const sol = component.solicitudes()[0];
      expect(sol.archivo).not.toBeNull();
      expect(sol.archivo?.tamano).toBe('—');
    });
  });
});
