import {
  ApplicationConfig,
  LOCALE_ID,
  provideBrowserGlobalErrorListeners
} from '@angular/core';

import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { mockInterceptor } from './api/mock.interceptor';

import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';

// Registrar idioma español
registerLocaleData(localeEs);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([mockInterceptor])
    ),

    // Configurar locale español
    {
      provide: LOCALE_ID,
      useValue: 'es'
    }
  ]
};