import { signal } from '@angular/core';

export interface Accion {
  id: string;
  nombre: string;
  icono: string;
}

export interface Modulo {
  id: string;
  nombre: string;
  icono: string;
  acciones: Accion[];
}

export interface Rol {
  id: string;
  nombre: string;
  descripcion: string;
  color: string;
  icono: string;
}

// Shared by the tenant-level and SaaS-level GestionRoles components, which each
// load their role/permission lists from their own backend-driven data source but
// apply identical toggle/state-summary logic against the shared Modulo/permisos shape.
export function togglePermisoAccion(
  permisos: Record<string, Record<string, boolean>>,
  rolSeleccionado: string,
  moduloId: string,
  accionId: string,
): void {
  const key = `${moduloId}.${accionId}`;
  permisos[rolSeleccionado][key] = !permisos[rolSeleccionado][key];
}

export function toggleModuloAcciones(
  modulos: Modulo[],
  permisos: Record<string, Record<string, boolean>>,
  rolSeleccionado: string,
  moduloId: string,
  value: boolean,
): void {
  const modulo = modulos.find(m => m.id === moduloId);
  if (!modulo) return;
  for (const accion of modulo.acciones) {
    permisos[rolSeleccionado][`${moduloId}.${accion.id}`] = value;
  }
}

function contarActivos(
  modulo: Modulo,
  permisos: Record<string, Record<string, boolean>>,
  rolSeleccionado: string,
  moduloId: string,
): number {
  let activos = 0;
  for (const accion of modulo.acciones) {
    if (permisos[rolSeleccionado][`${moduloId}.${accion.id}`]) activos++;
  }
  return activos;
}

export function estadoModulo(
  modulos: Modulo[],
  permisos: Record<string, Record<string, boolean>>,
  rolSeleccionado: string,
  moduloId: string,
): 'completo' | 'parcial' | 'vacio' {
  const modulo = modulos.find(m => m.id === moduloId);
  if (!modulo) return 'vacio';
  const activos = contarActivos(modulo, permisos, rolSeleccionado, moduloId);
  if (activos === 0) return 'vacio';
  if (activos === modulo.acciones.length) return 'completo';
  return 'parcial';
}

export function resumenModulo(
  modulos: Modulo[],
  permisos: Record<string, Record<string, boolean>>,
  rolSeleccionado: string,
  moduloId: string,
): string {
  const modulo = modulos.find(m => m.id === moduloId);
  if (!modulo) return '0/0';
  const activos = contarActivos(modulo, permisos, rolSeleccionado, moduloId);
  return `${activos}/${modulo.acciones.length}`;
}

export function restablecerPermisos(
  permisos: Record<string, Record<string, boolean>>,
  respaldoPermisos: Record<string, Record<string, boolean>>,
  rolSeleccionado: string,
): void {
  for (const key of Object.keys(permisos[rolSeleccionado])) {
    permisos[rolSeleccionado][key] = respaldoPermisos[rolSeleccionado][key];
  }
}

// Backend-agnostic UI state and permission-matrix behavior shared by the tenant-level
// and SaaS-level GestionRoles components. Loading/saving roles and permissions differs
// per data source (tenant vs SaaS APIs), so those stay abstract for each subclass to
// implement; everything here is pure UI-state manipulation identical in both.
export abstract class RoleMatrixComponent {
  readonly loading = signal(false);
  readonly error = signal('');

  roles: Rol[] = [];
  abstract readonly modulos: Modulo[];

  permisos: Record<string, Record<string, boolean>> = {};
  respaldoPermisos: Record<string, Record<string, boolean>> = {};
  rolSeleccionado = '';
  nuevoRolNombre = '';
  nuevoRolDescripcion = '';
  editandoRol: Rol | null = null;
  mostrarModalRol = false;
  mensajeGuardado = false;

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

  protected mostrarGuardado(): void {
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

  abstract guardarCambios(): void;
  abstract guardarRol(): void;
  abstract eliminarRol(rolId: string): void;
}
