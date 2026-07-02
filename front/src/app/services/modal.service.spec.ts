import { TestBed } from '@angular/core/testing';
import { ModalService } from './modal.service';

describe('ModalService', () => {
  let service: ModalService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ModalService);
  });

  it('creates the service', () => {
    expect(service).toBeTruthy();
  });

  describe('show', () => {
    it('should create bootstrap modal and show it', () => {
      const el = document.createElement('div');
      el.id = 'testModal';
      document.body.appendChild(el);
      const showSpy = jasmine.createSpy('show');
      (window as any).bootstrap = {
        Modal: function () {
          return { show: showSpy, hide: () => {} };
        },
      };

      service.show('testModal');
      expect(showSpy).toHaveBeenCalled();
      document.body.removeChild(el);
    });

    it('should do nothing if element not found', () => {
      expect(() => service.show('nonexistent')).not.toThrow();
    });
  });

  describe('hide', () => {
    it('should get bootstrap modal instance and hide it', () => {
      const el = document.createElement('div');
      el.id = 'testModal';
      document.body.appendChild(el);
      const hideSpy = jasmine.createSpy('hide');
      (window as any).bootstrap = {
        Modal: Object.assign(
          function () { return { show: () => {}, hide: () => {} }; },
          { getInstance: () => ({ hide: hideSpy }) }
        ),
      };

      service.hide('testModal');
      expect(hideSpy).toHaveBeenCalled();
      document.body.removeChild(el);
    });

    it('should do nothing if element not found', () => {
      expect(() => service.hide('nonexistent')).not.toThrow();
    });
  });
});
