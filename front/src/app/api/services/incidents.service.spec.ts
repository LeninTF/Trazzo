import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { IncidentsService } from './incidents.service';
import { API } from './helpers';

describe('IncidentsService', () => {
  let service: IncidentsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [IncidentsService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(IncidentsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('types', () => {
    it('should list types', () => {
      service.listTypes({ activo: true }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${API}/incidentes/tipos`);
      expect(req.request.params.get('activo')).toBe('true');
      req.flush({ data: [], meta: { total: 0 } });
    });

    it('should create type', () => {
      const body = { nombre: 'Tardanza', descripcion: 'Llegada tarde' } as any;
      service.createType(body).subscribe();
      const req = httpMock.expectOne(`${API}/incidentes/tipos`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 1, nombre: 'Tardanza' });
    });

    it('should patch type', () => {
      const body = { descripcion: 'Updated' } as any;
      service.patchType(1, body).subscribe();
      const req = httpMock.expectOne(`${API}/incidentes/tipos/1`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ id: 1, nombre: 'Tardanza' });
    });
  });

  describe('incidents', () => {
    it('should list incidents with filters', () => {
      service.list({ page: 1, state: 'PENDING', scope: 'all' }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${API}/incidentes`);
      expect(req.request.params.get('state')).toBe('PENDING');
      expect(req.request.params.get('page')).toBe('1');
      req.flush({ data: [], meta: { total: 0 } });
    });

    it('should create incident', () => {
      const body = { titulo: 'Test', descripcion: 'Test desc' } as any;
      service.create(body).subscribe();
      const req = httpMock.expectOne(`${API}/incidentes`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 1, titulo: 'Test' });
    });

    it('should get incident by id', () => {
      service.get(5).subscribe();
      const req = httpMock.expectOne(`${API}/incidentes/5`);
      expect(req.request.method).toBe('GET');
      req.flush({ id: 5 });
    });

    it('should patch incident', () => {
      const body = { titulo: 'Updated' } as any;
      service.patch(1, body).subscribe();
      const req = httpMock.expectOne(`${API}/incidentes/1`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ id: 1 });
    });

    it('should change state', () => {
      const body = { estado: 'APROBADO' } as any;
      service.changeState(1, body).subscribe();
      const req = httpMock.expectOne(`${API}/incidentes/1/estado`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 1, estado: 'APROBADO' });
    });
  });

  describe('evidence', () => {
    it('should list evidence', () => {
      service.listEvidence(1).subscribe();
      const req = httpMock.expectOne(`${API}/incidentes/1/evidencias`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });

    it('should create evidence', () => {
      const body = { url: 'https://example.com/doc.pdf', descripcion: 'PDF' } as any;
      service.createEvidence(1, body).subscribe();
      const req = httpMock.expectOne(`${API}/incidentes/1/evidencias`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 1, url: 'https://example.com/doc.pdf' });
    });

    it('should delete evidence', () => {
      service.deleteEvidence(1, 99).subscribe();
      const req = httpMock.expectOne(`${API}/incidentes/1/evidencias/99`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
