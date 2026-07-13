import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NotificationsModal } from './notifications-modal';
import { NotificationService } from '../../services/notification.service';
import { ModalService } from '../../services/modal.service';
import { RoleService } from '../../services/role.service';

describe('NotificationsModal', () => {
  let component: NotificationsModal;
  let fixture: ComponentFixture<NotificationsModal>;
  let notificationService: NotificationService;
  let modalService: jasmine.SpyObj<ModalService>;

  beforeEach(async () => {
    modalService = jasmine.createSpyObj('ModalService', ['show', 'hide']);
    await TestBed.configureTestingModule({
      imports: [NotificationsModal],
      providers: [
        { provide: ModalService, useValue: modalService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NotificationsModal);
    component = fixture.componentInstance;
    notificationService = TestBed.inject(NotificationService);
    TestBed.inject(RoleService).role.set('admin-tenant');
    fixture.detectChanges();
  });

  function cmp(): any {
    return component as any;
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('notificacionesFiltradas', () => {
    it('should return all notifications with filtro "todas"', () => {
      cmp().filtroActual = 'todas';
      const all = notificationService.notificaciones();
      expect(cmp().notificacionesFiltradas.length).toBe(all.length);
    });

    it('should return unread notifications with filtro "no-leidas"', () => {
      cmp().filtroActual = 'no-leidas';
      expect(cmp().notificacionesFiltradas.every((n: any) => !n.leida)).toBeTrue();
    });

    it('should return read notifications with filtro "leidas"', () => {
      notificationService.marcarTodasComoLeidas();
      cmp().filtroActual = 'leidas';
      expect(cmp().notificacionesFiltradas.every((n: any) => n.leida)).toBeTrue();
    });
  });

  describe('setFiltro', () => {
    it('should update filtroActual', () => {
      cmp().setFiltro('no-leidas');
      expect(cmp().filtroActual).toBe('no-leidas');
      cmp().setFiltro('leidas');
      expect(cmp().filtroActual).toBe('leidas');
      cmp().setFiltro('todas');
      expect(cmp().filtroActual).toBe('todas');
    });
  });

  describe('irANotificacion', () => {
    it('should mark notification as read, navigate, and hide modal', () => {
      const routerSpy = spyOn(cmp().router, 'navigateByUrl');
      const notif = notificationService.notificaciones()[0];
      cmp().irANotificacion(notif);
      expect(notificationService.notificaciones().find((n: any) => n.id === notif.id)!.leida).toBeTrue();
      expect(routerSpy).toHaveBeenCalledWith(notif.route);
      expect(modalService.hide).toHaveBeenCalledWith('notificationsModal');
    });
  });

  describe('marcarTodasComoLeidas', () => {
    it('should mark all notifications as read', () => {
      cmp().marcarTodasComoLeidas();
      expect(notificationService.noLeidas().length).toBe(0);
    });
  });

  describe('cerrarModal', () => {
    it('should hide the modal', () => {
      cmp().cerrarModal();
      expect(modalService.hide).toHaveBeenCalledWith('notificationsModal');
    });
  });

  describe('tipoColor', () => {
    it('should have colors for all notification types', () => {
      expect(cmp().tipoColor['danger']).toBe('#dc2626');
      expect(cmp().tipoColor['warning']).toBe('#f59e0b');
      expect(cmp().tipoColor['success']).toBe('#10b981');
      expect(cmp().tipoColor['info']).toBe('#3b82f6');
    });
  });
});
