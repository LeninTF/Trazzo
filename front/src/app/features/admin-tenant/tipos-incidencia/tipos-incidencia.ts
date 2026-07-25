import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../../../api/services/api.service';
import { ToastService } from '../../../services/toast.service';
import type { IncidentTypeProfile } from '../../../api/types';

@Component({
  selector: 'app-tipos-incidencia',
  imports: [ReactiveFormsModule],
  templateUrl: './tipos-incidencia.html',
  styleUrl: './tipos-incidencia.css',
})
export class TiposIncidencia implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toastService = inject(ToastService);
  readonly loading = signal(true);
  readonly error = signal('');

  tipos: IncidentTypeProfile[] = [];

  createForm: FormGroup;
  editForm: FormGroup;

  showCreateModal = false;
  editingId: number | null = null;

  constructor(private fb: FormBuilder) {
    this.createForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      descripcion: [''],
    });
    this.editForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      descripcion: [''],
      activo: [true],
    });
  }

  ngOnInit(): void {
    this.cargarTipos();
  }

  async cargarTipos(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      const res = await firstValueFrom(this.api.incidents.listTypes({ size: 100 }));
      this.tipos = res.content;
    } catch {
      this.error.set('Error al cargar los tipos de incidencia.');
      this.toastService.error('Error al cargar tipos de incidencia');
    } finally {
      this.loading.set(false);
    }
  }

  openCreateModal(): void {
    this.createForm.reset({ nombre: '', descripcion: '' });
    this.showCreateModal = true;
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
    this.createForm.reset();
  }

  async createTipo(): Promise<void> {
    if (this.createForm.invalid) return;
    try {
      await firstValueFrom(this.api.incidents.createType({
        nombre: this.createForm.value.nombre,
        descripcion: this.createForm.value.descripcion || null,
      }));
      await this.cargarTipos();
      this.toastService.success('Tipo de incidencia creado');
      this.closeCreateModal();
    } catch {
      this.toastService.error('Error al crear tipo de incidencia');
    }
  }

  startEdit(tipo: IncidentTypeProfile): void {
    this.editingId = tipo.id;
    this.editForm.setValue({
      nombre: tipo.nombre,
      descripcion: tipo.descripcion ?? '',
      activo: tipo.activo,
    });
  }

  cancelEdit(): void {
    this.editingId = null;
    this.editForm.reset();
  }

  async saveEdit(tipo: IncidentTypeProfile): Promise<void> {
    if (this.editForm.invalid) return;
    try {
      await firstValueFrom(this.api.incidents.patchType(tipo.id, {
        nombre: this.editForm.value.nombre,
        descripcion: this.editForm.value.descripcion || null,
      }));
      await this.cargarTipos();
      this.toastService.success('Tipo de incidencia actualizado');
      this.cancelEdit();
    } catch {
      this.toastService.error('Error al actualizar tipo de incidencia');
    }
  }

  async toggleActivo(tipo: IncidentTypeProfile): Promise<void> {
    try {
      const newActive = !tipo.activo;
      await firstValueFrom(this.api.incidents.patchType(tipo.id, {
        activo: newActive,
      }));
      await this.cargarTipos();
      this.toastService.success(newActive ? 'Tipo activado' : 'Tipo desactivado');
    } catch {
      this.toastService.error('Error al cambiar estado del tipo');
    }
  }

  isEditing(tipo: IncidentTypeProfile): boolean {
    return this.editingId === tipo.id;
  }

  formatFecha(iso: string): string {
    const d = new Date(iso);
    return d.toLocaleDateString('es-PE', { year: 'numeric', month: 'short', day: 'numeric' });
  }
}
