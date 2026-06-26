import { Injectable, Injector } from '@angular/core';

declare const bootstrap: {
  Modal: {
    new (element: Element, options?: Record<string, unknown>): ModalInstance;
    getInstance(element: Element): ModalInstance | null;
  };
};

interface ModalInstance {
  show(): void;
  hide(): void;
}

@Injectable({
  providedIn: 'root',
})
export class ModalService {
  show(modalId: string): void {
    const el = document.getElementById(modalId);
    if (el) {
      new bootstrap.Modal(el).show();
    }
  }

  hide(modalId: string): void {
    const el = document.getElementById(modalId);
    if (el) {
      bootstrap.Modal.getInstance(el)?.hide();
    }
  }
}
