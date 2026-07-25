import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type {
  MonthlyClosure, MonthlyClosureDetail, MonthlyClosureWithDetails,
  CreateMonthlyClosureRequest,
} from '../types';
import { API_BASE_URL, params } from './helpers';

@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = inject(API_BASE_URL);

  listClosures(opts?: { year?: number; month?: number }): Observable<MonthlyClosure[]> {
    return this.http.get<MonthlyClosure[]>(`${this.apiBase}/reports/monthly-closures`, { params: params(opts) });
  }

  getClosure(id: string): Observable<MonthlyClosure> {
    return this.http.get<MonthlyClosure>(`${this.apiBase}/reports/monthly-closures/${id}`);
  }

  createClosure(req: CreateMonthlyClosureRequest): Observable<MonthlyClosure> {
    return this.http.post<MonthlyClosure>(`${this.apiBase}/reports/monthly-closures`, req);
  }

  getClosureDetail(id: string): Observable<MonthlyClosureDetail> {
    return this.http.get<MonthlyClosureDetail>(`${this.apiBase}/reports/monthly-closure-details/${id}`);
  }

  getFullReport(id: string): Observable<MonthlyClosureWithDetails> {
    return this.http.get<MonthlyClosureWithDetails>(`${this.apiBase}/reports/monthly-reports/${id}`);
  }
}
