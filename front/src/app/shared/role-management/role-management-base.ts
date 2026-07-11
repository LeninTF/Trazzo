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

// Shared by BaseGestionRoles (tenant-level roles) and the SaaS-level GestionRoles
// component, which reimplements this same permission-toggling logic against a
// backend-driven role list instead of extending BaseGestionRoles directly.
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

export abstract class BaseGestionRoles {
  readonly loading = signal(false);
  readonly error = signal('');

  abstract roles: Rol[];
  abstract modulos: Modulo[];
  protected abstract readonly PERMISOS_DEFAULT: Record<string, Record<string, boolean>>;

  permisos: Record<string, Record<string, boolean>> = {};
  respaldoPermisos: Record<string, Record<string, boolean>> = {};
  rolSeleccionado = '';
  nuevoRolNombre = '';
  nuevoRolDescripcion = '';
  editandoRol: Rol | null = null;
  mostrarModalRol = false;
  mensajeGuardado = false;

  protected inicializarPermisos(): void {
    for (const rol of this.roles) {
      this.permisos[rol.id] = {};
      this.respaldoPermisos[rol.id] = {};
      const defaults = this.PERMISOS_DEFAULT[rol.id] ?? {};
      this.inicializarPermisosRol(rol.id, defaults);
    }
  }

  private inicializarPermisosRol(rolId: string, defaults: Record<string, boolean>): void {
    for (const modulo of this.modulos) {
      for (const accion of modulo.acciones) {
        const key = `${modulo.id}.${accion.id}`;
        const value = defaults[key] ?? false;
        this.permisos[rolId][key] = value;
        this.respaldoPermisos[rolId][key] = value;
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
    this.inicializarPermisosRol(id, {});

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

    this.roles = this.roles.filter(r => r.id !== rolId);
    delete this.permisos[rolId];
    delete this.respaldoPermisos[rolId];

    if (this.rolSeleccionado === rolId) {
      this.rolSeleccionado = this.roles.length > 0 ? this.roles[0].id : '';
    }
  }
}
