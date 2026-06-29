import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../../../../api/services/api.service';
import { ToastService } from '../../../../services/toast.service';
import type { NonWorkingDayProfile } from '../../../../api/types';

interface Feriado {
  id: number;
  fecha: string;
  nombre: string;
  tipo: string;
}

@Component({
  selector: 'app-feriados',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './feriados.html',
  styleUrl: './feriados.css',
})
export class FeriadosComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toastService = inject(ToastService);
  readonly loading = signal(true);
  readonly error = signal('');

  feriados: Feriado[] = [];

  feriadoForm: FormGroup;
  editFeriadoForm: FormGroup;

  showNewForm = false;
  editingFeriadoId: number | null = null;

  constructor(private fb: FormBuilder) {
    this.feriadoForm = this.fb.group({
      fecha: ['', [Validators.required]],
      nombre: ['', [Validators.required, Validators.minLength(3)]],
      tipo: ['nacional', [Validators.required]],
    });
    this.editFeriadoForm = this.fb.group({
      fecha: ['', [Validators.required]],
      nombre: ['', [Validators.required, Validators.minLength(3)]],
      tipo: ['nacional', [Validators.required]],
    });
  }

  ngOnInit(): void {
    this.cargarFeriados();
  }

  async cargarFeriados(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      const res = await firstValueFrom(this.api.corehr.listNonWorkingDays({ size: 100 }));
      this.feriados = res.content.map(d => ({
        id: d.id,
        fecha: d.date,
        nombre: d.description ?? '',
        tipo: 'nacional',
      }));
    } catch {
      this.error.set('Error al cargar feriados');
      this.toastService.error('Error al cargar feriados');
    } finally {
      this.loading.set(false);
    }
  }

  openNewForm(): void {
    this.feriadoForm.reset({ tipo: 'nacional' });
    this.showNewForm = true;
  }

  cancelNewForm(): void {
    this.showNewForm = false;
    this.feriadoForm.reset();
  }

  async addFeriado(): Promise<void> {
    if (this.feriadoForm.invalid) return;
    try {
      await firstValueFrom(this.api.corehr.createNonWorkingDay({
        date: this.feriadoForm.value.fecha,
        description: this.feriadoForm.value.nombre,
      }));
      await this.cargarFeriados();
      this.toastService.success('Feriado creado');
    } catch {
      this.toastService.error('Error al crear feriado');
    }
    this.cancelNewForm();
  }

  startEdit(feriado: Feriado): void {
    this.editingFeriadoId = feriado.id;
    this.editFeriadoForm.setValue({
      fecha: feriado.fecha,
      nombre: feriado.nombre,
      tipo: feriado.tipo,
    });
  }

  cancelEdit(): void {
    this.editingFeriadoId = null;
    this.editFeriadoForm.reset();
  }

  async saveEdit(feriado: Feriado): Promise<void> {
    if (this.editFeriadoForm.invalid) return;
    try {
      await firstValueFrom(this.api.corehr.patchNonWorkingDay(feriado.id, {
        date: this.editFeriadoForm.value.fecha,
        description: this.editFeriadoForm.value.nombre,
      }));
      await this.cargarFeriados();
      this.toastService.success('Feriado actualizado');
    } catch {
      this.toastService.error('Error al actualizar feriado');
    }
    this.cancelEdit();
  }

  async deleteFeriado(id: number): Promise<void> {
    try {
      await firstValueFrom(this.api.corehr.deleteNonWorkingDay(id));
      await this.cargarFeriados();
      this.toastService.success('Feriado eliminado');
    } catch {
      this.toastService.error('Error al eliminar feriado');
    }
  }
}
