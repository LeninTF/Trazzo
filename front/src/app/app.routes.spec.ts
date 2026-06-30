import { routes } from './app.routes';

describe('app.routes', () => {
  it('should have 33 route definitions', () => {
    expect(routes.length).toBe(33);
  });

  it('should define public routes', () => {
    const publicRoutes = routes.filter(r => !r.path?.startsWith('tenant') && !r.path?.startsWith('sass') && !r.path?.startsWith('usuario'));
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
    expect(paths).toContain('tenant/reglas-asistencia');
    expect(paths).toContain('tenant/sedes');
    expect(paths).toContain('tenant/gestion-roles');
    expect(paths).toContain('tenant/configuracion-tenant');
    expect(paths).toContain('tenant/planes');
    expect(paths).toContain('tenant/directorio-personal');
    expect(paths).toContain('tenant/gestion-horarios');
    expect(paths).toContain('tenant/perfil');
  });

  it('should define sass routes', () => {
    const sassRoutes = routes.filter(r => r.path?.startsWith('sass'));
    const paths = sassRoutes.map(r => r.path);
    expect(paths).toContain('sass');
    expect(paths).toContain('sass/tenants');
    expect(paths).toContain('sass/gestion-planes');
    expect(paths).toContain('sass/solicitudes');
    expect(paths).toContain('sass/log-auditoria');
    expect(paths).toContain('sass/facturas');
    expect(paths).toContain('sass/gestion-usuarios');
    expect(paths).toContain('sass/gestion-roles');
    expect(paths).toContain('sass/perfil');
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

  it('should have redirect routes for tenant, sass, and usuario', () => {
    const redirectRoutes = routes.filter(r => r.redirectTo);
    expect(redirectRoutes.length).toBe(3);

    const tenantRedirect = routes.find(r => r.path === 'tenant');
    expect(tenantRedirect?.redirectTo).toBe('tenant/dashboard');
    expect(tenantRedirect?.pathMatch).toBe('full');

    const sassRedirect = routes.find(r => r.path === 'sass');
    expect(sassRedirect?.redirectTo).toBe('sass/tenants');
    expect(sassRedirect?.pathMatch).toBe('full');

    const usuarioRedirect = routes.find(r => r.path === 'usuario');
    expect(usuarioRedirect?.redirectTo).toBe('usuario/dashboard');
    expect(usuarioRedirect?.pathMatch).toBe('full');
  });

  it('should have component associated with non-redirect routes', () => {
    const nonRedirectRoutes = routes.filter(r => !r.redirectTo);
    nonRedirectRoutes.forEach(r => {
      expect(r.component).toBeDefined();
    });
  });
});
