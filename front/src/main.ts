import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './app/api/auth.interceptor';

async function main(): Promise<void> {
  return bootstrapApplication(App, {
    ...appConfig,
    providers: [
      ...appConfig.providers,
      provideHttpClient(withInterceptors([authInterceptor])),
    ],
  }).then(() => undefined);
}

main().catch((err) => console.error(err));
