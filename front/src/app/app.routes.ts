import { Routes } from '@angular/router';
import { Index } from './pages/public/index/index';
import { Form } from './pages/public/form/form';
import { Shop } from './pages/shop/shop';

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
        path: 'contacto',
        component: Form
    }
];
