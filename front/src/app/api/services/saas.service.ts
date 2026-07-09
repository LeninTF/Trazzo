import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type { SaasPlanResult, CreateSaasPlanRequest, UpdateSaasPlanRequest } from '../types';
import { API_BASE_URL } from './helpers';

@Injectable({ providedIn: 'root' })
export class SaasService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = inject(API_BASE_URL);

  // ========== PLANS ==========

  listPlans(): Observable<SaasPlanResult[]> {
    return this.http.get<SaasPlanResult[]>(`${this.apiBase}/saas/plans`);
  }

  listActivePlans(): Observable<SaasPlanResult[]> {
    return this.http.get<SaasPlanResult[]>(`${this.apiBase}/saas/plans/active`);
  }

  getPlan(id: number): Observable<SaasPlanResult> {
    return this.http.get<SaasPlanResult>(`${this.apiBase}/saas/plans/${id}`);
  }

  createPlan(body: CreateSaasPlanRequest): Observable<SaasPlanResult> {
    return this.http.post<SaasPlanResult>(`${this.apiBase}/saas/plans`, body);
  }

  updatePlan(id: number, body: UpdateSaasPlanRequest): Observable<SaasPlanResult> {
    return this.http.put<SaasPlanResult>(`${this.apiBase}/saas/plans/${id}`, body);
  }

  activatePlan(id: number): Observable<SaasPlanResult> {
    return this.http.put<SaasPlanResult>(`${this.apiBase}/saas/plans/${id}/activate`, {});
  }

  deactivatePlan(id: number): Observable<SaasPlanResult> {
    return this.http.put<SaasPlanResult>(`${this.apiBase}/saas/plans/${id}/deactivate`, {});
  }

  deletePlan(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/saas/plans/${id}`);
  }
}
