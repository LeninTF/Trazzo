import { routes } from './app.routes';

describe('app.routes', () => {
  it('should have 36 route definitions', () => {
    expect(routes.length).toBe(36);
  });

  it('should define public routes', () => {
    const publicRoutes = routes.filter(r => !r.path?.startsWith('tenant') && !r.path?.startsWith('saas') && !r.path?.startsWith('usuario'));
    const paths = publicRoutes.map(r => r.path);
    expect(paths).toContain('');
    expect(paths).toContain('login');
    expect(paths).toContain('shop');
    expect(paths).toContain('contacto');
    expect(paths).toContain('legal/privacy-policy');
    expect(paths).toContain('legal/terms-and-conditions');
  });

  it('should define tenant routes', () => {
    const tenantRoutes = routes.filter(r => r.path?.startsWith('tenant'));
    const paths = tenantRoutes.map(r => r.path);
    expect(paths).toContain('tenant');
    expect(paths).toContain('tenant/dashboard');
    expect(paths).toContain('tenant/monitoreo');
    expect(paths).toContain('tenant/incidencias');
    expect(paths).toContain('tenant/tipos-incidencia');
    expect(paths).toContain('tenant/reglas-asistencia');
    expect(paths).toContain('tenant/sedes');
    expect(paths).toContain('tenant/gestion-roles');
    expect(paths).toContain('tenant/configuracion-tenant');
    expect(paths).toContain('tenant/planes');
    expect(paths).toContain('tenant/directorio-personal');
    expect(paths).toContain('tenant/gestion-horarios');
    expect(paths).toContain('tenant/perfil');
  });

  it('should define saas routes', () => {
    const saasRoutes = routes.filter(r => r.path?.startsWith('saas'));
    const paths = saasRoutes.map(r => r.path);
    expect(paths).toContain('saas');
    expect(paths).toContain('saas/tenants');
    expect(paths).toContain('saas/gestion-planes');
    expect(paths).toContain('saas/solicitudes');
    expect(paths).toContain('saas/log-auditoria');
    expect(paths).toContain('saas/facturas');
    expect(paths).toContain('saas/gestion-usuarios');
    expect(paths).toContain('saas/gestion-roles');
    expect(paths).toContain('saas/perfil');
  });

  it('should define usuario routes', () => {
    const usuarioRoutes = routes.filter(r => r.path?.startsWith('usuario'));
    const paths = usuarioRoutes.map(r => r.path);
    expect(paths).toContain('usuario');
    expect(paths).toContain('usuario/dashboard');
    expect(paths).toContain('usuario/calendario');
    expect(paths).toContain('usuario/historial-asistencia');
    expect(paths).toContain('usuario/incidencias');
    expect(paths).toContain('usuario/perfil');
  });

  it('should have redirect routes for tenant, saas, usuario, and ayuda', () => {
    const redirectRoutes = routes.filter(r => r.redirectTo);
    expect(redirectRoutes.length).toBe(4);

    const tenantRedirect = routes.find(r => r.path === 'tenant');
    expect(tenantRedirect?.redirectTo).toBe('tenant/dashboard');
    expect(tenantRedirect?.pathMatch).toBe('full');

    const saasRedirect = routes.find(r => r.path === 'saas');
    expect(saasRedirect?.redirectTo).toBe('saas/tenants');
    expect(saasRedirect?.pathMatch).toBe('full');

    const usuarioRedirect = routes.find(r => r.path === 'usuario');
    expect(usuarioRedirect?.redirectTo).toBe('usuario/dashboard');
    expect(usuarioRedirect?.pathMatch).toBe('full');

    const ayudaRedirect = routes.find(r => r.path === 'ayuda');
    expect(ayudaRedirect?.redirectTo).toBe('ayuda/guia-de-uso');
    expect(ayudaRedirect?.pathMatch).toBe('full');
  });

  it('should have component associated with non-redirect routes', () => {
    const nonRedirectRoutes = routes.filter(r => !r.redirectTo);
    nonRedirectRoutes.forEach(r => {
      expect(r.component).toBeDefined();
    });
  });
});
