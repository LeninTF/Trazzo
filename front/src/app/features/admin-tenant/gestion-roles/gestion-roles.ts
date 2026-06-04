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
    { id: 'administrador', nombre: 'Administrador', descripcion: 'El Administrador tiene acceso total a la configuración y seguridad del sistema, incluyendo gestión de trabajadores, horarios, reportes y auditoría.', color: '#1E40AF', icono: 'bi-shield-lock' },
    { id: 'director', nombre: 'Director', descripcion: 'El Director puede supervisar la estructura organizacional, revisar reportes y aprobar incidencias, con acceso a la mayoría de módulos operativos.', color: '#7C3AED', icono: 'bi-person-badge' },
    { id: 'coordinador', nombre: 'Coordinador', descripcion: 'El Coordinador gestiona horarios, control de asistencia y trabajadores a su cargo, con permisos limitados a módulos operativos.', color: '#0891B2', icono: 'bi-people' },
    { id: 'recursos-humanos', nombre: 'Recursos Humanos', descripcion: 'RRHH administra trabajadores, corrige marcaciones, gestiona permisos y licencias, y puede exportar reportes.', color: '#D97706', icono: 'bi-building' },
    { id: 'docente', nombre: 'Docente', descripcion: 'El Docente puede registrar su asistencia, consultar horarios, realizar solicitudes de corrección y gestionar su perfil personal.', color: '#059669', icono: 'bi-person-video' },
  ];

  modulos: Modulo[] = [
    {
      id: 'gestion-trabajadores',
      nombre: 'Gestión de Trabajadores',
      icono: 'bi-people',
      acciones: [
        { id: 'crear', nombre: 'Crear', icono: 'bi-person-plus' },
        { id: 'editar', nombre: 'Editar', icono: 'bi-pencil' },
        { id: 'eliminar', nombre: 'Eliminar', icono: 'bi-person-x' },
        { id: 'asignar-roles', nombre: 'Asignar roles', icono: 'bi-shield-check' },
      ],
    },
    {
      id: 'estructura-organizacional',
      nombre: 'Estructura Organizacional',
      icono: 'bi-diagram-3',
      acciones: [
        { id: 'crear-editar-areas', nombre: 'Crear/editar áreas', icono: 'bi-folder-plus' },
        { id: 'crear-editar-sedes', nombre: 'Crear/editar sedes', icono: 'bi-building-add' },
        { id: 'asignar-personal', nombre: 'Asignar personal', icono: 'bi-person-check' },
      ],
    },
    {
      id: 'gestion-horarios',
      nombre: 'Gestión de Horarios',
      icono: 'bi-calendar2-week',
      acciones: [
        { id: 'configurar-turnos', nombre: 'Configurar turnos', icono: 'bi-clock' },
        { id: 'configurar-tolerancias', nombre: 'Configurar tolerancias', icono: 'bi-sliders' },
        { id: 'asignacion-masiva', nombre: 'Asignación masiva', icono: 'bi-files' },
      ],
    },
    {
      id: 'control-asistencia',
      nombre: 'Control de Asistencia',
      icono: 'bi-fingerprint',
      acciones: [
        { id: 'corregir-marcaciones', nombre: 'Corregir marcaciones', icono: 'bi-pencil-square' },
        { id: 'justificar-marcaciones', nombre: 'Justificar marcaciones', icono: 'bi-check-circle' },
        { id: 'operador-asistencia', nombre: 'Operador de asistencia', icono: 'bi-tools' },
      ],
    },
    {
      id: 'reportes',
      nombre: 'Reportes',
      icono: 'bi-file-earmark-bar-graph',
      acciones: [
        { id: 'exportar-excel', nombre: 'Exportar a Excel', icono: 'bi-file-earmark-excel' },
        { id: 'exportar-pdf', nombre: 'Exportar a PDF', icono: 'bi-file-earmark-pdf' },
      ],
    },
    {
      id: 'permisos-licencias',
      nombre: 'Permisos y Licencias',
      icono: 'bi-file-earmark-text',
      acciones: [
        { id: 'aprobar-incidencias', nombre: 'Aprobar incidencias', icono: 'bi-check-lg' },
        { id: 'rechazar-incidencias', nombre: 'Rechazar incidencias', icono: 'bi-x-lg' },
      ],
    },
    {
      id: 'notificaciones',
      nombre: 'Notificaciones',
      icono: 'bi-bell',
      acciones: [
        { id: 'enviar-avisos', nombre: 'Enviar avisos', icono: 'bi-send' },
        { id: 'configurar-reportes', nombre: 'Configurar reportes automáticos', icono: 'bi-gear' },
      ],
    },
    {
      id: 'configuracion-tenant',
      nombre: 'Configuración del Tenant',
      icono: 'bi-gear-wide-connected',
      acciones: [
        { id: 'gestionar-metodos', nombre: 'Gestionar métodos de marcación', icono: 'bi-sliders2' },
      ],
    },
    {
      id: 'auditoria-seguridad',
      nombre: 'Auditoría y Seguridad',
      icono: 'bi-shield-shaded',
      acciones: [
        { id: 'acceso-logs', nombre: 'Acceso a logs', icono: 'bi-journal-text' },
        { id: 'historial-cambios', nombre: 'Historial de cambios', icono: 'bi-clock-history' },
      ],
    },
    {
      id: 'asistencia-docente',
      nombre: 'Asistencia',
      icono: 'bi-clock',
      acciones: [
        { id: 'registrar-entrada-salida', nombre: 'Registrar entrada/salida con huella', icono: 'bi-fingerprint' },
        { id: 'ver-historial', nombre: 'Ver historial', icono: 'bi-clock-history' },
        { id: 'ver-tardanzas-faltas', nombre: 'Ver tardanzas/faltas', icono: 'bi-exclamation-triangle' },
      ],
    },
    {
      id: 'horarios-docente',
      nombre: 'Horarios',
      icono: 'bi-calendar3',
      acciones: [
        { id: 'ver-turnos', nombre: 'Ver turnos asignados', icono: 'bi-calendar-check' },
        { id: 'ver-calendario', nombre: 'Ver calendario laboral', icono: 'bi-calendar-range' },
      ],
    },
    {
      id: 'solicitudes',
      nombre: 'Solicitudes',
      icono: 'bi-inbox',
      acciones: [
        { id: 'solicitar-correccion', nombre: 'Solicitar corrección de marcación', icono: 'bi-send-plus' },
        { id: 'ver-estado', nombre: 'Ver estado de solicitudes', icono: 'bi-eye' },
      ],
    },
    {
      id: 'notificaciones-docente',
      nombre: 'Notificaciones',
      icono: 'bi-bell',
      acciones: [
        { id: 'recibir-alertas', nombre: 'Recibir alertas', icono: 'bi-bell-fill' },
        { id: 'ver-cambios-horario', nombre: 'Ver cambios de horario', icono: 'bi-arrow-repeat' },
      ],
    },
    {
      id: 'perfil',
      nombre: 'Perfil',
      icono: 'bi-person-circle',
      acciones: [
        { id: 'ver-actualizar-datos', nombre: 'Ver/actualizar datos personales', icono: 'bi-pencil' },
        { id: 'cambiar-contrasena', nombre: 'Cambiar contraseña', icono: 'bi-key' },
      ],
    },
  ];

  private readonly PERMISOS_DEFAULT: Record<string, Record<string, boolean>> = {
    administrador: {
      'gestion-trabajadores.crear': true, 'gestion-trabajadores.editar': true, 'gestion-trabajadores.eliminar': true, 'gestion-trabajadores.asignar-roles': true,
      'estructura-organizacional.crear-editar-areas': true, 'estructura-organizacional.crear-editar-sedes': true, 'estructura-organizacional.asignar-personal': true,
      'gestion-horarios.configurar-turnos': true, 'gestion-horarios.configurar-tolerancias': true, 'gestion-horarios.asignacion-masiva': true,
      'control-asistencia.corregir-marcaciones': true, 'control-asistencia.justificar-marcaciones': true, 'control-asistencia.operador-asistencia': true,
      'reportes.exportar-excel': true, 'reportes.exportar-pdf': true,
      'permisos-licencias.aprobar-incidencias': true, 'permisos-licencias.rechazar-incidencias': true,
      'notificaciones.enviar-avisos': true, 'notificaciones.configurar-reportes': true,
      'configuracion-tenant.gestionar-metodos': true,
      'auditoria-seguridad.acceso-logs': true, 'auditoria-seguridad.historial-cambios': true,
    },
    director: {
      'gestion-trabajadores.crear': true, 'gestion-trabajadores.editar': true, 'gestion-trabajadores.eliminar': false, 'gestion-trabajadores.asignar-roles': false,
      'estructura-organizacional.crear-editar-areas': true, 'estructura-organizacional.crear-editar-sedes': true, 'estructura-organizacional.asignar-personal': true,
      'gestion-horarios.configurar-turnos': true, 'gestion-horarios.configurar-tolerancias': true, 'gestion-horarios.asignacion-masiva': false,
      'control-asistencia.corregir-marcaciones': true, 'control-asistencia.justificar-marcaciones': true, 'control-asistencia.operador-asistencia': false,
      'reportes.exportar-excel': true, 'reportes.exportar-pdf': true,
      'permisos-licencias.aprobar-incidencias': true, 'permisos-licencias.rechazar-incidencias': true,
      'notificaciones.enviar-avisos': true, 'notificaciones.configurar-reportes': false,
      'configuracion-tenant.gestionar-metodos': false,
      'auditoria-seguridad.acceso-logs': true, 'auditoria-seguridad.historial-cambios': false,
    },
    coordinador: {
      'gestion-trabajadores.crear': true, 'gestion-trabajadores.editar': true, 'gestion-trabajadores.eliminar': false, 'gestion-trabajadores.asignar-roles': false,
      'estructura-organizacional.crear-editar-areas': false, 'estructura-organizacional.crear-editar-sedes': false, 'estructura-organizacional.asignar-personal': false,
      'gestion-horarios.configurar-turnos': true, 'gestion-horarios.configurar-tolerancias': false, 'gestion-horarios.asignacion-masiva': true,
      'control-asistencia.corregir-marcaciones': true, 'control-asistencia.justificar-marcaciones': false, 'control-asistencia.operador-asistencia': false,
      'reportes.exportar-excel': true, 'reportes.exportar-pdf': false,
      'permisos-licencias.aprobar-incidencias': false, 'permisos-licencias.rechazar-incidencias': false,
      'notificaciones.enviar-avisos': true, 'notificaciones.configurar-reportes': false,
      'configuracion-tenant.gestionar-metodos': false,
      'auditoria-seguridad.acceso-logs': false, 'auditoria-seguridad.historial-cambios': false,
    },
    'recursos-humanos': {
      'gestion-trabajadores.crear': true, 'gestion-trabajadores.editar': true, 'gestion-trabajadores.eliminar': true, 'gestion-trabajadores.asignar-roles': false,
      'estructura-organizacional.crear-editar-areas': false, 'estructura-organizacional.crear-editar-sedes': false, 'estructura-organizacional.asignar-personal': true,
      'gestion-horarios.configurar-turnos': false, 'gestion-horarios.configurar-tolerancias': false, 'gestion-horarios.asignacion-masiva': false,
      'control-asistencia.corregir-marcaciones': true, 'control-asistencia.justificar-marcaciones': true, 'control-asistencia.operador-asistencia': false,
      'reportes.exportar-excel': true, 'reportes.exportar-pdf': true,
      'permisos-licencias.aprobar-incidencias': true, 'permisos-licencias.rechazar-incidencias': true,
      'notificaciones.enviar-avisos': true, 'notificaciones.configurar-reportes': false,
      'configuracion-tenant.gestionar-metodos': false,
      'auditoria-seguridad.acceso-logs': false, 'auditoria-seguridad.historial-cambios': false,
    },
    docente: {
      'asistencia-docente.registrar-entrada-salida': true, 'asistencia-docente.ver-historial': true, 'asistencia-docente.ver-tardanzas-faltas': true,
      'horarios-docente.ver-turnos': true, 'horarios-docente.ver-calendario': true,
      'solicitudes.solicitar-correccion': true, 'solicitudes.ver-estado': true,
      'notificaciones-docente.recibir-alertas': true, 'notificaciones-docente.ver-cambios-horario': true,
      'perfil.ver-actualizar-datos': true, 'perfil.cambiar-contrasena': true,
    },
  };

  permisos: Record<string, Record<string, boolean>> = {};
  respaldoPermisos: Record<string, Record<string, boolean>> = {};
  rolSeleccionado = 'administrador';
  nuevoRolNombre = '';
  nuevoRolDescripcion = '';
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

  modulosVisibles(): Modulo[] {
    if (this.rolSeleccionado === 'docente') {
      return this.modulos.filter(m =>
        ['asistencia-docente', 'horarios-docente', 'solicitudes', 'notificaciones-docente', 'perfil'].includes(m.id)
      );
    }
    return this.modulos.filter(m =>
      !['asistencia-docente', 'horarios-docente', 'solicitudes', 'notificaciones-docente', 'perfil'].includes(m.id)
    );
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
    this.nuevoRolNombre = '';
    this.nuevoRolDescripcion = '';
    this.mostrarModalRol = true;
  }

  cerrarModalNuevoRol(): void {
    this.mostrarModalRol = false;
  }

  agregarRol(): void {
    const nombre = this.nuevoRolNombre.trim();
    if (!nombre) return;

    const id = nombre.toLowerCase().replace(/\s+/g, '-');
    if (this.roles.some(r => r.id === id)) return;

    const descripcion = this.nuevoRolDescripcion.trim() || `Rol personalizado «${nombre}» con permisos configurados según necesidad.`;

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
  }
}
