import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { IncidentService } from './incident.service';
import { Evidence, CreateEvidenceRequest, PresignedUrlResponse } from '../shared/models/evidence.model';

describe('IncidentService', () => {
  let service: IncidentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        IncidentService
      ]
    });
    service = TestBed.inject(IncidentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('creates the incident service', () => {
    expect(service).toBeTruthy();
  });

  describe('getPresignedUrl', () => {
    it('should GET presigned URL with query params', () => {
      const mockResponse: PresignedUrlResponse = {
        presigned_url: 'https://r2.example.com/upload?token=abc',
        object_key: 'evidences/123/photo.jpg'
      };

      service.getPresignedUrl('photo.jpg', 'image/jpeg').subscribe(res => {
        expect(res).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(
        r => r.url === '/api/v1/storage/presigned-url'
          && r.params.get('fileName') === 'photo.jpg'
          && r.params.get('contentType') === 'image/jpeg'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should handle special characters in fileName', () => {
      const mockResponse: PresignedUrlResponse = {
        presigned_url: 'https://r2.example.com/upload',
        object_key: 'evidences/file key.pdf'
      };

      service.getPresignedUrl('file key.pdf', 'application/pdf').subscribe(res => {
        expect(res).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(r => r.url === '/api/v1/storage/presigned-url');
      expect(req.request.params.get('fileName')).toBe('file key.pdf');
      expect(req.request.params.get('contentType')).toBe('application/pdf');
      req.flush(mockResponse);
    });
  });

  describe('createEvidence', () => {
    it('should POST evidence to the correct endpoint', () => {
      const incidentId = 'inc-001';
      const request: CreateEvidenceRequest = {
        file_name: 'photo.jpg',
        file_key: 'evidences/inc-001/photo.jpg',
        mime_type: 'image/jpeg',
        file_size: 1024000
      };
      const mockEvidence: Evidence = {
        id: 'ev-001',
        incidencia_id: incidentId,
        file_name: 'photo.jpg',
        file_url: 'https://r2.example.com/evidences/inc-001/photo.jpg',
        file_key: 'evidences/inc-001/photo.jpg',
        mime_type: 'image/jpeg',
        file_size: 1024000,
        created_at: '2025-07-11T10:00:00Z',
        updated_at: '2025-07-11T10:00:00Z'
      };

      service.createEvidence(incidentId, request).subscribe(res => {
        expect(res).toEqual(mockEvidence);
      });

      const req = httpMock.expectOne(`/api/v1/incidentes/${incidentId}/evidencias`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockEvidence);
    });

    it('should handle empty incidentId', () => {
      const request: CreateEvidenceRequest = {
        file_name: 'test.png',
        file_key: 'key',
        mime_type: 'image/png',
        file_size: 500
      };

      service.createEvidence('', request).subscribe();

      const req = httpMock.expectOne(r => r.url === '/api/v1/incidentes//evidencias');
      expect(req.request.method).toBe('POST');
      req.flush({} as Evidence);
    });
  });

  describe('listEvidences', () => {
    it('should GET evidences for the given incident', () => {
      const incidentId = 'inc-002';
      const mockEvidences: Evidence[] = [
        {
          id: 'ev-001',
          incidencia_id: incidentId,
          file_name: 'a.jpg',
          file_url: 'https://example.com/a.jpg',
          file_key: 'a.jpg',
          mime_type: 'image/jpeg',
          file_size: 100,
          created_at: '2025-07-11T10:00:00Z',
          updated_at: '2025-07-11T10:00:00Z'
        },
        {
          id: 'ev-002',
          incidencia_id: incidentId,
          file_name: 'b.pdf',
          file_url: 'https://example.com/b.pdf',
          file_key: 'b.pdf',
          mime_type: 'application/pdf',
          file_size: 200,
          created_at: '2025-07-11T11:00:00Z',
          updated_at: '2025-07-11T11:00:00Z'
        }
      ];

      service.listEvidences(incidentId).subscribe(res => {
        expect(res.length).toBe(2);
        expect(res).toEqual(mockEvidences);
      });

      const req = httpMock.expectOne(`/api/v1/incidentes/${incidentId}/evidencias`);
      expect(req.request.method).toBe('GET');
      req.flush(mockEvidences);
    });

    it('should return empty array when no evidences exist', () => {
      service.listEvidences('inc-empty').subscribe(res => {
        expect(res).toEqual([]);
      });

      const req = httpMock.expectOne('/api/v1/incidentes/inc-empty/evidencias');
      req.flush([]);
    });
  });

  describe('deleteEvidence', () => {
    it('should DELETE the evidence at the correct endpoint', () => {
      let result: void | null = null;
      service.deleteEvidence('inc-003', 'ev-010').subscribe(res => {
        result = res;
      });

      const req = httpMock.expectOne('/api/v1/incidentes/inc-003/evidencias/ev-010');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
      expect(result).toBeNull();
    });

    it('should propagate HTTP errors', () => {
      service.deleteEvidence('inc-004', 'ev-not-found').subscribe({
        next: () => fail('expected an error'),
        error: (err) => {
          expect(err.status).toBe(404);
        }
      });

      const req = httpMock.expectOne('/api/v1/incidentes/inc-004/evidencias/ev-not-found');
      req.flush('Not found', { status: 404, statusText: 'Not Found' });
    });
  });
});
