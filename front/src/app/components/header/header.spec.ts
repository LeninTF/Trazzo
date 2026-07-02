import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Header } from './header';
import { RoleService } from '../../services/role.service';
import { NotificationService } from '../../services/notification.service';

describe('Header', () => {
  let component: Header;
  let fixture: ComponentFixture<Header>;
  let roleService: RoleService;
  let notificationService: NotificationService;
  let router: Router;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [provideRouter([]), RoleService, NotificationService],
    }).compileComponents();

    fixture = TestBed.createComponent(Header);
    component = fixture.componentInstance;
    roleService = TestBed.inject(RoleService);
    notificationService = TestBed.inject(NotificationService);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('creates the header component', () => {
    expect(component).toBeTruthy();
  });

  describe('settingsUrl', () => {
    it('should return tenant config url for admin-tenant role', () => {
      roleService.switchRole('admin-tenant');
      fixture.detectChanges();
      expect(component['settingsUrl']()).toBe('/tenant/configuracion-tenant');
    });

    it('should return sass profile url for admin-sass role', () => {
      roleService.switchRole('admin-sass');
      fixture.detectChanges();
      expect(component['settingsUrl']()).toBe('/sass/perfil');
    });

    it('should return usuario profile url for usuario role', () => {
      roleService.switchRole('usuario');
      fixture.detectChanges();
      expect(component['settingsUrl']()).toBe('/usuario/perfil');
    });
  });

  describe('notificaciones', () => {
    it('should return notifications based on role from NotificationService', () => {
      roleService.switchRole('admin-tenant');
      fixture.detectChanges();
      const notifs = notificationService.notificaciones();
      expect(notifs.length).toBe(10);
    });

    it('should have correct tipoColor mapping including info', () => {
      expect(component['tipoColor']['danger']).toBe('#dc2626');
      expect(component['tipoColor']['warning']).toBe('#f59e0b');
      expect(component['tipoColor']['success']).toBe('#10b981');
      expect(component['tipoColor']['info']).toBe('#3b82f6');
    });
  });

  describe('roles', () => {
    it('should contain 3 role options', () => {
      expect(component['roles'].length).toBe(3);
      expect(component['roles'][0].value).toBe('admin-tenant');
      expect(component['roles'][1].value).toBe('admin-sass');
      expect(component['roles'][2].value).toBe('usuario');
    });
  });

  describe('switchRoleAndNavigate', () => {
    it('should switch role and navigate to dashboard', () => {
      const navigateSpy = spyOn(router, 'navigateByUrl');
      const switchSpy = spyOn(roleService, 'switchRole');

      component['switchRoleAndNavigate']('admin-sass');

      expect(switchSpy).toHaveBeenCalledWith('admin-sass');
      expect(navigateSpy).toHaveBeenCalledWith('/sass/tenants');
    });
  });

  describe('onUserChipEnter', () => {
    it('should click the target element', () => {
      const clickSpy = jasmine.createSpy('click');
      const event = { currentTarget: { click: clickSpy } } as unknown as Event;

      component['onUserChipEnter'](event);

      expect(clickSpy).toHaveBeenCalled();
    });
  });
});
