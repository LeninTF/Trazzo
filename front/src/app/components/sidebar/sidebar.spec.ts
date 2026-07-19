import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs';
import { Sidebar } from './sidebar';
import { RoleService } from '../../services/role.service';
import { AuthService } from '../../api/services/auth.service';

describe('Sidebar', () => {
  let component: Sidebar;
  let fixture: ComponentFixture<Sidebar>;
  let roleService: RoleService;
  let router: Router;
  let mockAuth: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    localStorage.clear();
    mockAuth = jasmine.createSpyObj('AuthService', ['logout']);

    await TestBed.configureTestingModule({
      imports: [Sidebar],
      providers: [
        provideRouter([]),
        RoleService,
        { provide: AuthService, useValue: mockAuth },
      ],
    }).compileComponents();

    roleService = TestBed.inject(RoleService);
    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(Sidebar);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('creates the sidebar component', () => {
    expect(component).toBeTruthy();
  });

  describe('roleUrlPrefix', () => {
    it('should return "saas" for admin-saas role', () => {
      roleService.switchRole('admin-saas');
      fixture.detectChanges();
      expect(component['roleUrlPrefix']()).toBe('saas');
    });

    it('should return "usuario" for usuario role', () => {
      roleService.switchRole('usuario');
      fixture.detectChanges();
      expect(component['roleUrlPrefix']()).toBe('usuario');
    });

    it('should return "tenant" for admin-tenant role', () => {
      roleService.switchRole('admin-tenant');
      fixture.detectChanges();
      expect(component['roleUrlPrefix']()).toBe('tenant');
    });
  });

  describe('roleLabel', () => {
    it('should return "ADMINISTRADOR TENANT" for admin-tenant', () => {
      roleService.switchRole('admin-tenant');
      fixture.detectChanges();
      expect(component['roleLabel']()).toBe('ADMINISTRADOR TENANT');
    });

    it('should return "ADMINISTRADOR SAAS" for admin-saas', () => {
      roleService.switchRole('admin-saas');
      fixture.detectChanges();
      expect(component['roleLabel']()).toBe('ADMINISTRADOR SAAS');
    });

    it('should return "USUARIO" for usuario', () => {
      roleService.switchRole('usuario');
      fixture.detectChanges();
      expect(component['roleLabel']()).toBe('USUARIO');
    });
  });

  describe('NavigationEnd subscription', () => {
    it('should set up subscription on construction', () => {
      expect(component['sub']).toBeDefined();
      expect(component['sub'].closed).toBeFalse();
    });
  });

  describe('ngOnDestroy', () => {
    it('should unsubscribe on destroy', () => {
      const subSpy = spyOn(component['sub'], 'unsubscribe');
      component.ngOnDestroy();
      expect(subSpy).toHaveBeenCalled();
    });
  });

  describe('cerrarSesion', () => {
    it('should call logout, reset role state, and navigate to /login', () => {
      roleService.setUserInfo('Admin', 'admin@trazzo.pe');
      roleService.switchRole('admin-saas');
      const navigateSpy = spyOn(router, 'navigateByUrl');

      component['cerrarSesion']();

      expect(mockAuth.logout).toHaveBeenCalled();
      expect(roleService.userName()).toBe('');
      expect(roleService.role()).toBe('admin-tenant');
      expect(navigateSpy).toHaveBeenCalledWith('/login');
    });
  });
});
