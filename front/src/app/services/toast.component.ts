import { Component } from '@angular/core';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  template: `
    @if (toastService.toast(); as t) {
      <div class="toast-notification" [class.toast--success]="t.type === 'success'" [class.toast--error]="t.type === 'error'" [class.toast--info]="t.type === 'info'">
        <i class="bi" [class.bi-check-circle-fill]="t.type === 'success'" [class.bi-x-circle-fill]="t.type === 'error'" [class.bi-info-circle-fill]="t.type === 'info'"></i>
        <span>{{ t.message }}</span>
      </div>
    }
  `,
  styles: [`
    .toast-notification {
      position: fixed;
      bottom: 2rem;
      right: 2rem;
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 1rem 1.5rem;
      border-radius: 12px;
      background: #1e293b;
      color: #f8fafc;
      font-weight: 500;
      font-size: 0.9rem;
      box-shadow: 0 10px 40px rgba(0,0,0,0.3);
      z-index: 99999;
      animation: slideUp 0.3s ease-out;
      max-width: 400px;
    }
    .toast--success { border-left: 4px solid #22c55e; }
    .toast--error { border-left: 4px solid #ef4444; }
    .toast--info { border-left: 4px solid #3b82f6; }
    .toast-notification i { font-size: 1.2rem; }
    .toast--success i { color: #22c55e; }
    .toast--error i { color: #ef4444; }
    .toast--info i { color: #3b82f6; }
    @keyframes slideUp {
      from { opacity: 0; transform: translateY(20px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `],
})
export class ToastComponent {
  constructor(protected readonly toastService: ToastService) {}
}
