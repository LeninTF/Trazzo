import { Component, computed, signal, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../../../../api/services/api.service';
import { ToastService } from '../../../../services/toast.service';

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
export class AsignacionComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toastService = inject(ToastService);
  readonly loading = signal(true);
  readonly error = signal('');

  searchTerm = signal('');
  areaFilter = signal('');
  showModal = false;
  selectedTurnoHorarios: { id: number; label: string }[] = [];

  readonly areas = ['Administración', 'Ventas', 'Producción', 'Logística', 'Sistemas', 'RRHH'];

  turnosDisponibles: TurnoOption[] = [];

  readonly asignaciones = signal<Asignacion[]>([]);

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

  constructor(private fb: FormBuilder) {
    this.asignacionForm = this.fb.group({
      trabajador: ['', [Validators.required, Validators.minLength(3)]],
      area: ['', [Validators.required]],
      turnoId: ['', [Validators.required]],
      horarioId: ['', [Validators.required]],
    });
  }

  ngOnInit(): void {
    this.cargarDatos();
  }

  async cargarDatos(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      const [shiftsRes, userSchedulesRes] = await Promise.all([
        firstValueFrom(this.api.horarios.listShifts({ size: 50 })),
        firstValueFrom(this.api.horarios.listUserSchedules({ size: 100 })),
      ]);

      this.turnosDisponibles = shiftsRes.content.map(s => ({
        id: s.id,
        nombre: s.name,
        horarios: (s.schedules ?? []).map(h => ({
          id: h.id,
          label: `${h.entry_time.slice(0, 5)} – ${h.departure_time.slice(0, 5)}`,
        })),
      }));

      const allSchedules = shiftsRes.content.flatMap(s =>
        (s.schedules ?? []).map(h => ({ shiftId: s.id, shiftName: s.name, ...h }))
      );

      this.asignaciones.set(userSchedulesRes.content.map(us => {
        const sched = allSchedules.find(s => s.id === us.schedule_id);
        return {
          id: us.id,
          trabajador: `Usuario #${us.tenant_user_id}`,
          area: '',
          turno: sched?.shiftName ?? '',
          horario: sched ? `${sched.entry_time.slice(0, 5)} – ${sched.departure_time.slice(0, 5)}` : '',
        };
      }));
    } catch {
      this.error.set('Error al cargar datos');
      this.toastService.error('Error al cargar datos');
    } finally {
      this.loading.set(false);
    }
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

  async submitAsignacion(): Promise<void> {
    if (this.asignacionForm.invalid) return;
    const { turnoId, horarioId } = this.asignacionForm.value;
    const turno = this.turnosDisponibles.find(t => t.id === Number(turnoId));
    const horario = turno?.horarios.find(h => h.id === Number(horarioId));
    if (!turno || !horario) return;

    try {
      const [labelStart, labelEnd] = horario.label.split(' – ');
      await firstValueFrom(this.api.horarios.createUserSchedule({
        tenant_user_id: 0,
        schedule_id: Number(horarioId),
        entry_time: labelStart,
        departure_time: labelEnd,
      }));
      await this.cargarDatos();
      this.toastService.success('Asignación creada');
    } catch {
      this.toastService.error('Error al crear asignación');
    }
    this.closeModal();
  }

  async deleteAsignacion(id: number): Promise<void> {
    try {
      await firstValueFrom(this.api.horarios.deleteUserSchedule(id));
      await this.cargarDatos();
      this.toastService.success('Asignación eliminada');
    } catch {
      this.toastService.error('Error al eliminar asignación');
    }
  }
}
