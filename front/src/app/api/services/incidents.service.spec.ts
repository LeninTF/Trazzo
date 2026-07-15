import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { IncidentsService } from './incidents.service';
import { API_BASE_URL } from './helpers';

describe('IncidentsService', () => {
  let service: IncidentsService;
  let httpMock: HttpTestingController;
  let apiBase: string;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        IncidentsService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'https://api.trazzo.pe/api/v1' },
      ],
    });
    service = TestBed.inject(IncidentsService);
    httpMock = TestBed.inject(HttpTestingController);
    apiBase = TestBed.inject(API_BASE_URL);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('types', () => {
    it('should list types', () => {
      service.listTypes({ activo: true }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/incidentes/tipos`);
      expect(req.request.params.get('activo')).toBe('true');
      req.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    });

    it('should list types with pagination', () => {
      service.listTypes({ page: 1, size: 10 }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/incidentes/tipos`);
      expect(req.request.params.get('page')).toBe('1');
      expect(req.request.params.get('size')).toBe('10');
      req.flush({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 });
    });

    it('should create type', () => {
      const body = { nombre: 'Tardanza', descripcion: 'Llegada tarde' };
      service.createType(body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/incidentes/tipos`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 1, nombre: 'Tardanza', descripcion: 'Llegada tarde', activo: true, created_at: '', updated_at: '' });
    });

    it('should patch type', () => {
      const body = { descripcion: 'Updated' };
      service.patchType(1, body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/incidentes/tipos/1`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 1, nombre: 'Tardanza', descripcion: 'Updated', activo: true, created_at: '', updated_at: '' });
    });
  });

  describe('incidents', () => {
    it('should list incidents with filters', () => {
      service.list({ page: 1, state: 'PENDIENTE', scope: 'ALL_TENANT' }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/incidentes`);
      expect(req.request.params.get('state')).toBe('PENDIENTE');
      expect(req.request.params.get('page')).toBe('1');
      expect(req.request.params.get('scope')).toBe('ALL_TENANT');
      req.flush({ content: [], page: 1, size: 20, totalElements: 0, totalPages: 0 });
    });

    it('should list incidents without filters', () => {
      service.list().subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/incidentes`);
      expect(req.request.params.keys().length).toBe(0);
      req.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    });

    it('should create incident', () => {
      const body = { incidencia_type_id: 1, comment: 'Justificación médica' };
      service.create(body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/incidentes`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 1, incidencia_type_id: 1, comment: 'Justificación médica', state: 'PENDIENTE' });
    });

    it('should get incident by id', () => {
      service.get(5).subscribe();
      const req = httpMock.expectOne(`${apiBase}/incidentes/5`);
      expect(req.request.method).toBe('GET');
      req.flush({ id: 5 });
    });

    it('should patch incident', () => {
      const body = { comment: 'Updated comment' };
      service.patch(1, body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/incidentes/1`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 1, comment: 'Updated comment' });
    });

    it('should change state to APROBADO', () => {
      const body = { state: 'APROBADO' as const, days_granted: 3 };
      service.changeState(1, body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/incidentes/1/estado`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 1, state: 'APROBADO' });
    });

    it('should change state to DENEGADO', () => {
      const body = { state: 'DENEGADO' as const, motivo_rechazo: 'Documentación insuficiente' };
      service.changeState(2, body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/incidentes/2/estado`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 2, state: 'DENEGADO' });
    });
  });

  describe('evidence', () => {
    it('should list evidence', () => {
      service.listEvidence(1).subscribe();
      const req = httpMock.expectOne(`${apiBase}/incidentes/1/evidencias`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });

    it('should create evidence', () => {
      const body = { file_name: 'doc.pdf', file_key: 'evidences/1/doc.pdf', mime_type: 'application/pdf', file_size: 1024 };
      service.createEvidence(1, body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/incidentes/1/evidencias`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 1, incidencia_id: 1, ...body, file_url: '#', created_at: '', updated_at: '' });
    });

    it('should delete evidence', () => {
      service.deleteEvidence(1, 99).subscribe();
      const req = httpMock.expectOne(`${apiBase}/incidentes/1/evidencias/99`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
