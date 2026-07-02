import { Injectable, computed, inject, signal } from '@angular/core';
import { RoleService, Role } from './role.service';

export interface Notification {
  id: string;
  icono: string;
  titulo: string;
  descripcion: string;
  hora: string;
  timestamp: Date;
  tipo: 'danger' | 'warning' | 'success' | 'info';
  route: string;
  leida: boolean;
}

function timeAgo(date: Date): string {
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMin = Math.floor(diffMs / 60000);
  if (diffMin < 1) return 'Ahora';
  if (diffMin < 60) return `Hace ${diffMin} min`;
  const diffHrs = Math.floor(diffMin / 60);
  if (diffHrs < 24) return `Hace ${diffHrs} h`;
  const diffDays = Math.floor(diffHrs / 24);
  if (diffDays < 30) return `Hace ${diffDays} día${diffDays > 1 ? 's' : ''}`;
  return `Hace ${Math.floor(diffDays / 30)} mes`;
}

function addMinutes(date: Date, minutes: number): Date {
  return new Date(date.getTime() + minutes * 60000);
}

function addHours(date: Date, hours: number): Date {
  return new Date(date.getTime() + hours * 3600000);
}

function addDays(date: Date, days: number): Date {
  return new Date(date.getTime() + days * 86400000);
}

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private readonly roleService = inject(RoleService);

  private readonly _readState = signal<Record<string, boolean>>({});

  private readonly _baseNotificaciones = computed(() =>
    this.generarNotificaciones(this.roleService.role())
  );

  readonly notificaciones = computed(() => {
    const base = this._baseNotificaciones();
    const read = this._readState();
    const role = this.roleService.role();
    return base.map(n => ({
      ...n,
      leida: read[`${role}:${n.id}`] ?? false,
    }));
  });

  readonly noLeidas = computed(() =>
    this.notificaciones().filter(n => !n.leida)
  );

  readonly notificacionesRecientes = computed(() =>
    this.notificaciones().slice(0, 5)
  );

  marcarComoLeida(id: string): void {
    const role = this.roleService.role();
    this._readState.update(state => ({ ...state, [`${role}:${id}`]: true }));
  }

  marcarTodasComoLeidas(): void {
    const role = this.roleService.role();
    this._readState.update(state => {
      const next = { ...state };
      this._baseNotificaciones().forEach(n => {
        next[`${role}:${n.id}`] = true;
      });
      return next;
    });
  }

  private generarNotificaciones(role: Role): Notification[] {
    const now = new Date();
    switch (role) {
      case 'admin-tenant':
        return [
          { id: 'at-1', icono: 'bi-exclamation-triangle-fill', titulo: 'Nueva incidencia reportada', descripcion: 'Empleado reportó retraso en ingreso', hora: timeAgo(addMinutes(now, -5)), timestamp: addMinutes(now, -5), tipo: 'danger', route: '/tenant/incidencias', leida: false },
          { id: 'at-2', icono: 'bi-person-x-fill', titulo: 'Inasistencia detectada', descripcion: '3 empleados no marcaron ingreso hoy', hora: timeAgo(addMinutes(now, -15)), timestamp: addMinutes(now, -15), tipo: 'warning', route: '/tenant/monitoreo', leida: false },
          { id: 'at-3', icono: 'bi-calendar2-week-fill', titulo: 'Solicitud de cambio de horario', descripcion: 'Juan Pérez solicita cambio de turno', hora: timeAgo(addHours(now, -1)), timestamp: addHours(now, -1), tipo: 'info', route: '/tenant/gestion-horarios', leida: false },
          { id: 'at-4', icono: 'bi-person-plus-fill', titulo: 'Nuevo empleado registrado', descripcion: 'María García ha sido añadida al directorio', hora: timeAgo(addHours(now, -2)), timestamp: addHours(now, -2), tipo: 'success', route: '/tenant/directorio-personal', leida: false },
          { id: 'at-5', icono: 'bi-sliders', titulo: 'Regla de asistencia actualizada', descripcion: 'Se modificó la regla de tolerancia', hora: timeAgo(addHours(now, -4)), timestamp: addHours(now, -4), tipo: 'info', route: '/tenant/reglas-asistencia', leida: false },
          { id: 'at-6', icono: 'bi-kanban-fill', titulo: 'Plan próximo a vencer', descripcion: 'El plan básico vence en 7 días', hora: timeAgo(addDays(now, -1)), timestamp: addDays(now, -1), tipo: 'warning', route: '/tenant/planes', leida: false },
          { id: 'at-7', icono: 'bi-building-fill', titulo: 'Sede actualizada', descripcion: 'Se actualizó la sede principal', hora: timeAgo(addDays(now, -2)), timestamp: addDays(now, -2), tipo: 'info', route: '/tenant/sedes', leida: false },
          { id: 'at-8', icono: 'bi-gear-wide-connected-fill', titulo: 'Configuración modificada', descripcion: 'Se cambió la configuración del tenant', hora: timeAgo(addDays(now, -3)), timestamp: addDays(now, -3), tipo: 'info', route: '/tenant/configuracion-tenant', leida: false },
          { id: 'at-9', icono: 'bi-person-gear-fill', titulo: 'Nuevo rol creado', descripcion: 'Se creó el rol de supervisor', hora: timeAgo(addDays(now, -5)), timestamp: addDays(now, -5), tipo: 'success', route: '/tenant/gestion-roles', leida: false },
          { id: 'at-10', icono: 'bi-check-circle-fill', titulo: 'Incidencia resuelta', descripcion: 'La incidencia #123 fue marcada como resuelta', hora: timeAgo(addDays(now, -7)), timestamp: addDays(now, -7), tipo: 'success', route: '/tenant/incidencias', leida: false },
        ];
      case 'usuario':
        return [
          { id: 'us-1', icono: 'bi-calendar3-fill', titulo: 'Cambio en tu horario', descripcion: 'Tu turno del jueves ha sido modificado', hora: timeAgo(addMinutes(now, -10)), timestamp: addMinutes(now, -10), tipo: 'warning', route: '/usuario/calendario', leida: false },
          { id: 'us-2', icono: 'bi-clock-history', titulo: 'Inasistencia registrada', descripcion: 'No registraste ingreso el día de ayer', hora: timeAgo(addHours(now, -3)), timestamp: addHours(now, -3), tipo: 'danger', route: '/usuario/historial-asistencia', leida: false },
          { id: 'us-3', icono: 'bi-check-circle-fill', titulo: 'Incidencia resuelta', descripcion: 'Tu incidencia ha sido resuelta', hora: timeAgo(addHours(now, -5)), timestamp: addHours(now, -5), tipo: 'success', route: '/usuario/incidencias', leida: false },
          { id: 'us-4', icono: 'bi-calendar-plus-fill', titulo: 'Nuevo horario asignado', descripcion: 'Se te ha asignado un nuevo turno', hora: timeAgo(addDays(now, -1)), timestamp: addDays(now, -1), tipo: 'info', route: '/usuario/calendario', leida: false },
          { id: 'us-5', icono: 'bi-exclamation-circle-fill', titulo: 'Incidente reportado', descripcion: 'Se ha reportado un incidente en tu horario', hora: timeAgo(addDays(now, -2)), timestamp: addDays(now, -2), tipo: 'warning', route: '/usuario/incidencias', leida: false },
        ];
      case 'admin-sass':
        return [
          { id: 'sass-1', icono: 'bi-building-add-fill', titulo: 'Nuevo tenant registrado', descripcion: 'Empresa XYZ se ha registrado en la plataforma', hora: timeAgo(addMinutes(now, -8)), timestamp: addMinutes(now, -8), tipo: 'success', route: '/sass/tenants', leida: false },
          { id: 'sass-2', icono: 'bi-file-text-fill', titulo: 'Nueva solicitud de soporte', descripcion: 'Tenant ABC requiere asistencia técnica', hora: timeAgo(addMinutes(now, -25)), timestamp: addMinutes(now, -25), tipo: 'warning', route: '/sass/solicitudes', leida: false },
          { id: 'sass-3', icono: 'bi-receipt-fill', titulo: 'Factura generada', descripcion: 'Factura del mes de junio generada', hora: timeAgo(addHours(now, -2)), timestamp: addHours(now, -2), tipo: 'info', route: '/sass/facturas', leida: false },
          { id: 'sass-4', icono: 'bi-box-seam-fill', titulo: 'Nuevo plan creado', descripcion: 'Se creó el plan Enterprise Plus', hora: timeAgo(addHours(now, -6)), timestamp: addHours(now, -6), tipo: 'success', route: '/sass/gestion-planes', leida: false },
          { id: 'sass-5', icono: 'bi-journal-text-fill', titulo: 'Evento de auditoría', descripcion: 'Cambio de configuración en tenant XYZ', hora: timeAgo(addDays(now, -1)), timestamp: addDays(now, -1), tipo: 'info', route: '/sass/log-auditoria', leida: false },
          { id: 'sass-6', icono: 'bi-people-fill', titulo: 'Usuario administrador creado', descripcion: 'Nuevo admin para tenant DEF', hora: timeAgo(addDays(now, -2)), timestamp: addDays(now, -2), tipo: 'info', route: '/sass/gestion-usuarios', leida: false },
          { id: 'sass-7', icono: 'bi-person-gear-fill', titulo: 'Rol modificado', descripcion: 'Se actualizaron permisos del rol Gerente', hora: timeAgo(addDays(now, -3)), timestamp: addDays(now, -3), tipo: 'warning', route: '/sass/gestion-roles', leida: false },
        ];
    }
  }
}
