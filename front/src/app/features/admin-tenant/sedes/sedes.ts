import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ToastService } from '../../../services/toast.service';
import { ModalService } from '../../../services/modal.service';
import { OrgService } from '../../../api/services/org.service';

interface Departamento {
  id: number;
  nombre: string;
  descripcion: string;
}

interface Area {
  id: number;
  nombre: string;
  descripcion: string;
  departamentos: Departamento[];
}

interface Sede {
  id: number;
  nombre: string;
  descripcion: string;
  estado: 'activo' | 'inactivo';
  areas: Area[];
}

@Component({
  selector: 'app-sedes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sedes.html',
  styleUrl: './sedes.css',
})
export class Sedes implements OnInit {

  readonly loading = signal(false);
  readonly error = signal('');

  private readonly toastService = inject(ToastService);
  private readonly modalService = inject(ModalService);
  private readonly orgService = inject(OrgService);

  sedes: Sede[] = [];

  ngOnInit(): void {
    this.cargarSedes();
  }

  cargarSedes(): void {
    this.loading.set(true);
    this.error.set('');

    forkJoin({
      branches: this.orgService.listBranches({ size: 200 }),
      areas:    this.orgService.listAreas({ size: 1000 }),
      depts:    this.orgService.listDepartments({ size: 2000 }),
    }).subscribe({
      next: ({ branches, areas, depts }) => {
        const areasByBranch = new Map<number, Area[]>();
        for (const a of areas.content) {
          const list = areasByBranch.get(a.branchId) ?? [];
          list.push({ id: a.id, nombre: a.name, descripcion: a.description ?? '', departamentos: [] });
          areasByBranch.set(a.branchId, list);
        }

        const deptosByArea = new Map<number, Departamento[]>();
        for (const d of depts.content) {
          const list = deptosByArea.get(d.areaId) ?? [];
          list.push({ id: d.id, nombre: d.name, descripcion: d.description ?? '' });
          deptosByArea.set(d.areaId, list);
        }

        this.sedes = branches.content.map(b => {
          const branchAreas = (areasByBranch.get(b.id) ?? []).map(a => ({
            ...a,
            departamentos: deptosByArea.get(a.id) ?? [],
          }));
          return {
            id: b.id,
            nombre: b.name,
            descripcion: b.description ?? '',
            estado: b.state ? 'activo' : 'inactivo',
            areas: branchAreas,
          };
        });
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar las sedes');
        this.loading.set(false);
      },
    });
  }

  get totalSedes(): number {
    return this.sedes.length;
  }

  get totalAreas(): number {
    return this.sedes.reduce((t, s) => t + s.areas.length, 0);
  }

  get totalDepartamentos(): number {
    return this.sedes.reduce((t, s) =>
      t + s.areas.reduce((sub, a) => sub + a.departamentos.length, 0), 0);
  }

  totalDepartamentosPorSede(sede: Sede): number {
    return sede.areas.reduce((t, a) => t + a.departamentos.length, 0);
  }

  sedeIdParaDepto: number = 0;

  get areasDeSedeSeleccionada(): Area[] {
    return this.sedes.find(s => s.id === this.sedeIdParaDepto)?.areas ?? [];
  }

  sedeForm = {
    id: 0,
    nombre: '',
    descripcion: '',
    estado: 'activo' as 'activo' | 'inactivo',
  };
  sedeEditando = false;

  areaForm = { sedeId: 0, id: 0, nombre: '', descripcion: '' };
  areaEditando = false;

  deptoForm = { areaId: 0, id: 0, nombre: '', descripcion: '' };
  deptoEditando = false;

  abrirModalSede(): void {
    this.sedeEditando = false;
    this.sedeForm = { id: 0, nombre: '', descripcion: '', estado: 'activo' };
    this.modalService.show('modalSede');
  }

  editarSede(sede: Sede): void {
    this.sedeEditando = true;
    this.sedeForm = { id: sede.id, nombre: sede.nombre, descripcion: sede.descripcion, estado: sede.estado };
    this.modalService.show('modalSede');
  }

  cancelarModalSede(): void {
    this.cerrarModal('modalSede');
  }

  guardarSede(): void {
    if (!this.sedeForm.nombre) {
      this.toastService.error('Por favor complete el nombre de la sede');
      return;
    }
    this.loading.set(true);
    const body = { name: this.sedeForm.nombre, description: this.sedeForm.descripcion };

    const req$ = this.sedeEditando
      ? this.orgService.updateBranch(this.sedeForm.id, body)
      : this.orgService.createBranch(body);

    req$.subscribe({
      next: () => {
        this.cerrarModal('modalSede');
        this.cargarSedes();
      },
      error: () => {
        this.toastService.error('Error al guardar la sede');
        this.loading.set(false);
      },
    });
  }

  eliminarSede(id: number): void {
    const sede = this.sedes.find(s => s.id === id);
    if (!sede) return;
    this.orgService.deleteBranch(id).subscribe({
      next: () => {
        this.toastService.success(`Sede "${sede.nombre}" eliminada`);
        this.cargarSedes();
      },
      error: () => this.toastService.error('Error al eliminar la sede'),
    });
  }

  abrirModalArea(sedeId: number): void {
    this.areaEditando = false;
    this.areaForm = { sedeId, id: 0, nombre: '', descripcion: '' };
    this.modalService.show('modalArea');
  }

  editarArea(sedeId: number, areaId: number): void {
    const area = this.sedes.find(s => s.id === sedeId)?.areas.find(a => a.id === areaId);
    if (!area) return;
    this.areaEditando = true;
    this.areaForm = { sedeId, id: areaId, nombre: area.nombre, descripcion: area.descripcion };
    this.modalService.show('modalArea');
  }

  guardarArea(): void {
    if (!this.areaForm.nombre) {
      this.toastService.error('El nombre del área es obligatorio');
      return;
    }
    this.loading.set(true);
    const body = { branchId: this.areaForm.sedeId, name: this.areaForm.nombre, description: this.areaForm.descripcion };

    const req$ = this.areaEditando
      ? this.orgService.updateArea(this.areaForm.id, { name: body.name, description: body.description })
      : this.orgService.createArea(body);

    req$.subscribe({
      next: () => {
        this.cerrarModal('modalArea');
        this.cargarSedes();
      },
      error: () => {
        this.toastService.error('Error al guardar el área');
        this.loading.set(false);
      },
    });
  }

  cancelarModalArea(): void {
    this.cerrarModal('modalArea');
  }

  eliminarArea(sedeId: number, areaId: number): void {
    this.orgService.deleteArea(areaId).subscribe({
      next: () => this.cargarSedes(),
      error: () => this.toastService.error('Error al eliminar el área'),
    });
  }

  abrirModalDepartamento(areaId: number, sedeId: number): void {
    this.deptoEditando = false;
    this.sedeIdParaDepto = sedeId;
    this.deptoForm = { areaId, id: 0, nombre: '', descripcion: '' };
    this.modalService.show('modalDepartamento');
  }

  editarDepartamento(sedeId: number, areaId: number, deptoId: number): void {
    const area = this.sedes.find(s => s.id === sedeId)?.areas.find(a => a.id === areaId);
    const depto = area?.departamentos.find(d => d.id === deptoId);
    if (!depto) return;
    this.deptoEditando = true;
    this.sedeIdParaDepto = sedeId;
    this.deptoForm = { areaId, id: deptoId, nombre: depto.nombre, descripcion: depto.descripcion };
    this.modalService.show('modalDepartamento');
  }

  guardarDepartamento(): void {
    if (!this.deptoForm.nombre) {
      this.toastService.error('El nombre del departamento es obligatorio');
      return;
    }
    this.loading.set(true);
    const body = { areaId: this.deptoForm.areaId, name: this.deptoForm.nombre, description: this.deptoForm.descripcion };

    const req$ = this.deptoEditando
      ? this.orgService.updateDepartment(this.deptoForm.id, { name: body.name, description: body.description })
      : this.orgService.createDepartment(body);

    req$.subscribe({
      next: () => {
        this.cerrarModal('modalDepartamento');
        this.cargarSedes();
      },
      error: () => {
        this.toastService.error('Error al guardar el departamento');
        this.loading.set(false);
      },
    });
  }

  cancelarModalDepartamento(): void {
    this.cerrarModal('modalDepartamento');
  }

  eliminarDepartamento(sedeId: number, areaId: number, deptoId: number): void {
    this.orgService.deleteDepartment(deptoId).subscribe({
      next: () => this.cargarSedes(),
      error: () => this.toastService.error('Error al eliminar el departamento'),
    });
  }

  onSedeChangeForArea(): void {
    this.areaForm.sedeId = Number(this.areaForm.sedeId);
  }

  onSedeChangeForDepto(sedeId: number): void {
    const sede = this.sedes.find(s => s.id === sedeId);
    this.deptoForm.areaId = sede?.areas[0]?.id ?? 0;
  }

  private cerrarModal(modalId: string): void {
    this.modalService.hide(modalId);
  }
}
