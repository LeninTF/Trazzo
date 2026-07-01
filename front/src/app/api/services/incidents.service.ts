import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type {
  IncidentTypeProfile, IncidentTypeListResponse,
  IncidentProfile, IncidentListResponse,
  CreateIncidentRequest, PatchIncidentRequest, IncidentStateChangeRequest,
  CreateIncidentTypeRequest, PatchIncidentTypeRequest,
  IncidentEvidenceProfile, CreateEvidenceRequest,
} from '../types';
import { API_BASE_URL, params } from './helpers';

@Injectable({ providedIn: 'root' })
export class IncidentsService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = inject(API_BASE_URL);

  // ========== TYPES ==========
  listTypes(opts?: { activo?: boolean; page?: number; size?: number }): Observable<IncidentTypeListResponse> {
    return this.http.get<IncidentTypeListResponse>(`${this.apiBase}/incidentes/tipos`, { params: params(opts) });
  }

  createType(body: CreateIncidentTypeRequest): Observable<IncidentTypeProfile> {
    return this.http.post<IncidentTypeProfile>(`${this.apiBase}/incidentes/tipos`, body);
  }

  patchType(id: number, body: PatchIncidentTypeRequest): Observable<IncidentTypeProfile> {
    return this.http.patch<IncidentTypeProfile>(`${this.apiBase}/incidentes/tipos/${id}`, body);
  }

  // ========== INCIDENTS ==========
  list(opts?: {
    scope?: string; sede_id?: number; area_id?: number;
    departamento_id?: number; state?: string; tipo_id?: number;
    desde?: string; hasta?: string; search?: string;
    page?: number; size?: number; sort?: string;
  }): Observable<IncidentListResponse> {
    return this.http.get<IncidentListResponse>(`${this.apiBase}/incidentes`, { params: params(opts) });
  }

  create(body: CreateIncidentRequest): Observable<IncidentProfile> {
    return this.http.post<IncidentProfile>(`${this.apiBase}/incidentes`, body);
  }

  get(id: number): Observable<IncidentProfile> {
    return this.http.get<IncidentProfile>(`${this.apiBase}/incidentes/${id}`);
  }

  patch(id: number, body: PatchIncidentRequest): Observable<IncidentProfile> {
    return this.http.patch<IncidentProfile>(`${this.apiBase}/incidentes/${id}`, body);
  }

  changeState(id: number, body: IncidentStateChangeRequest): Observable<IncidentProfile> {
    return this.http.patch<IncidentProfile>(`${this.apiBase}/incidentes/${id}/estado`, body);
  }

  // ========== EVIDENCE ==========
  listEvidence(incidentId: number): Observable<IncidentEvidenceProfile[]> {
    return this.http.get<IncidentEvidenceProfile[]>(`${this.apiBase}/incidentes/${incidentId}/evidencias`);
  }

  createEvidence(incidentId: number, body: CreateEvidenceRequest): Observable<IncidentEvidenceProfile> {
    return this.http.post<IncidentEvidenceProfile>(`${this.apiBase}/incidentes/${incidentId}/evidencias`, body);
  }

  deleteEvidence(incidentId: number, evidenceId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/incidentes/${incidentId}/evidencias/${evidenceId}`);
  }
}
