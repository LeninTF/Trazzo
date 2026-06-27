import { HttpParams } from '@angular/common/http';
import type { TenantUserProfile } from '../types';

export function params(opts?: Record<string, string | number | boolean | undefined | null>): HttpParams {
  let p = new HttpParams();
  if (opts) {
    for (const [k, v] of Object.entries(opts)) {
      if (v !== undefined && v !== null && v !== '') {
        p = p.set(k, String(v));
      }
    }
  }
  return p;
}

export const API = 'https://api.trazzo.pe/api/v1';

export interface PersonalItem {
  id: number; nombre: string; idPersonal: string; sede: string;
  area: string; departamento: string; cargo: string; estado: string;
  email: string | null; telefono: string | null; fechaIngreso: string; imagenUrl: string | null;
}

export function tenantUserToPersonal(u: TenantUserProfile): PersonalItem {
  return {
    id: u.id,
    nombre: `${u.persona.name} ${u.persona.father_surname} ${u.persona.mother_surname}`,
    idPersonal: `#TU-${String(u.id).padStart(4, '0')}`,
    sede: u.sedes[0]?.nombre ?? '',
    area: u.areas[0]?.nombre ?? '',
    departamento: u.departamentos[0]?.nombre ?? '',
    cargo: '',
    estado: u.estado,
    email: u.email,
    telefono: u.phone,
    fechaIngreso: u.created_at.slice(0, 10),
    imagenUrl: u.persona.img_url,
  };
}
