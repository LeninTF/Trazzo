import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

/**
 * Stateless JWT: "authenticated" is approximated client-side by token presence
 * (matches auth.interceptor.ts, which reads the same key and clears it on a
 * 401). This doesn't validate the token's signature/expiry — an expired-but-
 * present token still passes the guard and simply 401s on the first API call,
 * which the interceptor already handles by redirecting to /login.
 */
export const authGuard: CanActivateFn = () => {
  if (localStorage.getItem('trazzo_token')) {
    return true;
  }
  const router = inject(Router);
  return router.parseUrl('/login');
};
