import 'bootstrap';

import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { isDevMode } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './app/api/auth.interceptor';

async function main(): Promise<void> {
  const interceptors = isDevMode()
    ? [authInterceptor, (await import('./app/api/mock.interceptor')).mockInterceptor]
    : [authInterceptor];

  return bootstrapApplication(App, {
    ...appConfig,
    providers: [
      ...appConfig.providers,
      provideHttpClient(withInterceptors(interceptors)),
    ],
  }).then(() => undefined);
}

main().catch((err) => console.error(err));
