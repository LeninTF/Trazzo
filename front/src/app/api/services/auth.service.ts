import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type { AuthResponse, LoginRequest, PublicKeyResponse } from '../types';
import { API_BASE_URL } from './helpers';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiBase = inject(API_BASE_URL);

  login(body: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiBase}/auth/login`, body);
  }

  getPublicKey(): Observable<PublicKeyResponse> {
    return this.http.get<PublicKeyResponse>(`${this.apiBase}/security/public-key`);
  }

  /**
   * Stateless JWT: there is no server-side session to invalidate, so logout is
   * purely a client-side token discard (matches how the interceptor clears it
   * on a 401). Callers should also reset RoleService's in-memory user state.
   */
  logout(): void {
    localStorage.removeItem('trazzo_token');
  }
}
