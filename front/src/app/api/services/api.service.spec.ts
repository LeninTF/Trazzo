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
      httpMock.expectOne('https://api.trazzo.pe/api/v1/auth/login').flush({} as any);
    });

    it('should bind getMe to users.getMe', () => {
      service.getMe().subscribe();
      httpMock.expectOne('https://api.trazzo.pe/api/v1/usuarios/me').flush({} as any);
    });

    it('should bind listIncidents to incidents.list', () => {
      service.listIncidents({}).subscribe();
      httpMock.expectOne('https://api.trazzo.pe/api/v1/incidentes').flush({ data: [], meta: { total: 0 } });
    });

    it('should bind listShifts to horarios.listShifts', () => {
      service.listShifts({}).subscribe();
      httpMock.expectOne('https://api.trazzo.pe/api/v1/corehr/shifts').flush({ data: [], meta: { total: 0 } });
    });

    it('should bind listDevices to corehr.listDevices', () => {
      service.listDevices({}).subscribe();
      httpMock.expectOne('https://api.trazzo.pe/api/v1/corehr/devices').flush({ data: [], meta: { total: 0 } });
    });

    it('should expose static tenantUserToPersonal', () => {
      expect(ApiService.tenantUserToPersonal).toBeDefined();
    });
  });
});
