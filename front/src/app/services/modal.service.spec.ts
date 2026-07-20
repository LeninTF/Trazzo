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

    it('should do nothing on hide if element does not have show class', () => {
      const el = document.createElement('div');
      el.id = 'testModal';
      document.body.appendChild(el);

      service.hide('testModal');

      expect(el.style.display).toBe('');
      document.body.removeChild(el);
    });
  });

  describe('show', () => {
    it('should auto-hide previous modal when showing a second one', (done) => {
      const el1 = document.createElement('div');
      el1.id = 'modal1';
      el1.classList.add('modal', 'fade');
      document.body.appendChild(el1);

      const el2 = document.createElement('div');
      el2.id = 'modal2';
      el2.classList.add('modal', 'fade');
      document.body.appendChild(el2);

      service.show('modal1');
      expect(el1.classList.contains('show')).toBeTrue();

      service.show('modal2');
      expect(el2.classList.contains('show')).toBeTrue();

      setTimeout(() => {
        expect(el1.classList.contains('show')).toBeFalse();
        document.body.removeChild(el1);
        document.body.removeChild(el2);
        done();
      }, 350);
    });

    it('should not move element if it is already a child of body', () => {
      const el = document.createElement('div');
      el.id = 'bodyModal';
      el.classList.add('modal', 'fade');
      document.body.appendChild(el);

      service.show('bodyModal');
      expect(el.parentElement).toBe(document.body);
      expect(el.classList.contains('show')).toBeTrue();
      document.body.removeChild(el);
    });
  });

  describe('restore to original parent', () => {
    it('should move element back to its original parent after hide', (done) => {
      const container = document.createElement('div');
      document.body.appendChild(container);

      const el = document.createElement('div');
      el.id = 'parentModal';
      el.classList.add('modal', 'fade');
      container.appendChild(el);

      service.show('parentModal');
      expect(el.parentElement).toBe(document.body);

      service.hide('parentModal');

      setTimeout(() => {
        expect(el.parentElement).toBe(container);
        document.body.removeChild(container);
        done();
      }, 350);
    });
  });

  describe('escape key', () => {
    it('should close the modal on Escape key press', (done) => {
      const el = document.createElement('div');
      el.id = 'escModal';
      el.classList.add('modal', 'fade');
      document.body.appendChild(el);

      service.show('escModal');
      expect(el.classList.contains('show')).toBeTrue();

      const event = new KeyboardEvent('keydown', { key: 'Escape' });
      document.dispatchEvent(event);

      expect(el.classList.contains('show')).toBeFalse();

      setTimeout(() => {
        document.body.removeChild(el);
        done();
      }, 350);
    });

    it('should not close anything on Escape when no modal is open', () => {
      const event = new KeyboardEvent('keydown', { key: 'Escape' });
      expect(() => document.dispatchEvent(event)).not.toThrow();
    });
  });
});
