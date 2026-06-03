import { Routes } from '@angular/router';
import { Index } from './pages/public/index/index';
import { Form } from './pages/public/form/form';
import { Shop } from './pages/shop/shop';
import { PrivacyPolicy } from './pages/public/legal/privacy-policy/privacy-policy';
import { TermsAndConditions } from './pages/public/legal/terms-and-conditions/terms-and-conditions';
import { Login } from './auth/login/login';
import { Dashboard } from './features/admin-sass/dashboard/dashboard';
import { Monitoreo } from './features/admin-sass/monitoreo/monitoreo';
import { Section } from './features/admin-sass/section/section';

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
        path: 'admin',
        pathMatch: 'full',
        redirectTo: 'admin/dashboard'
    },
    {
        path: 'admin/dashboard',
        component: Dashboard
    },
    {
        path: 'admin/monitoreo',
        component: Monitoreo
    },
    {
        path: 'admin/incidencias',
        component: Section
    },
    {
        path: 'admin/reglas-asistencia',
        component: Section
    },
    {
        path: 'admin/sedes',
        component: Section
    },
    {
        path: 'admin/gestion-roles',
        component: Section
    },
    {
        path: 'admin/configuracion-tenant',
        component: Section
    },
    {
        path: 'admin/planes',
        component: Section
    },
    {
        path: 'admin/directorio-personal',
        component: Section
    },
    {
        path: 'admin/gestion-horarios',
        component: Section
    }
];
