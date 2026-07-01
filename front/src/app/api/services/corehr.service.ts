import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type {
  DeviceProfile, DeviceListResponse,
  UserBiometriaListResponse, EnrollSessionResponse, InitEnrollRequest,
  AttendanceProfile, AttendanceListResponse, PatchAttendanceRequest,
  NonWorkingDayProfile, NonWorkingDayListResponse,
  TenantContactProfile, TenantContactListResponse,
  TenantUserDepartmentProfile, TenantUserDepartmentListResponse,
  CreateDeviceRequest, PatchDeviceRequest,
  CreateNonWorkingDayRequest, PatchNonWorkingDayRequest,
  CreateTenantContactRequest,
  CreateTenantUserDepartmentRequest, PatchTenantUserDepartmentRequest,
  MarcacionRequest, MessageResponse,
} from '../types';
import { API_BASE_URL, params } from './helpers';

@Injectable({ providedIn: 'root' })
export class CorehrService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = inject(API_BASE_URL);

  // ========== DEVICES ==========
  listDevices(opts?: { branch_id?: number; state?: boolean; page?: number; size?: number }): Observable<DeviceListResponse> {
    return this.http.get<DeviceListResponse>(`${this.apiBase}/corehr/devices`, { params: params(opts) });
  }

  createDevice(body: CreateDeviceRequest): Observable<DeviceProfile> {
    return this.http.post<DeviceProfile>(`${this.apiBase}/corehr/devices`, body);
  }

  getDevice(id: number): Observable<DeviceProfile> {
    return this.http.get<DeviceProfile>(`${this.apiBase}/corehr/devices/${id}`);
  }

  patchDevice(id: number, body: PatchDeviceRequest): Observable<DeviceProfile> {
    return this.http.patch<DeviceProfile>(`${this.apiBase}/corehr/devices/${id}`, body);
  }

  deleteDevice(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/corehr/devices/${id}`);
  }

  // ========== BIOMETRIA ==========
  listBiometria(opts?: { tenant_user_id?: number; device_id?: number; activo?: boolean; page?: number; size?: number }): Observable<UserBiometriaListResponse> {
    return this.http.get<UserBiometriaListResponse>(`${this.apiBase}/corehr/biometria`, { params: params(opts) });
  }

  initEnroll(body: InitEnrollRequest): Observable<EnrollSessionResponse> {
    return this.http.post<EnrollSessionResponse>(`${this.apiBase}/corehr/biometria/enroll/iniciar`, body);
  }

  // ========== ATTENDANCE ==========
  listAttendance(opts?: {
    scope?: string; branch_id?: number; area_id?: number; departamento_id?: number;
    date_from?: string; date_to?: string; state?: string; tenant_user_id?: number;
    page?: number; size?: number; sort?: string;
  }): Observable<AttendanceListResponse> {
    return this.http.get<AttendanceListResponse>(`${this.apiBase}/corehr/attendance`, { params: params(opts) });
  }

  getAttendance(id: string): Observable<AttendanceProfile> {
    return this.http.get<AttendanceProfile>(`${this.apiBase}/corehr/attendance/${id}`);
  }

  patchAttendance(id: string, body: PatchAttendanceRequest): Observable<AttendanceProfile> {
    return this.http.patch<AttendanceProfile>(`${this.apiBase}/corehr/attendance/${id}`, body);
  }

  // ========== NON WORKING DAYS ==========
  listNonWorkingDays(opts?: {
    date_from?: string; date_to?: string; is_recurring?: boolean;
    page?: number; size?: number;
  }): Observable<NonWorkingDayListResponse> {
    return this.http.get<NonWorkingDayListResponse>(`${this.apiBase}/corehr/non-working-days`, { params: params(opts) });
  }

  createNonWorkingDay(body: CreateNonWorkingDayRequest): Observable<NonWorkingDayProfile> {
    return this.http.post<NonWorkingDayProfile>(`${this.apiBase}/corehr/non-working-days`, body);
  }

  patchNonWorkingDay(id: number, body: PatchNonWorkingDayRequest): Observable<NonWorkingDayProfile> {
    return this.http.patch<NonWorkingDayProfile>(`${this.apiBase}/corehr/non-working-days/${id}`, body);
  }

  deleteNonWorkingDay(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/corehr/non-working-days/${id}`);
  }

  // ========== TENANT CONTACTS ==========
  listTenantContacts(opts?: { type?: string; page?: number; size?: number }): Observable<TenantContactListResponse> {
    return this.http.get<TenantContactListResponse>(`${this.apiBase}/corehr/tenant-contacts`, { params: params(opts) });
  }

  createTenantContact(body: CreateTenantContactRequest): Observable<TenantContactProfile> {
    return this.http.post<TenantContactProfile>(`${this.apiBase}/corehr/tenant-contacts`, body);
  }

  deleteTenantContact(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/corehr/tenant-contacts/${id}`);
  }

  // ========== USER DEPARTMENTS ==========
  listUserDepartments(userId: number, opts?: { activa?: boolean; page?: number; size?: number }): Observable<TenantUserDepartmentListResponse> {
    return this.http.get<TenantUserDepartmentListResponse>(`${this.apiBase}/corehr/usuarios/${userId}/departamentos`, { params: params(opts) });
  }

  createUserDepartment(userId: number, body: CreateTenantUserDepartmentRequest): Observable<TenantUserDepartmentProfile> {
    return this.http.post<TenantUserDepartmentProfile>(`${this.apiBase}/corehr/usuarios/${userId}/departamentos`, body);
  }

  patchUserDepartment(userId: number, deptId: number, body: PatchTenantUserDepartmentRequest): Observable<TenantUserDepartmentProfile> {
    return this.http.patch<TenantUserDepartmentProfile>(`${this.apiBase}/corehr/usuarios/${userId}/departamentos/${deptId}`, body);
  }

  // ========== ASISTENCIA (MIDDLEWARE) ==========
  marcar(body: MarcacionRequest): Observable<AttendanceProfile> {
    return this.http.post<AttendanceProfile>(`${this.apiBase}/asistencia/marcar`, body);
  }

  syncAttendance(body: MarcacionRequest[]): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.apiBase}/asistencia/sync`, body);
  }
}
