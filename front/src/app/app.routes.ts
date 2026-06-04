import { Routes } from '@angular/router';
import { Index } from './pages/public/index/index';
import { Form } from './pages/public/form/form';
import { Shop } from './pages/shop/shop';
import { PrivacyPolicy } from './pages/public/legal/privacy-policy/privacy-policy';
import { TermsAndConditions } from './pages/public/legal/terms-and-conditions/terms-and-conditions';
import { Login } from './auth/login/login';
import { Dashboard } from './features/admin-tenant/dashboard/dashboard';
import { Monitoreo } from './features/admin-tenant/monitoreo/monitoreo';
import { Section } from './features/admin-tenant/section/section';
import { Incidencias } from './features/admin-tenant/incidencias/incidencias';
import { ReglasAsistencia } from './features/admin-tenant/reglas-asistencia/reglas-asistencia';

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
        component: Section
    },
    {
        path: 'tenant/gestion-roles',
        component: Section
    },
    {
        path: 'tenant/configuracion-tenant',
        component: Section
    },
    {
        path: 'tenant/planes',
        component: Section
    },
    {
        path: 'tenant/directorio-personal',
        component: Section
    },
    {
        path: 'tenant/gestion-horarios',
        component: Section
    }
];
