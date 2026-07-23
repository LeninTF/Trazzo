import { inject } from '@angular/core';
import { CanActivateFn, Router, type UrlTree } from '@angular/router';
import { RoleService, Role } from '../services/role.service';

const ROLE_PREFIX: Record<Role, string> = {
  'admin-saas': '/saas/',
  'admin-tenant': '/tenant/',
  'usuario': '/usuario/',
};

const ROLE_FALLBACK: Record<Role, string> = {
  'admin-saas': '/saas/tenants',
  'admin-tenant': '/tenant/dashboard',
  'usuario': '/usuario/dashboard',
};

/**
 * Route guard that blocks access to route segments that don't match the user's
 * current role. For example, a user with role 'admin-saas' cannot access '/tenant/...'
 * and will be redirected to their default dashboard.
 *
 * This guard should be used IN ADDITION to authGuard (which only checks token presence).
 * It provides role-based route protection at the frontend level.
 */
export const roleGuard: CanActivateFn = (route) => {
  const roleService = inject(RoleService);
  const router = inject(Router);

  const currentRole = roleService.role();
  const routePath = route.url.map(segment => segment.path).join('/');
  const requiredPrefix = ROLE_PREFIX[currentRole];

  if (routePath.startsWith(requiredPrefix)) {
    return true;
  }

  const fallback = ROLE_FALLBACK[currentRole];
  return router.parseUrl(fallback);
};
