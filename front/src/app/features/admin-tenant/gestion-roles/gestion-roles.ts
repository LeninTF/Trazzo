import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import {
  Modulo, Rol,
  togglePermisoAccion, toggleModuloAcciones, estadoModulo, resumenModulo, restablecerPermisos,
} from '../../../shared/role-management/role-management-base';
import { OrgService } from '../../../api/services/org.service';
import { ToastService } from '../../../services/toast.service';
import type { OrgRoleResult } from '../../../api/types';

// role/permissions have no color/icon/display-label columns in the tenant schema —
// purely a frontend display concern, keyed by the role's machine name.
const ROLE_META: Record<string, { color: string; icono: string; label: string }> = {
  administrador: { color: '#1E40AF', icono: 'bi-shield-lock', label: 'Administrador' },
  director: { color: '#7C3AED', icono: 'bi-person-badge', label: 'Director' },
  coordinador: { color: '#0891B2', icono: 'bi-people', label: 'Coordinador' },
  'recursos-humanos': { color: '#D97706', icono: 'bi-building', label: 'Recursos Humanos' },
  docente: { color: '#059669', icono: 'bi-person-video', label: 'Docente' },
};
const DEFAULT_META = { color: '#6B7280', icono: 'bi-person', label: '' };

const DOCENTE_MODULE_IDS = ['asistencia-docente', 'horarios-docente', 'solicitudes', 'notificaciones-docente', 'perfil'];

type AccionSeed = readonly [id: string, nombre: string, icono: string];
type ModuloSeed = readonly [id: string, nombre: string, icono: string, acciones: readonly AccionSeed[]];

const MODULOS_SEED: readonly ModuloSeed[] = [
  ['gestion-trabajadores', 'Gestión de Trabajadores', 'bi-people', [
    ['crear', 'Crear', 'bi-person-plus'],
    ['editar', 'Editar', 'bi-pencil'],
    ['eliminar', 'Eliminar', 'bi-person-x'],
    ['asignar-roles', 'Asignar roles', 'bi-shield-check'],
  ]],
  ['estructura-organizacional', 'Estructura Organizacional', 'bi-diagram-3', [
    ['crear-editar-areas', 'Crear/editar áreas', 'bi-folder-plus'],
    ['crear-editar-sedes', 'Crear/editar sedes', 'bi-building-add'],
    ['asignar-personal', 'Asignar personal', 'bi-person-check'],
  ]],
  ['gestion-horarios', 'Gestión de Horarios', 'bi-calendar2-week', [
    ['configurar-turnos', 'Configurar turnos', 'bi-clock'],
    ['configurar-tolerancias', 'Configurar tolerancias', 'bi-sliders'],
    ['asignacion-masiva', 'Asignación masiva', 'bi-files'],
  ]],
  ['control-asistencia', 'Control de Asistencia', 'bi-fingerprint', [
    ['corregir-marcaciones', 'Corregir marcaciones', 'bi-pencil-square'],
    ['justificar-marcaciones', 'Justificar marcaciones', 'bi-check-circle'],
    ['operador-asistencia', 'Operador de asistencia', 'bi-tools'],
  ]],
  ['reportes', 'Reportes', 'bi-file-earmark-bar-graph', [
    ['exportar-excel', 'Exportar a Excel', 'bi-file-earmark-excel'],
    ['exportar-pdf', 'Exportar a PDF', 'bi-file-earmark-pdf'],
  ]],
  ['permisos-licencias', 'Permisos y Licencias', 'bi-file-earmark-text', [
    ['aprobar-incidencias', 'Aprobar incidencias', 'bi-check-lg'],
    ['rechazar-incidencias', 'Rechazar incidencias', 'bi-x-lg'],
  ]],
  ['notificaciones', 'Notificaciones', 'bi-bell', [
    ['enviar-avisos', 'Enviar avisos', 'bi-send'],
    ['configurar-reportes', 'Configurar reportes automáticos', 'bi-gear'],
  ]],
  ['configuracion-tenant', 'Configuración del Tenant', 'bi-gear-wide-connected', [
    ['gestionar-metodos', 'Gestionar métodos de marcación', 'bi-sliders2'],
  ]],
  ['auditoria-seguridad', 'Auditoría y Seguridad', 'bi-shield-shaded', [
    ['acceso-logs', 'Acceso a logs', 'bi-journal-text'],
    ['historial-cambios', 'Historial de cambios', 'bi-clock-history'],
  ]],
  ['asistencia-docente', 'Asistencia', 'bi-clock', [
    ['registrar-entrada-salida', 'Registrar entrada/salida con huella', 'bi-fingerprint'],
    ['ver-historial', 'Ver historial', 'bi-clock-history'],
    ['ver-tardanzas-faltas', 'Ver tardanzas/faltas', 'bi-exclamation-triangle'],
  ]],
  ['horarios-docente', 'Horarios', 'bi-calendar3', [
    ['ver-turnos', 'Ver turnos asignados', 'bi-calendar-check'],
    ['ver-calendario', 'Ver calendario laboral', 'bi-calendar-range'],
  ]],
  ['solicitudes', 'Solicitudes', 'bi-inbox', [
    ['solicitar-correccion', 'Solicitar corrección de marcación', 'bi-send-plus'],
    ['ver-estado', 'Ver estado de solicitudes', 'bi-eye'],
  ]],
  ['notificaciones-docente', 'Notificaciones', 'bi-bell', [
    ['recibir-alertas', 'Recibir alertas', 'bi-bell-fill'],
    ['ver-cambios-horario', 'Ver cambios de horario', 'bi-arrow-repeat'],
  ]],
  ['perfil', 'Perfil', 'bi-person-circle', [
    ['ver-actualizar-datos', 'Ver/actualizar datos personales', 'bi-pencil'],
    ['cambiar-contrasena', 'Cambiar contraseña', 'bi-key'],
  ]],
];

function buildModulos(seed: readonly ModuloSeed[]): Modulo[] {
  return seed.map(([id, nombre, icono, acciones]) => ({
    id, nombre, icono,
    acciones: acciones.map(([aid, anombre, aicono]) => ({ id: aid, nombre: anombre, icono: aicono })),
  }));
}

@Component({
  selector: 'app-gestion-roles',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './gestion-roles.html',
  styleUrl: '../../../shared/role-management/gestion-roles.css',
})
export class GestionRoles {
  private readonly orgService = inject(OrgService);
  private readonly toastService = inject(ToastService);

  readonly loading = signal(false);
  readonly error = signal('');

  roles: Rol[] = [];
  private rolesById = new Map<string, OrgRoleResult>();
  private permissionIdByCode = new Map<string, string>();
  private permissionCodeById = new Map<string, string>();

  readonly modulos: Modulo[] = buildModulos(MODULOS_SEED);

  permisos: Record<string, Record<string, boolean>> = {};
  respaldoPermisos: Record<string, Record<string, boolean>> = {};
  rolSeleccionado = '';
  nuevoRolNombre = '';
  nuevoRolDescripcion = '';
  editandoRol: Rol | null = null;
  mostrarModalRol = false;
  mensajeGuardado = false;

  constructor() {
    this.cargarRoles();
  }

  private cargarRoles(): void {
    this.loading.set(true);
    this.error.set('');

    forkJoin({
      roles: this.orgService.listRoles({ size: 100 }),
      permissions: this.orgService.listPermissions({ size: 200 }),
    }).subscribe({
      next: ({ roles, permissions }) => {
        this.permissionIdByCode.clear();
        this.permissionCodeById.clear();
        for (const p of permissions.content) {
          this.permissionIdByCode.set(p.name, p.id);
          this.permissionCodeById.set(p.id, p.name);
        }

        this.rolesById = new Map(roles.content.map(r => [r.id, r]));
        this.roles = roles.content.map(r => this.toRol(r));

        if (roles.content.length === 0) {
          this.permisos = {};
          this.respaldoPermisos = {};
          this.loading.set(false);
          return;
        }

        forkJoin(roles.content.map(r => this.orgService.listRolePermissions(r.id))).subscribe({
          next: perRoleGrants => {
            this.permisos = {};
            this.respaldoPermisos = {};
            roles.content.forEach((r, idx) => {
              const grantedCodes = new Set(
                perRoleGrants[idx]
                  .map(g => this.permissionCodeById.get(g.permissionId))
                  .filter((c): c is string => !!c)
              );
              const map = Object.fromEntries(
                this.modulos.flatMap(m => m.acciones.map(a => {
                  const key = `${m.id}.${a.id}`;
                  return [key, grantedCodes.has(key)];
                })),
              );
              this.permisos[r.id] = { ...map };
              this.respaldoPermisos[r.id] = { ...map };
            });
            if (!this.rolSeleccionado && this.roles.length > 0) {
              this.rolSeleccionado = this.roles[0].id;
            }
            this.loading.set(false);
          },
          error: () => {
            this.error.set('No se pudieron cargar los permisos de los roles.');
            this.loading.set(false);
          },
        });
      },
      error: () => {
        this.error.set('No se pudieron cargar los roles.');
        this.loading.set(false);
      },
    });
  }

  private toRol(p: OrgRoleResult): Rol {
    const meta = ROLE_META[p.name] ?? DEFAULT_META;
    return {
      id: p.id,
      nombre: meta.label || p.name,
      descripcion: p.description ?? '',
      color: meta.color,
      icono: meta.icono,
    };
  }

  get rolActual(): Rol {
    return this.roles.find(r => r.id === this.rolSeleccionado) ?? this.roles[0];
  }

  get permisosActuales(): Record<string, boolean> {
    return this.permisos[this.rolSeleccionado] ?? {};
  }

  modulosVisibles(): Modulo[] {
    const esDocente = this.rolesById.get(this.rolSeleccionado)?.name === 'docente';
    return this.modulos.filter(m => DOCENTE_MODULE_IDS.includes(m.id) === esDocente);
  }

  togglePermiso(moduloId: string, accionId: string): void {
    togglePermisoAccion(this.permisos, this.rolSeleccionado, moduloId, accionId);
  }

  toggleModulo(moduloId: string, value: boolean): void {
    toggleModuloAcciones(this.modulos, this.permisos, this.rolSeleccionado, moduloId, value);
  }

  getEstadoModulo(moduloId: string): 'completo' | 'parcial' | 'vacio' {
    return estadoModulo(this.modulos, this.permisos, this.rolSeleccionado, moduloId);
  }

  getResumenModulo(moduloId: string): string {
    return resumenModulo(this.modulos, this.permisos, this.rolSeleccionado, moduloId);
  }

  seleccionarRol(rolId: string): void {
    this.rolSeleccionado = rolId;
    this.mensajeGuardado = false;
  }

  restablecer(): void {
    restablecerPermisos(this.permisos, this.respaldoPermisos, this.rolSeleccionado);
    this.mensajeGuardado = false;
  }

  guardarCambios(): void {
    const roleId = this.rolSeleccionado;
    const actuales = this.permisos[roleId] ?? {};
    const anteriores = this.respaldoPermisos[roleId] ?? {};
    const toAssign: string[] = [];
    const toRemove: string[] = [];

    for (const key of Object.keys(actuales)) {
      if (actuales[key] === anteriores[key]) continue;
      const permissionId = this.permissionIdByCode.get(key);
      if (!permissionId) continue;
      (actuales[key] ? toAssign : toRemove).push(permissionId);
    }

    if (toAssign.length === 0 && toRemove.length === 0) {
      this.mostrarGuardado();
      return;
    }

    forkJoin([
      ...toAssign.map(pid => this.orgService.assignPermissionToRole(roleId, pid)),
      ...toRemove.map(pid => this.orgService.removePermissionFromRole(roleId, pid)),
    ]).subscribe({
      next: () => {
        this.respaldoPermisos[roleId] = { ...actuales };
        this.mostrarGuardado();
      },
      error: () => this.toastService.error('No se pudo guardar la configuración del rol.'),
    });
  }

  private mostrarGuardado(): void {
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
    const nombre = this.nuevoRolNombre.trim();
    if (!nombre) return;
    const descripcion = this.nuevoRolDescripcion.trim() || undefined;

    if (this.editandoRol) {
      this.orgService.updateRole(this.editandoRol.id, { name: nombre, description: descripcion }).subscribe({
        next: () => { this.mostrarModalRol = false; this.editandoRol = null; this.cargarRoles(); },
        error: () => this.toastService.error('No se pudo actualizar el rol.'),
      });
      return;
    }

    this.orgService.createRole({ name: nombre, description: descripcion }).subscribe({
      next: created => {
        this.mostrarModalRol = false;
        this.editandoRol = null;
        this.rolSeleccionado = created.id;
        this.cargarRoles();
      },
      error: () => this.toastService.error('No se pudo crear el rol.'),
    });
  }

  eliminarRol(rolId: string): void {
    this.orgService.deleteRole(rolId).subscribe({
      next: () => {
        if (this.rolSeleccionado === rolId) this.rolSeleccionado = '';
        this.cargarRoles();
      },
      error: () => this.toastService.error('No se pudo eliminar el rol (puede tener usuarios asignados).'),
    });
  }
}
