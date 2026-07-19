import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { API_BASE_URL } from './helpers';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let apiBase: string;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'https://api.trazzo.pe/api/v1' },
      ],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    apiBase = TestBed.inject(API_BASE_URL);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should login and return AuthResponse', () => {
    const credentials = { email: 'test@test.com', password: '123456' };
    const mockResponse = { accessToken: 'token123', tokenType: 'Bearer', usuario: { id: 1, email: 'test@test.com', nombre: 'Test', apellido_paterno: '', apellido_materno: '', status: 'ACTIVO' as const, ultimo_acceso: '', rol: [] } };

    service.login(credentials).subscribe(res => {
      expect(res.accessToken).toBe('token123');
      expect(res.tokenType).toBe('Bearer');
      expect(res.usuario.email).toBe('test@test.com');
    });

    const req = httpMock.expectOne(`${apiBase}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(credentials);
    req.flush(mockResponse);
  });

  it('should getPublicKey and return key', () => {
    const mockResponse = { publicKey: 'pub-key-abc', kid: 'key-id-1' };

    service.getPublicKey().subscribe(res => {
      expect(res.publicKey).toBe('pub-key-abc');
      expect(res.kid).toBe('key-id-1');
    });

    const req = httpMock.expectOne(`${apiBase}/security/public-key`);
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should handle login error', () => {
    const credentials = { email: 'bad@test.com', password: 'wrong' };

    service.login(credentials).subscribe({
      error: err => {
        expect(err.status).toBe(401);
      },
    });

    const req = httpMock.expectOne(`${apiBase}/auth/login`);
    req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });
  });

  describe('logout', () => {
    it('should remove the JWT token from localStorage', () => {
      localStorage.setItem('trazzo_token', 'fake-jwt');
      service.logout();
      expect(localStorage.getItem('trazzo_token')).toBeNull();
    });

    it('should not throw when there is no token to clear', () => {
      localStorage.removeItem('trazzo_token');
      expect(() => service.logout()).not.toThrow();
    });
  });
});
