import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let router: Router;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([])],
    });
    router = TestBed.inject(Router);
  });

  function runGuard(): boolean | UrlTree {
    return TestBed.runInInjectionContext(() =>
      authGuard(null as never, null as never),
    ) as boolean | UrlTree;
  }

  it('should allow activation when a token is present', () => {
    localStorage.setItem('trazzo_token', 'fake-jwt');
    expect(runGuard()).toBeTrue();
  });

  it('should redirect to /login when there is no token', () => {
    localStorage.removeItem('trazzo_token');
    const result = runGuard();
    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe('/login');
  });
});
