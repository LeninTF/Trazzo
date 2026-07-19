import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type {
  RequestDetail, RequestListResponse, RequestCommentProfile, RequestSummary,
  SubmitRequestPayload, ChangeRequestStatusPayload,
} from '../types';
import { API_BASE_URL, params } from './helpers';

@Injectable({ providedIn: 'root' })
export class RequestsService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = inject(API_BASE_URL);

  /** Public, unauthenticated: used by the marketing-site contact form. */
  submit(body: SubmitRequestPayload): Observable<RequestSummary> {
    return this.http.post<RequestSummary>(`${this.apiBase}/requests`, body);
  }

  list(opts?: {
    status?: string; type?: string; search?: string; page?: number; size?: number;
  }): Observable<RequestListResponse> {
    return this.http.get<RequestListResponse>(`${this.apiBase}/saas/requests`, { params: params(opts) });
  }

  getById(id: number): Observable<RequestDetail> {
    return this.http.get<RequestDetail>(`${this.apiBase}/saas/requests/${id}`);
  }

  changeStatus(id: number, body: ChangeRequestStatusPayload): Observable<RequestSummary> {
    return this.http.patch<RequestSummary>(`${this.apiBase}/saas/requests/${id}/status`, body);
  }

  addComment(id: number, comment: string): Observable<RequestCommentProfile> {
    return this.http.post<RequestCommentProfile>(`${this.apiBase}/saas/requests/${id}/comments`, { comment });
  }
}
