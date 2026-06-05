import { Component } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

interface Feriado {
  id: number;
  fecha: string;
  nombre: string;
  tipo: 'nacional' | 'institucional';
}

@Component({
  selector: 'app-feriados',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './feriados.html',
  styleUrl: './feriados.css',
})
export class FeriadosComponent {
  feriados: Feriado[] = [
    { id: 1, fecha: '2026-01-01', nombre: 'Año Nuevo', tipo: 'nacional' },
    { id: 2, fecha: '2026-07-28', nombre: 'Fiestas Patrias', tipo: 'nacional' },
    { id: 3, fecha: '2026-12-25', nombre: 'Navidad', tipo: 'nacional' },
    { id: 4, fecha: '2026-09-15', nombre: 'Aniversario Institucional', tipo: 'institucional' },
  ];

  feriadoForm: FormGroup;
  editFeriadoForm: FormGroup;

  showNewForm = false;
  editingFeriadoId: number | null = null;

  private nextId = 5;

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

  openNewForm(): void {
    this.feriadoForm.reset({ tipo: 'nacional' });
    this.showNewForm = true;
  }

  cancelNewForm(): void {
    this.showNewForm = false;
    this.feriadoForm.reset();
  }

  addFeriado(): void {
    if (this.feriadoForm.invalid) return;
    this.feriados.push({
      id: this.nextId++,
      fecha: this.feriadoForm.value.fecha,
      nombre: this.feriadoForm.value.nombre,
      tipo: this.feriadoForm.value.tipo,
    });
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

  saveEdit(feriado: Feriado): void {
    if (this.editFeriadoForm.invalid) return;
    feriado.fecha = this.editFeriadoForm.value.fecha;
    feriado.nombre = this.editFeriadoForm.value.nombre;
    feriado.tipo = this.editFeriadoForm.value.tipo;
    this.cancelEdit();
  }

  deleteFeriado(id: number): void {
    this.feriados = this.feriados.filter(f => f.id !== id);
  }
}
