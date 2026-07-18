import { HttpRequest, HttpErrorResponse, HttpHandlerFn } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let router: jasmine.SpyObj<Router>;
  let next: jasmine.Spy<HttpHandlerFn>;

  beforeEach(() => {
    localStorage.clear();
    router = jasmine.createSpyObj('Router', ['navigate']);
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
      ],
    });
    next = jasmine.createSpy('next').and.returnValue(of({} as any));
  });

  function createReq(method: string, url: string) {
    return new (HttpRequest as any)(method, url) as HttpRequest<unknown>;
  }

  it('should add Authorization header when token exists', (done) => {
    localStorage.setItem('trazzo_token', 'my-token');
    const req = createReq('GET', '/api/v1/usuarios');
    next.and.callFake((r: HttpRequest<unknown>) => {
      expect(r.headers.get('Authorization')).toBe('Bearer my-token');
      return of({} as any);
    });
    TestBed.runInInjectionContext(() => authInterceptor(req, next));
    done();
  });

  it('should not add Authorization header for public endpoints', (done) => {
    localStorage.setItem('trazzo_token', 'my-token');
    const req = createReq('POST', '/api/v1/auth/login');
    next.and.callFake((r: HttpRequest<unknown>) => {
      expect(r.headers.has('Authorization')).toBeFalse();
      return of({} as any);
    });
    TestBed.runInInjectionContext(() => authInterceptor(req, next));
    done();
  });

  it('should not add Authorization header for public-key endpoint', (done) => {
    localStorage.setItem('trazzo_token', 'my-token');
    const req = createReq('GET', '/api/v1/security/public-key');
    next.and.callFake((r: HttpRequest<unknown>) => {
      expect(r.headers.has('Authorization')).toBeFalse();
      return of({} as any);
    });
    TestBed.runInInjectionContext(() => authInterceptor(req, next));
    done();
  });

  it('should not add Authorization header when no token', (done) => {
    const req = createReq('GET', '/api/v1/usuarios');
    next.and.callFake((r: HttpRequest<unknown>) => {
      expect(r.headers.has('Authorization')).toBeFalse();
      return of({} as any);
    });
    TestBed.runInInjectionContext(() => authInterceptor(req, next));
    done();
  });

  it('should redirect to /login on 401 error', (done) => {
    localStorage.setItem('trazzo_token', 'my-token');
    const req = createReq('GET', '/api/v1/usuarios');
    next.and.returnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
    TestBed.runInInjectionContext(() => {
      authInterceptor(req, next).subscribe({
        error: () => {
          expect(localStorage.getItem('trazzo_token')).toBeNull();
          expect(router.navigate).toHaveBeenCalledWith(['/login']);
          done();
        },
      });
    });
  });

  it('should not redirect on non-401 error', (done) => {
    localStorage.setItem('trazzo_token', 'my-token');
    const req = createReq('GET', '/api/v1/usuarios');
    next.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    TestBed.runInInjectionContext(() => {
      authInterceptor(req, next).subscribe({
        error: () => {
          expect(router.navigate).not.toHaveBeenCalled();
          done();
        },
      });
    });
  });
});
