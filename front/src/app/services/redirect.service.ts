import { Injectable } from '@angular/core';

/**
 * Thin wrapper around window.location.href so external redirects (e.g. to Mercado Pago's
 * hosted checkout) can be spied on in tests — window.location's own properties are
 * non-configurable in real browsers, so they can't be stubbed directly.
 */
@Injectable({ providedIn: 'root' })
export class RedirectService {
  redirectTo(url: string): void {
    window.location.href = url;
  }
}
