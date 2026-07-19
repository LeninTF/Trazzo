import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';
import { UsersService } from './users.service';
import { IncidentsService } from './incidents.service';
import { HorariosService } from './horarios.service';
import { CorehrService } from './corehr.service';
import { API_BASE_URL } from './helpers';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'https://api.trazzo.pe/api/v1' },
      ],
    });
    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should have auth service injected', () => {
    expect(service.auth).toBeInstanceOf(AuthService);
  });

  it('should have users service injected', () => {
    expect(service.users).toBeInstanceOf(UsersService);
  });

  it('should have incidents service injected', () => {
    expect(service.incidents).toBeInstanceOf(IncidentsService);
  });

  it('should have horarios service injected', () => {
    expect(service.horarios).toBeInstanceOf(HorariosService);
  });

  it('should have corehr service injected', () => {
    expect(service.corehr).toBeInstanceOf(CorehrService);
  });

  describe('deprecated aliases', () => {
    it('should bind login to auth.login', () => {
      const credentials = { email: 'test@test.com', password: '123' };
      service.login(credentials).subscribe();
      const req = httpMock.expectOne('https://api.trazzo.pe/api/v1/auth/login');
      expect(req.request.method).toBe('POST');
      req.flush({} as any);
    });

    it('should bind getMe to users.getMe', () => {
      service.getMe().subscribe();
      const req = httpMock.expectOne('https://api.trazzo.pe/api/v1/usuarios/me');
      expect(req.request.method).toBe('GET');
      req.flush({} as any);
    });

    it('should bind listIncidents to incidents.list', () => {
      service.listIncidents({}).subscribe();
      const req = httpMock.expectOne('https://api.trazzo.pe/api/v1/incidentes');
      expect(req.request.method).toBe('GET');
      req.flush({ data: [], meta: { total: 0 } });
    });

    it('should bind listShifts to horarios.listShifts', () => {
      service.listShifts({}).subscribe();
      const req = httpMock.expectOne('https://api.trazzo.pe/api/v1/corehr/shifts');
      expect(req.request.method).toBe('GET');
      req.flush({ data: [], meta: { total: 0 } });
    });

    it('should bind listDevices to corehr.listDevices', () => {
      service.listDevices({}).subscribe();
      const req = httpMock.expectOne('https://api.trazzo.pe/api/v1/corehr/devices');
      expect(req.request.method).toBe('GET');
      req.flush({ data: [], meta: { total: 0 } });
    });

    it('should expose static tenantUserToPersonal', () => {
      expect(ApiService.tenantUserToPersonal).toBeDefined();
    });
  });
});
