import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type {
  DashboardQueryParams, DashboardSummaryResponse,
  PuntualidadRolResponse, AlertasResponse,
} from '../types';
import { API_BASE_URL, params } from './helpers';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = inject(API_BASE_URL);

  getSummary(query?: DashboardQueryParams): Observable<DashboardSummaryResponse> {
    return this.http.get<DashboardSummaryResponse>(`${this.apiBase}/dashboard/summary`, { params: params(query as Record<string, string | number | boolean | null | undefined> | undefined) });
  }

  getPuntualidadPorRol(query?: DashboardQueryParams): Observable<PuntualidadRolResponse> {
    return this.http.get<PuntualidadRolResponse>(`${this.apiBase}/dashboard/puntualidad-por-rol`, { params: params(query as Record<string, string | number | boolean | null | undefined> | undefined) });
  }

  getAlertas(): Observable<AlertasResponse> {
    return this.http.get<AlertasResponse>(`${this.apiBase}/dashboard/alertas`);
  }
}
