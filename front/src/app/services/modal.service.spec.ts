import { TestBed } from '@angular/core/testing';
import { ModalService } from './modal.service';

describe('ModalService', () => {
  let service: ModalService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ModalService);
  });

  afterEach(() => {
    document.querySelectorAll('.modal-backdrop').forEach(b => b.remove());
    document.body.classList.remove('modal-open');
  });

  it('creates the service', () => {
    expect(service).toBeTruthy();
  });

  describe('show', () => {
    it('should show the modal and add backdrop', () => {
      const el = document.createElement('div');
      el.id = 'testModal';
      el.classList.add('modal', 'fade');
      document.body.appendChild(el);

      service.show('testModal');

      expect(el.style.display).toBe('block');
      expect(el.classList.contains('show')).toBeTrue();
      expect(document.body.classList.contains('modal-open')).toBeTrue();
      expect(document.querySelector('.modal-backdrop')).toBeTruthy();
      document.body.removeChild(el);
    });

    it('should do nothing if element not found', () => {
      expect(() => service.show('nonexistent')).not.toThrow();
    });
  });

  describe('hide', () => {
    it('should hide the modal and remove backdrop', (done) => {
      const el = document.createElement('div');
      el.id = 'testModal';
      el.classList.add('modal', 'fade');
      document.body.appendChild(el);
      service.show('testModal');

      service.hide('testModal');

      expect(el.classList.contains('show')).toBeFalse();

      setTimeout(() => {
        expect(el.style.display).toBe('');
        expect(document.body.classList.contains('modal-open')).toBeFalse();
        expect(document.querySelector('.modal-backdrop')).toBeFalsy();
        document.body.removeChild(el);
        done();
      }, 350);
    });

    it('should do nothing if element not found', () => {
      expect(() => service.hide('nonexistent')).not.toThrow();
    });
  });
});
