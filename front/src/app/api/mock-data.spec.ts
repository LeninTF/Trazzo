import {
  mockTenantUsers, mockMasterUsers, mockUsuarioProfile, mockAuthResponse,
  mockIncidentTypes, mockIncidencias, mockShifts, mockSchedules,
  mockUserSchedules, mockDevices, mockBiometria, mockAttendance,
  mockNonWorkingDays, mockTenantContacts, mockUserDepartments,
  mockPublicKey, paginate,
} from './mock-data';

describe('mock-data', () => {
  describe('paginate', () => {
    it('should paginate items correctly', () => {
      const items = [1, 2, 3, 4, 5];
      const res = paginate(items, 0, 2);
      expect(res.content).toEqual([1, 2]);
      expect(res.page).toBe(0);
      expect(res.size).toBe(2);
      expect(res.totalElements).toBe(5);
      expect(res.totalPages).toBe(3);
    });

    it('should return empty for out of bounds page', () => {
      const res = paginate([1, 2], 5, 10);
      expect(res.content).toEqual([]);
      expect(res.page).toBe(5);
    });
  });

  describe('mockTenantUsers', () => {
    it('should have 10 users', () => {
      expect(mockTenantUsers.length).toBe(10);
    });

    it('each user should have required fields', () => {
      for (const u of mockTenantUsers) {
        expect(u.id).toBeDefined();
        expect(u.email).toContain('@');
        expect(u.persona).toBeDefined();
        expect(u.persona.name).toBeTruthy();
        expect(u.rol).toBeDefined();
        expect(['ACTIVO', 'LICENCIA', 'INACTIVO']).toContain(u.estado);
      }
    });
  });

  describe('mockMasterUsers', () => {
    it('should have 3 users', () => {
      expect(mockMasterUsers.length).toBe(3);
    });

    it('each master user should have required fields', () => {
      for (const u of mockMasterUsers) {
        expect(u.id).toBeDefined();
        expect(u.email).toContain('@');
        expect(u.persona).toBeDefined();
        expect(u.roles).toBeDefined();
      }
    });
  });

  describe('mockUsuarioProfile', () => {
    it('should have valid structure', () => {
      expect(mockUsuarioProfile.id).toBe(1);
      expect(mockUsuarioProfile.email).toContain('@');
      expect(mockUsuarioProfile.status).toBe('ACTIVO');
      expect(mockUsuarioProfile.rol.length).toBeGreaterThan(0);
    });
  });

  describe('mockAuthResponse', () => {
    it('should have token and usuario', () => {
      expect(mockAuthResponse.accessToken).toBeTruthy();
      expect(mockAuthResponse.tokenType).toBe('Bearer');
      expect(mockAuthResponse.usuario).toEqual(mockUsuarioProfile);
    });
  });

  describe('mockIncidentTypes', () => {
    it('should have 8 incident types', () => {
      expect(mockIncidentTypes.length).toBe(8);
    });

    it('each type should have required fields', () => {
      for (const t of mockIncidentTypes) {
        expect(t.id).toBeDefined();
        expect(t.nombre).toBeTruthy();
      }
    });
  });

  describe('mockIncidencias', () => {
    it('should have 8 incidencias', () => {
      expect(mockIncidencias.length).toBe(8);
    });

    it('each incidencia should have required fields', () => {
      for (const inc of mockIncidencias) {
        expect(inc.id).toBeDefined();
        expect(inc.incidencia_type_id).toBeDefined();
        expect(inc.state).toMatch(/^(PENDIENTE|APROBADO|DENEGADO)$/);
        expect(inc.tipo).toBeDefined();
        expect(inc.tenant_user).toBeDefined();
      }
    });
  });

  describe('mockShifts', () => {
    it('should have at least one shift', () => {
      expect(mockShifts.length).toBeGreaterThan(0);
    });

    it('each shift should have schedules', () => {
      for (const s of mockShifts) {
        expect(s.id).toBeDefined();
        expect(s.name).toBeTruthy();
        expect(s.schedules).toBeDefined();
      }
    });
  });

  describe('mockSchedules', () => {
    it('should have at least one schedule', () => {
      expect(mockSchedules.length).toBeGreaterThan(0);
    });

    it('each schedule should have entry and departure time', () => {
      for (const s of mockSchedules) {
        expect(s.entry_time).toMatch(/^\d{2}:\d{2}/);
        expect(s.departure_time).toMatch(/^\d{2}:\d{2}/);
      }
    });
  });

  describe('mockDevices', () => {
    it('should have at least one device', () => {
      expect(mockDevices.length).toBeGreaterThan(0);
    });

    it('each device should have code and branch_id', () => {
      for (const d of mockDevices) {
        expect(d.id).toBeDefined();
        expect(d.code).toBeTruthy();
        expect(d.branch_id).toBeDefined();
      }
    });
  });

  describe('mockAttendance', () => {
    it('should have at least one attendance record', () => {
      expect(mockAttendance.length).toBeGreaterThan(0);
    });

    it('each record should have attendance_date and state', () => {
      for (const a of mockAttendance) {
        expect(a.attendance_date).toMatch(/^\d{4}-\d{2}-\d{2}/);
        expect(a.state).toMatch(/^(PUNTUAL|TARDANZA|FALTA)$/);
      }
    });
  });

  describe('mockNonWorkingDays', () => {
    it('should have at least one non-working day', () => {
      expect(mockNonWorkingDays.length).toBeGreaterThan(0);
    });

    it('each day should have date and description', () => {
      for (const d of mockNonWorkingDays) {
        expect(d.date).toMatch(/^\d{4}-\d{2}-\d{2}/);
        expect(d.description).toBeTruthy();
      }
    });
  });

  describe('mockPublicKey', () => {
    it('should have key fields', () => {
      expect(mockPublicKey.publicKey).toBeTruthy();
      expect(mockPublicKey.kid).toBeTruthy();
    });
  });
});
