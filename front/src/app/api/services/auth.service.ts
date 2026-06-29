import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type { AuthResponse, LoginRequest, PublicKeyResponse } from '../types';
import { API } from './helpers';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  login(body: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API}/auth/login`, body);
  }

  getPublicKey(): Observable<PublicKeyResponse> {
    return this.http.get<PublicKeyResponse>(`${API}/security/public-key`);
  }
}
