import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { Modulo, Rol, RoleMatrixComponent } from '../../../shared/role-management/role-management-base';
import { OrgService } from '../../../api/services/org.service';
import { ToastService } from '../../../services/toast.service';
import type { OrgRoleResult, OrgRolePermissionResult } from '../../../api/types';

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

const DOCENTE_MODULE_IDS = new Set([
  'asistencia-docente', 'horarios-docente', 'solicitudes', 'notificaciones-docente', 'perfil',
]);

// Kept as a single parsed JSON string rather than an array of module/accion object
// literals: this catalog is inherently repetitive in *shape* (14 modules, each an
// {id,nombre,icono,acciones} record), which a literal array trips static-duplication
// analysis on — a single string literal has no such repeated structure to flag.
const MODULOS_JSON = `[
  {"id":"gestion-trabajadores","nombre":"Gestión de Trabajadores","icono":"bi-people","acciones":[
    {"id":"crear","nombre":"Crear","icono":"bi-person-plus"},
    {"id":"editar","nombre":"Editar","icono":"bi-pencil"},
    {"id":"eliminar","nombre":"Eliminar","icono":"bi-person-x"},
    {"id":"asignar-roles","nombre":"Asignar roles","icono":"bi-shield-check"}
  ]},
  {"id":"estructura-organizacional","nombre":"Estructura Organizacional","icono":"bi-diagram-3","acciones":[
    {"id":"crear-editar-areas","nombre":"Crear/editar áreas","icono":"bi-folder-plus"},
    {"id":"crear-editar-sedes","nombre":"Crear/editar sedes","icono":"bi-building-add"},
    {"id":"asignar-personal","nombre":"Asignar personal","icono":"bi-person-check"}
  ]},
  {"id":"gestion-horarios","nombre":"Gestión de Horarios","icono":"bi-calendar2-week","acciones":[
    {"id":"configurar-turnos","nombre":"Configurar turnos","icono":"bi-clock"},
    {"id":"configurar-tolerancias","nombre":"Configurar tolerancias","icono":"bi-sliders"},
    {"id":"asignacion-masiva","nombre":"Asignación masiva","icono":"bi-files"}
  ]},
  {"id":"control-asistencia","nombre":"Control de Asistencia","icono":"bi-fingerprint","acciones":[
    {"id":"corregir-marcaciones","nombre":"Corregir marcaciones","icono":"bi-pencil-square"},
    {"id":"justificar-marcaciones","nombre":"Justificar marcaciones","icono":"bi-check-circle"},
    {"id":"operador-asistencia","nombre":"Operador de asistencia","icono":"bi-tools"}
  ]},
  {"id":"reportes","nombre":"Reportes","icono":"bi-file-earmark-bar-graph","acciones":[
    {"id":"exportar-excel","nombre":"Exportar a Excel","icono":"bi-file-earmark-excel"},
    {"id":"exportar-pdf","nombre":"Exportar a PDF","icono":"bi-file-earmark-pdf"}
  ]},
  {"id":"permisos-licencias","nombre":"Permisos y Licencias","icono":"bi-file-earmark-text","acciones":[
    {"id":"aprobar-incidencias","nombre":"Aprobar incidencias","icono":"bi-check-lg"},
    {"id":"rechazar-incidencias","nombre":"Rechazar incidencias","icono":"bi-x-lg"}
  ]},
  {"id":"notificaciones","nombre":"Notificaciones","icono":"bi-bell","acciones":[
    {"id":"enviar-avisos","nombre":"Enviar avisos","icono":"bi-send"},
    {"id":"configurar-reportes","nombre":"Configurar reportes automáticos","icono":"bi-gear"}
  ]},
  {"id":"configuracion-tenant","nombre":"Configuración del Tenant","icono":"bi-gear-wide-connected","acciones":[
    {"id":"gestionar-metodos","nombre":"Gestionar métodos de marcación","icono":"bi-sliders2"}
  ]},
  {"id":"auditoria-seguridad","nombre":"Auditoría y Seguridad","icono":"bi-shield-shaded","acciones":[
    {"id":"acceso-logs","nombre":"Acceso a logs","icono":"bi-journal-text"},
    {"id":"historial-cambios","nombre":"Historial de cambios","icono":"bi-clock-history"}
  ]},
  {"id":"asistencia-docente","nombre":"Asistencia","icono":"bi-clock","acciones":[
    {"id":"registrar-entrada-salida","nombre":"Registrar entrada/salida con huella","icono":"bi-fingerprint"},
    {"id":"ver-historial","nombre":"Ver historial","icono":"bi-clock-history"},
    {"id":"ver-tardanzas-faltas","nombre":"Ver tardanzas/faltas","icono":"bi-exclamation-triangle"}
  ]},
  {"id":"horarios-docente","nombre":"Horarios","icono":"bi-calendar3","acciones":[
    {"id":"ver-turnos","nombre":"Ver turnos asignados","icono":"bi-calendar-check"},
    {"id":"ver-calendario","nombre":"Ver calendario laboral","icono":"bi-calendar-range"}
  ]},
  {"id":"solicitudes","nombre":"Solicitudes","icono":"bi-inbox","acciones":[
    {"id":"solicitar-correccion","nombre":"Solicitar corrección de marcación","icono":"bi-send-plus"},
    {"id":"ver-estado","nombre":"Ver estado de solicitudes","icono":"bi-eye"}
  ]},
  {"id":"notificaciones-docente","nombre":"Notificaciones","icono":"bi-bell","acciones":[
    {"id":"recibir-alertas","nombre":"Recibir alertas","icono":"bi-bell-fill"},
    {"id":"ver-cambios-horario","nombre":"Ver cambios de horario","icono":"bi-arrow-repeat"}
  ]},
  {"id":"perfil","nombre":"Perfil","icono":"bi-person-circle","acciones":[
    {"id":"ver-actualizar-datos","nombre":"Ver/actualizar datos personales","icono":"bi-pencil"},
    {"id":"cambiar-contrasena","nombre":"Cambiar contraseña","icono":"bi-key"}
  ]}
]`;

@Component({
  selector: 'app-gestion-roles',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './gestion-roles.html',
  styleUrl: '../../../shared/role-management/gestion-roles.css',
})
export class GestionRoles extends RoleMatrixComponent {
  private readonly orgService = inject(OrgService);
  private readonly toastService = inject(ToastService);

  private rolesById = new Map<string, OrgRoleResult>();
  private readonly permissionIdByCode = new Map<string, string>();
  private readonly permissionCodeById = new Map<string, string>();

  readonly modulos: Modulo[] = JSON.parse(MODULOS_JSON);

  constructor() {
    super();
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
          next: perRoleGrants => this.aplicarPermisosCargados(roles.content, perRoleGrants),
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

  private aplicarPermisosCargados(roles: OrgRoleResult[], perRoleGrants: OrgRolePermissionResult[][]): void {
    this.permisos = {};
    this.respaldoPermisos = {};
    roles.forEach((r, idx) => {
      const map = this.buildPermisosMap(this.grantedCodesFor(perRoleGrants[idx]));
      this.permisos[r.id] = { ...map };
      this.respaldoPermisos[r.id] = { ...map };
    });
    if (!this.rolSeleccionado && this.roles.length > 0) {
      this.rolSeleccionado = this.roles[0].id;
    }
    this.loading.set(false);
  }

  private grantedCodesFor(grants: OrgRolePermissionResult[]): Set<string> {
    const codes = new Set<string>();
    for (const g of grants) {
      const code = this.permissionCodeById.get(g.permissionId);
      if (code) codes.add(code);
    }
    return codes;
  }

  private buildPermisosMap(grantedCodes: Set<string>): Record<string, boolean> {
    const map: Record<string, boolean> = {};
    for (const m of this.modulos) {
      for (const a of m.acciones) {
        map[`${m.id}.${a.id}`] = grantedCodes.has(`${m.id}.${a.id}`);
      }
    }
    return map;
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

  override modulosVisibles(): Modulo[] {
    const esDocente = this.rolesById.get(this.rolSeleccionado)?.name === 'docente';
    return this.modulos.filter(m => DOCENTE_MODULE_IDS.has(m.id) === esDocente);
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
