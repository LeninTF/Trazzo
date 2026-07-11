import { TestBed } from '@angular/core/testing';
import { FingerprintStoreService, EnrolledFingerprint } from './fingerprint-store.service';

describe('FingerprintStoreService', () => {
  let service: FingerprintStoreService;

  const mockEnrollment: EnrolledFingerprint = {
    userId: 1,
    userName: 'Juan Pérez',
    userDisplayId: '76543210',
    templateBase64: 'bigTemplateBase64==',
    templateSize: 1500,
    referenceTemplateBase64: 'refTemplateBase64==',
    referenceTemplateSize: 480,
    encryptedTemplate: null,
    enrolledAt: '2025-06-01T10:00:00.000Z',
  };

  const mockEnrollment2: EnrolledFingerprint = {
    userId: 2,
    userName: 'Ana Rojas',
    userDisplayId: '87654321',
    templateBase64: 'bigTemplate2==',
    templateSize: 1400,
    referenceTemplateBase64: 'refTemplate2==',
    referenceTemplateSize: 460,
    encryptedTemplate: null,
    enrolledAt: '2025-06-02T10:00:00.000Z',
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(FingerprintStoreService);
  });

  it('creates the service', () => {
    expect(service).toBeTruthy();
  });

  it('should start with empty list', () => {
    expect(service.getAll()()).toEqual([]);
  });

  describe('save', () => {
    it('should add enrollment', () => {
      service.save(mockEnrollment);
      expect(service.getAll()().length).toBe(1);
      expect(service.getAll()()[0].userName).toBe('Juan Pérez');
    });

    it('should replace existing enrollment for same userId', () => {
      service.save(mockEnrollment);
      const updated = { ...mockEnrollment, templateSize: 1600 };
      service.save(updated);
      expect(service.getAll()().length).toBe(1);
      expect(service.getAll()()[0].templateSize).toBe(1600);
    });

    it('should persist to localStorage', () => {
      service.save(mockEnrollment);
      const raw = localStorage.getItem('trazzo_fingerprints');
      expect(raw).toBeTruthy();
      const parsed = JSON.parse(raw!);
      expect(parsed.length).toBe(1);
      expect(parsed[0].userId).toBe(1);
    });
  });

  describe('getAll', () => {
    it('should return readonly signal', () => {
      service.save(mockEnrollment);
      const all = service.getAll();
      expect(all()).toEqual([mockEnrollment]);
    });
  });

  describe('findByTemplate', () => {
    it('should return no match when list is empty', () => {
      const result = service.findByTemplate(480);
      expect(result.match).toBeNull();
      expect(result.reason).toBe('No hay huellas enroladas');
    });

    it('should return no match when no enrollments have reference template', () => {
      const noRef = { ...mockEnrollment, referenceTemplateSize: 0, referenceTemplateBase64: '' };
      service.save(noRef);
      const result = service.findByTemplate(480);
      expect(result.match).toBeNull();
      expect(result.reason).toBe('Ningún enrolamiento tiene template de referencia. Re-enrole las huellas.');
    });

    it('should find match by size within threshold', () => {
      service.save(mockEnrollment);
      const result = service.findByTemplate(480);
      expect(result.match).not.toBeNull();
      expect(result.match!.userName).toBe('Juan Pérez');
      expect(result.reason).toContain('Coincide por tamaño');
    });

    it('should find closest match when multiple enrollments', () => {
      service.save(mockEnrollment);
      service.save(mockEnrollment2);
      const result = service.findByTemplate(460);
      expect(result.match!.userName).toBe('Ana Rojas');
    });

    it('should not match when difference exceeds threshold', () => {
      service.save(mockEnrollment);
      const result = service.findByTemplate(100);
      expect(result.match).toBeNull();
      expect(result.reason).toContain('Ninguna coincide');
    });
  });

  describe('remove', () => {
    it('should remove enrollment by userId', () => {
      service.save(mockEnrollment);
      service.save(mockEnrollment2);
      service.remove(1);
      expect(service.getAll()().length).toBe(1);
      expect(service.getAll()()[0].userId).toBe(2);
    });

    it('should update localStorage', () => {
      service.save(mockEnrollment);
      service.remove(1);
      const raw = localStorage.getItem('trazzo_fingerprints');
      expect(JSON.parse(raw!).length).toBe(0);
    });
  });

  describe('clear', () => {
    it('should clear all enrollments', () => {
      service.save(mockEnrollment);
      service.save(mockEnrollment2);
      service.clear();
      expect(service.getAll()()).toEqual([]);
    });

    it('should clear localStorage', () => {
      service.save(mockEnrollment);
      service.clear();
      expect(localStorage.getItem('trazzo_fingerprints')).toBe('[]');
    });
  });

  describe('count', () => {
    it('should return number of enrollments', () => {
      expect(service.count).toBe(0);
      service.save(mockEnrollment);
      expect(service.count).toBe(1);
      service.save(mockEnrollment2);
      expect(service.count).toBe(2);
    });
  });

  describe('loadFromStorage backward compatibility', () => {
    it('should add empty reference fields for old enrollments', () => {
      const oldData = [{
        userId: 1,
        userName: 'Old User',
        userDisplayId: '123',
        templateBase64: 'oldTemplate==',
        templateSize: 1500,
        encryptedTemplate: null,
        enrolledAt: '2025-01-01T00:00:00.000Z',
      }];
      localStorage.setItem('trazzo_fingerprints', JSON.stringify(oldData));
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({});
      const newService = TestBed.inject(FingerprintStoreService);
      const items = newService.getAll()();
      expect(items.length).toBe(1);
      expect(items[0].referenceTemplateBase64).toBe('');
      expect(items[0].referenceTemplateSize).toBe(0);
    });

    it('should handle corrupt localStorage gracefully', () => {
      localStorage.setItem('trazzo_fingerprints', 'not valid json');
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({});
      const newService = TestBed.inject(FingerprintStoreService);
      expect(newService.getAll()()).toEqual([]);
    });
  });
});
