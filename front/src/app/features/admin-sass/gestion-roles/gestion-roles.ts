import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface Accion {
  id: string;
  nombre: string;
  icono: string;
}

interface Modulo {
  id: string;
  nombre: string;
  icono: string;
  acciones: Accion[];
}

interface Rol {
  id: string;
  nombre: string;
  descripcion: string;
  color: string;
  icono: string;
}

@Component({
  selector: 'app-gestion-roles',
  imports: [FormsModule],
  templateUrl: './gestion-roles.html',
  styleUrl: './gestion-roles.css',
})
export class GestionRoles {
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

  private readonly PERMISOS_DEFAULT: Record<string, Record<string, boolean>> = {
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

  permisos: Record<string, Record<string, boolean>> = {};
  respaldoPermisos: Record<string, Record<string, boolean>> = {};
  rolSeleccionado = 'super-administrador';
  nuevoRolNombre = '';
  nuevoRolDescripcion = '';
  editandoRol: Rol | null = null;
  mostrarModalRol = false;
  mensajeGuardado = false;

  constructor() {
    this.inicializarPermisos();
  }

  private inicializarPermisos(): void {
    for (const rol of this.roles) {
      this.permisos[rol.id] = {};
      this.respaldoPermisos[rol.id] = {};
      const defaults = this.PERMISOS_DEFAULT[rol.id] ?? {};
      for (const modulo of this.modulos) {
        for (const accion of modulo.acciones) {
          const key = `${modulo.id}.${accion.id}`;
          const value = defaults[key] ?? false;
          this.permisos[rol.id][key] = value;
          this.respaldoPermisos[rol.id][key] = value;
        }
      }
    }
  }

  get rolActual(): Rol {
    return this.roles.find(r => r.id === this.rolSeleccionado) ?? this.roles[0];
  }

  get permisosActuales(): Record<string, boolean> {
    return this.permisos[this.rolSeleccionado] ?? {};
  }

  togglePermiso(moduloId: string, accionId: string): void {
    const key = `${moduloId}.${accionId}`;
    this.permisos[this.rolSeleccionado][key] = !this.permisos[this.rolSeleccionado][key];
  }

  toggleModulo(moduloId: string, value: boolean): void {
    const modulo = this.modulos.find(m => m.id === moduloId);
    if (!modulo) return;
    for (const accion of modulo.acciones) {
      const key = `${moduloId}.${accion.id}`;
      this.permisos[this.rolSeleccionado][key] = value;
    }
  }

  getEstadoModulo(moduloId: string): 'completo' | 'parcial' | 'vacio' {
    const modulo = this.modulos.find(m => m.id === moduloId);
    if (!modulo) return 'vacio';
    let activos = 0;
    for (const accion of modulo.acciones) {
      const key = `${moduloId}.${accion.id}`;
      if (this.permisos[this.rolSeleccionado][key]) activos++;
    }
    if (activos === 0) return 'vacio';
    if (activos === modulo.acciones.length) return 'completo';
    return 'parcial';
  }

  getResumenModulo(moduloId: string): string {
    const modulo = this.modulos.find(m => m.id === moduloId);
    if (!modulo) return '0/0';
    let activos = 0;
    for (const accion of modulo.acciones) {
      const key = `${moduloId}.${accion.id}`;
      if (this.permisos[this.rolSeleccionado][key]) activos++;
    }
    return `${activos}/${modulo.acciones.length}`;
  }

  seleccionarRol(rolId: string): void {
    this.rolSeleccionado = rolId;
    this.mensajeGuardado = false;
  }

  restablecer(): void {
    for (const key of Object.keys(this.permisos[this.rolSeleccionado])) {
      this.permisos[this.rolSeleccionado][key] = this.respaldoPermisos[this.rolSeleccionado][key];
    }
    this.mensajeGuardado = false;
  }

  guardarCambios(): void {
    for (const key of Object.keys(this.permisos[this.rolSeleccionado])) {
      this.respaldoPermisos[this.rolSeleccionado][key] = this.permisos[this.rolSeleccionado][key];
    }
    this.mensajeGuardado = true;
    setTimeout(() => this.mensajeGuardado = false, 3000);
  }

  abrirModalNuevoRol(): void {
    this.editandoRol = null;
    this.nuevoRolNombre = '';
    this.nuevoRolDescripcion = '';
    this.mostrarModalRol = true;
  }

  abrirModalEditarRol(rol: Rol): void {
    this.editandoRol = rol;
    this.nuevoRolNombre = rol.nombre;
    this.nuevoRolDescripcion = rol.descripcion;
    this.mostrarModalRol = true;
  }

  cerrarModalNuevoRol(): void {
    this.mostrarModalRol = false;
    this.editandoRol = null;
  }

  guardarRol(): void {
    if (this.editandoRol) {
      this.guardarEdicionRol();
    } else {
      this.agregarRol();
    }
  }

  private agregarRol(): void {
    const nombre = this.nuevoRolNombre.trim();
    if (!nombre) return;

    const id = nombre.toLowerCase().replace(/\s+/g, '-');
    if (this.roles.some(r => r.id === id)) return;

    const descripcion = this.nuevoRolDescripcion.trim() || `Rol personalizado \u00AB${nombre}\u00BB con permisos configurados seg\u00FAn necesidad.`;

    const nuevoRol: Rol = {
      id,
      nombre,
      descripcion,
      color: '#6B7280',
      icono: 'bi-person',
    };

    this.roles.push(nuevoRol);
    this.permisos[id] = {};
    this.respaldoPermisos[id] = {};
    for (const modulo of this.modulos) {
      for (const accion of modulo.acciones) {
        const key = `${modulo.id}.${accion.id}`;
        this.permisos[id][key] = false;
        this.respaldoPermisos[id][key] = false;
      }
    }

    this.rolSeleccionado = id;
    this.mostrarModalRol = false;
    this.editandoRol = null;
  }

  private guardarEdicionRol(): void {
    if (!this.editandoRol) return;
    const nombre = this.nuevoRolNombre.trim();
    if (!nombre) return;

    this.editandoRol.nombre = nombre;
    this.editandoRol.descripcion = this.nuevoRolDescripcion.trim() || this.editandoRol.descripcion;

    this.mostrarModalRol = false;
    this.editandoRol = null;
  }

  eliminarRol(rolId: string): void {
    const rol = this.roles.find(r => r.id === rolId);
    if (!rol) return;

    const confirmar = confirm(`\u00BFEliminar el rol \u00AB${rol.nombre}\u00BB? Esta acci\u00F3n no se puede deshacer.`);
    if (!confirmar) return;

    this.roles = this.roles.filter(r => r.id !== rolId);
    delete this.permisos[rolId];
    delete this.respaldoPermisos[rolId];

    if (this.rolSeleccionado === rolId) {
      this.rolSeleccionado = this.roles.length > 0 ? this.roles[0].id : '';
    }
  }
}