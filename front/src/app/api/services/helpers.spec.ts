import { HttpParams } from '@angular/common/http';
import { params, tenantUserToPersonal, API } from './helpers';
import type { TenantUserProfile } from '../types';

describe('helpers', () => {
  describe('API constant', () => {
    it('should have correct base URL', () => {
      expect(API).toBe('https://api.trazzo.pe/api/v1');
    });
  });

  describe('params()', () => {
    it('should return empty HttpParams for undefined input', () => {
      const p = params(undefined);
      expect(p.toString()).toBe('');
    });

    it('should return empty HttpParams for empty object', () => {
      const p = params({});
      expect(p.toString()).toBe('');
    });

    it('should include valid string values', () => {
      const p = params({ search: 'jose', scope: 'all' });
      expect(p.get('search')).toBe('jose');
      expect(p.get('scope')).toBe('all');
    });

    it('should include numeric values', () => {
      const p = params({ page: 1, size: 20 });
      expect(p.get('page')).toBe('1');
      expect(p.get('size')).toBe('20');
    });

    it('should include boolean values', () => {
      const p = params({ activo: true });
      expect(p.get('activo')).toBe('true');
    });

    it('should exclude undefined values', () => {
      const p = params({ search: undefined });
      expect(p.toString()).toBe('');
    });

    it('should exclude null values', () => {
      const p = params({ search: null });
      expect(p.toString()).toBe('');
    });

    it('should exclude empty string values', () => {
      const p = params({ search: '' });
      expect(p.toString()).toBe('');
    });

    it('should mix included and excluded values', () => {
      const p = params({ page: 2, search: '', status: null, scope: undefined, sort: 'name' });
      expect(p.get('page')).toBe('2');
      expect(p.get('sort')).toBe('name');
      expect(p.keys().length).toBe(2);
    });
  });

  describe('tenantUserToPersonal()', () => {
    const mockUser: TenantUserProfile = {
      id: 42,
      email: 'jose@test.com',
      phone: '999888777',
      estado: 'ACTIVO',
      must_change_password: false,
      created_at: '2024-06-15T10:30:00Z',
      updated_at: '2024-06-15T10:30:00Z',
      persona: {
        id: 1,
        name: 'Jose',
        father_surname: 'Alata',
        mother_surname: 'Perez',
        document_type: 'DNI',
        document_value: '12345678',
        birth_date: null,
        img_url: 'https://example.com/foto.jpg',
      },
      MetodoRecuperacion: [],
      rol: { id: 1, name: 'Docente', descripcion: null, permissions: [] },
      sedes: [{ id: 1, nombre: 'Central' }],
      areas: [{ id: 1, nombre: 'Academica' }],
      departamentos: [{ id: 1, nombre: 'Academic' }],
    };

    it('should transform full user data', () => {
      const item = tenantUserToPersonal(mockUser);
      expect(item.id).toBe(42);
      expect(item.nombre).toBe('Jose Alata Perez');
      expect(item.idPersonal).toBe('#TU-0042');
      expect(item.sede).toBe('Central');
      expect(item.area).toBe('Academica');
      expect(item.departamento).toBe('Academic');
      expect(item.estado).toBe('ACTIVO');
      expect(item.email).toBe('jose@test.com');
      expect(item.telefono).toBe('999888777');
      expect(item.fechaIngreso).toBe('2024-06-15');
      expect(item.imagenUrl).toBe('https://example.com/foto.jpg');
    });

    it('should handle missing optional fields', () => {
      const minimal: TenantUserProfile = {
        id: 1, email: null, phone: null,
        estado: 'INACTIVO', must_change_password: false,
        created_at: '2024-01-01T00:00:00Z', updated_at: '2024-01-01T00:00:00Z',
        persona: { id: 1, name: 'Ana', father_surname: '', mother_surname: '', document_type: 'DNI', document_value: '', birth_date: null, img_url: null },
        MetodoRecuperacion: [],
        rol: null as any,
        sedes: [], areas: [], departamentos: [],
      };
      const item = tenantUserToPersonal(minimal);
      expect(item.nombre).toBe('Ana');
      expect(item.sede).toBe('');
      expect.item.area;
      expect(item.email).toBeNull();
      expect(item.telefono).toBeNull();
      expect(item.imagenUrl).toBeNull();
    });

    it('should handle missing mother surname', () => {
      const user = { ...mockUser };
      user.persona = { ...mockUser.persona, mother_surname: '' };
      const item = tenantUserToPersonal(user);
      expect(item.nombre).toBe('Jose Alata');
    });
  });
});
