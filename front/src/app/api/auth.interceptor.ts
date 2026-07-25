import type { HttpEvent, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, throwError } from 'rxjs';

const PUBLIC_ENDPOINTS = ['/auth/login', '/security/public-key'];

function isExternalOrigin(req: HttpRequest<unknown>): boolean {
  if (req.url.startsWith('http://') || req.url.startsWith('https://')) {
    try {
      const host = new URL(req.url).hostname;
      return host !== window.location.hostname;
    } catch {
      return false;
    }
  }
  return false;
}

export function authInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> {
  const router = inject(Router);
  const token = localStorage.getItem('trazzo_token');

  const shouldAttachToken =
    token &&
    !isExternalOrigin(req) &&
    !PUBLIC_ENDPOINTS.some(e => req.url.includes(e));

  if (shouldAttachToken) {
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
