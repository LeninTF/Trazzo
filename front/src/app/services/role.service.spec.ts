import { TestBed } from '@angular/core/testing';
import { RoleService } from './role.service';

describe('RoleService', () => {
  let service: RoleService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(RoleService);
  });

  it('creates the role service', () => {
    expect(service).toBeTruthy();
  });

  describe('initial values', () => {
    it('should have default role as admin-tenant', () => {
      expect(service.role()).toBe('admin-tenant');
    });

    it('should have empty userName', () => {
      expect(service.userName()).toBe('');
    });

    it('should have empty userEmail', () => {
      expect(service.userEmail()).toBe('');
    });

    it('should have sidebarOpen as false', () => {
      expect(service.sidebarOpen()).toBeFalse();
    });
  });

  describe('setUserInfo', () => {
    it('should set userName and userEmail', () => {
      service.setUserInfo('John Doe', 'john@test.com');
      expect(service.userName()).toBe('John Doe');
      expect(service.userEmail()).toBe('john@test.com');
    });

    it('should persist userName to localStorage', () => {
      service.setUserInfo('John Doe', 'john@test.com');
      expect(localStorage.getItem('trazzo_user_name')).toBe('John Doe');
    });
  });

  describe('switchRole', () => {
    it('should update role signal', () => {
      service.switchRole('admin-saas');
      expect(service.role()).toBe('admin-saas');
    });

    it('should persist role to localStorage', () => {
      service.switchRole('admin-saas');
      expect(localStorage.getItem('trazzo_role')).toBe('admin-saas');
    });

    it('should allow switching to admin-tenant', () => {
      service.switchRole('admin-tenant');
      expect(service.role()).toBe('admin-tenant');
    });

    it('should allow switching to usuario', () => {
      service.switchRole('usuario');
      expect(service.role()).toBe('usuario');
    });
  });

  describe('loadRole from localStorage', () => {
    it('should load saved role from localStorage on creation', () => {
      localStorage.clear();
      localStorage.setItem('trazzo_role', 'admin-saas');
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({});
      const newService = TestBed.inject(RoleService);
      expect(newService.role()).toBe('admin-saas');
    });

    it('should default to admin-tenant for invalid stored role', () => {
      localStorage.clear();
      localStorage.setItem('trazzo_role', 'invalid-role');
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({});
      const newService = TestBed.inject(RoleService);
      expect(newService.role()).toBe('admin-tenant');
    });
  });

  describe('toggleSidebar', () => {
    it('should toggle sidebarOpen', () => {
      service.toggleSidebar();
      expect(service.sidebarOpen()).toBeTrue();
      service.toggleSidebar();
      expect(service.sidebarOpen()).toBeFalse();
    });
  });

  describe('closeSidebar', () => {
    it('should set sidebarOpen to false', () => {
      service.sidebarOpen.set(true);
      service.closeSidebar();
      expect(service.sidebarOpen()).toBeFalse();
    });
  });

  describe('roleLabel', () => {
    it('should return correct labels for each role', () => {
      expect(service.roleLabel['admin-tenant']).toBe('Administrador Tenant');
      expect(service.roleLabel['admin-saas']).toBe('Administrador SaaS');
      expect(service.roleLabel['usuario']).toBe('Usuario');
    });
  });

  describe('clearSession', () => {
    it('should reset userName, userEmail and role to their defaults', () => {
      service.setUserInfo('John Doe', 'john@test.com');
      service.switchRole('admin-saas');

      service.clearSession();

      expect(service.userName()).toBe('');
      expect(service.userEmail()).toBe('');
      expect(service.role()).toBe('admin-tenant');
    });

    it('should remove userName and role from localStorage', () => {
      service.setUserInfo('John Doe', 'john@test.com');
      service.switchRole('admin-saas');

      service.clearSession();

      expect(localStorage.getItem('trazzo_user_name')).toBeNull();
      expect(localStorage.getItem('trazzo_role')).toBeNull();
    });

    it('should not resurrect the old role on next app load after clearing', () => {
      service.switchRole('admin-saas');
      service.clearSession();

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({});
      const freshService = TestBed.inject(RoleService);

      expect(freshService.role()).toBe('admin-tenant');
    });
  });
});
