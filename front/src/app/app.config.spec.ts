import { LOCALE_ID } from '@angular/core';
import { appConfig } from './app.config';

describe('appConfig', () => {
  it('should be defined with providers array', () => {
    expect(appConfig).toBeDefined();
    expect(appConfig.providers).toBeDefined();
    expect(Array.isArray(appConfig.providers)).toBeTrue();
    expect(appConfig.providers.length).toBeGreaterThan(0);
  });

  it('should configure LOCALE_ID as es', () => {
    const localeProviders = appConfig.providers.filter(
      (p: any) => p && p.provide === LOCALE_ID
    );
    expect(localeProviders.length).toBe(1);
    expect((localeProviders[0] as any).useValue).toBe('es');
  });
});
