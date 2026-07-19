import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type { SaasPlanResult, CreateSaasPlanRequest, UpdateSaasPlanRequest, InvoiceProfile, InvoiceListResponse, SubscriptionListResponse, HoldingProfile, ShopCheckoutRequest, ShopCheckoutResponse } from '../types';
import { API_BASE_URL, params } from './helpers';

@Injectable({ providedIn: 'root' })
export class SaasService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = inject(API_BASE_URL);

  // ========== PLANS ==========

  /** Public, unauthenticated: used by the marketing-site pricing section. */
  listPublicPlans(): Observable<SaasPlanResult[]> {
    return this.http.get<SaasPlanResult[]>(`${this.apiBase}/public/plans`);
  }

  // ========== SHOP CHECKOUT ==========

  /** Public, unauthenticated: self-signup flow at /shop. */
  checkout(body: ShopCheckoutRequest): Observable<ShopCheckoutResponse> {
    return this.http.post<ShopCheckoutResponse>(`${this.apiBase}/shop/checkout`, body);
  }

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

  // ========== INVOICES ==========

  listInvoices(opts?: {
    paymentStatus?: string; tenantId?: string; dateFrom?: string; dateTo?: string;
    page?: number; size?: number;
  }): Observable<InvoiceListResponse> {
    return this.http.get<InvoiceListResponse>(`${this.apiBase}/saas/invoices`, { params: params(opts) });
  }

  getInvoice(id: string): Observable<InvoiceProfile> {
    return this.http.get<InvoiceProfile>(`${this.apiBase}/saas/invoices/${id}`);
  }

  exportInvoicesExcel(opts?: {
    paymentStatus?: string; tenantId?: string; dateFrom?: string; dateTo?: string;
  }): Observable<Blob> {
    return this.http.get(`${this.apiBase}/saas/invoices/export/excel`,
        { params: params(opts), responseType: 'blob' });
  }

  exportInvoicesPdf(opts?: {
    paymentStatus?: string; tenantId?: string; dateFrom?: string; dateTo?: string;
  }): Observable<Blob> {
    return this.http.get(`${this.apiBase}/saas/invoices/export/pdf`,
        { params: params(opts), responseType: 'blob' });
  }

  // ========== SUBSCRIPTIONS ==========

  listSubscriptions(opts?: { page?: number; size?: number }): Observable<SubscriptionListResponse> {
    return this.http.get<SubscriptionListResponse>(`${this.apiBase}/saas/subscriptions`, { params: params(opts) });
  }

  // ========== HOLDINGS ==========

  listHoldings(): Observable<HoldingProfile[]> {
    return this.http.get<HoldingProfile[]>(`${this.apiBase}/saas/holdings`);
  }
}
