import { Component, computed, signal, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../../../../api/services/api.service';
import { ToastService } from '../../../../services/toast.service';
import type { TenantUserProfile, BulkAssignUserSchedulesResponse, UserScheduleProfile } from '../../../../api/types';

interface Asignacion {
  id: number;
  tenant_user_id: number;
  trabajador: string;
  sede: string;
  area: string;
  departamento: string;
  turno: string;
  horario: string;
}

interface TurnoOption {
  id: number;
  nombre: string;
  horarios: { id: number; label: string }[];
}

interface WorkerCandidate extends TenantUserProfile {
  selected: boolean;
}

@Component({
  selector: 'app-asignacion',
  imports: [ReactiveFormsModule],
  templateUrl: './asignacion.html',
  styleUrl: './asignacion.css',
})
export class AsignacionComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toastService = inject(ToastService);
  readonly loading = signal(true);
  readonly error = signal('');

  searchTerm = signal('');
  sedeFilter = signal('');
  areaFilter = signal('');

  showModal = false;
  showBulkModal = false;
  selectedTurnoHorarios: { id: number; label: string }[] = [];

  turnosDisponibles: TurnoOption[] = [];
  workers: TenantUserProfile[] = [];

  readonly candidates = signal<WorkerCandidate[]>([]);
  readonly candidatesSearch = signal('');
  readonly selectedCount = computed(() => this.candidates().filter(c => c.selected).length);
  readonly filteredCandidates = computed(() => {
    const s = this.candidatesSearch().toLowerCase();
    return this.candidates().filter(c => {
      const full = `${c.persona.name} ${c.persona.father_surname} ${c.persona.mother_surname}`.toLowerCase();
      return !s || full.includes(s);
    });
  });

  readonly asignaciones = signal<Asignacion[]>([]);

  readonly sedes = computed(() => {
    const set = new Set<string>();
    this.asignaciones().forEach(a => { if (a.sede) set.add(a.sede); });
    return [...set].sort();
  });

  readonly areas = computed(() => {
    const set = new Set<string>();
    this.asignaciones().forEach(a => { if (a.area) set.add(a.area); });
    return [...set].sort();
  });

  readonly filteredAsignaciones = computed(() => {
    const search = this.searchTerm().toLowerCase();
    const sede = this.sedeFilter();
    const area = this.areaFilter();
    return this.asignaciones().filter(a => {
      const matchSearch = !search || a.trabajador.toLowerCase().includes(search) || a.turno.toLowerCase().includes(search);
      const matchSede = !sede || a.sede === sede;
      const matchArea = !area || a.area === area;
      return matchSearch && matchSede && matchArea;
    });
  });

  asignacionForm: FormGroup;
  bulkForm: FormGroup;

  constructor(private fb: FormBuilder) {
    this.asignacionForm = this.fb.group({
      trabajadorId: ['', [Validators.required]],
      turnoId: ['', [Validators.required]],
      horarioId: ['', [Validators.required]],
    });
    this.bulkForm = this.fb.group({
      turnoId: ['', [Validators.required]],
      horarioId: ['', [Validators.required]],
      description: [''],
    });
  }

  ngOnInit(): void {
    this.cargarDatos();
  }

  async cargarDatos(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      const [shiftsRes, userSchedulesRes, usersRes] = await Promise.all([
        firstValueFrom(this.api.horarios.listShifts({ size: 50 })),
        firstValueFrom(this.api.horarios.listUserSchedules({ size: 100 })),
        firstValueFrom(this.api.users.list({ size: 100 })),
      ]);

      this.workers = usersRes.content;

      this.turnosDisponibles = shiftsRes.content.map(s => ({
        id: s.id,
        nombre: s.name,
        horarios: (s.schedules ?? []).map(h => ({
          id: h.id,
          label: `${h.entry_time.slice(0, 5)} – ${h.departure_time.slice(0, 5)}`,
        })),
      }));

      this.asignaciones.set(userSchedulesRes.content.map(us => this.toAsignacion(us)));
    } catch {
      this.error.set('Error al cargar datos');
      this.toastService.error('Error al cargar datos');
    } finally {
      this.loading.set(false);
    }
  }

  private toAsignacion(us: UserScheduleProfile): Asignacion {
    const tu = us.tenant_user;
    const trabajador = tu
      ? `${tu.name} ${tu.father_surname} ${tu.mother_surname ?? ''}`.trim()
      : `Usuario #${us.tenant_user_id}`;
    const sched = us.schedule;
    const shift = this.turnosDisponibles.find(t =>
      t.horarios.some(h => h.id === sched?.id));
    return {
      id: us.id,
      tenant_user_id: us.tenant_user_id,
      trabajador,
      sede: tu?.sede ?? '',
      area: tu?.area ?? '',
      departamento: tu?.department ?? '',
      turno: shift?.nombre ?? '',
      horario: sched ? `${sched.entry_time.slice(0, 5)} – ${sched.departure_time.slice(0, 5)}` : '',
    };
  }

  onSearch(value: string): void {
    this.searchTerm.set(value);
  }

  onSedeFilter(value: string): void {
    this.sedeFilter.set(value);
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
    this.selectedTurnoHorarios = [];
  }

  openBulkModal(): void {
    this.bulkForm.reset();
    this.selectedTurnoHorarios = [];
    this.candidates.set(this.workers.map(w => ({ ...w, selected: false })));
    this.candidatesSearch.set('');
    this.showBulkModal = true;
  }

  closeBulkModal(): void {
    this.showBulkModal = false;
    this.bulkForm.reset();
    this.selectedTurnoHorarios = [];
    this.candidates.set([]);
  }

  onTurnoChange(form: FormGroup): void {
    const turnoId = Number(form.get('turnoId')?.value);
    const turno = this.turnosDisponibles.find(t => t.id === turnoId);
    this.selectedTurnoHorarios = turno?.horarios ?? [];
    form.get('horarioId')?.setValue('');
  }

  candidatesSearchInput(value: string): void {
    this.candidatesSearch.set(value);
  }

  toggleCandidate(c: WorkerCandidate): void {
    this.candidates.update(items =>
      items.map(item => item === c ? { ...item, selected: !item.selected } : item)
    );
  }

  selectAllCandidates(): void {
    const selectedIds = new Set(this.filteredCandidates().map(c => c.id));
    this.candidates.update(items =>
      items.map(item => selectedIds.has(item.id) ? { ...item, selected: true } : item)
    );
  }

  clearCandidateSelection(): void {
    this.candidates.update(items =>
      items.map(item => ({ ...item, selected: false }))
    );
  }

  async submitAsignacion(): Promise<void> {
    if (this.asignacionForm.invalid) return;
    const { trabajadorId, horarioId } = this.asignacionForm.value;
    const turno = this.turnosDisponibles.find(t => t.id === Number(this.asignacionForm.value.turnoId));
    const horario = turno?.horarios.find(h => h.id === Number(horarioId));
    if (!turno || !horario) return;

    try {
      const [labelStart, labelEnd] = horario.label.split(' – ');
      await firstValueFrom(this.api.horarios.createUserSchedule({
        tenant_user_id: Number(trabajadorId),
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

  async submitBulk(): Promise<void> {
    if (this.bulkForm.invalid) return;
    const selectedIds = this.candidates().filter(c => c.selected).map(c => c.id);
    if (selectedIds.length === 0) {
      this.toastService.error('Seleccione al menos un trabajador');
      return;
    }
    const scheduleId = Number(this.bulkForm.value.horarioId);
    const description: string | null = this.bulkForm.value.description?.trim() || null;
    try {
      const res: BulkAssignUserSchedulesResponse = await firstValueFrom(
        this.api.horarios.bulkAssignUserSchedules({
          tenant_user_ids: selectedIds,
          schedule_id: scheduleId,
          description,
        }),
      );
      const created = res.items.filter(i => i.status === 'CREATED').length;
      const skipped = res.items.filter(i => i.status === 'SKIPPED').length;
      const errors = res.items.filter(i => i.status === 'ERROR').length;
      let msg = `${created} asignación(es) creada(s)`;
      if (skipped) msg += `, ${skipped} omitida(s)`;
      if (errors) msg += `, ${errors} con error`;
      if (errors) this.toastService.error(msg);
      else if (created) this.toastService.success(msg);
      else this.toastService.info(msg || 'Sin cambios');
      await this.cargarDatos();
    } catch {
      this.toastService.error('Error al asignar masivamente');
    }
    this.closeBulkModal();
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
