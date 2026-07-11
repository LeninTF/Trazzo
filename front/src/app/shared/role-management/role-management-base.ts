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
