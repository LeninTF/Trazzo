import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

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
  usuarios: number;
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
export class Sedes {

  sedes: Sede[] = [
    {
      id: 1,
      nombre: 'Centro Logístico Valencia',
      descripcion: 'Centro de operaciones logísticas en la región de Valencia.',
      usuarios: 120,
      estado: 'activo',
      areas: [
        {
          id: 1,
          nombre: 'Operaciones',
          descripcion: 'Área encargada de la gestión operativa y de almacén.',
          departamentos: [
            { id: 1, nombre: 'Operaciones de Alota', descripcion: 'Gestión de alota y distribución.' },
            { id: 2, nombre: 'Control de Inventario', descripcion: 'Control y seguimiento de inventario.' }
          ]
        },
        {
          id: 2,
          nombre: 'Administración',
          descripcion: 'Área administrativa y de gestión de personal.',
          departamentos: [
            { id: 3, nombre: 'Recursos Humanos', descripcion: 'Gestión de personal y nóminas.' }
          ]
        }
      ]
    },
    {
      id: 2,
      nombre: 'Corporativo Madrid',
      descripcion: 'Sede central de la corporación en Madrid.',
      usuarios: 340,
      estado: 'activo',
      areas: [
        {
          id: 3,
          nombre: 'Comercial',
          descripcion: 'Área de ventas y desarrollo de negocio.',
          departamentos: [
            { id: 4, nombre: 'Ventas Globales', descripcion: 'Ventas internacionales y nacionales.' },
            { id: 5, nombre: 'Marketing', descripcion: 'Estrategias de marketing y publicidad.' }
          ]
        },
        {
          id: 4,
          nombre: 'Tecnología',
          descripcion: 'Área de ingeniería y desarrollo tecnológico.',
          departamentos: [
            { id: 6, nombre: 'Ingeniería & DevOps', descripcion: 'Desarrollo de software y operaciones.' },
            { id: 7, nombre: 'Data Science', descripcion: 'Análisis de datos e inteligencia de negocio.' }
          ]
        }
      ]
    },
    {
      id: 3,
      nombre: 'Hub Barcelona',
      descripcion: 'Centro de innovación y atención al cliente en Barcelona.',
      usuarios: 200,
      estado: 'activo',
      areas: [
        {
          id: 5,
          nombre: 'Atención al Cliente',
          descripcion: 'Área de soporte y atención al cliente.',
          departamentos: [
            { id: 8, nombre: 'Atención al Cliente', descripcion: 'Soporte telefónico y digital.' },
            { id: 9, nombre: 'UX/UI Design Lab', descripcion: 'Diseño de experiencia de usuario.' }
          ]
        }
      ]
    }
  ];

  get totalSedes(): number {
    return this.sedes.length;
  }

  get totalAreas(): number {
    return this.sedes.reduce((total, sede) => total + sede.areas.length, 0);
  }

  get totalDepartamentos(): number {
    return this.sedes.reduce((total, sede) =>
      total + sede.areas.reduce((sub, area) => sub + area.departamentos.length, 0), 0);
  }

  get totalUsuarios(): number {
    return this.sedes.reduce((total, sede) => total + sede.usuarios, 0);
  }

  totalDepartamentosPorSede(sede: Sede): number {
    return sede.areas.reduce((total, area) => total + area.departamentos.length, 0);
  }

  sedeIdParaDepto: number = 0;

  get areasDeSedeSeleccionada(): Area[] {
    const sede = this.sedes.find(s => s.id === this.sedeIdParaDepto);
    return sede ? sede.areas : [];
  }

  sedeForm = {
    id: 0,
    nombre: '',
    descripcion: '',
    estado: 'activo' as 'activo' | 'inactivo'
  };
  sedeEditando: boolean = false;

  areaForm = {
    sedeId: 0,
    id: 0,
    nombre: '',
    descripcion: ''
  };
  areaEditando: boolean = false;
  private sedeIdAreaSeleccionada: number = 0;

  deptoForm = {
    areaId: 0,
    id: 0,
    nombre: '',
    descripcion: ''
  };
  deptoEditando: boolean = false;
  private areaIdSeleccionada: number = 0;

  abrirModalSede(): void {
    this.sedeEditando = false;
    this.sedeForm = { id: 0, nombre: '', descripcion: '', estado: 'activo' };
  }

  editarSede(sede: Sede): void {
    this.sedeEditando = true;
    this.sedeForm = {
      id: sede.id,
      nombre: sede.nombre,
      descripcion: sede.descripcion,
      estado: sede.estado
    };
    const modalElement = document.getElementById('modalSede');
    if (modalElement) {
      const modal = new (window as any).bootstrap.Modal(modalElement);
      modal.show();
    }
  }

  guardarSede(): void {
    if (!this.sedeForm.nombre) {
      alert('Por favor complete el nombre de la sede');
      return;
    }

    if (this.sedeEditando) {
      const index = this.sedes.findIndex(s => s.id === this.sedeForm.id);
      if (index !== -1) {
        this.sedes[index] = {
          ...this.sedes[index],
          nombre: this.sedeForm.nombre,
          descripcion: this.sedeForm.descripcion,
          estado: this.sedeForm.estado
        };
      }
    } else {
      const nuevaSede: Sede = {
        id: Date.now(),
        nombre: this.sedeForm.nombre,
        descripcion: this.sedeForm.descripcion,
        usuarios: 0,
        estado: this.sedeForm.estado,
        areas: []
      };
      this.sedes.push(nuevaSede);
    }

    this.limpiarFormularioSede();
    this.cerrarModal('modalSede');
  }

  eliminarSede(id: number): void {
    const sede = this.sedes.find(s => s.id === id);
    if (sede && confirm(`¿Eliminar la sede "${sede.nombre}" y todas sus áreas y departamentos?`)) {
      this.sedes = this.sedes.filter(s => s.id !== id);
    }
  }

  private limpiarFormularioSede(): void {
    this.sedeForm = { id: 0, nombre: '', descripcion: '', estado: 'activo' };
    this.sedeEditando = false;
  }

  abrirModalArea(sedeId: number): void {
    this.areaEditando = false;
    this.sedeIdAreaSeleccionada = sedeId;
    this.areaForm = { sedeId: sedeId, id: 0, nombre: '', descripcion: '' };
    const modalElement = document.getElementById('modalArea');
    if (modalElement) {
      const modal = new (window as any).bootstrap.Modal(modalElement);
      modal.show();
    }
  }

  editarArea(sedeId: number, areaId: number): void {
    const sede = this.sedes.find(s => s.id === sedeId);
    const area = sede?.areas.find(a => a.id === areaId);
    if (area) {
      this.areaEditando = true;
      this.areaForm = { sedeId, id: areaId, nombre: area.nombre, descripcion: area.descripcion };
      const modalElement = document.getElementById('modalArea');
      if (modalElement) {
        const modal = new (window as any).bootstrap.Modal(modalElement);
        modal.show();
      }
    }
  }

  guardarArea(): void {
    const sede = this.sedes.find(s => s.id === this.areaForm.sedeId);
    if (!sede) { alert('Sede no encontrada'); return; }
    if (!this.areaForm.nombre) { alert('El nombre del área es obligatorio'); return; }

    if (this.areaEditando) {
      const index = sede.areas.findIndex(a => a.id === this.areaForm.id);
      if (index !== -1) {
        sede.areas[index] = {
          ...sede.areas[index],
          nombre: this.areaForm.nombre,
          descripcion: this.areaForm.descripcion
        };
      }
    } else {
      sede.areas.push({
        id: Date.now(),
        nombre: this.areaForm.nombre,
        descripcion: this.areaForm.descripcion,
        departamentos: []
      });
    }

    this.limpiarFormularioArea();
    this.cerrarModal('modalArea');
  }

  eliminarArea(sedeId: number, areaId: number): void {
    const sede = this.sedes.find(s => s.id === sedeId);
    const area = sede?.areas.find(a => a.id === areaId);
    if (area && confirm(`¿Eliminar el área "${area.nombre}" y todos sus departamentos?`)) {
      if (sede) {
        sede.areas = sede.areas.filter(a => a.id !== areaId);
      }
    }
  }

  private limpiarFormularioArea(): void {
    this.areaForm = { sedeId: 0, id: 0, nombre: '', descripcion: '' };
    this.areaEditando = false;
    this.sedeIdAreaSeleccionada = 0;
  }

  abrirModalDepartamento(areaId: number, sedeId: number): void {
    this.deptoEditando = false;
    this.areaIdSeleccionada = areaId;
    this.sedeIdParaDepto = sedeId;
    this.deptoForm = { areaId, id: 0, nombre: '', descripcion: '' };
    const modalElement = document.getElementById('modalDepartamento');
    if (modalElement) {
      const modal = new (window as any).bootstrap.Modal(modalElement);
      modal.show();
    }
  }

  editarDepartamento(sedeId: number, areaId: number, deptoId: number): void {
    const sede = this.sedes.find(s => s.id === sedeId);
    const area = sede?.areas.find(a => a.id === areaId);
    const departamento = area?.departamentos.find(d => d.id === deptoId);
    if (departamento) {
      this.deptoEditando = true;
      this.sedeIdParaDepto = sedeId;
      this.deptoForm = { areaId, id: deptoId, nombre: departamento.nombre, descripcion: departamento.descripcion };
      const modalElement = document.getElementById('modalDepartamento');
      if (modalElement) {
        const modal = new (window as any).bootstrap.Modal(modalElement);
        modal.show();
      }
    }
  }

  guardarDepartamento(): void {
    const sede = this.sedes.find(s => s.areas.some(a => a.id === this.deptoForm.areaId));
    const area = sede?.areas.find(a => a.id === this.deptoForm.areaId);
    if (!area) { alert('Área no encontrada'); return; }
    if (!this.deptoForm.nombre) { alert('El nombre del departamento es obligatorio'); return; }

    if (this.deptoEditando) {
      const index = area.departamentos.findIndex(d => d.id === this.deptoForm.id);
      if (index !== -1) {
        area.departamentos[index] = {
          id: this.deptoForm.id,
          nombre: this.deptoForm.nombre,
          descripcion: this.deptoForm.descripcion
        };
      }
    } else {
      area.departamentos.push({
        id: Date.now(),
        nombre: this.deptoForm.nombre,
        descripcion: this.deptoForm.descripcion
      });
    }

    this.limpiarFormularioDepartamento();
    this.cerrarModal('modalDepartamento');
  }

  eliminarDepartamento(sedeId: number, areaId: number, deptoId: number): void {
    const sede = this.sedes.find(s => s.id === sedeId);
    const area = sede?.areas.find(a => a.id === areaId);
    const departamento = area?.departamentos.find(d => d.id === deptoId);
    if (departamento && confirm(`¿Eliminar el departamento "${departamento.nombre}"?`)) {
      if (area) {
        area.departamentos = area.departamentos.filter(d => d.id !== deptoId);
      }
    }
  }

  private limpiarFormularioDepartamento(): void {
    this.deptoForm = { areaId: 0, id: 0, nombre: '', descripcion: '' };
    this.deptoEditando = false;
    this.areaIdSeleccionada = 0;
  }

  onSedeChangeForArea(): void {
    this.areaForm.sedeId = Number(this.areaForm.sedeId);
  }

  onSedeChangeForDepto(sedeId: number): void {
    const sede = this.sedes.find(s => s.id === sedeId);
    if (sede && sede.areas.length > 0) {
      this.deptoForm.areaId = sede.areas[0].id;
    } else {
      this.deptoForm.areaId = 0;
    }
  }

  private cerrarModal(modalId: string): void {
    const modalElement = document.getElementById(modalId);
    if (modalElement) {
      const modal = (window as any).bootstrap.Modal.getInstance(modalElement);
      modal?.hide();
    }
  }
}
