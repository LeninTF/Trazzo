import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Modulo, Rol } from '../../../shared/role-management/role-management-base';
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

@Component({
  selector: 'app-gestion-roles',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './gestion-roles.html',
  styleUrl: '../../../shared/role-management/gestion-roles.css',
})
export class GestionRoles {
  private readonly api = inject(ApiService);
  private readonly toastService = inject(ToastService);

  readonly loading = signal(false);
  readonly error = signal('');

  roles: Rol[] = [];
  private rolesById = new Map<string, SaasRoleProfile>();

  readonly modulos: Modulo[] = [
    {
      id: 'gestion-tenants',
      nombre: 'Gestión de Tenants',
      icono: 'bi-building',
      acciones: [
        { id: 'crear', nombre: 'Crear', icono: 'bi-plus-circle' },
        { id: 'editar', nombre: 'Editar', icono: 'bi-pencil' },
        { id: 'eliminar', nombre: 'Eliminar', icono: 'bi-trash' },
        { id: 'activar-suspender', nombre: 'Activar / Suspender', icono: 'bi-toggle-on' },
        { id: 'configurar-identidad', nombre: 'Configurar Identidad', icono: 'bi-card-text' },
        { id: 'zonas-horarias', nombre: 'Zonas Horarias', icono: 'bi-clock' },
        { id: 'asignacion-planes', nombre: 'Asignación de Planes', icono: 'bi-box-seam' },
        { id: 'tipos-marcacion', nombre: 'Tipos de Marcación', icono: 'bi-qr-code' },
      ],
    },
    {
      id: 'billing-suscripciones',
      nombre: 'Billing / Suscripciones',
      icono: 'bi-credit-card',
      acciones: [
        { id: 'gestionar-pagos', nombre: 'Gestión de Pagos', icono: 'bi-cash' },
        { id: 'historial-facturacion', nombre: 'Historial de Facturación', icono: 'bi-receipt' },
        { id: 'bloqueo-impago', nombre: 'Bloqueo por Impago', icono: 'bi-lock' },
      ],
    },
    {
      id: 'configuracion-global',
      nombre: 'Configuración Global',
      icono: 'bi-gear',
      acciones: [
        { id: 'modulos-por-plan', nombre: 'Gestión de Módulos por Plan', icono: 'bi-puzzle' },
      ],
    },
    {
      id: 'monitoreo-sistema',
      nombre: 'Monitoreo del Sistema',
      icono: 'bi-bar-chart',
      acciones: [
        { id: 'dashboard-global', nombre: 'Dashboard Global', icono: 'bi-speedometer2' },
        { id: 'logs-sistema', nombre: 'Logs del Sistema', icono: 'bi-journal-text' },
        { id: 'auditoria-acciones', nombre: 'Auditoría de Acciones', icono: 'bi-search' },
      ],
    },
  ];

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

  get rolActual(): Rol {
    return this.roles.find(r => r.id === this.rolSeleccionado) ?? this.roles[0];
  }

  get permisosActuales(): Record<string, boolean> {
    return this.permisos[this.rolSeleccionado] ?? {};
  }

  modulosVisibles(): Modulo[] {
    return this.modulos;
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
    const rolId = Number(this.rolSeleccionado);
    const permisos = this.permisos[this.rolSeleccionado];
    const activos = Object.keys(permisos).filter(key => permisos[key]);

    this.api.roles.updatePermissions(rolId, { permissions: activos }).subscribe({
      next: () => {
        this.respaldoPermisos[this.rolSeleccionado] = { ...permisos };
        this.mensajeGuardado = true;
        setTimeout(() => this.mensajeGuardado = false, 3000);
      },
      error: () => this.toastService.error('No se pudo guardar la configuración del rol.'),
    });
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
