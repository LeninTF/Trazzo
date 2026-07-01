import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type {
  TenantUserProfile, TenantUserListResponse,
  CrearTenantUsuarioRequest, PatchTenantUsuarioRequest, AsignarRolRequest,
  CambiarPasswordRequest, SoftDeleteResponse,
  MasterUserProfile, MasterUserListResponse,
} from '../types';
import { API_BASE_URL, params } from './helpers';

@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = inject(API_BASE_URL);

  list(opts?: {
    scope?: string; branch_id?: number; area_id?: number;
    departamento_id?: number; status?: string; role_id?: number;
    search?: string; page?: number; size?: number; sort?: string;
  }): Observable<TenantUserListResponse> {
    return this.http.get<TenantUserListResponse>(`${this.apiBase}/usuarios`, { params: params(opts) });
  }

  get(id: number): Observable<TenantUserProfile> {
    return this.http.get<TenantUserProfile>(`${this.apiBase}/usuarios/${id}`);
  }

  create(body: CrearTenantUsuarioRequest): Observable<TenantUserProfile> {
    return this.http.post<TenantUserProfile>(`${this.apiBase}/usuarios`, body);
  }

  update(id: number, body: CrearTenantUsuarioRequest): Observable<TenantUserProfile> {
    return this.http.put<TenantUserProfile>(`${this.apiBase}/usuarios/${id}`, body);
  }

  patch(id: number, body: PatchTenantUsuarioRequest): Observable<TenantUserProfile> {
    return this.http.patch<TenantUserProfile>(`${this.apiBase}/usuarios/${id}`, body);
  }

  delete(id: number): Observable<SoftDeleteResponse> {
    return this.http.delete<SoftDeleteResponse>(`${this.apiBase}/usuarios/${id}`);
  }

  assignRole(id: number, body: AsignarRolRequest): Observable<TenantUserProfile> {
    return this.http.put<TenantUserProfile>(`${this.apiBase}/usuarios/${id}/rol`, body);
  }

  changePassword(id: number, body: CambiarPasswordRequest): Observable<void> {
    return this.http.patch<void>(`${this.apiBase}/usuarios/${id}/password`, body);
  }

  getMe(): Observable<TenantUserProfile> {
    return this.http.get<TenantUserProfile>(`${this.apiBase}/usuarios/me`);
  }

  patchMe(body: {
    phone?: string | null;
    img_url?: string | null;
    email?: string;
    persona?: { name?: string; father_surname?: string; mother_surname?: string };
  }): Observable<TenantUserProfile> {
    return this.http.patch<TenantUserProfile>(`${this.apiBase}/usuarios/me`, body);
  }

  // ========== SAAS USERS ==========
  listMasters(opts?: {
    tenant_id?: string; search?: string; page?: number; size?: number; sort?: string;
  }): Observable<MasterUserListResponse> {
    return this.http.get<MasterUserListResponse>(`${this.apiBase}/saas/users`, { params: params(opts) });
  }

  getMaster(id: number): Observable<MasterUserProfile> {
    return this.http.get<MasterUserProfile>(`${this.apiBase}/saas/users/${id}`);
  }

  getMasterMe(): Observable<MasterUserProfile> {
    return this.http.get<MasterUserProfile>(`${this.apiBase}/saas/users/me`);
  }

  patchMasterMe(body: {
    img_url?: string | null;
    email?: string;
    phone?: string | null;
    persona?: { name?: string; father_surname?: string; mother_surname?: string };
  }): Observable<MasterUserProfile> {
    return this.http.patch<MasterUserProfile>(`${this.apiBase}/saas/users/me`, body);
  }
}
