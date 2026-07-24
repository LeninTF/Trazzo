import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import { roleGuard } from './auth/role.guard';
import { Index } from './pages/public/index/index';
import { Form } from './pages/public/form/form';
import { Shop } from './pages/shop/shop';
import { PrivacyPolicy } from './pages/public/legal/privacy-policy/privacy-policy';
import { TermsAndConditions } from './pages/public/legal/terms-and-conditions/terms-and-conditions';
import { Login } from './auth/login/login';
import { HelpPage } from './pages/public/help/help-page';
import { Dashboard } from './features/admin-tenant/dashboard/dashboard';
import { Monitoreo } from './features/admin-tenant/monitoreo/monitoreo';
import { GestionHorarios } from './features/admin-tenant/gestion-horarios/gestion-horarios';
import { Incidencias } from './features/admin-tenant/incidencias/incidencias';
import { TiposIncidencia } from './features/admin-tenant/tipos-incidencia/tipos-incidencia';
import { ReglasAsistencia } from './features/admin-tenant/reglas-asistencia/reglas-asistencia';
import { Sedes } from './features/admin-tenant/sedes/sedes';
import { GestionRoles } from './features/admin-tenant/gestion-roles/gestion-roles';
import { ConfiguracionTenant } from './features/admin-tenant/configuracion-tenant/configuracion-tenant';
import { Planes } from './features/admin-tenant/planes/planes';
import { DirectorioPersonal } from './features/admin-tenant/directorio-personal/directorio-personal';
import { Perfil } from './features/admin-tenant/perfil/perfil';
import { Perfil as PerfilSass } from './features/admin-saas/perfil/perfil';
import { Tenants } from './features/admin-saas/tenants/tenants';
import { GestionPlanes } from './features/admin-saas/gestion-planes/gestion-planes';
import { Solicitudes } from './features/admin-saas/solicitudes/solicitudes';
import { LogAuditoria } from './features/admin-saas/log-auditoria/log-auditoria';
import { Facturas } from './features/admin-saas/facturas/facturas';
import { GestionUsuarios } from './features/admin-saas/gestion-usuarios/gestion-usuarios';
import { GestionRoles as GestionRolesSaas} from './features/admin-saas/gestion-roles/gestion-roles';
import { Dashboard as DashboardUsuario } from './features/usuario/dashboard/dashboard';
import { Calendario as CalendarioUsuario } from './features/usuario/calendario/calendario';
import { HistorialAsistencia as HistorialAsistenciaUsuario } from './features/usuario/historial-asistencia/historial-asistencia';
import { Incidencias as IncidenciasUsuario } from './features/usuario/incidencias/incidencias';

export const routes: Routes = [
    {
        path: '',
        component: Index
    },
    {
        path: 'shop',
        component: Shop
    },
    {
        path: 'login',
        component: Login
    },
    {
        path: 'contacto',
        component: Form
    },
    {
        path: 'legal/privacy-policy',
        component: PrivacyPolicy
    },
    {
        path: 'legal/terms-and-conditions',
        component: TermsAndConditions
    },
    {
        path: 'tenant',
        pathMatch: 'full',
        redirectTo: 'tenant/dashboard'
    },
    {
        path: 'tenant/dashboard',
        component: Dashboard,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'tenant/monitoreo',
        component: Monitoreo,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'tenant/incidencias',
        component: Incidencias,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'tenant/tipos-incidencia',
        component: TiposIncidencia,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'tenant/reglas-asistencia',
        component: ReglasAsistencia,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'tenant/sedes',
        component: Sedes,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'tenant/gestion-roles',
        component: GestionRoles,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'tenant/configuracion-tenant',
        component: ConfiguracionTenant,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'tenant/planes',
        component: Planes,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'tenant/directorio-personal',
        component: DirectorioPersonal,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'tenant/gestion-horarios',
        component: GestionHorarios,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'tenant/perfil',
        component: Perfil,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'usuario',
        pathMatch: 'full',
        redirectTo: 'usuario/dashboard'
    },
    {
        path: 'usuario/dashboard',
        component: DashboardUsuario,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'usuario/calendario',
        component: CalendarioUsuario,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'usuario/historial-asistencia',
        component: HistorialAsistenciaUsuario,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'usuario/incidencias',
        component: IncidenciasUsuario,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'saas',
        pathMatch: 'full',
        redirectTo: 'saas/tenants'
    },

    {
        path: 'saas/tenants',
        component: Tenants,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'saas/gestion-planes',
        component: GestionPlanes,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'saas/solicitudes',
        component: Solicitudes,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'saas/log-auditoria',
        component: LogAuditoria,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'saas/facturas',
        component: Facturas,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'saas/gestion-usuarios',
        component: GestionUsuarios,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'saas/gestion-roles',
        component: GestionRolesSaas,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'saas/perfil',
        component: PerfilSass,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'usuario/perfil',
        component: Perfil,
        canActivate: [authGuard, roleGuard]
    },
    {
        path: 'ayuda/:seccion',
        component: HelpPage
    },
    {
        path: 'ayuda',
        pathMatch: 'full',
        redirectTo: 'ayuda/guia-de-uso'
    }
];
