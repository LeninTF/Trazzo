import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type {
  OrgPaginatedResult,
  OrgBranchResult,
  OrgAreaResult,
  OrgDepartmentResult,
  OrgRoleResult,
  OrgPermissionResult,
  OrgRolePermissionResult,
  OrgUserRoleResult,
} from '../types';
import { API_BASE_URL, params } from './helpers';

@Injectable({ providedIn: 'root' })
export class OrgService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = inject(API_BASE_URL);

  // ========== BRANCHES (Sedes) ==========

  listBranches(opts?: { state?: boolean; search?: string; page?: number; size?: number }): Observable<OrgPaginatedResult<OrgBranchResult>> {
    return this.http.get<OrgPaginatedResult<OrgBranchResult>>(`${this.apiBase}/org/branches`, { params: params(opts) });
  }

  createBranch(body: { name: string; description?: string }): Observable<OrgBranchResult> {
    return this.http.post<OrgBranchResult>(`${this.apiBase}/org/branches`, body);
  }

  updateBranch(id: number, body: { name: string; description?: string }): Observable<OrgBranchResult> {
    return this.http.put<OrgBranchResult>(`${this.apiBase}/org/branches/${id}`, body);
  }

  deleteBranch(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/org/branches/${id}`);
  }

  // ========== AREAS ==========

  listAreas(opts?: { branchId?: number; state?: boolean; search?: string; page?: number; size?: number }): Observable<OrgPaginatedResult<OrgAreaResult>> {
    return this.http.get<OrgPaginatedResult<OrgAreaResult>>(`${this.apiBase}/org/areas`, { params: params(opts) });
  }

  createArea(body: { branchId: number; name: string; description?: string }): Observable<OrgAreaResult> {
    return this.http.post<OrgAreaResult>(`${this.apiBase}/org/areas`, body);
  }

  updateArea(id: number, body: { name: string; description?: string }): Observable<OrgAreaResult> {
    return this.http.put<OrgAreaResult>(`${this.apiBase}/org/areas/${id}`, body);
  }

  deleteArea(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/org/areas/${id}`);
  }

  // ========== DEPARTMENTS ==========

  listDepartments(opts?: { areaId?: number; state?: boolean; search?: string; page?: number; size?: number }): Observable<OrgPaginatedResult<OrgDepartmentResult>> {
    return this.http.get<OrgPaginatedResult<OrgDepartmentResult>>(`${this.apiBase}/org/departments`, { params: params(opts) });
  }

  createDepartment(body: { areaId: number; name: string; description?: string }): Observable<OrgDepartmentResult> {
    return this.http.post<OrgDepartmentResult>(`${this.apiBase}/org/departments`, body);
  }

  updateDepartment(id: number, body: { name: string; description?: string }): Observable<OrgDepartmentResult> {
    return this.http.put<OrgDepartmentResult>(`${this.apiBase}/org/departments/${id}`, body);
  }

  deleteDepartment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/org/departments/${id}`);
  }

  // ========== ROLES ==========

  listRoles(opts?: { search?: string; page?: number; size?: number }): Observable<OrgPaginatedResult<OrgRoleResult>> {
    return this.http.get<OrgPaginatedResult<OrgRoleResult>>(`${this.apiBase}/org/roles`, { params: params(opts) });
  }

  createRole(body: { name: string; description?: string }): Observable<OrgRoleResult> {
    return this.http.post<OrgRoleResult>(`${this.apiBase}/org/roles`, body);
  }

  updateRole(id: string, body: { name: string; description?: string }): Observable<OrgRoleResult> {
    return this.http.put<OrgRoleResult>(`${this.apiBase}/org/roles/${id}`, body);
  }

  deleteRole(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/org/roles/${id}`);
  }

  listRolePermissions(roleId: string): Observable<OrgRolePermissionResult[]> {
    return this.http.get<OrgRolePermissionResult[]>(`${this.apiBase}/org/roles/${roleId}/permissions`);
  }

  assignPermissionToRole(roleId: string, permissionId: string): Observable<OrgRolePermissionResult> {
    return this.http.post<OrgRolePermissionResult>(`${this.apiBase}/org/roles/${roleId}/permissions`, { permissionId });
  }

  removePermissionFromRole(roleId: string, permissionId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/org/roles/${roleId}/permissions/${permissionId}`);
  }

  // ========== PERMISSIONS ==========

  listPermissions(opts?: { search?: string; page?: number; size?: number }): Observable<OrgPaginatedResult<OrgPermissionResult>> {
    return this.http.get<OrgPaginatedResult<OrgPermissionResult>>(`${this.apiBase}/org/permissions`, { params: params(opts) });
  }

  createPermission(body: { name: string; description?: string; masterFeaturesCode?: string }): Observable<OrgPermissionResult> {
    return this.http.post<OrgPermissionResult>(`${this.apiBase}/org/permissions`, body);
  }

  updatePermission(id: string, body: { name: string; description?: string; masterFeaturesCode?: string }): Observable<OrgPermissionResult> {
    return this.http.put<OrgPermissionResult>(`${this.apiBase}/org/permissions/${id}`, body);
  }

  deletePermission(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/org/permissions/${id}`);
  }

  // ========== USER ROLES ==========

  listUserRoles(tenantUserId: number): Observable<OrgUserRoleResult[]> {
    return this.http.get<OrgUserRoleResult[]>(`${this.apiBase}/org/users/${tenantUserId}/roles`);
  }

  assignRoleToUser(tenantUserId: number, body: { roleId: string; departmentId?: number }): Observable<OrgUserRoleResult> {
    return this.http.post<OrgUserRoleResult>(`${this.apiBase}/org/users/${tenantUserId}/roles`, body);
  }

  removeUserRole(tenantUserId: number, assignmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/org/users/${tenantUserId}/roles/${assignmentId}`);
  }
}
