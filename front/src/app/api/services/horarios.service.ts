import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type {
  ShiftProfile, ShiftListResponse,
  ScheduleProfile, ScheduleListResponse,
  UserScheduleProfile, UserScheduleListResponse,
  ToleranciaProfile,
  CreateShiftRequest, PatchShiftRequest,
  CreateScheduleRequest, PatchScheduleRequest,
  CreateUserScheduleRequest,
  CreateToleranciaRequest, PatchToleranciaRequest,
  PageResponse,
} from '../types';
import { API_BASE_URL, params } from './helpers';

@Injectable({ providedIn: 'root' })
export class HorariosService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = inject(API_BASE_URL);

  // ========== SHIFTS ==========
  listShifts(opts?: { search?: string; page?: number; size?: number }): Observable<ShiftListResponse> {
    return this.http.get<ShiftListResponse>(`${this.apiBase}/corehr/shifts`, { params: params(opts) });
  }

  createShift(body: CreateShiftRequest): Observable<ShiftProfile> {
    return this.http.post<ShiftProfile>(`${this.apiBase}/corehr/shifts`, body);
  }

  getShift(id: number): Observable<ShiftProfile> {
    return this.http.get<ShiftProfile>(`${this.apiBase}/corehr/shifts/${id}`);
  }

  patchShift(id: number, body: PatchShiftRequest): Observable<ShiftProfile> {
    return this.http.patch<ShiftProfile>(`${this.apiBase}/corehr/shifts/${id}`, body);
  }

  deleteShift(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/corehr/shifts/${id}`);
  }

  // ========== SCHEDULES ==========
  listSchedules(opts?: { shift_id?: number; page?: number; size?: number }): Observable<ScheduleListResponse> {
    return this.http.get<ScheduleListResponse>(`${this.apiBase}/corehr/schedules`, { params: params(opts) });
  }

  createSchedule(body: CreateScheduleRequest): Observable<ScheduleProfile> {
    return this.http.post<ScheduleProfile>(`${this.apiBase}/corehr/schedules`, body);
  }

  getSchedule(id: number): Observable<ScheduleProfile> {
    return this.http.get<ScheduleProfile>(`${this.apiBase}/corehr/schedules/${id}`);
  }

  patchSchedule(id: number, body: PatchScheduleRequest): Observable<ScheduleProfile> {
    return this.http.patch<ScheduleProfile>(`${this.apiBase}/corehr/schedules/${id}`, body);
  }

  deleteSchedule(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/corehr/schedules/${id}`);
  }

  // ========== TOLERANCIAS ==========
  listTolerancias(scheduleId: number, opts?: { page?: number; size?: number }): Observable<PageResponse<ToleranciaProfile>> {
    return this.http.get<PageResponse<ToleranciaProfile>>(`${this.apiBase}/corehr/schedules/${scheduleId}/tolerancias`, { params: params(opts) });
  }

  createTolerancia(scheduleId: number, body: CreateToleranciaRequest): Observable<ToleranciaProfile> {
    return this.http.post<ToleranciaProfile>(`${this.apiBase}/corehr/schedules/${scheduleId}/tolerancias`, body);
  }

  patchTolerancia(scheduleId: number, toleranciaId: number, body: PatchToleranciaRequest): Observable<ToleranciaProfile> {
    return this.http.patch<ToleranciaProfile>(`${this.apiBase}/corehr/schedules/${scheduleId}/tolerancias/${toleranciaId}`, body);
  }

  deleteTolerancia(scheduleId: number, toleranciaId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/corehr/schedules/${scheduleId}/tolerancias/${toleranciaId}`);
  }

  // ========== USER SCHEDULES ==========
  listUserSchedules(opts?: { tenant_user_id?: number; schedule_id?: number; page?: number; size?: number }): Observable<UserScheduleListResponse> {
    return this.http.get<UserScheduleListResponse>(`${this.apiBase}/corehr/user-schedules`, { params: params(opts) });
  }

  createUserSchedule(body: CreateUserScheduleRequest): Observable<UserScheduleProfile> {
    return this.http.post<UserScheduleProfile>(`${this.apiBase}/corehr/user-schedules`, body);
  }

  deleteUserSchedule(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/corehr/user-schedules/${id}`);
  }
}
