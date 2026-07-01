import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Section } from './section';

describe('Section', () => {
  let component: Section;
  let fixture: ComponentFixture<Section>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj<Router>('Router', [], {
      url: '/tenant/incidencias',
    });

    await TestBed.configureTestingModule({
      imports: [Section],
      providers: [
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Section);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('section getter', () => {
    it('should return incidencias metadata for /tenant/incidencias', () => {
      const section = (component as any).section;
      expect(section.title).toBe('Incidencias');
      expect(section.subtitle).toContain('Centraliza alertas');
      expect(section.icon).toBe('bi-exclamation-triangle');
    });

    it('should return reglas-asistencia metadata for /tenant/reglas-asistencia', () => {
      Object.defineProperty(routerSpy, 'url', { get: () => '/tenant/reglas-asistencia' });
      const section = (component as any).section;
      expect(section.title).toBe('Reglas de asistencia');
      expect(section.icon).toBe('bi-sliders');
    });

    it('should return sedes metadata for /tenant/sedes', () => {
      Object.defineProperty(routerSpy, 'url', { get: () => '/tenant/sedes' });
      const section = (component as any).section;
      expect(section.title).toBe('Sedes');
    });

    it('should return gestion-roles metadata for /tenant/gestion-roles', () => {
      Object.defineProperty(routerSpy, 'url', { get: () => '/tenant/gestion-roles' });
      const section = (component as any).section;
      expect(section.title).toBe('Gestión de roles');
    });

    it('should return configuracion-tenant metadata for /tenant/configuracion-tenant', () => {
      Object.defineProperty(routerSpy, 'url', { get: () => '/tenant/configuracion-tenant' });
      const section = (component as any).section;
      expect(section.title).toBe('Configuración tenant');
    });

    it('should return planes metadata for /tenant/planes', () => {
      Object.defineProperty(routerSpy, 'url', { get: () => '/tenant/planes' });
      const section = (component as any).section;
      expect(section.title).toBe('Planes');
    });

    it('should return directorio-personal metadata for /tenant/directorio-personal', () => {
      Object.defineProperty(routerSpy, 'url', { get: () => '/tenant/directorio-personal' });
      const section = (component as any).section;
      expect(section.title).toBe('Directorio del personal');
    });

    it('should return gestion-horarios metadata for /tenant/gestion-horarios', () => {
      Object.defineProperty(routerSpy, 'url', { get: () => '/tenant/gestion-horarios' });
      const section = (component as any).section;
      expect(section.title).toBe('Gestión de horarios');
    });

    it('should return calendario metadata for /usuario/calendario', () => {
      Object.defineProperty(routerSpy, 'url', { get: () => '/usuario/calendario' });
      const section = (component as any).section;
      expect(section.title).toBe('Calendario');
    });

    it('should return historial-asistencia metadata for /usuario/historial-asistencia', () => {
      Object.defineProperty(routerSpy, 'url', { get: () => '/usuario/historial-asistencia' });
      const section = (component as any).section;
      expect(section.title).toBe('Historial de asistencia');
    });

    it('should return usuario incidencias metadata for /usuario/incidencias', () => {
      Object.defineProperty(routerSpy, 'url', { get: () => '/usuario/incidencias' });
      const section = (component as any).section;
      expect(section.title).toBe('Incidencias');
    });

    it('should strip query params when matching section', () => {
      Object.defineProperty(routerSpy, 'url', { get: () => '/tenant/incidencias?page=2' });
      const section = (component as any).section;
      expect(section.title).toBe('Incidencias');
    });

    it('should return default metadata for unknown path', () => {
      Object.defineProperty(routerSpy, 'url', { get: () => '/unknown/path' });
      const section = (component as any).section;
      expect(section.title).toBe('Panel');
      expect(section.subtitle).toContain('Selecciona una opción');
      expect(section.icon).toBe('bi-grid-3x3-gap');
    });
  });
});
