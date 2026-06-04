import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

// Interfaces
interface Departamento {
  id: number;
  nombre: string;
  personas: number;
}

interface Sede {
  id: number;
  nombre: string;
  direccion: string;
  personal: number;
  capacidad: number;
  departamentos: Departamento[];
}

@Component({
  selector: 'app-sedes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sedes.html',
  styleUrl: './sedes.css',
})
export class Sedes {
  
  // ==========================================
  // DATOS PRINCIPALES
  // ==========================================
  sedes: Sede[] = [
    {
      id: 1,
      nombre: 'Centro Logístico Valencia',
      direccion: 'Polígono Industrial de Riba-roja, 46394, Valencia',
      personal: 890,
      capacidad: 68,
      departamentos: [
        { id: 1, nombre: 'Operaciones de Alota', personas: 540 },
        { id: 2, nombre: 'Control de Inventario', personas: 210 }
      ]
    },
    {
      id: 2,
      nombre: 'Corporativo Madrid',
      direccion: 'Calle Castellana 25, 28046, Madrid',
      personal: 1240,
      capacidad: 85,
      departamentos: [
        { id: 3, nombre: 'Ventas Globales', personas: 180 },
        { id: 4, nombre: 'Recursos Humanos', personas: 32 },
        { id: 5, nombre: 'Ingeniería & DevOps', personas: 450 }
      ]
    },
    {
      id: 3,
      nombre: 'Hub Barcelona',
      direccion: 'Carrer de Sancho de Ávila 65, 08018, Barcelona',
      personal: 640,
      capacidad: 92,
      departamentos: [
        { id: 6, nombre: 'Atención al Cliente', personas: 310 },
        { id: 7, nombre: 'UX/UI Design Lab', personas: 45 },
        { id: 8, nombre: 'Data Science', personas: 120 }
      ]
    }
  ];

  // ==========================================
  // CONTADORES Y GETTERS
  // ==========================================
  get totalSedes(): number {
    return this.sedes.length;
  }

  get totalDepartamentos(): number {
    return this.sedes.reduce((total, sede) => total + sede.departamentos.length, 0);
  }

  get totalPersonal(): number {
    return this.sedes.reduce((total, sede) => total + sede.personal, 0);
  }

  get ratioPersonalPorSede(): number {
    return Math.round(this.totalPersonal / this.totalSedes);
  }

  get crecimientoAnual(): string {
    return '+14%';
  }

  // ==========================================
  // FORMULARIOS
  // ==========================================
  // Formulario Sede
  sedeForm = {
    id: 0,
    nombre: '',
    direccion: '',
    personal: 0,
    capacidad: 0
  };
  sedeEditando: boolean = false;

  // Formulario Departamento
  deptoForm = {
    sedeId: 0,
    id: 0,
    nombre: '',
    personas: 0
  };
  deptoEditando: boolean = false;
  private sedeIdSeleccionada: number = 0;

  // ==========================================
  // MÉTODOS PARA SEDES
  // ==========================================
  
  /**
   * Abre modal para nueva sede
   */
  abrirModalSede(): void {
    this.sedeEditando = false;
    this.sedeForm = {
      id: 0,
      nombre: '',
      direccion: '',
      personal: 0,
      capacidad: 0
    };
  }

  /**
   * Editar sede existente
   */
  editarSede(sede: Sede): void {
    this.sedeEditando = true;
    this.sedeForm = {
      id: sede.id,
      nombre: sede.nombre,
      direccion: sede.direccion,
      personal: sede.personal,
      capacidad: sede.capacidad
    };
    
    // Abrir modal
    const modalElement = document.getElementById('modalSede');
    if (modalElement) {
      // @ts-ignore
      const modal = new bootstrap.Modal(modalElement);
      modal.show();
    }
  }

  /**
   * Guardar sede (crear o actualizar)
   */
  guardarSede(): void {
    if (!this.sedeForm.nombre || !this.sedeForm.direccion) {
      alert(' Por favor complete los campos obligatorios');
      return;
    }

    if (this.sedeEditando) {
      // Actualizar sede existente
      const index = this.sedes.findIndex(s => s.id === this.sedeForm.id);
      if (index !== -1) {
        this.sedes[index] = {
          ...this.sedes[index],
          nombre: this.sedeForm.nombre,
          direccion: this.sedeForm.direccion,
          personal: this.sedeForm.personal,
          capacidad: this.sedeForm.capacidad
        };
        console.log(' Sede actualizada:', this.sedeForm.nombre);
      }
    } else {
      // Crear nueva sede
      const nuevaSede: Sede = {
        id: Date.now(),
        nombre: this.sedeForm.nombre,
        direccion: this.sedeForm.direccion,
        personal: this.sedeForm.personal || 0,
        capacidad: this.sedeForm.capacidad || 0,
        departamentos: []
      };
      this.sedes.push(nuevaSede);
      console.log(' Nueva sede creada:', nuevaSede.nombre);
    }

    // Limpiar formulario y cerrar modal
    this.limpiarFormularioSede();
    this.cerrarModal('modalSede');
  }

  /**
   * Eliminar sede
   */
  eliminarSede(id: number): void {
    const sede = this.sedes.find(s => s.id === id);
    if (sede && confirm(`¿Eliminar la sede "${sede.nombre}" y todos sus departamentos?`)) {
      this.sedes = this.sedes.filter(s => s.id !== id);
      console.log(` Sede ${id} eliminada`);
    }
  }

  /**
   * Limpiar formulario de sede
   */
  private limpiarFormularioSede(): void {
    this.sedeForm = {
      id: 0,
      nombre: '',
      direccion: '',
      personal: 0,
      capacidad: 0
    };
    this.sedeEditando = false;
  }

  // ==========================================
  // MÉTODOS PARA DEPARTAMENTOS
  // ==========================================
  
  /**
   * Abre modal para nuevo departamento
   */
  abrirModalDepartamento(sedeId: number): void {
    this.deptoEditando = false;
    this.sedeIdSeleccionada = sedeId;
    this.deptoForm = {
      sedeId: sedeId,
      id: 0,
      nombre: '',
      personas: 0
    };
    
    const modalElement = document.getElementById('modalDepartamento');
    if (modalElement) {
      // @ts-ignore
      const modal = new bootstrap.Modal(modalElement);
      modal.show();
    }
  }

  /**
   * Editar departamento existente
   */
  editarDepartamento(sedeId: number, deptoId: number): void {
    const sede = this.sedes.find(s => s.id === sedeId);
    const departamento = sede?.departamentos.find(d => d.id === deptoId);
    
    if (departamento) {
      this.deptoEditando = true;
      this.deptoForm = {
        sedeId: sedeId,
        id: deptoId,
        nombre: departamento.nombre,
        personas: departamento.personas
      };
      
      const modalElement = document.getElementById('modalDepartamento');
      if (modalElement) {
        // @ts-ignore
        const modal = new bootstrap.Modal(modalElement);
        modal.show();
      }
    }
  }

  /**
   * Guardar departamento (crear o actualizar)
   */
  guardarDepartamento(): void {
    const sede = this.sedes.find(s => s.id === this.deptoForm.sedeId);
    
    if (!sede) {
      alert(' Sede no encontrada');
      return;
    }

    if (!this.deptoForm.nombre) {
      alert(' El nombre del departamento es obligatorio');
      return;
    }

    if (this.deptoEditando) {
      // Actualizar departamento existente
      const index = sede.departamentos.findIndex(d => d.id === this.deptoForm.id);
      if (index !== -1) {
        sede.departamentos[index] = {
          id: this.deptoForm.id,
          nombre: this.deptoForm.nombre,
          personas: this.deptoForm.personas || 0
        };
        console.log(' Departamento actualizado:', this.deptoForm.nombre);
      }
    } else {
      // Crear nuevo departamento
      const nuevoDepartamento: Departamento = {
        id: Date.now(),
        nombre: this.deptoForm.nombre,
        personas: this.deptoForm.personas || 0
      };
      sede.departamentos.push(nuevoDepartamento);
      console.log(' Nuevo departamento creado:', nuevoDepartamento.nombre);
    }

    // Actualizar el total de personal de la sede
    this.actualizarPersonalSede(sede.id);

    // Limpiar y cerrar modal
    this.limpiarFormularioDepartamento();
    this.cerrarModal('modalDepartamento');
  }

  /**
   * Eliminar departamento
   */
  eliminarDepartamento(sedeId: number, deptoId: number): void {
    const sede = this.sedes.find(s => s.id === sedeId);
    const departamento = sede?.departamentos.find(d => d.id === deptoId);
    
    if (departamento && confirm(`¿Eliminar el departamento "${departamento.nombre}"?`)) {
      if (sede) {
        sede.departamentos = sede.departamentos.filter(d => d.id !== deptoId);
        this.actualizarPersonalSede(sedeId);
        console.log(` Departamento ${deptoId} eliminado`);
      }
    }
  }

  /**
   * Actualiza el contador de personal de una sede
   */
  private actualizarPersonalSede(sedeId: number): void {
    const sede = this.sedes.find(s => s.id === sedeId);
    if (sede) {
      const totalPersonal = sede.departamentos.reduce((total, depto) => total + depto.personas, 0);
      sede.personal = totalPersonal;
      
      // Actualizar capacidad (ejemplo: basado en un máximo de 2000 personas)
      const capacidadMaxima = 2000;
      sede.capacidad = Math.min(100, Math.round((totalPersonal / capacidadMaxima) * 100));
    }
  }

  /**
   * Limpiar formulario de departamento
   */
  private limpiarFormularioDepartamento(): void {
    this.deptoForm = {
      sedeId: 0,
      id: 0,
      nombre: '',
      personas: 0
    };
    this.deptoEditando = false;
    this.sedeIdSeleccionada = 0;
  }

  // ==========================================
  // MÉTODOS UTILITARIOS
  // ==========================================
  
  /**
   * Cerrar modal por ID
   */
  private cerrarModal(modalId: string): void {
    const modalElement = document.getElementById(modalId);
    if (modalElement) {
      // @ts-ignore
      const modal = bootstrap.Modal.getInstance(modalElement);
      modal?.hide();
    }
  }
}