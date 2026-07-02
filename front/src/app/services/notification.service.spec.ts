import { TestBed } from '@angular/core/testing';
import { NotificationService } from './notification.service';
import { RoleService } from './role.service';

describe('NotificationService', () => {
  let service: NotificationService;
  let roleService: RoleService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NotificationService);
    roleService = TestBed.inject(RoleService);
  });

  it('creates the service', () => {
    expect(service).toBeTruthy();
  });

  it('should have notifications for admin-tenant role', () => {
    roleService.role.set('admin-tenant');
    expect(service.notificaciones().length).toBe(10);
  });

  it('should have notifications for usuario role', () => {
    roleService.role.set('usuario');
    expect(service.notificaciones().length).toBe(5);
  });

  it('should have notifications for admin-sass role', () => {
    roleService.role.set('admin-sass');
    expect(service.notificaciones().length).toBe(7);
  });

  it('should have all notifications unread by default', () => {
    roleService.role.set('admin-tenant');
    expect(service.noLeidas().length).toBe(10);
    expect(service.notificaciones().every(n => !n.leida)).toBeTrue();
  });

  describe('notificacionesRecientes', () => {
    it('should return first 5 notifications', () => {
      roleService.role.set('admin-tenant');
      expect(service.notificacionesRecientes().length).toBe(5);
    });

    it('should return all notifications if less than 5', () => {
      roleService.role.set('usuario');
      expect(service.notificacionesRecientes().length).toBe(5);
    });
  });

  describe('marcarComoLeida', () => {
    it('should mark a notification as read', () => {
      roleService.role.set('admin-tenant');
      service.marcarComoLeida('at-1');
      const notif = service.notificaciones().find(n => n.id === 'at-1');
      expect(notif!.leida).toBeTrue();
    });

    it('should update noLeidas count', () => {
      roleService.role.set('admin-tenant');
      service.marcarComoLeida('at-1');
      expect(service.noLeidas().length).toBe(9);
    });

    it('should track read state per role', () => {
      roleService.role.set('admin-tenant');
      service.marcarComoLeida('at-1');
      roleService.role.set('usuario');
      expect(service.notificaciones().every(n => !n.leida)).toBeTrue();
    });
  });

  describe('marcarTodasComoLeidas', () => {
    it('should mark all notifications as read', () => {
      roleService.role.set('admin-tenant');
      service.marcarTodasComoLeidas();
      expect(service.noLeidas().length).toBe(0);
      expect(service.notificaciones().every(n => n.leida)).toBeTrue();
    });

    it('should only mark current role notifications', () => {
      roleService.role.set('admin-tenant');
      service.marcarTodasComoLeidas();
      roleService.role.set('usuario');
      expect(service.notificaciones().every(n => !n.leida)).toBeTrue();
    });
  });

  describe('notificaciones', () => {
    it('should recompute when role changes', () => {
      roleService.role.set('admin-tenant');
      expect(service.notificaciones().length).toBe(10);
      roleService.role.set('usuario');
      expect(service.notificaciones().length).toBe(5);
    });

    it('should have correct icon and tipo fields', () => {
      roleService.role.set('admin-tenant');
      const notif = service.notificaciones()[0];
      expect(notif.icono).toBeTruthy();
      expect(notif.titulo).toBeTruthy();
      expect(notif.descripcion).toBeTruthy();
      expect(['danger', 'warning', 'success', 'info']).toContain(notif.tipo);
      expect(notif.route).toContain('/');
    });
  });
});
