import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { CorehrService } from './corehr.service';
import { API_BASE_URL } from './helpers';

describe('CorehrService', () => {
  let service: CorehrService;
  let httpMock: HttpTestingController;
  let apiBase: string;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CorehrService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'https://api.trazzo.pe/api/v1' },
      ],
    });
    service = TestBed.inject(CorehrService);
    httpMock = TestBed.inject(HttpTestingController);
    apiBase = TestBed.inject(API_BASE_URL);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('devices', () => {
    it('should list devices', () => {
      service.listDevices({ branch_id: 1 }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/corehr/devices`);
      expect(req.request.params.get('branch_id')).toBe('1');
      req.flush({ data: [], meta: { total: 0 } });
    });

    it('should create device', () => {
      const body = { nombre: 'Reloj 1', ip: '192.168.1.100' } as any;
      service.createDevice(body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/corehr/devices`);
      expect(req.request.method).toBe('POST');
      req.flush({ id: 1 });
    });

    it('should get device', () => {
      service.getDevice(1).subscribe();
      const req = httpMock.expectOne(`${apiBase}/corehr/devices/1`);
      req.flush({ id: 1 });
    });

    it('should patch device', () => {
      service.patchDevice(1, { nombre: 'Reloj 2' } as any).subscribe();
      const req = httpMock.expectOne(`${apiBase}/corehr/devices/1`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ id: 1 });
    });

    it('should delete device', () => {
      service.deleteDevice(1).subscribe();
      const req = httpMock.expectOne(`${apiBase}/corehr/devices/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('biometria', () => {
    it('should list biometria', () => {
      service.listBiometria({ tenant_user_id: 5 }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/corehr/biometria`);
      expect(req.request.params.get('tenant_user_id')).toBe('5');
      req.flush({ data: [], meta: { total: 0 } });
    });

    it('should init enroll', () => {
      const body = { tenant_user_id: 5, device_id: 1 } as any;
      service.initEnroll(body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/corehr/biometria/enroll/iniciar`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ session_id: 'abc123' });
    });
  });

  describe('attendance', () => {
    it('should list attendance', () => {
      service.listAttendance({ date_from: '2024-01-01', date_to: '2024-01-31' }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/corehr/attendance`);
      expect(req.request.params.get('date_from')).toBe('2024-01-01');
      req.flush({ data: [], meta: { total: 0 } });
    });

    it('should get attendance by id', () => {
      service.getAttendance('att-123').subscribe();
      const req = httpMock.expectOne(`${apiBase}/corehr/attendance/att-123`);
      req.flush({ id: 'att-123' });
    });

    it('should patch attendance', () => {
      const body = { estado: 'JUSTIFICADO' } as any;
      service.patchAttendance('att-123', body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/corehr/attendance/att-123`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 'att-123' });
    });
  });

  describe('non-working days', () => {
    it('should list non-working days', () => {
      service.listNonWorkingDays({ is_recurring: true }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/corehr/non-working-days`);
      expect(req.request.params.get('is_recurring')).toBe('true');
      req.flush({ data: [], meta: { total: 0 } });
    });

    it('should create non-working day', () => {
      const body = { fecha: '2024-12-25', motivo: 'Navidad' } as any;
      service.createNonWorkingDay(body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/corehr/non-working-days`);
      expect(req.request.method).toBe('POST');
      req.flush({ id: 1 });
    });

    it('should patch non-working day', () => {
      service.patchNonWorkingDay(1, { motivo: 'Ferhado' } as any).subscribe();
      const req = httpMock.expectOne(`${apiBase}/corehr/non-working-days/1`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ id: 1 });
    });

    it('should delete non-working day', () => {
      service.deleteNonWorkingDay(1).subscribe();
      const req = httpMock.expectOne(`${apiBase}/corehr/non-working-days/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('tenant contacts', () => {
    it('should list contacts', () => {
      service.listTenantContacts({ type: 'email' }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/corehr/tenant-contacts`);
      expect(req.request.params.get('type')).toBe('email');
      req.flush({ data: [], meta: { total: 0 } });
    });

    it('should create contact', () => {
      const body = { type: 'email', value: 'admin@test.com' } as any;
      service.createTenantContact(body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/corehr/tenant-contacts`);
      expect(req.request.method).toBe('POST');
      req.flush({ id: 1 });
    });

    it('should delete contact', () => {
      service.deleteTenantContact(1).subscribe();
      const req = httpMock.expectOne(`${apiBase}/corehr/tenant-contacts/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('user departments', () => {
    it('should list departments', () => {
      service.listUserDepartments(5).subscribe();
      const req = httpMock.expectOne(`${apiBase}/corehr/usuarios/5/departamentos`);
      expect(req.request.method).toBe('GET');
      req.flush({ data: [], meta: { total: 0 } });
    });

    it('should create department', () => {
      const body = { departamento_id: 3 } as any;
      service.createUserDepartment(5, body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/corehr/usuarios/5/departamentos`);
      expect(req.request.method).toBe('POST');
      req.flush({ id: 1 });
    });

    it('should patch department', () => {
      service.patchUserDepartment(5, 3, { activa: false } as any).subscribe();
      const req = httpMock.expectOne(`${apiBase}/corehr/usuarios/5/departamentos/3`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ id: 3 });
    });
  });

  describe('asistencia middleware', () => {
    it('should marcar', () => {
      const body = { usuario_id: 5, dispositivo_id: 1, timestamp: '2024-01-01T08:00:00' } as any;
      service.marcar(body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/asistencia/marcar`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 'att-1' });
    });

    it('should sync attendance', () => {
      const body = [{ usuario_id: 5, timestamp: '2024-01-01T08:00:00' }] as any;
      service.syncAttendance(body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/asistencia/sync`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ message: 'Synced' });
    });
  });

  describe('pendingEnroll', () => {
    it('should get pending enroll', () => {
      service.pendingEnroll('DEV-001').subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/corehr/biometria/enroll/pendiente`);
      expect(req.request.params.get('device_code')).toBe('DEV-001');
      req.flush({ session_id: 's1' });
    });
  });

  describe('completeEnroll', () => {
    it('should complete enroll', () => {
      const body = { session_id: 's1', template_base64: 'abc' } as any;
      service.completeEnroll(body).subscribe();
      const req = httpMock.expectOne(`${apiBase}/corehr/biometria/enroll/completar`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 1 });
    });
  });

  describe('listDevices with no params', () => {
    it('should list devices without params', () => {
      service.listDevices().subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/corehr/devices`);
      req.flush({ data: [], meta: { total: 0 } });
    });
  });

  describe('listBiometria with no params', () => {
    it('should list biometria without params', () => {
      service.listBiometria().subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/corehr/biometria`);
      req.flush({ data: [], meta: { total: 0 } });
    });
  });

  describe('listAttendance with all params', () => {
    it('should pass all params', () => {
      service.listAttendance({
        scope: 'ALL', branch_id: 1, area_id: 2, departamento_id: 3,
        date_from: '2024-01-01', date_to: '2024-12-31', state: 'PUNTUAL',
        tenant_user_id: 5, page: 0, size: 20, sort: 'check_in',
      }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/corehr/attendance`);
      expect(req.request.params.get('scope')).toBe('ALL');
      expect(req.request.params.get('branch_id')).toBe('1');
      expect(req.request.params.get('sort')).toBe('check_in');
      req.flush({ data: [], meta: { total: 0 } });
    });
  });

  describe('listNonWorkingDays with no params', () => {
    it('should list without params', () => {
      service.listNonWorkingDays().subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/corehr/non-working-days`);
      req.flush({ data: [], meta: { total: 0 } });
    });
  });

  describe('listTenantContacts with no params', () => {
    it('should list without params', () => {
      service.listTenantContacts().subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/corehr/tenant-contacts`);
      req.flush({ data: [], meta: { total: 0 } });
    });
  });

  describe('listUserDepartments with opts', () => {
    it('should pass opts', () => {
      service.listUserDepartments(5, { activa: true, page: 0, size: 10 }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${apiBase}/corehr/usuarios/5/departamentos`);
      expect(req.request.params.get('activa')).toBe('true');
      expect(req.request.params.get('page')).toBe('0');
      req.flush({ data: [], meta: { total: 0 } });
    });
  });
});
