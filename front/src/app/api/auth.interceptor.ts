import type { HttpEvent, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, throwError } from 'rxjs';

const PUBLIC_ENDPOINTS = ['/auth/login', '/security/public-key'];

export function authInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> {
  const router = inject(Router);
  const token = localStorage.getItem('trazzo_token');

  if (token && !PUBLIC_ENDPOINTS.some(e => req.url.includes(e))) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  return next(req).pipe(
    catchError((err) => {
      if (err instanceof HttpErrorResponse && err.status === 401) {
        localStorage.removeItem('trazzo_token');
        router.navigate(['/login']);
      }
      return throwError(() => err);
    }),
  );
}
