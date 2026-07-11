import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Evidence, CreateEvidenceRequest, PresignedUrlResponse } from '../shared/models/evidence.model';

@Injectable({ providedIn: 'root' })
export class IncidentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1';

  getPresignedUrl(fileName: string, contentType: string): Observable<PresignedUrlResponse> {
    return this.http.get<PresignedUrlResponse>(`${this.baseUrl}/storage/presigned-url`, {
      params: { fileName, contentType }
    });
  }

  createEvidence(incidentId: string, request: CreateEvidenceRequest): Observable<Evidence> {
    return this.http.post<Evidence>(`${this.baseUrl}/incidentes/${incidentId}/evidencias`, request);
  }

  listEvidences(incidentId: string): Observable<Evidence[]> {
    return this.http.get<Evidence[]>(`${this.baseUrl}/incidentes/${incidentId}/evidencias`);
  }

  deleteEvidence(incidentId: string, evidenceId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/incidentes/${incidentId}/evidencias/${evidenceId}`);
  }
}
