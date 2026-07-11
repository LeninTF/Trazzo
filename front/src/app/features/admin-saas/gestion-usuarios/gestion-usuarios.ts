import { Component, computed, signal, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../../services/toast.service';
import { ModalService } from '../../../services/modal.service';

interface Usuario {
	id: number;
	nombre: string;
	correo: string;
	rol: 'Superadmin' | 'Soporte' | 'Facturacion';
	estado: 'activo' | 'inactivo';
	ultimaConexion: string;
	foto?: string;
}

@Component({
	selector: 'app-gestion-usuarios',
	standalone: true,
	imports: [ReactiveFormsModule],
	templateUrl: './gestion-usuarios.html',
	styleUrl: './gestion-usuarios.css',
})
export class GestionUsuarios {
  readonly loading = signal(false);
  readonly error = signal('');

  private readonly fb = new FormBuilder();
  private readonly toastService = inject(ToastService);
  private readonly modalService = inject(ModalService);

	readonly usuarios = signal<Usuario[]>([
		{ id: 1, nombre: 'Carlos Méndez', correo: 'carlos.mendez@trazzo.pe', rol: 'Superadmin', estado: 'activo', ultimaConexion: 'Hace 12 min' },
		{ id: 2, nombre: 'Elena Rodríguez', correo: 'elena.rodriguez@trazzo.pe', rol: 'Soporte', estado: 'activo', ultimaConexion: 'Ayer 16:45' },
		{ id: 3, nombre: 'Lucía Paredes', correo: 'lucia.paredes@trazzo.pe', rol: 'Facturacion', estado: 'inactivo', ultimaConexion: 'Hace 14 días' },
		{ id: 4, nombre: 'Miguel Ángel Ruiz', correo: 'miguel.ruiz@trazzo.pe', rol: 'Superadmin', estado: 'activo', ultimaConexion: 'Hace 2 horas' },
		{ id: 5, nombre: 'Sofía Castillo', correo: 'sofia.castillo@trazzo.pe', rol: 'Soporte', estado: 'inactivo', ultimaConexion: 'Hace 7 días' },
		{ id: 6, nombre: 'Diego Morales', correo: 'diego.morales@trazzo.pe', rol: 'Facturacion', estado: 'activo', ultimaConexion: 'Hoy 09:30' },
		{ id: 7, nombre: 'Valentina Torres', correo: 'valentina.torres@trazzo.pe', rol: 'Superadmin', estado: 'activo', ultimaConexion: 'Hace 45 min' },
	]);

	readonly filterRol = signal('todos');
	readonly filterEstado = signal('todos');
	readonly vistaCrear = signal(false);
	readonly paso = signal(1);
	editandoUsuario = signal<Usuario | null>(null);

	readonly usuariosFiltrados = computed(() =>
		this.usuarios().filter(u => {
			if (this.filterRol() !== 'todos' && u.rol !== this.filterRol()) return false;
			if (this.filterEstado() !== 'todos' && u.estado !== this.filterEstado()) return false;
			return true;
		})
	);

	readonly activos = computed(() => this.usuarios().filter(u => u.estado === 'activo').length);
	readonly inactivos = computed(() => this.usuarios().filter(u => u.estado === 'inactivo').length);
	readonly total = computed(() => this.usuarios().length);
	readonly actividadSemanal = computed(() =>
		Math.round((this.activos() / Math.max(this.total(), 1)) * 100)
	);
	readonly superadmins = computed(() => this.usuarios().filter(u => u.rol === 'Superadmin').length);
	readonly soportes = computed(() => this.usuarios().filter(u => u.rol === 'Soporte').length);
	readonly facturaciones = computed(() => this.usuarios().filter(u => u.rol === 'Facturacion').length);

	readonly pctSuperadmin = computed(() => (this.superadmins() / Math.max(this.total(), 1)) * 100);
	readonly pctSoporte = computed(() => (this.soportes() / Math.max(this.total(), 1)) * 100);
	readonly pctFacturacion = computed(() => (this.facturaciones() / Math.max(this.total(), 1)) * 100);

	readonly createForm = this.fb.group({
		foto: [''],
		nombreCompleto: ['', [Validators.required, Validators.minLength(3)]],
		tipoDocumento: ['DNI'],
		numDocumento: ['', Validators.required],
		correo: ['', [Validators.required, Validators.email]],
		telefono: ['', Validators.required],
		fechaNacimiento: [''],
		empleadoId: ['', Validators.required],
		roles: this.fb.group({
			tenant: [false],
			finanzas: [false],
			auditoria: [false],
		}),
		nombreUsuario: ['', [Validators.required, Validators.minLength(4)]],
		contrasena: ['', [Validators.required, Validators.minLength(8)]],
		rolInstitucional: ['Empleado Estandar'],
	});

	readonly totalPasos = 3;
	readonly progreso = computed(() => Math.round((this.paso() / this.totalPasos) * 100));

	rolesDisponibles = [
		{ id: 'tenant', label: 'Gestión Tenant' },
		{ id: 'finanzas', label: 'Finanzas' },
		{ id: 'auditoria', label: 'Auditoría' },
	];

	setFilterRol(val: string): void { this.filterRol.set(val); }
	setFilterEstado(val: string): void { this.filterEstado.set(val); }

	limpiarFiltros(): void {
		this.filterRol.set('todos');
		this.filterEstado.set('todos');
	}

	abrirCrear(u?: Usuario): void {
		this.editandoUsuario.set(u ?? null);
		this.vistaCrear.set(true);
		this.paso.set(1);
		if (u) {
			this.createForm.patchValue({
				nombreCompleto: u.nombre,
				correo: u.correo,
				nombreUsuario: u.correo.split('@')[0],
			});
		} else {
			this.createForm.reset({
				foto: '', nombreCompleto: '', tipoDocumento: 'DNI', numDocumento: '',
				correo: '', telefono: '', fechaNacimiento: '', empleadoId: '',
				roles: { tenant: false, finanzas: false, auditoria: false },
				nombreUsuario: '', contrasena: '', rolInstitucional: 'Empleado Estandar',
			});
		}
	}

	cancelarCrear(): void {
		this.vistaCrear.set(false);
		this.editandoUsuario.set(null);
		this.paso.set(1);
		this.createForm.reset();
	}

	siguientePaso(): void {
		if (this.paso() < this.totalPasos) this.paso.update(p => p + 1);
	}

	pasoAnterior(): void {
		if (this.paso() > 1) this.paso.update(p => p - 1);
	}

	irPaso(n: number): void {
		if (n >= 1 && n <= this.totalPasos) this.paso.set(n);
	}

	registrarPersonal(): void {
		if (this.createForm.invalid) {
			this.createForm.markAllAsTouched();
			this.mostrarToast('Corrige los errores antes de registrar.', 'error');
			return;
		}
		const v = this.createForm.value;
		const rolesSel = ['tenant', 'finanzas', 'auditoria'].filter(r => (v.roles as Record<string, boolean | null>)[r]);
		const nuevoId = Math.max(...this.usuarios().map(u => u.id), 0) + 1;
		this.usuarios.update(list => [...list, {
			id: nuevoId,
			nombre: v.nombreCompleto!,
			correo: v.correo!,
			rol: 'Soporte' as const,
			estado: 'activo' as const,
			ultimaConexion: 'Ahora',
		}]);
		this.mostrarToast('Administrador registrado correctamente.', 'success');
		this.cancelarCrear();
	}

	generarUsuario(): void {
		const nombre = this.createForm.get('nombreCompleto')?.value?.toLowerCase().replace(/\s+/g, '.') || '';
		if (nombre) this.createForm.patchValue({ nombreUsuario: nombre });
	}

	guardarBorrador(): void {
		this.mostrarToast('Borrador guardado.', 'success');
	}

	toggleEstado(u: Usuario): void {
		this.usuarios.update(list =>
			list.map(user =>
				user.id === u.id ? { ...user, estado: user.estado === 'activo' ? 'inactivo' as const : 'activo' as const } : user
			)
		);
		const nuevoEstado = u.estado === 'activo' ? 'inactivo' : 'activo';
		this.mostrarToast(`${u.nombre} ahora está ${nuevoEstado === 'activo' ? 'activo' : 'inactivo'}.`, 'success');
	}

  confirmarEliminar(u: Usuario): void {
    this.editandoUsuario.set(u);
    this.modalService.show('modalConfirmarEliminar');
  }

	eliminarUsuario(): void {
		const u = this.editandoUsuario();
		if (!u) return;
		this.usuarios.update(list => list.filter(user => user.id !== u.id));
		this.cerrarModal('modalConfirmarEliminar');
		this.mostrarToast(`${u.nombre} eliminado correctamente.`, 'success');
		this.editandoUsuario.set(null);
	}

  private cerrarModal(id: string): void {
    this.modalService.hide(id);
  }

  private mostrarToast(message: string, type: 'success' | 'error'): void {
    this.toastService.show(message, type);
  }
}
