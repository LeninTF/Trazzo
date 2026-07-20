import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type {
  TenantSaasProfile, TenantListResponse, TenantMetrics,
  CreateTrialTenantPayload, UpdateTenantBrandingPayload,
} from '../types';
import { API_BASE_URL, params } from './helpers';

@Injectable({ providedIn: 'root' })
export class TenantsService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = inject(API_BASE_URL);

  /** Reuses the existing trial-provisioning flow (creates tenant, schema, activates it). */
  createTrial(body: CreateTrialTenantPayload): Observable<{ id: string }> {
    return this.http.post<{ id: string }>(`${this.apiBase}/tenants/trial`, body);
  }

  list(opts?: {
    search?: string; planId?: number; status?: string; page?: number; size?: number;
  }): Observable<TenantListResponse> {
    return this.http.get<TenantListResponse>(`${this.apiBase}/saas/tenants`, { params: params(opts) });
  }

  getById(id: string): Observable<TenantSaasProfile> {
    return this.http.get<TenantSaasProfile>(`${this.apiBase}/saas/tenants/${id}`);
  }

  getMetrics(): Observable<TenantMetrics> {
    return this.http.get<TenantMetrics>(`${this.apiBase}/saas/tenants/metrics`);
  }

  suspend(id: string): Observable<TenantSaasProfile> {
    return this.http.put<TenantSaasProfile>(`${this.apiBase}/saas/tenants/${id}/suspend`, {});
  }

  reactivate(id: string): Observable<TenantSaasProfile> {
    return this.http.put<TenantSaasProfile>(`${this.apiBase}/saas/tenants/${id}/reactivate`, {});
  }

  updateBranding(id: string, body: UpdateTenantBrandingPayload): Observable<TenantSaasProfile> {
    return this.http.put<TenantSaasProfile>(`${this.apiBase}/saas/tenants/${id}/branding`, body);
  }

  deleteById(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/saas/tenants/${id}`);
  }
}
