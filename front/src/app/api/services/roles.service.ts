import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type {
  SaasRoleProfile, CreateSaasRoleRequest, UpdateSaasRoleRequest, UpdateSaasRolePermissionsRequest,
} from '../types';
import { API_BASE_URL } from './helpers';

@Injectable({ providedIn: 'root' })
export class RolesService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = inject(API_BASE_URL);

  list(): Observable<SaasRoleProfile[]> {
    return this.http.get<SaasRoleProfile[]>(`${this.apiBase}/saas/roles`);
  }

  getById(id: number): Observable<SaasRoleProfile> {
    return this.http.get<SaasRoleProfile>(`${this.apiBase}/saas/roles/${id}`);
  }

  create(body: CreateSaasRoleRequest): Observable<SaasRoleProfile> {
    return this.http.post<SaasRoleProfile>(`${this.apiBase}/saas/roles`, body);
  }

  update(id: number, body: UpdateSaasRoleRequest): Observable<SaasRoleProfile> {
    return this.http.put<SaasRoleProfile>(`${this.apiBase}/saas/roles/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/saas/roles/${id}`);
  }

  updatePermissions(id: number, body: UpdateSaasRolePermissionsRequest): Observable<SaasRoleProfile> {
    return this.http.put<SaasRoleProfile>(`${this.apiBase}/saas/roles/${id}/permissions`, body);
  }
}
