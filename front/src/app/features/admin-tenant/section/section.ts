import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

type SectionMeta = {
	title: string;
	subtitle: string;
	icon: string;
};

const SECTION_MAP: Record<string, SectionMeta> = {
	'/tenant/incidencias': {
		title: 'Incidencias',
		subtitle: 'Centraliza alertas, observaciones y el seguimiento operativo de cada caso.',
		icon: 'bi-exclamation-triangle',
	},
	'/tenant/reglas-asistencia': {
		title: 'Reglas de asistencia',
		subtitle: 'Configura criterios, tolerancias y validaciones de marcación.',
		icon: 'bi-sliders',
	},
	'/tenant/sedes': {
		title: 'Sedes',
		subtitle: 'Administra las ubicaciones y la estructura física de la institución.',
		icon: 'bi-building',
	},
	'/tenant/gestion-roles': {
		title: 'Gestión de roles',
		subtitle: 'Define permisos y niveles de acceso para cada perfil administrativo.',
		icon: 'bi-person-gear',
	},
	'/tenant/configuracion-tenant': {
		title: 'Configuración tenant',
		subtitle: 'Ajusta parámetros globales, branding y comportamiento de la plataforma.',
		icon: 'bi-gear-wide-connected',
	},
	'/tenant/planes': {
		title: 'Planes',
		subtitle: 'Revisa y administra los planes habilitados para la institución.',
		icon: 'bi-kanban',
	},
	'/tenant/directorio-personal': {
		title: 'Directorio del personal',
		subtitle: 'Consulta la información del personal y sus datos operativos.',
		icon: 'bi-people',
	},
	'/tenant/gestion-horarios': {
		title: 'Gestión de horarios',
		subtitle: 'Organiza turnos, franjas horarias y asignaciones de asistencia.',
		icon: 'bi-calendar2-week',
	},
};

@Component({
	selector: 'app-section',
	imports: [],
	templateUrl: './section.html',
	styleUrl: './section.css',
})
export class Section {
	private readonly router = inject(Router);

	protected get section(): SectionMeta {
		const path = this.router.url.split('?')[0];

		return SECTION_MAP[path] ?? {
			title: 'Panel',
			subtitle: 'Selecciona una opción del menú lateral para comenzar.',
			icon: 'bi-grid-3x3-gap',
		};
	}
}