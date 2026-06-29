import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ToastService);
  });

  it('creates the toast service', () => {
    expect(service).toBeTruthy();
  });

  it('should have initial toast as null', () => {
    expect(service.toast()).toBeNull();
  });

  describe('show', () => {
    it('should set toast with message and type', fakeAsync(() => {
      service.show('Hello', 'success');
      expect(service.toast()).toEqual({ message: 'Hello', type: 'success' });
      tick(3000);
    }));

    it('should default to info type when not specified', fakeAsync(() => {
      service.show('Info message');
      expect(service.toast()).toEqual({ message: 'Info message', type: 'info' });
      tick(3000);
    }));

    it('should clear toast after default duration', fakeAsync(() => {
      service.show('Temp');
      expect(service.toast()).not.toBeNull();
      tick(3000);
      expect(service.toast()).toBeNull();
    }));

    it('should use custom duration', fakeAsync(() => {
      service.show('Custom', 'info', 5000);
      tick(4000);
      expect(service.toast()).not.toBeNull();
      tick(1000);
      expect(service.toast()).toBeNull();
    }));

    it('should clear existing timer when showing new toast', fakeAsync(() => {
      service.show('First', 'info', 5000);
      service.show('Second', 'info', 1000);
      tick(2000);
      expect(service.toast()).toBeNull();
    }));
  });

  describe('success', () => {
    it('should show success toast', fakeAsync(() => {
      service.success('Operation completed');
      expect(service.toast()).toEqual({ message: 'Operation completed', type: 'success' });
      tick(3000);
    }));
  });

  describe('error', () => {
    it('should show error toast', fakeAsync(() => {
      service.error('Something went wrong');
      expect(service.toast()).toEqual({ message: 'Something went wrong', type: 'error' });
      tick(3000);
    }));
  });

  describe('info', () => {
    it('should show info toast', fakeAsync(() => {
      service.info('Informational message');
      expect(service.toast()).toEqual({ message: 'Informational message', type: 'info' });
      tick(3000);
    }));
  });
});
