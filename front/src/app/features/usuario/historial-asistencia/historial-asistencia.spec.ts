import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { HistorialAsistencia } from './historial-asistencia';
import { CorehrService } from '../../../api/services/corehr.service';
import type { AttendanceListResponse, AttendanceProfile } from '../../../api/types';

function makeAtt(id: string, day: number, state: AttendanceProfile['state'], minutesLate = 0): AttendanceProfile {
  const date = `2026-06-${String(day).padStart(2, '0')}`;
  const isLate = state === 'TARDANZA';
  return { id, tenant_user_id: 1, tenant_user: { id: 1, nombre: 'Test', apellido_paterno: 'User' }, schedule_id: 1, schedule: { id: 1, name: 'Mañana', entry_time: '07:00:00', departure_time: '13:00:00' }, device_id: 1, device_code: 'D1', check_in: isLate ? `${date}T07:25:00Z` : `${date}T06:55:00Z`, check_out: `${date}T13:00:00Z`, attendance_date: date, minutes_late: minutesLate, state, created_at: `${date}T06:30:00Z`, updated_at: `${date}T13:00:00Z` };
}

const mockAttendance: AttendanceProfile[] = [
  makeAtt('att-1', 1, 'PUNTUAL'), makeAtt('att-2', 2, 'PUNTUAL'),
  makeAtt('att-3', 3, 'PUNTUAL'), makeAtt('att-4', 4, 'PUNTUAL'),
  makeAtt('att-5', 5, 'PUNTUAL'), makeAtt('att-6', 6, 'PUNTUAL'),
  makeAtt('att-7', 7, 'PUNTUAL'),
  makeAtt('att-8', 8, 'TARDANZA', 15), makeAtt('att-9', 9, 'TARDANZA', 15),
  makeAtt('att-10', 10, 'TARDANZA', 15),
];

const mockResponse: AttendanceListResponse = {
  content: mockAttendance,
  page: 0,
  size: 50,
  totalElements: 10,
  totalPages: 1,
};

describe('HistorialAsistencia', () => {
  let component: HistorialAsistencia;
  let fixture: ComponentFixture<HistorialAsistencia>;
  let corehrServiceSpy: jasmine.SpyObj<CorehrService>;

  beforeEach(async () => {
    corehrServiceSpy = jasmine.createSpyObj<CorehrService>('CorehrService', ['listAttendance']);
    corehrServiceSpy.listAttendance.and.returnValue(of(mockResponse));

    await TestBed.configureTestingModule({
      imports: [HistorialAsistencia],
      providers: [
        { provide: CorehrService, useValue: corehrServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HistorialAsistencia);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('creates the historial-asistencia component', () => {
    expect(component).toBeTruthy();
  });

  it('has default historial signal state', () => {
    expect(component.loading()).toBeFalse();
    expect(component.error()).toBe('');
  });

  it('should have default mesActual derived from attendance data', () => {
    expect(component.mesActual).toBe('Mayo 2026');
  });

  it('should have 10 registros', () => {
    expect(component.registros().length).toBe(10);
  });

  describe('cambiarMes', () => {
    it('should advance to next month', () => {
      component.cambiarMes(1);
      expect(component.mesActual).toBe('Junio 2026');
    });

    it('should go to previous month', () => {
      component.cambiarMes(-1);
      expect(component.mesActual).toBe('Abril 2026');
    });

    it('should wrap to next year when advancing past December', () => {
      component.mesActual = 'Diciembre 2026';
      component.cambiarMes(1);
      expect(component.mesActual).toBe('Enero 2027');
    });

    it('should wrap to previous year when going before January', () => {
      component.mesActual = 'Enero 2026';
      component.cambiarMes(-1);
      expect(component.mesActual).toBe('Diciembre 2025');
    });
  });

  describe('getters', () => {
    it('should count completos', () => {
      expect(component.completos).toBe(7);
    });

    it('should count tardanzas', () => {
      expect(component.tardanzas).toBe(3);
    });

    it('should count faltas', () => {
      expect(component.faltas).toBe(0);
    });

    it('should count justificados', () => {
      expect(component.justificados).toBe(0);
    });

    it('should return resumen with 4 entries', () => {
      expect(component.resumen.length).toBe(4);
      expect(component.resumen[0].label).toBe('A tiempo');
      expect(component.resumen[0].valor).toBe(7);
    });

    it('should calculate eficiencia', () => {
      expect(component.eficiencia).toBe(70);
    });
  });

  describe('exportarCSV', () => {
    it('should create and click a download link', () => {
      const link = document.createElement('a');
      const clickSpy = spyOn(link, 'click');
      spyOn(document, 'createElement').and.returnValue(link);
      spyOn(URL, 'createObjectURL').and.returnValue('blob:csv');
      spyOn(URL, 'revokeObjectURL');

      component.exportarCSV();

      expect(clickSpy).toHaveBeenCalled();
      expect(link.download).toContain('asistencia_');
      expect(link.download).toMatch(/asistencia_\d{8}\.csv/);
    });
  });

  describe('cargarHistorial error', () => {
    it('should set error message when listAttendance fails', () => {
      corehrServiceSpy.listAttendance.and.returnValue(throwError(() => new Error('fail')));
      component['cargarHistorial']();
      expect(component.error()).toBe('No fue posible cargar el historial de asistencia.');
      expect(component.loading()).toBeFalse();
    });
  });

  describe('mapEstado with unknown state', () => {
    it('should return Falta for an unrecognized state', () => {
      const att = makeAtt('att-x', 15, 'PUNTUAL');
      att.state = 'JUSTIFICADO' as any;
      corehrServiceSpy.listAttendance.and.returnValue(of({
        content: [att], page: 0, size: 50, totalElements: 1, totalPages: 1,
      }));
      component['cargarHistorial']();
      expect(component.registros()[0].estado).toBe('Falta');
    });
  });

  describe('toRegistro with null fields', () => {
    it('should display dashes when check_in and check_out are null', () => {
      const att = makeAtt('att-null', 20, 'PUNTUAL');
      att.check_in = null as any;
      att.check_out = null as any;
      corehrServiceSpy.listAttendance.and.returnValue(of({
        content: [att], page: 0, size: 50, totalElements: 1, totalPages: 1,
      }));
      component['cargarHistorial']();
      expect(component.registros()[0].ingreso).toBe('—');
      expect(component.registros()[0].salida).toBe('—');
    });

    it('should display dash when schedule is null', () => {
      const att = makeAtt('att-nosched', 21, 'PUNTUAL');
      att.schedule = null as any;
      corehrServiceSpy.listAttendance.and.returnValue(of({
        content: [att], page: 0, size: 50, totalElements: 1, totalPages: 1,
      }));
      component['cargarHistorial']();
      expect(component.registros()[0].turno).toBe('—');
    });
  });

  describe('eficiencia with empty registros', () => {
    it('should return 0 when there are no registros', () => {
      corehrServiceSpy.listAttendance.and.returnValue(of({
        content: [], page: 0, size: 50, totalElements: 0, totalPages: 0,
      }));
      component['cargarHistorial']();
      expect(component.eficiencia).toBe(0);
    });
  });

  describe('formatearMes with empty content', () => {
    it('should use current date when response content is empty', () => {
      corehrServiceSpy.listAttendance.and.returnValue(of({
        content: [], page: 0, size: 50, totalElements: 0, totalPages: 0,
      }));
      component['cargarHistorial']();
      const now = new Date();
      const meses = ['Enero','Febrero','Marzo','Abril','Mayo','Junio','Julio','Agosto','Setiembre','Octubre','Noviembre','Diciembre'];
      expect(component.mesActual).toBe(`${meses[now.getMonth()]} ${now.getFullYear()}`);
    });
  });
});
