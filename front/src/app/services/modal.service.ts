import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class ModalService {
  private openModalId: string | null = null;
  private originalParent: HTMLElement | null = null;
  private escapeHandler = (e: KeyboardEvent) => {
    if (e.key === 'Escape' && this.openModalId) {
      this.hide(this.openModalId);
    }
  };

  show(modalId: string): void {
    if (this.openModalId) this.hide(this.openModalId);
    let el = document.getElementById(modalId);
    if (!el) return;

    if (el.parentElement !== document.body) {
      this.originalParent = el.parentElement;
      el.remove();
      document.body.appendChild(el);
    }

    el.style.display = 'block';
    document.body.classList.add('modal-open');

    void el.offsetHeight;
    el.classList.add('show');

    const backdrop = document.createElement('div');
    backdrop.className = 'modal-backdrop fade';
    document.body.appendChild(backdrop);
    void backdrop.offsetHeight;
    backdrop.classList.add('show');

    backdrop.addEventListener('click', () => this.hide(modalId));

    this.openModalId = modalId;
    document.addEventListener('keydown', this.escapeHandler);
  }

  hide(modalId: string): void {
    const el = document.getElementById(modalId);
    if (!el || !el.classList.contains('show')) return;

    el.classList.remove('show');
    const backdrop = document.querySelector('.modal-backdrop.show') as HTMLElement;
    if (backdrop) backdrop.classList.remove('show');

    const cleanup = () => {
      if (el) el.style.display = '';
      if (backdrop) backdrop.remove();
      document.body.classList.remove('modal-open');

      if (this.originalParent && el && el.parentElement === document.body) {
        el.remove();
        this.originalParent.appendChild(el);
      }
      this.originalParent = null;

      if (this.openModalId === modalId) {
        this.openModalId = null;
        document.removeEventListener('keydown', this.escapeHandler);
      }
    };

    setTimeout(cleanup, 300);
  }
}
