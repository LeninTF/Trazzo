import { Routes } from '@angular/router';
import { Index } from './pages/public/index/index';
import { Form } from './pages/public/form/form';
import { Shop } from './pages/shop/shop';
import { PrivacyPolicy } from './pages/public/legal/privacy-policy/privacy-policy';
import { TermsAndConditions } from './pages/public/legal/terms-and-conditions/terms-and-conditions';
import { Login } from './auth/login/login';
import { Dashboard } from './features/admin-tenant/dashboard/dashboard';
import { Monitoreo } from './features/admin-tenant/monitoreo/monitoreo';
import { GestionHorarios } from './features/admin-tenant/gestion-horarios/gestion-horarios';
import { Incidencias } from './features/admin-tenant/incidencias/incidencias';
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
        component: Dashboard
    },
    {
        path: 'tenant/monitoreo',
        component: Monitoreo
    },
    {
        path: 'tenant/incidencias',
        component: Incidencias
    },
    {
        path: 'tenant/reglas-asistencia',
        component: ReglasAsistencia
    },
    {
        path: 'tenant/sedes',
        component: Sedes
    },
    {
        path: 'tenant/gestion-roles',
        component: GestionRoles
    },
    {
        path: 'tenant/configuracion-tenant',
        component: ConfiguracionTenant
    },
    {
        path: 'tenant/planes',
        component: Planes
    },
    {
        path: 'tenant/directorio-personal',
        component: DirectorioPersonal
    },
    {
        path: 'tenant/gestion-horarios',
        component: GestionHorarios
    },
    {
        path: 'tenant/perfil',
        component: Perfil
    },
    {
        path: 'usuario',
        pathMatch: 'full',
        redirectTo: 'usuario/dashboard'
    },
    {
        path: 'usuario/dashboard',
        component: DashboardUsuario
    },
    {
        path: 'usuario/calendario',
        component: CalendarioUsuario
    },
    {
        path: 'usuario/historial-asistencia',
        component: HistorialAsistenciaUsuario
    },
    {
        path: 'usuario/incidencias',
        component: IncidenciasUsuario
    },
    {
        path: 'sass',
        pathMatch: 'full',
        redirectTo: 'sass/tenants'
    },
    
    {
        path: 'sass/tenants',
        component: Tenants
    },
    {
        path: 'sass/gestion-planes',
        component: GestionPlanes
    },
    {
        path: 'sass/solicitudes',
        component: Solicitudes
    },
    {
        path: 'sass/log-auditoria',
        component: LogAuditoria
    },
    {
        path: 'sass/facturas',
        component: Facturas
    },
    {
        path: 'sass/gestion-usuarios',
        component: GestionUsuarios
    },
    {
        path: 'sass/gestion-roles',
        component: GestionRolesSaas
    },
    {
        path: 'sass/perfil',
        component: PerfilSass
    },
    {
        path: 'usuario/perfil',
        component: Perfil
    }
];
