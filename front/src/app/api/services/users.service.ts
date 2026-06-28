import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type {
  TenantUserProfile, TenantUserListResponse,
  CrearTenantUsuarioRequest, PatchTenantUsuarioRequest, AsignarRolRequest,
  CambiarPasswordRequest, SoftDeleteResponse,
  MasterUserProfile, MasterUserListResponse,
} from '../types';
import { API, params } from './helpers';

@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly http = inject(HttpClient);

  // ========== TENANT USERS ==========
  list(opts?: {
    scope?: string; branch_id?: number; area_id?: number;
    departamento_id?: number; status?: string; role_id?: number;
    search?: string; page?: number; size?: number; sort?: string;
  }): Observable<TenantUserListResponse> {
    return this.http.get<TenantUserListResponse>(`${API}/usuarios`, { params: params(opts) });
  }

  get(id: number): Observable<TenantUserProfile> {
    return this.http.get<TenantUserProfile>(`${API}/usuarios/${id}`);
  }

  create(body: CrearTenantUsuarioRequest): Observable<TenantUserProfile> {
    return this.http.post<TenantUserProfile>(`${API}/usuarios`, body);
  }

  update(id: number, body: CrearTenantUsuarioRequest): Observable<TenantUserProfile> {
    return this.http.put<TenantUserProfile>(`${API}/usuarios/${id}`, body);
  }

  patch(id: number, body: PatchTenantUsuarioRequest): Observable<TenantUserProfile> {
    return this.http.patch<TenantUserProfile>(`${API}/usuarios/${id}`, body);
  }

  delete(id: number): Observable<SoftDeleteResponse> {
    return this.http.delete<SoftDeleteResponse>(`${API}/usuarios/${id}`);
  }

  assignRole(id: number, body: AsignarRolRequest): Observable<TenantUserProfile> {
    return this.http.put<TenantUserProfile>(`${API}/usuarios/${id}/rol`, body);
  }

  changePassword(id: number, body: CambiarPasswordRequest): Observable<void> {
    return this.http.patch<void>(`${API}/usuarios/${id}/password`, body);
  }

  getMe(): Observable<TenantUserProfile> {
    return this.http.get<TenantUserProfile>(`${API}/usuarios/me`);
  }

  patchMe(body: { phone?: string | null; img_url?: string | null }): Observable<TenantUserProfile> {
    return this.http.patch<TenantUserProfile>(`${API}/usuarios/me`, body);
  }

  // ========== SAAS USERS ==========
  listMasters(opts?: {
    tenant_id?: string; search?: string; page?: number; size?: number; sort?: string;
  }): Observable<MasterUserListResponse> {
    return this.http.get<MasterUserListResponse>(`${API}/saas/users`, { params: params(opts) });
  }

  getMaster(id: number): Observable<MasterUserProfile> {
    return this.http.get<MasterUserProfile>(`${API}/saas/users/${id}`);
  }

  getMasterMe(): Observable<MasterUserProfile> {
    return this.http.get<MasterUserProfile>(`${API}/saas/users/me`);
  }

  patchMasterMe(body: { img_url?: string | null }): Observable<MasterUserProfile> {
    return this.http.patch<MasterUserProfile>(`${API}/saas/users/me`, body);
  }
}
