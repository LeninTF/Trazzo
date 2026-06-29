import 'bootstrap';

import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { isDevMode } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './app/api/auth.interceptor';

async function main() {
  if (isDevMode()) {
    const { mockInterceptor } = await import('./app/api/mock.interceptor');
    bootstrapApplication(App, {
      ...appConfig,
      providers: [
        ...appConfig.providers,
        provideHttpClient(withInterceptors([authInterceptor, mockInterceptor])),
      ],
    });
  } else {
    bootstrapApplication(App, {
      ...appConfig,
      providers: [
        ...appConfig.providers,
        provideHttpClient(withInterceptors([authInterceptor])),
      ],
    });
  }
}

main().catch((err) => console.error(err));
