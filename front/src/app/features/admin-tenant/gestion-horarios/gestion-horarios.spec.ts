import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { GestionHorarios } from './gestion-horarios';
import { ApiService } from '../../../api/services/api.service';

function createMockApiService(): ApiService {
  return {
    horarios: {
      listShifts: jasmine.createSpy('listShifts').and.returnValue(of({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 })),
      listSchedules: jasmine.createSpy('listSchedules'),
      getSchedule: jasmine.createSpy('getSchedule'),
      listUserSchedules: jasmine.createSpy('listUserSchedules').and.returnValue(of({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 })),
      createUserSchedule: jasmine.createSpy('createUserSchedule'),
      deleteUserSchedule: jasmine.createSpy('deleteUserSchedule'),
      createShift: jasmine.createSpy('createShift'),
      patchShift: jasmine.createSpy('patchShift'),
      deleteShift: jasmine.createSpy('deleteShift'),
      getShift: jasmine.createSpy('getShift'),
      createSchedule: jasmine.createSpy('createSchedule'),
      patchSchedule: jasmine.createSpy('patchSchedule'),
      deleteSchedule: jasmine.createSpy('deleteSchedule'),
      listTolerancias: jasmine.createSpy('listTolerancias'),
      createTolerancia: jasmine.createSpy('createTolerancia'),
      patchTolerancia: jasmine.createSpy('patchTolerancia'),
      deleteTolerancia: jasmine.createSpy('deleteTolerancia'),
    },
    corehr: {
      listNonWorkingDays: jasmine.createSpy('listNonWorkingDays').and.returnValue(of({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 })),
      createNonWorkingDay: jasmine.createSpy('createNonWorkingDay'),
      patchNonWorkingDay: jasmine.createSpy('patchNonWorkingDay'),
      deleteNonWorkingDay: jasmine.createSpy('deleteNonWorkingDay'),
    },
    users: {},
    incidents: {},
    auth: {},
  } as unknown as ApiService;
}

describe('GestionHorarios', () => {
  let component: GestionHorarios;
  let fixture: ComponentFixture<GestionHorarios>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GestionHorarios],
      providers: [
        { provide: ApiService, useValue: createMockApiService() },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GestionHorarios);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have default active section turnos', () => {
    expect(component.activeSection).toBe('turnos');
  });

  it('should have section meta for all sections', () => {
    expect(component.sectionMeta['turnos']).toBeDefined();
    expect(component.sectionMeta['asignacion']).toBeDefined();
    expect(component.sectionMeta['feriados']).toBeDefined();
  });

  it('should get currentSection meta', () => {
    expect(component.currentSection.title).toBe('Turnos');
    component.activeSection = 'asignacion';
    expect(component.currentSection.title).toBe('Asignación');
  });

  it('should change section', () => {
    component.activeSection = 'feriados';
    expect(component.activeSection).toBe('feriados');
  });

  it('should call nuevoTurno', () => {
    component.nuevoTurno();
    expect(component.activeSection).toBe('turnos');
  });

});
