import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

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
export class TurnosComponent {
  turnos: Turno[] = [
    {
      id: 1,
      nombre: 'Turno Mañana',
      horarios: [
        { id: 1, inicio: '08:00', fin: '12:00' },
        { id: 2, inicio: '14:00', fin: '18:00' },
      ],
    },
    {
      id: 2,
      nombre: 'Turno Tarde',
      horarios: [
        { id: 3, inicio: '13:00', fin: '17:00' },
        { id: 4, inicio: '18:00', fin: '22:00' },
      ],
    },
    {
      id: 3,
      nombre: 'Turno Noche',
      horarios: [
        { id: 5, inicio: '22:00', fin: '02:00' },
      ],
    },
  ];

  turnoForm: FormGroup;
  editTurnoForm: FormGroup;
  horarioForm: FormGroup;
  editHorarioForm: FormGroup;

  showNewTurnoModal = false;
  editingTurnoId: number | null = null;
  activeHorarioTurnoId: number | null = null;
  editingHorarioKey: string | null = null;

  private nextTurnoId = 4;
  private nextHorarioId = 6;

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

  openNewTurnoModal(): void {
    this.turnoForm.reset();
    this.showNewTurnoModal = true;
  }

  cancelNewTurno(): void {
    this.showNewTurnoModal = false;
    this.turnoForm.reset();
  }

  addTurno(): void {
    if (this.turnoForm.invalid) return;
    this.turnos.push({
      id: this.nextTurnoId++,
      nombre: this.turnoForm.value.nombre,
      horarios: [],
    });
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

  saveEditTurno(turno: Turno): void {
    if (this.editTurnoForm.invalid) return;
    turno.nombre = this.editTurnoForm.value.nombre;
    this.cancelEditTurno();
  }

  deleteTurno(id: number): void {
    this.turnos = this.turnos.filter(t => t.id !== id);
  }

  showAddHorario(turnoId: number): void {
    this.activeHorarioTurnoId = turnoId;
    this.horarioForm.reset();
  }

  cancelAddHorario(): void {
    this.activeHorarioTurnoId = null;
    this.horarioForm.reset();
  }

  addHorario(turnoId: number): void {
    if (this.horarioForm.invalid) return;
    const turno = this.turnos.find(t => t.id === turnoId);
    if (!turno) return;
    turno.horarios.push({
      id: this.nextHorarioId++,
      inicio: this.horarioForm.value.inicio,
      fin: this.horarioForm.value.fin,
    });
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

  saveEditHorario(turnoId: number, horario: Horario): void {
    if (this.editHorarioForm.invalid) return;
    horario.inicio = this.editHorarioForm.value.inicio;
    horario.fin = this.editHorarioForm.value.fin;
    this.cancelEditHorario();
  }

  deleteHorario(turnoId: number, horarioId: number): void {
    const turno = this.turnos.find(t => t.id === turnoId);
    if (!turno) return;
    turno.horarios = turno.horarios.filter(h => h.id !== horarioId);
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
