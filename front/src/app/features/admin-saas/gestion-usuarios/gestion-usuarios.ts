import { Component, computed, signal, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { SlicePipe } from '@angular/common';
import { ToastService } from '../../../services/toast.service';
import { ModalService } from '../../../services/modal.service';
import { ApiService } from '../../../api/services/api.service';
import type { MasterUserProfile, SaasRoleProfile } from '../../../api/types';

@Component({
  selector: 'app-gestion-usuarios',
  standalone: true,
  imports: [ReactiveFormsModule, SlicePipe],
  templateUrl: './gestion-usuarios.html',
  styleUrl: './gestion-usuarios.css',
})
export class GestionUsuarios {
  readonly loading = signal(false);
  readonly error = signal('');

  private readonly fb = new FormBuilder();
  private readonly toastService = inject(ToastService);
  private readonly modalService = inject(ModalService);
  private readonly api = inject(ApiService);

  readonly usuarios = signal<MasterUserProfile[]>([]);
  readonly rolesDisponibles = signal<SaasRoleProfile[]>([]);
  readonly selectedRoleIds = signal<Set<number>>(new Set());

  readonly filterRol = signal('todos');
  readonly vistaCrear = signal(false);
  readonly paso = signal(1);
  editandoUsuario = signal<MasterUserProfile | null>(null);

  readonly usuariosFiltrados = computed(() =>
    this.usuarios().filter(u => {
      if (this.filterRol() === 'todos') return true;
      return u.roles.some(r => String(r.id) === this.filterRol());
    })
  );

  readonly total = computed(() => this.usuarios().length);
  readonly distribucionRoles = computed(() => {
    const roles = this.rolesDisponibles();
    const totalUsuarios = Math.max(this.total(), 1);
    return roles.map(rol => {
      const count = this.usuarios().filter(u => u.roles.some(r => r.id === rol.id)).length;
      return { rol, count, pct: (count / totalUsuarios) * 100 };
    }).filter(d => d.count > 0);
  });

  readonly createForm = this.fb.group({
    nombres: ['', [Validators.required, Validators.minLength(2)]],
    apellidoPaterno: ['', Validators.required],
    apellidoMaterno: ['', Validators.required],
    tipoDocumento: ['DNI'],
    numDocumento: ['', Validators.required],
    correo: ['', [Validators.required, Validators.email]],
    telefono: [''],
    contrasena: ['', [Validators.minLength(8)]],
  });

  readonly totalPasos = 2;
  readonly progreso = computed(() => Math.round((this.paso() / this.totalPasos) * 100));

  constructor() {
    this.cargarUsuarios();
    this.api.roles.list().subscribe(roles => this.rolesDisponibles.set(roles));
  }

  private cargarUsuarios(): void {
    this.loading.set(true);
    this.api.users.listMasters().subscribe({
      next: res => {
        this.usuarios.set(res.content);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los usuarios.');
        this.loading.set(false);
      },
    });
  }

  setFilterRol(val: string): void { this.filterRol.set(val); }

  limpiarFiltros(): void {
    this.filterRol.set('todos');
  }

  abrirCrear(u?: MasterUserProfile): void {
    this.editandoUsuario.set(u ?? null);
    this.vistaCrear.set(true);
    this.paso.set(1);
    if (u) {
      this.createForm.patchValue({
        nombres: u.persona.name,
        apellidoPaterno: u.persona.father_surname,
        apellidoMaterno: u.persona.mother_surname,
        tipoDocumento: u.persona.document_type,
        numDocumento: u.persona.document_value,
        correo: u.email ?? '',
        telefono: u.phone ?? '',
      });
      this.selectedRoleIds.set(new Set(u.roles.map(r => r.id)));
    } else {
      this.createForm.reset({
        nombres: '', apellidoPaterno: '', apellidoMaterno: '', tipoDocumento: 'DNI',
        numDocumento: '', correo: '', telefono: '', contrasena: '',
      });
      this.selectedRoleIds.set(new Set());
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

  isRoleSelected(id: number): boolean {
    return this.selectedRoleIds().has(id);
  }

  toggleRole(id: number): void {
    this.selectedRoleIds.update(set => {
      const next = new Set(set);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }

  registrarPersonal(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      this.mostrarToast('Corrige los errores antes de registrar.', 'error');
      return;
    }
    const v = this.createForm.value;
    const roleIds = [...this.selectedRoleIds()];
    const editando = this.editandoUsuario();

    if (editando) {
      this.api.users.updateMaster(editando.id, { email: v.correo!, phone: v.telefono || null }).subscribe({
        next: () => {
          this.api.users.assignMasterRole(editando.id, { role_ids: roleIds }).subscribe({
            next: () => {
              this.mostrarToast('Administrador actualizado correctamente.', 'success');
              this.cancelarCrear();
              this.cargarUsuarios();
            },
            error: () => this.mostrarToast('No se pudieron actualizar los roles.', 'error'),
          });
        },
        error: () => this.mostrarToast('No se pudo actualizar el administrador.', 'error'),
      });
      return;
    }

    this.api.users.createMaster({
      document_type: v.tipoDocumento as 'DNI' | 'CARNET_EXTRANJERIA' | 'PASAPORTE' | 'OTRO',
      document_value: v.numDocumento!,
      name: v.nombres!,
      father_surname: v.apellidoPaterno!,
      mother_surname: v.apellidoMaterno!,
      email: v.correo!,
      phone: v.telefono || null,
      password: v.contrasena || undefined,
      role_ids: roleIds,
    }).subscribe({
      next: () => {
        this.mostrarToast('Administrador registrado correctamente.', 'success');
        this.cancelarCrear();
        this.cargarUsuarios();
      },
      error: () => this.mostrarToast('No se pudo registrar el administrador.', 'error'),
    });
  }

  confirmarEliminar(u: MasterUserProfile): void {
    this.editandoUsuario.set(u);
    this.modalService.show('modalConfirmarEliminar');
  }

  eliminarUsuario(): void {
    const u = this.editandoUsuario();
    if (!u) return;
    this.api.users.deleteMaster(u.id).subscribe({
      next: () => {
        this.cerrarModal('modalConfirmarEliminar');
        this.mostrarToast(`${u.persona.name} eliminado correctamente.`, 'success');
        this.editandoUsuario.set(null);
        this.cargarUsuarios();
      },
      error: () => this.mostrarToast('No se pudo eliminar el administrador.', 'error'),
    });
  }

  nombreCompleto(u: MasterUserProfile): string {
    return `${u.persona.name} ${u.persona.father_surname}`.trim();
  }

  private cerrarModal(id: string): void {
    this.modalService.hide(id);
  }

  private mostrarToast(message: string, type: 'success' | 'error'): void {
    this.toastService.show(message, type);
  }
}
