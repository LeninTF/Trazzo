import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type { AuditLogEntry, AuditLogListResponse, AuditMetricsResult } from '../types';
import { API_BASE_URL, params } from './helpers';

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = inject(API_BASE_URL);

  listLogs(opts?: {
    searchTerm?: string;
    tenant_id?: string;
    action?: string;
    entity?: string;
    fecha_desde?: string;
    fecha_hasta?: string;
    page?: number;
    size?: number;
  }): Observable<AuditLogListResponse> {
    return this.http.get<AuditLogListResponse>(`${this.apiBase}/audit/logs`, { params: params(opts) });
  }

  getLog(id: string): Observable<AuditLogEntry> {
    return this.http.get<AuditLogEntry>(`${this.apiBase}/audit/logs/${id}`);
  }

  getMetrics(): Observable<AuditMetricsResult> {
    return this.http.get<AuditMetricsResult>(`${this.apiBase}/audit/metrics`);
  }
}
