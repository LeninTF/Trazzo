import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';
import { UsersService } from './users.service';
import { IncidentsService } from './incidents.service';
import { HorariosService } from './horarios.service';
import { CorehrService } from './corehr.service';
import { OrgService } from './org.service';
import { AuditService } from './audit.service';
import { SaasService } from './saas.service';
import { RequestsService } from './requests.service';
import { RolesService } from './roles.service';
import { TenantsService } from './tenants.service';
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

  it('should have org service injected', () => {
    expect(service.org).toBeInstanceOf(OrgService);
  });

  it('should have audit service injected', () => {
    expect(service.audit).toBeInstanceOf(AuditService);
  });

  it('should have saas service injected', () => {
    expect(service.saas).toBeInstanceOf(SaasService);
  });

  it('should have requests service injected', () => {
    expect(service.requests).toBeInstanceOf(RequestsService);
  });

  it('should have roles service injected', () => {
    expect(service.roles).toBeInstanceOf(RolesService);
  });

  it('should have tenants service injected', () => {
    expect(service.tenants).toBeInstanceOf(TenantsService);
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

    it('should bind listTenantUsers', () => {
      service.listTenantUsers({}).subscribe();
      httpMock.expectOne(r => r.url.includes('/usuarios')).flush({ content: [], totalElements: 0 } as any);
    });

    it('should bind getTenantUser', () => {
      service.getTenantUser(1).subscribe();
      httpMock.expectOne(r => r.url.includes('/usuarios/1')).flush({ id: 1 } as any);
    });

    it('should bind createTenantUser', () => {
      service.createTenantUser({} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/usuarios') && r.method === 'POST').flush({ id: 1 } as any);
    });

    it('should bind updateTenantUser', () => {
      service.updateTenantUser(1, {} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/usuarios/1') && r.method === 'PUT').flush({ id: 1 } as any);
    });

    it('should bind patchTenantUser', () => {
      service.patchTenantUser(1, {} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/usuarios/1') && r.method === 'PATCH').flush({ id: 1 } as any);
    });

    it('should bind deleteTenantUser', () => {
      service.deleteTenantUser(1).subscribe();
      httpMock.expectOne(r => r.url.includes('/usuarios/1') && r.method === 'DELETE').flush(null);
    });

    it('should bind listIncidentTypes', () => {
      service.listIncidentTypes().subscribe();
      httpMock.expectOne(r => r.url.includes('/incidentes/tipos')).flush([] as any);
    });

    it('should bind createIncidentType', () => {
      service.createIncidentType({} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/incidentes/tipos') && r.method === 'POST').flush({ id: 1 } as any);
    });

    it('should bind patchIncidentType', () => {
      service.patchIncidentType(1, {} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/incidentes/tipos/1') && r.method === 'PATCH').flush({ id: 1 } as any);
    });

    it('should bind createIncident', () => {
      service.createIncident({} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/incidentes') && r.method === 'POST').flush({ id: 1 } as any);
    });

    it('should bind getIncident', () => {
      service.getIncident(1).subscribe();
      httpMock.expectOne(r => r.url.includes('/incidentes/1')).flush({ id: 1 } as any);
    });

    it('should bind patchIncident', () => {
      service.patchIncident(1, {} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/incidentes/1') && r.method === 'PATCH').flush({ id: 1 } as any);
    });

    it('should bind changeIncidentState', () => {
      service.changeIncidentState(1, { state: 'APPROVED' } as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/incidentes/1') && r.method === 'PATCH').flush({ id: 1 } as any);
    });

    it('should bind listEvidence', () => {
      service.listEvidence(1).subscribe();
      httpMock.expectOne(r => r.url.includes('/evidencias')).flush([] as any);
    });

    it('should bind createEvidence', () => {
      service.createEvidence(1, {} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/evidencias') && r.method === 'POST').flush({ id: 1 } as any);
    });

    it('should bind deleteEvidence', () => {
      service.deleteEvidence(1, 2).subscribe();
      httpMock.expectOne(r => r.url.includes('/evidencias/2') && r.method === 'DELETE').flush(null);
    });

    it('should bind createShift', () => {
      service.createShift({} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/shifts') && r.method === 'POST').flush({ id: 1 } as any);
    });

    it('should bind getShift', () => {
      service.getShift(1).subscribe();
      httpMock.expectOne(r => r.url.includes('/shifts/1')).flush({ id: 1 } as any);
    });

    it('should bind patchShift', () => {
      service.patchShift(1, {} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/shifts/1') && r.method === 'PATCH').flush({ id: 1 } as any);
    });

    it('should bind deleteShift', () => {
      service.deleteShift(1).subscribe();
      httpMock.expectOne(r => r.url.includes('/shifts/1') && r.method === 'DELETE').flush(null);
    });

    it('should bind listSchedules', () => {
      service.listSchedules({}).subscribe();
      httpMock.expectOne(r => r.url.includes('/schedules')).flush({ data: [], meta: { total: 0 } } as any);
    });

    it('should bind createSchedule', () => {
      service.createSchedule({} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/schedules') && r.method === 'POST').flush({ id: 1 } as any);
    });

    it('should bind getSchedule', () => {
      service.getSchedule(1).subscribe();
      httpMock.expectOne(r => r.url.includes('/schedules/1')).flush({ id: 1 } as any);
    });

    it('should bind patchSchedule', () => {
      service.patchSchedule(1, {} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/schedules/1') && r.method === 'PATCH').flush({ id: 1 } as any);
    });

    it('should bind deleteSchedule', () => {
      service.deleteSchedule(1).subscribe();
      httpMock.expectOne(r => r.url.includes('/schedules/1') && r.method === 'DELETE').flush(null);
    });

    it('should bind listTolerancias', () => {
      service.listTolerancias(1, {}).subscribe();
      httpMock.expectOne(r => r.url.includes('/schedules/1/tolerancias')).flush({ data: [], meta: { total: 0 } } as any);
    });

    it('should bind createTolerancia', () => {
      service.createTolerancia(1, {} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/schedules/1/tolerancias') && r.method === 'POST').flush({ id: 1 } as any);
    });

    it('should bind patchTolerancia', () => {
      service.patchTolerancia(1, 2, {} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/schedules/1/tolerancias/2') && r.method === 'PATCH').flush({ id: 1 } as any);
    });

    it('should bind deleteTolerancia', () => {
      service.deleteTolerancia(1, 2).subscribe();
      httpMock.expectOne(r => r.url.includes('/schedules/1/tolerancias/2') && r.method === 'DELETE').flush(null);
    });

    it('should bind listUserSchedules', () => {
      service.listUserSchedules({}).subscribe();
      httpMock.expectOne(r => r.url.includes('/user-schedules')).flush({ data: [], meta: { total: 0 } } as any);
    });

    it('should bind createUserSchedule', () => {
      service.createUserSchedule({} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/user-schedules') && r.method === 'POST').flush({ id: 1 } as any);
    });

    it('should bind deleteUserSchedule', () => {
      service.deleteUserSchedule(1).subscribe();
      httpMock.expectOne(r => r.url.includes('/user-schedules/1') && r.method === 'DELETE').flush(null);
    });

    it('should bind createDevice', () => {
      service.createDevice({} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/corehr/devices') && r.method === 'POST').flush({ id: 1 } as any);
    });

    it('should bind getDevice', () => {
      service.getDevice(1).subscribe();
      httpMock.expectOne(r => r.url.includes('/corehr/devices/1')).flush({ id: 1 } as any);
    });

    it('should bind patchDevice', () => {
      service.patchDevice(1, {} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/corehr/devices/1') && r.method === 'PATCH').flush({ id: 1 } as any);
    });

    it('should bind deleteDevice', () => {
      service.deleteDevice(1).subscribe();
      httpMock.expectOne(r => r.url.includes('/corehr/devices/1') && r.method === 'DELETE').flush(null);
    });

    it('should bind listBiometria', () => {
      service.listBiometria({}).subscribe();
      httpMock.expectOne(r => r.url.includes('/corehr/biometria')).flush({ data: [], meta: { total: 0 } } as any);
    });

    it('should bind initEnroll', () => {
      service.initEnroll({} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/enroll/iniciar')).flush({ session_id: 's1' } as any);
    });

    it('should bind listAttendance', () => {
      service.listAttendance({}).subscribe();
      httpMock.expectOne(r => r.url.includes('/corehr/attendance')).flush({ data: [], meta: { total: 0 } } as any);
    });

    it('should bind getAttendance', () => {
      service.getAttendance('att-1').subscribe();
      httpMock.expectOne(r => r.url.includes('/corehr/attendance/att-1')).flush({ id: 'att-1' } as any);
    });

    it('should bind patchAttendance', () => {
      service.patchAttendance('att-1', {} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/corehr/attendance/att-1') && r.method === 'PATCH').flush({ id: 'att-1' } as any);
    });

    it('should bind listNonWorkingDays', () => {
      service.listNonWorkingDays({}).subscribe();
      httpMock.expectOne(r => r.url.includes('/non-working-days')).flush({ data: [], meta: { total: 0 } } as any);
    });

    it('should bind createNonWorkingDay', () => {
      service.createNonWorkingDay({} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/non-working-days') && r.method === 'POST').flush({ id: 1 } as any);
    });

    it('should bind patchNonWorkingDay', () => {
      service.patchNonWorkingDay(1, {} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/non-working-days/1') && r.method === 'PATCH').flush({ id: 1 } as any);
    });

    it('should bind deleteNonWorkingDay', () => {
      service.deleteNonWorkingDay(1).subscribe();
      httpMock.expectOne(r => r.url.includes('/non-working-days/1') && r.method === 'DELETE').flush(null);
    });

    it('should bind listTenantContacts', () => {
      service.listTenantContacts({}).subscribe();
      httpMock.expectOne(r => r.url.includes('/tenant-contacts')).flush({ data: [], meta: { total: 0 } } as any);
    });

    it('should bind createTenantContact', () => {
      service.createTenantContact({} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/tenant-contacts') && r.method === 'POST').flush({ id: 1 } as any);
    });

    it('should bind deleteTenantContact', () => {
      service.deleteTenantContact(1).subscribe();
      httpMock.expectOne(r => r.url.includes('/tenant-contacts/1') && r.method === 'DELETE').flush(null);
    });

    it('should bind listUserDepartments', () => {
      service.listUserDepartments(1, {}).subscribe();
      httpMock.expectOne(r => r.url.includes('/usuarios/1/departamentos')).flush({ data: [], meta: { total: 0 } } as any);
    });

    it('should bind createUserDepartment', () => {
      service.createUserDepartment(1, {} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/usuarios/1/departamentos') && r.method === 'POST').flush({ id: 1 } as any);
    });

    it('should bind patchUserDepartment', () => {
      service.patchUserDepartment(1, 2, {} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/usuarios/1/departamentos/2') && r.method === 'PATCH').flush({ id: 1 } as any);
    });

    it('should bind marcar', () => {
      service.marcar({} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/asistencia/marcar')).flush({ id: 'a1' } as any);
    });

    it('should bind syncAttendance', () => {
      service.syncAttendance([] as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/asistencia/sync')).flush({ message: 'ok' } as any);
    });

    it('should bind getPublicKey', () => {
      service.getPublicKey().subscribe();
      httpMock.expectOne(r => r.url.includes('/security/public-key')).flush({ key: 'abc' } as any);
    });

    it('should bind assignRole', () => {
      service.assignRole(1, { roleId: 'r-1' } as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/usuarios/1/rol') && r.method === 'PUT').flush({ id: 1 } as any);
    });

    it('should bind changePassword', () => {
      service.changePassword(1, { current_password: 'old', new_password: 'new' } as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/usuarios/1/password') && r.method === 'PATCH').flush({ id: 1 } as any);
    });

    it('should bind patchMe', () => {
      service.patchMe({} as any).subscribe();
      httpMock.expectOne(r => r.url.includes('/usuarios/me') && r.method === 'PATCH').flush({ id: 1 } as any);
    });

    it('should bind listMasterUsers', () => {
      service.listMasterUsers().subscribe();
      httpMock.expectOne(r => r.url.includes('/saas/users')).flush([] as any);
    });

    it('should bind getMasterUser', () => {
      service.getMasterUser('m-1').subscribe();
      httpMock.expectOne(r => r.url.includes('/saas/users/m-1')).flush({ id: 'm-1' } as any);
    });

    it('should bind getMasterMe', () => {
      service.getMasterMe().subscribe();
      httpMock.expectOne(r => r.url.includes('/saas/users/me')).flush({ id: 1 } as any);
    });

    it('should expose static tenantUserToPersonal', () => {
      expect(ApiService.tenantUserToPersonal).toBeDefined();
    });
  });
});
