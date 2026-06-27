import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../../../../api/services/api.service';
import { ToastService } from '../../../../services/toast.service';

interface Horario {
  id: number;
  inicio: string;
  fin: string;
}

interface Turno {
  id: number;
  nombre: string;
  horarios: Horario[];
}

@Component({
  selector: 'app-turnos',
  imports: [ReactiveFormsModule],
  templateUrl: './turnos.html',
  styleUrl: './turnos.css',
})
export class TurnosComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toastService = inject(ToastService);
  readonly loading = signal(true);
  readonly error = signal('');

  turnos: Turno[] = [];

  turnoForm: FormGroup;
  editTurnoForm: FormGroup;
  horarioForm: FormGroup;
  editHorarioForm: FormGroup;

  showNewTurnoModal = false;
  editingTurnoId: number | null = null;
  activeHorarioTurnoId: number | null = null;
  editingHorarioKey: string | null = null;

  constructor(private fb: FormBuilder) {
    this.turnoForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3)]],
    });
    this.editTurnoForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3)]],
    });
    this.horarioForm = this.fb.group({
      inicio: ['', [Validators.required, Validators.pattern(/^\d{2}:\d{2}$/)]],
      fin: ['', [Validators.required, Validators.pattern(/^\d{2}:\d{2}$/)]],
    });
    this.editHorarioForm = this.fb.group({
      inicio: ['', [Validators.required, Validators.pattern(/^\d{2}:\d{2}$/)]],
      fin: ['', [Validators.required, Validators.pattern(/^\d{2}:\d{2}$/)]],
    });
  }

  async ngOnInit(): Promise<void> {
    await this.cargarTurnos();
  }

  async cargarTurnos(): Promise<void> {
    this.loading.set(true);
    try {
      const res = await firstValueFrom(this.api.horarios.listShifts({ size: 50 }));
      this.turnos = res.content.map(s => ({
        id: s.id,
        nombre: s.name,
        horarios: (s.schedules ?? []).map(h => ({
          id: h.id,
          inicio: h.entry_time.slice(0, 5),
          fin: h.departure_time.slice(0, 5),
        })),
      }));
    } catch {
      this.error.set('Error al cargar turnos');
      this.toastService.error('Error al cargar turnos');
    } finally {
      this.loading.set(false);
    }
  }

  openNewTurnoModal(): void {
    this.turnoForm.reset();
    this.showNewTurnoModal = true;
  }

  cancelNewTurno(): void {
    this.showNewTurnoModal = false;
    this.turnoForm.reset();
  }

  async addTurno(): Promise<void> {
    if (this.turnoForm.invalid) return;
    try {
      await firstValueFrom(this.api.horarios.createShift({ name: this.turnoForm.value.nombre }));
      await this.cargarTurnos();
      this.toastService.success('Turno creado');
    } catch {
      this.toastService.error('Error al crear turno');
    }
    this.cancelNewTurno();
  }

  startEditTurno(turno: Turno): void {
    this.editingTurnoId = turno.id;
    this.editTurnoForm.setValue({ nombre: turno.nombre });
  }

  cancelEditTurno(): void {
    this.editingTurnoId = null;
    this.editTurnoForm.reset();
  }

  async saveEditTurno(turno: Turno): Promise<void> {
    if (this.editTurnoForm.invalid) return;
    try {
      await firstValueFrom(this.api.horarios.patchShift(turno.id, { name: this.editTurnoForm.value.nombre }));
      await this.cargarTurnos();
      this.toastService.success('Turno actualizado');
    } catch {
      this.toastService.error('Error al actualizar turno');
    }
    this.cancelEditTurno();
  }

  async deleteTurno(id: number): Promise<void> {
    try {
      await firstValueFrom(this.api.horarios.deleteShift(id));
      await this.cargarTurnos();
      this.toastService.success('Turno eliminado');
    } catch {
      this.toastService.error('Error al eliminar turno');
    }
  }

  showAddHorario(turnoId: number): void {
    this.activeHorarioTurnoId = turnoId;
    this.horarioForm.reset();
  }

  cancelAddHorario(): void {
    this.activeHorarioTurnoId = null;
    this.horarioForm.reset();
  }

  async addHorario(turnoId: number): Promise<void> {
    if (this.horarioForm.invalid) return;
    try {
      await firstValueFrom(this.api.horarios.createSchedule({
        shift_id: turnoId,
        name: `${this.horarioForm.value.inicio}-${this.horarioForm.value.fin}`,
        entry_time: this.horarioForm.value.inicio,
        departure_time: this.horarioForm.value.fin,
      }));
      await this.cargarTurnos();
      this.toastService.success('Horario agregado');
    } catch {
      this.toastService.error('Error al agregar horario');
    }
    this.cancelAddHorario();
  }

  startEditHorario(turnoId: number, horario: Horario): void {
    this.editingHorarioKey = `${turnoId}-${horario.id}`;
    this.editHorarioForm.setValue({ inicio: horario.inicio, fin: horario.fin });
  }

  cancelEditHorario(): void {
    this.editingHorarioKey = null;
    this.editHorarioForm.reset();
  }

  async saveEditHorario(turnoId: number, horario: Horario): Promise<void> {
    if (this.editHorarioForm.invalid) return;
    try {
      await firstValueFrom(this.api.horarios.patchSchedule(horario.id, {
        entry_time: this.editHorarioForm.value.inicio,
        departure_time: this.editHorarioForm.value.fin,
      }));
      await this.cargarTurnos();
      this.toastService.success('Horario actualizado');
    } catch {
      this.toastService.error('Error al actualizar horario');
    }
    this.cancelEditHorario();
  }

  async deleteHorario(turnoId: number, horarioId: number): Promise<void> {
    try {
      await firstValueFrom(this.api.horarios.deleteSchedule(horarioId));
      await this.cargarTurnos();
      this.toastService.success('Horario eliminado');
    } catch {
      this.toastService.error('Error al eliminar horario');
    }
  }

  isEditingHorario(turnoId: number, horarioId: number): boolean {
    return this.editingHorarioKey === `${turnoId}-${horarioId}`;
  }

  calcularDuracion(inicio: string, fin: string): string {
    const [h1, m1] = inicio.split(':').map(Number);
    const [h2, m2] = fin.split(':').map(Number);
    let minutos = (h2 * 60 + m2) - (h1 * 60 + m1);
    if (minutos < 0) minutos += 24 * 60;
    const h = Math.floor(minutos / 60);
    const m = minutos % 60;
    return m > 0 ? `${h}h ${m}m` : `${h}h`;
  }
}
