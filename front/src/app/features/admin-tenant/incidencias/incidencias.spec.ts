import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ApiService } from '../../../api/services/api.service';
import type { IncidentProfile } from '../../../api/types';
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
    changeState: (id: number, body: { state: 'APROBADO' | 'DENEGADO' }) => {
      const inc = incidentsData.find(x => x.id === id);
      if (inc) inc.state = body.state;
      return of({} as any);
    },
  },
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

    await TestBed.configureTestingModule({
      imports: [Incidencias],
      providers: [{ provide: ApiService, useValue: mockApi }],
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

  it('should rechazar solicitud', async () => {
    const sol = component.solicitudes()[0];
    await component.rechazar(sol);
    expect(component.solicitudes()[0].estado).toBe('Rechazado');
    expect(component.modalOpen).toBeFalse();
  });

  it('should descargarArchivo', () => {
    spyOn(document, 'createElement').and.returnValue({ href: '', download: '', click: () => {} } as unknown as HTMLAnchorElement);
    component.descargarArchivo(component.solicitudes()[0]);
    expect(document.createElement).toHaveBeenCalledWith('a');
  });

  it('should exportarCSV', () => {
    spyOn(URL, 'createObjectURL').and.returnValue('blob:test');
    spyOn(URL, 'revokeObjectURL');
    component.exportarCSV();
    expect(URL.createObjectURL).toHaveBeenCalled();
  });
});
