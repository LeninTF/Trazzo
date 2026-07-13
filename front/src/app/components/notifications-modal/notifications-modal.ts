import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { NotificationService, Notification } from '../../services/notification.service';
import { ModalService } from '../../services/modal.service';

type Filtro = 'todas' | 'no-leidas' | 'leidas';

@Component({
  selector: 'app-notifications-modal',
  imports: [],
  templateUrl: './notifications-modal.html',
  styleUrl: './notifications-modal.css',
})
export class NotificationsModal {
  protected readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly modalService = inject(ModalService);

  protected filtroActual: Filtro = 'todas';

  protected get notificacionesFiltradas(): Notification[] {
    const todas = this.notificationService.notificaciones();
    switch (this.filtroActual) {
      case 'no-leidas': return todas.filter(n => !n.leida);
      case 'leidas': return todas.filter(n => n.leida);
      default: return todas;
    }
  }

  protected readonly tipoColor: Record<string, string> = {
    danger: '#dc2626',
    warning: '#f59e0b',
    success: '#10b981',
    info: '#3b82f6',
  };

  protected setFiltro(filtro: Filtro): void {
    this.filtroActual = filtro;
  }

  protected irANotificacion(notif: Notification): void {
    this.notificationService.marcarComoLeida(notif.id);
    this.router.navigateByUrl(notif.route);
    this.modalService.hide('notificationsModal');
  }

  protected marcarTodasComoLeidas(): void {
    this.notificationService.marcarTodasComoLeidas();
  }

  protected cerrarModal(): void {
    this.modalService.hide('notificationsModal');
  }
}
