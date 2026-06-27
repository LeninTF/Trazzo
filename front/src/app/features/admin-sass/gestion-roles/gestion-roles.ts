import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BaseGestionRoles, Rol, Modulo } from '../../../shared/role-management/role-management-base';

@Component({
  selector: 'app-gestion-roles',
  imports: [FormsModule],
  templateUrl: './gestion-roles.html',
  styleUrl: '../../../shared/role-management/gestion-roles.css',
})
export class GestionRoles extends BaseGestionRoles {
  readonly loading = signal(false);
  readonly error = signal('');

  roles: Rol[] = [
    { id: 'super-administrador', nombre: 'Super Administrador', descripcion: 'Acceso total a la plataforma SASS: gesti\u00F3n completa de tenants, billing, configuraci\u00F3n global y monitoreo del sistema.', color: '#7C3AED', icono: 'bi-shield-lock' },
    { id: 'soporte', nombre: 'Administrador de Soporte', descripcion: 'Gesti\u00F3n operativa de tenants y monitoreo del sistema. Sin acceso a billing ni configuraci\u00F3n global de planes.', color: '#0891B2', icono: 'bi-headset' },
    { id: 'operaciones', nombre: 'Administrador de Operaciones', descripcion: 'Administraci\u00F3n de tenants, configuraci\u00F3n de identidad, zonas horarias y tipos de marcaci\u00F3n. Acceso de solo lectura a monitoreo.', color: '#D97706', icono: 'bi-gear-wide-connected' },
    { id: 'financiero', nombre: 'Administrador Financiero', descripcion: 'Gesti\u00F3n completa de billing y suscripciones: pagos, facturaci\u00F3n y bloqueo por impago. Acceso de solo lectura a tenants.', color: '#059669', icono: 'bi-credit-card' },
    { id: 'consultor', nombre: 'Consultor / Vista', descripcion: 'Acceso de solo lectura a dashboards, logs y auditor\u00EDa del sistema. Sin permisos de escritura en ning\u00FAn m\u00F3dulo.', color: '#6B7280', icono: 'bi-eye' },
  ];

  modulos: Modulo[] = [
    {
      id: 'gestion-tenants',
      nombre: 'Gesti\u00F3n de Tenants',
      icono: 'bi-building',
      acciones: [
        { id: 'crear', nombre: 'Crear', icono: 'bi-plus-circle' },
        { id: 'editar', nombre: 'Editar', icono: 'bi-pencil' },
        { id: 'eliminar', nombre: 'Eliminar', icono: 'bi-trash' },
        { id: 'activar-suspender', nombre: 'Activar / Suspender', icono: 'bi-toggle-on' },
        { id: 'configurar-identidad', nombre: 'Configurar Identidad', icono: 'bi-card-text' },
        { id: 'zonas-horarias', nombre: 'Zonas Horarias', icono: 'bi-clock' },
        { id: 'asignacion-planes', nombre: 'Asignaci\u00F3n de Planes', icono: 'bi-box-seam' },
        { id: 'tipos-marcacion', nombre: 'Tipos de Marcaci\u00F3n', icono: 'bi-qr-code' },
      ],
    },
    {
      id: 'billing-suscripciones',
      nombre: 'Billing / Suscripciones',
      icono: 'bi-credit-card',
      acciones: [
        { id: 'gestionar-pagos', nombre: 'Gesti\u00F3n de Pagos', icono: 'bi-cash' },
        { id: 'historial-facturacion', nombre: 'Historial de Facturaci\u00F3n', icono: 'bi-receipt' },
        { id: 'bloqueo-impago', nombre: 'Bloqueo por Impago', icono: 'bi-lock' },
      ],
    },
    {
      id: 'configuracion-global',
      nombre: 'Configuraci\u00F3n Global',
      icono: 'bi-gear',
      acciones: [
        { id: 'modulos-por-plan', nombre: 'Gesti\u00F3n de M\u00F3dulos por Plan', icono: 'bi-puzzle' },
      ],
    },
    {
      id: 'monitoreo-sistema',
      nombre: 'Monitoreo del Sistema',
      icono: 'bi-bar-chart',
      acciones: [
        { id: 'dashboard-global', nombre: 'Dashboard Global', icono: 'bi-speedometer2' },
        { id: 'logs-sistema', nombre: 'Logs del Sistema', icono: 'bi-journal-text' },
        { id: 'auditoria-acciones', nombre: 'Auditor\u00EDa de Acciones', icono: 'bi-search' },
      ],
    },
  ];

  protected readonly PERMISOS_DEFAULT: Record<string, Record<string, boolean>> = {
    'super-administrador': {
      'gestion-tenants.crear': true, 'gestion-tenants.editar': true, 'gestion-tenants.eliminar': true, 'gestion-tenants.activar-suspender': true,
      'gestion-tenants.configurar-identidad': true, 'gestion-tenants.zonas-horarias': true, 'gestion-tenants.asignacion-planes': true, 'gestion-tenants.tipos-marcacion': true,
      'billing-suscripciones.gestionar-pagos': true, 'billing-suscripciones.historial-facturacion': true, 'billing-suscripciones.bloqueo-impago': true,
      'configuracion-global.modulos-por-plan': true,
      'monitoreo-sistema.dashboard-global': true, 'monitoreo-sistema.logs-sistema': true, 'monitoreo-sistema.auditoria-acciones': true,
    },
    soporte: {
      'gestion-tenants.crear': true, 'gestion-tenants.editar': true, 'gestion-tenants.eliminar': false, 'gestion-tenants.activar-suspender': true,
      'gestion-tenants.configurar-identidad': true, 'gestion-tenants.zonas-horarias': true, 'gestion-tenants.asignacion-planes': false, 'gestion-tenants.tipos-marcacion': true,
      'billing-suscripciones.gestionar-pagos': false, 'billing-suscripciones.historial-facturacion': true, 'billing-suscripciones.bloqueo-impago': false,
      'configuracion-global.modulos-por-plan': false,
      'monitoreo-sistema.dashboard-global': true, 'monitoreo-sistema.logs-sistema': true, 'monitoreo-sistema.auditoria-acciones': true,
    },
    operaciones: {
      'gestion-tenants.crear': true, 'gestion-tenants.editar': true, 'gestion-tenants.eliminar': false, 'gestion-tenants.activar-suspender': true,
      'gestion-tenants.configurar-identidad': true, 'gestion-tenants.zonas-horarias': true, 'gestion-tenants.asignacion-planes': false, 'gestion-tenants.tipos-marcacion': true,
      'billing-suscripciones.gestionar-pagos': false, 'billing-suscripciones.historial-facturacion': false, 'billing-suscripciones.bloqueo-impago': false,
      'configuracion-global.modulos-por-plan': false,
      'monitoreo-sistema.dashboard-global': true, 'monitoreo-sistema.logs-sistema': false, 'monitoreo-sistema.auditoria-acciones': false,
    },
    financiero: {
      'gestion-tenants.crear': false, 'gestion-tenants.editar': false, 'gestion-tenants.eliminar': false, 'gestion-tenants.activar-suspender': false,
      'gestion-tenants.configurar-identidad': false, 'gestion-tenants.zonas-horarias': false, 'gestion-tenants.asignacion-planes': true, 'gestion-tenants.tipos-marcacion': false,
      'billing-suscripciones.gestionar-pagos': true, 'billing-suscripciones.historial-facturacion': true, 'billing-suscripciones.bloqueo-impago': true,
      'configuracion-global.modulos-por-plan': false,
      'monitoreo-sistema.dashboard-global': true, 'monitoreo-sistema.logs-sistema': false, 'monitoreo-sistema.auditoria-acciones': true,
    },
    consultor: {
      'gestion-tenants.crear': false, 'gestion-tenants.editar': false, 'gestion-tenants.eliminar': false, 'gestion-tenants.activar-suspender': false,
      'gestion-tenants.configurar-identidad': false, 'gestion-tenants.zonas-horarias': false, 'gestion-tenants.asignacion-planes': false, 'gestion-tenants.tipos-marcacion': false,
      'billing-suscripciones.gestionar-pagos': false, 'billing-suscripciones.historial-facturacion': false, 'billing-suscripciones.bloqueo-impago': false,
      'configuracion-global.modulos-por-plan': false,
      'monitoreo-sistema.dashboard-global': true, 'monitoreo-sistema.logs-sistema': true, 'monitoreo-sistema.auditoria-acciones': true,
    },
  };

  protected readonly defaultRolId = 'super-administrador';

  override rolSeleccionado = 'super-administrador';

  constructor() {
    super();
    this.inicializarPermisos();
  }
}
