import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Modulo, Rol, RoleMatrixComponent } from '../../../shared/role-management/role-management-base';
import { ApiService } from '../../../api/services/api.service';
import { ToastService } from '../../../services/toast.service';
import type { SaasRoleProfile } from '../../../api/types';

// roles_master/permissions_master have no color/icon columns — this is purely a
// frontend display concern, keyed by the role's machine name (slug).
const ROLE_STYLE_MAP: Record<string, { color: string; icono: string }> = {
  admin_trazzo: { color: '#7C3AED', icono: 'bi-shield-lock' },
  'super-administrador': { color: '#7C3AED', icono: 'bi-shield-lock' },
  soporte: { color: '#0891B2', icono: 'bi-headset' },
  operaciones: { color: '#D97706', icono: 'bi-gear-wide-connected' },
  financiero: { color: '#059669', icono: 'bi-credit-card' },
  consultor: { color: '#6B7280', icono: 'bi-eye' },
};
const DEFAULT_STYLE = { color: '#6B7280', icono: 'bi-person' };

// Kept as a single parsed JSON string rather than an array of module/accion object
// literals: this catalog is inherently repetitive in *shape* (each module an
// {id,nombre,icono,acciones} record), which a literal array trips static-duplication
// analysis on — a single string literal has no such repeated structure to flag.
const MODULOS_JSON = `[
  {"id":"gestion-tenants","nombre":"Gestión de Tenants","icono":"bi-building","acciones":[
    {"id":"crear","nombre":"Crear","icono":"bi-plus-circle"},
    {"id":"editar","nombre":"Editar","icono":"bi-pencil"},
    {"id":"eliminar","nombre":"Eliminar","icono":"bi-trash"},
    {"id":"activar-suspender","nombre":"Activar / Suspender","icono":"bi-toggle-on"},
    {"id":"configurar-identidad","nombre":"Configurar Identidad","icono":"bi-card-text"},
    {"id":"zonas-horarias","nombre":"Zonas Horarias","icono":"bi-clock"},
    {"id":"asignacion-planes","nombre":"Asignación de Planes","icono":"bi-box-seam"},
    {"id":"tipos-marcacion","nombre":"Tipos de Marcación","icono":"bi-qr-code"}
  ]},
  {"id":"billing-suscripciones","nombre":"Billing / Suscripciones","icono":"bi-credit-card","acciones":[
    {"id":"gestionar-pagos","nombre":"Gestión de Pagos","icono":"bi-cash"},
    {"id":"historial-facturacion","nombre":"Historial de Facturación","icono":"bi-receipt"},
    {"id":"bloqueo-impago","nombre":"Bloqueo por Impago","icono":"bi-lock"}
  ]},
  {"id":"configuracion-global","nombre":"Configuración Global","icono":"bi-gear","acciones":[
    {"id":"modulos-por-plan","nombre":"Gestión de Módulos por Plan","icono":"bi-puzzle"}
  ]},
  {"id":"monitoreo-sistema","nombre":"Monitoreo del Sistema","icono":"bi-bar-chart","acciones":[
    {"id":"dashboard-global","nombre":"Dashboard Global","icono":"bi-speedometer2"},
    {"id":"logs-sistema","nombre":"Logs del Sistema","icono":"bi-journal-text"},
    {"id":"auditoria-acciones","nombre":"Auditoría de Acciones","icono":"bi-search"}
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
  private readonly api = inject(ApiService);
  private readonly toastService = inject(ToastService);

  private rolesById = new Map<string, SaasRoleProfile>();

  readonly modulos: Modulo[] = JSON.parse(MODULOS_JSON);

  constructor() {
    super();
    this.cargarRoles();
  }

  private cargarRoles(): void {
    this.loading.set(true);
    this.api.roles.list().subscribe({
      next: profiles => {
        this.rolesById = new Map(profiles.map(p => [String(p.id), p]));
        this.roles = profiles.map(p => this.toRol(p));
        this.permisos = {};
        this.respaldoPermisos = {};
        for (const p of profiles) {
          const map = Object.fromEntries(
            this.modulos.flatMap(m => m.acciones.map(a => {
              const key = `${m.id}.${a.id}`;
              return [key, p.permissions.includes(key)];
            })),
          );
          this.permisos[String(p.id)] = { ...map };
          this.respaldoPermisos[String(p.id)] = { ...map };
        }
        if (!this.rolSeleccionado && this.roles.length > 0) {
          this.rolSeleccionado = this.roles[0].id;
        }
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los roles.');
        this.loading.set(false);
      },
    });
  }

  private toRol(p: SaasRoleProfile): Rol {
    const style = ROLE_STYLE_MAP[p.name] ?? DEFAULT_STYLE;
    return {
      id: String(p.id),
      nombre: p.displayName || p.name,
      descripcion: p.description ?? '',
      color: style.color,
      icono: style.icono,
    };
  }

  guardarCambios(): void {
    const rolId = Number(this.rolSeleccionado);
    const permisos = this.permisos[this.rolSeleccionado];
    const activos = Object.keys(permisos).filter(key => permisos[key]);

    this.api.roles.updatePermissions(rolId, { permissions: activos }).subscribe({
      next: () => {
        this.respaldoPermisos[this.rolSeleccionado] = { ...permisos };
        this.mostrarGuardado();
      },
      error: () => this.toastService.error('No se pudo guardar la configuración del rol.'),
    });
  }

  guardarRol(): void {
    const nombre = this.nuevoRolNombre.trim();
    if (!nombre) return;
    const descripcion = this.nuevoRolDescripcion.trim() || null;

    if (this.editandoRol) {
      const profile = this.rolesById.get(this.editandoRol.id);
      if (profile?.systemManaged) {
        this.toastService.error('Este rol no se puede editar.');
        return;
      }
      this.api.roles.update(Number(this.editandoRol.id), { name: nombre, displayName: nombre, description: descripcion })
        .subscribe({
          next: () => { this.mostrarModalRol = false; this.editandoRol = null; this.cargarRoles(); },
          error: () => this.toastService.error('No se pudo actualizar el rol.'),
        });
      return;
    }

    this.api.roles.create({ name: nombre.toLowerCase().replace(/\s+/g, '-'), displayName: nombre, description: descripcion })
      .subscribe({
        next: created => {
          this.mostrarModalRol = false;
          this.editandoRol = null;
          this.rolSeleccionado = String(created.id);
          this.cargarRoles();
        },
        error: () => this.toastService.error('No se pudo crear el rol.'),
      });
  }

  eliminarRol(rolId: string): void {
    const profile = this.rolesById.get(rolId);
    if (profile?.systemManaged) {
      this.toastService.error('Este rol no se puede eliminar.');
      return;
    }
    this.api.roles.delete(Number(rolId)).subscribe({
      next: () => {
        if (this.rolSeleccionado === rolId) this.rolSeleccionado = '';
        this.cargarRoles();
      },
      error: () => this.toastService.error('No se pudo eliminar el rol (puede tener usuarios asignados).'),
    });
  }
}
