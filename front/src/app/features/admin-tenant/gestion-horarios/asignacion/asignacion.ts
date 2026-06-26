import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

interface Asignacion {
  id: number;
  trabajador: string;
  area: string;
  turno: string;
  horario: string;
}

interface TurnoOption {
  id: number;
  nombre: string;
  horarios: { id: number; label: string }[];
}

@Component({
  selector: 'app-asignacion',
  imports: [ReactiveFormsModule, FormsModule],
  templateUrl: './asignacion.html',
  styleUrl: './asignacion.css',
})
export class AsignacionComponent {
  searchTerm = signal('');
  areaFilter = signal('');
  showModal = false;
  selectedTurnoHorarios: { id: number; label: string }[] = [];

  readonly areas = ['Administración', 'Ventas', 'Producción', 'Logística', 'Sistemas', 'RRHH'];

  readonly turnosDisponibles: TurnoOption[] = [
    { id: 1, nombre: 'Turno Mañana', horarios: [{ id: 1, label: '08:00 – 12:00' }, { id: 2, label: '14:00 – 18:00' }] },
    { id: 2, nombre: 'Turno Tarde', horarios: [{ id: 3, label: '13:00 – 17:00' }, { id: 4, label: '18:00 – 22:00' }] },
    { id: 3, nombre: 'Turno Noche', horarios: [{ id: 5, label: '22:00 – 02:00' }] },
  ];

  readonly asignaciones = signal<Asignacion[]>([
    { id: 1, trabajador: 'María García', area: 'Administración', turno: 'Turno Mañana', horario: '08:00 – 12:00' },
    { id: 2, trabajador: 'Carlos López', area: 'Ventas', turno: 'Turno Tarde', horario: '13:00 – 17:00' },
    { id: 3, trabajador: 'Ana Martínez', area: 'Producción', turno: 'Turno Noche', horario: '22:00 – 02:00' },
    { id: 4, trabajador: 'Pedro Rodríguez', area: 'Logística', turno: 'Turno Mañana', horario: '14:00 – 18:00' },
    { id: 5, trabajador: 'Laura Sánchez', area: 'Sistemas', turno: 'Turno Tarde', horario: '18:00 – 22:00' },
  ]);

  readonly filteredAsignaciones = computed(() => {
    const search = this.searchTerm().toLowerCase();
    const area = this.areaFilter();
    return this.asignaciones().filter(a => {
      const matchSearch = !search || a.trabajador.toLowerCase().includes(search) || a.turno.toLowerCase().includes(search);
      const matchArea = !area || a.area === area;
      return matchSearch && matchArea;
    });
  });

  asignacionForm: FormGroup;
  private nextId = 6;

  constructor(private fb: FormBuilder) {
    this.asignacionForm = this.fb.group({
      trabajador: ['', [Validators.required, Validators.minLength(3)]],
      area: ['', [Validators.required]],
      turnoId: ['', [Validators.required]],
      horarioId: ['', [Validators.required]],
    });
  }

  onSearch(value: string): void {
    this.searchTerm.set(value);
  }

  onAreaFilter(value: string): void {
    this.areaFilter.set(value);
  }

  openModal(): void {
    this.asignacionForm.reset();
    this.selectedTurnoHorarios = [];
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.asignacionForm.reset();
  }

  onTurnoChange(): void {
    const turnoId = Number(this.asignacionForm.get('turnoId')?.value);
    const turno = this.turnosDisponibles.find(t => t.id === turnoId);
    this.selectedTurnoHorarios = turno?.horarios ?? [];
    this.asignacionForm.get('horarioId')?.setValue('');
  }

  submitAsignacion(): void {
    if (this.asignacionForm.invalid) return;
    const { trabajador, area, turnoId, horarioId } = this.asignacionForm.value;
    const turno = this.turnosDisponibles.find(t => t.id === Number(turnoId));
    const horario = turno?.horarios.find(h => h.id === Number(horarioId));
    if (!turno || !horario) return;

    this.asignaciones.update(list => [...list, {
      id: this.nextId++,
      trabajador,
      area,
      turno: turno.nombre,
      horario: horario.label,
    }]);
    this.closeModal();
  }

  deleteAsignacion(id: number): void {
    this.asignaciones.update(list => list.filter(a => a.id !== id));
  }
}
