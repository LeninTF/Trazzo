import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { HorariosService } from './horarios.service';
import { API } from './helpers';

describe('HorariosService', () => {
  let service: HorariosService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [HorariosService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(HorariosService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('shifts', () => {
    it('should list shifts', () => {
      service.listShifts({ search: 'mañana' }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${API}/corehr/shifts`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('search')).toBe('mañana');
      req.flush({ data: [], meta: { total: 0 } });
    });

    it('should create shift', () => {
      const body = { nombre: 'Turno Mañana', hora_inicio: '08:00', hora_fin: '17:00' } as any;
      service.createShift(body).subscribe();
      const req = httpMock.expectOne(`${API}/corehr/shifts`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 1, nombre: 'Turno Mañana' });
    });

    it('should get shift', () => {
      service.getShift(1).subscribe();
      const req = httpMock.expectOne(`${API}/corehr/shifts/1`);
      expect(req.request.method).toBe('GET');
      req.flush({ id: 1 });
    });

    it('should patch shift', () => {
      const body = { hora_inicio: '09:00' } as any;
      service.patchShift(1, body).subscribe();
      const req = httpMock.expectOne(`${API}/corehr/shifts/1`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ id: 1 });
    });

    it('should delete shift', () => {
      service.deleteShift(1).subscribe();
      const req = httpMock.expectOne(`${API}/corehr/shifts/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('schedules', () => {
    it('should list schedules', () => {
      service.listSchedules({ shift_id: 1 }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${API}/corehr/schedules`);
      expect(req.request.params.get('shift_id')).toBe('1');
      req.flush({ data: [], meta: { total: 0 } });
    });

    it('should create schedule', () => {
      const body = { nombre: 'Horario A', dias: [1, 2, 3, 4, 5] } as any;
      service.createSchedule(body).subscribe();
      const req = httpMock.expectOne(`${API}/corehr/schedules`);
      expect(req.request.method).toBe('POST');
      req.flush({ id: 1 });
    });

    it('should get schedule', () => {
      service.getSchedule(1).subscribe();
      const req = httpMock.expectOne(`${API}/corehr/schedules/1`);
      req.flush({ id: 1 });
    });

    it('should patch schedule', () => {
      service.patchSchedule(1, { dias: [1, 3, 5] } as any).subscribe();
      const req = httpMock.expectOne(`${API}/corehr/schedules/1`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ id: 1 });
    });

    it('should delete schedule', () => {
      service.deleteSchedule(1).subscribe();
      const req = httpMock.expectOne(`${API}/corehr/schedules/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('tolerancias', () => {
    it('should list tolerancias', () => {
      service.listTolerancias(1).subscribe();
      const req = httpMock.expectOne(`${API}/corehr/schedules/1/tolerancias`);
      expect(req.request.method).toBe('GET');
      req.flush({ data: [], meta: { total: 0 } });
    });

    it('should create tolerancia', () => {
      const body = { minutos: 15, tipo: 'llegada_tarde' } as any;
      service.createTolerancia(1, body).subscribe();
      const req = httpMock.expectOne(`${API}/corehr/schedules/1/tolerancias`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 1 });
    });

    it('should patch tolerancia', () => {
      service.patchTolerancia(1, 2, { minutos: 10 } as any).subscribe();
      const req = httpMock.expectOne(`${API}/corehr/schedules/1/tolerancias/2`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ id: 2 });
    });

    it('should delete tolerancia', () => {
      service.deleteTolerancia(1, 2).subscribe();
      const req = httpMock.expectOne(`${API}/corehr/schedules/1/tolerancias/2`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('user schedules', () => {
    it('should list user schedules', () => {
      service.listUserSchedules({ tenant_user_id: 5 }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${API}/corehr/user-schedules`);
      expect(req.request.params.get('tenant_user_id')).toBe('5');
      req.flush({ data: [], meta: { total: 0 } });
    });

    it('should create user schedule', () => {
      const body = { tenant_user_id: 5, schedule_id: 1 } as any;
      service.createUserSchedule(body).subscribe();
      const req = httpMock.expectOne(`${API}/corehr/user-schedules`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 1 });
    });

    it('should delete user schedule', () => {
      service.deleteUserSchedule(1).subscribe();
      const req = httpMock.expectOne(`${API}/corehr/user-schedules/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
