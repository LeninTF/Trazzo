import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { roleGuard } from './role.guard';
import { RoleService, Role } from '../services/role.service';

describe('roleGuard', () => {
  let router: Router;
  let roleService: RoleService;

  function buildRouteSnapshot(pathSegments: string[]) {
    return {
      url: pathSegments.map(seg => ({ path: seg })),
    } as any;
  }

  function runGuard(pathSegments: string[]): boolean | UrlTree {
    return TestBed.runInInjectionContext(() =>
      roleGuard(buildRouteSnapshot(pathSegments), null as never),
    ) as boolean | UrlTree;
  }

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([])],
    });
    router = TestBed.inject(Router);
    roleService = TestBed.inject(RoleService);
  });

  describe('admin-saas role', () => {
    beforeEach(() => roleService.role.set('admin-saas'));

    it('allows access to /saas/tenants', () => {
      expect(runGuard(['saas', 'tenants'])).toBeTrue();
    });

    it('allows access to /saas/gestion-planes', () => {
      expect(runGuard(['saas', 'gestion-planes'])).toBeTrue();
    });

    it('blocks access to /tenant/dashboard and redirects to /saas/tenants', () => {
      const result = runGuard(['tenant', 'dashboard']);
      expect(result instanceof UrlTree).toBeTrue();
      expect(router.serializeUrl(result as UrlTree)).toBe('/saas/tenants');
    });

    it('blocks access to /usuario/dashboard and redirects to /saas/tenants', () => {
      const result = runGuard(['usuario', 'dashboard']);
      expect(result instanceof UrlTree).toBeTrue();
      expect(router.serializeUrl(result as UrlTree)).toBe('/saas/tenants');
    });
  });

  describe('admin-tenant role', () => {
    beforeEach(() => roleService.role.set('admin-tenant'));

    it('allows access to /tenant/dashboard', () => {
      expect(runGuard(['tenant', 'dashboard'])).toBeTrue();
    });

    it('allows access to /tenant/incidencias', () => {
      expect(runGuard(['tenant', 'incidencias'])).toBeTrue();
    });

    it('blocks access to /saas/tenants and redirects to /tenant/dashboard', () => {
      const result = runGuard(['saas', 'tenants']);
      expect(result instanceof UrlTree).toBeTrue();
      expect(router.serializeUrl(result as UrlTree)).toBe('/tenant/dashboard');
    });

    it('blocks access to /usuario/calendario and redirects to /tenant/dashboard', () => {
      const result = runGuard(['usuario', 'calendario']);
      expect(result instanceof UrlTree).toBeTrue();
      expect(router.serializeUrl(result as UrlTree)).toBe('/tenant/dashboard');
    });
  });

  describe('usuario role', () => {
    beforeEach(() => roleService.role.set('usuario'));

    it('allows access to /usuario/dashboard', () => {
      expect(runGuard(['usuario', 'dashboard'])).toBeTrue();
    });

    it('allows access to /usuario/incidencias', () => {
      expect(runGuard(['usuario', 'incidencias'])).toBeTrue();
    });

    it('blocks access to /tenant/dashboard and redirects to /usuario/dashboard', () => {
      const result = runGuard(['tenant', 'dashboard']);
      expect(result instanceof UrlTree).toBeTrue();
      expect(router.serializeUrl(result as UrlTree)).toBe('/usuario/dashboard');
    });

    it('blocks access to /saas/tenants and redirects to /usuario/dashboard', () => {
      const result = runGuard(['saas', 'tenants']);
      expect(result instanceof UrlTree).toBeTrue();
      expect(router.serializeUrl(result as UrlTree)).toBe('/usuario/dashboard');
    });
  });

  describe('configuring Role directly via setAvailableRoles', () => {
    it('switches role and affects guard decision', () => {
      roleService.setAvailableRoles(['admin-tenant', 'usuario'] as Role[]);
      expect(roleService.availableRoles()).toEqual(['admin-tenant', 'usuario']);

      roleService.switchRole('usuario');
      const blocked = runGuard(['tenant', 'dashboard']);
      expect(blocked instanceof UrlTree).toBeTrue();
      expect(router.serializeUrl(blocked as UrlTree)).toBe('/usuario/dashboard');
    });

    it('default role after clearSession is admin-tenant and redirects are recalculated', () => {
      roleService.clearSession();
      expect(roleService.role()).toBe('admin-tenant');

      const allowed = runGuard(['tenant', 'sedes']);
      expect(allowed).toBeTrue();
    });
  });
});
