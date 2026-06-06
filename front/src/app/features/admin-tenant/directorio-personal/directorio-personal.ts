import { Component, signal, WritableSignal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface Personal {
  id: number;
  nombre: string;
  idPersonal: string;
  sede: string;
  area: string;
  departamento: string;
  cargo: string;
  estado: 'ACTIVO' | 'PRUEBA' | 'SUSPENDIDO';
  email?: string;
  telefono?: string;
  fechaIngreso?: string;
  imagenUrl?: string;
}

interface Metricas {
  personalTotal: number;
  activosHoy: number;
  incorporaciones: number;
  deLicencia: number;
  crecimiento: number;
  porcentajeActivos: number;
}

@Component({
  selector: 'app-directorio-personal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './directorio-personal.html',
  styleUrl: './directorio-personal.css',
})
export class DirectorioPersonal {
  
  // ==========================================
  // DATOS PRINCIPALES
  // ==========================================
  
  personal: WritableSignal<Personal[]> = signal<Personal[]>([
    { id: 1, nombre: 'Dr. Sarah Jenkins', idPersonal: '#AX-9021', sede: 'Central', area: 'Académica', departamento: 'Academic', cargo: 'Lead Faculty', estado: 'ACTIVO', email: 'sarah.jenkins@empresa.com', telefono: '+51 987 654 321', fechaIngreso: '2020-03-15', imagenUrl: 'https://randomuser.me/api/portraits/women/1.jpg' },
    { id: 2, nombre: 'Marcus Thompson', idPersonal: '#AX-4432', sede: 'North', area: 'Administrativa', departamento: 'Admin', cargo: 'Registrar', estado: 'PRUEBA', email: 'marcus.thompson@empresa.com', telefono: '+51 987 654 322', fechaIngreso: '2021-06-20', imagenUrl: 'https://randomuser.me/api/portraits/men/2.jpg' },
    { id: 3, nombre: 'Elena Rodriguez', idPersonal: '#AX-8812', sede: 'South', area: 'Soporte', departamento: 'Support', cargo: 'IT Support', estado: 'SUSPENDIDO', email: 'elena.rodriguez@empresa.com', telefono: '+51 987 654 323', fechaIngreso: '2019-11-10', imagenUrl: 'https://randomuser.me/api/portraits/women/3.jpg' },
    { id: 4, nombre: 'Jameson Cooper', idPersonal: '#AX-1029', sede: 'Central', area: 'Académica', departamento: 'Academic', cargo: 'Professor', estado: 'ACTIVO', email: 'jameson.cooper@empresa.com', telefono: '+51 987 654 324', fechaIngreso: '2022-01-05', imagenUrl: 'https://randomuser.me/api/portraits/men/4.jpg' },
    { id: 5, nombre: 'Maria Gonzales', idPersonal: '#AX-2345', sede: 'Central', area: 'Administrativa', departamento: 'Admin', cargo: 'HR Manager', estado: 'ACTIVO', email: 'maria.gonzales@empresa.com', telefono: '+51 987 654 325', fechaIngreso: '2018-08-22', imagenUrl: 'https://randomuser.me/api/portraits/women/5.jpg' },
    { id: 6, nombre: 'Robert Chen', idPersonal: '#AX-6789', sede: 'North', area: 'Académica', departamento: 'Academic', cargo: 'Senior Lecturer', estado: 'ACTIVO', email: 'robert.chen@empresa.com', telefono: '+51 987 654 326', fechaIngreso: '2021-02-14', imagenUrl: 'https://randomuser.me/api/portraits/men/6.jpg' },
    { id: 7, nombre: 'Lisa Wong', idPersonal: '#AX-3456', sede: 'South', area: 'Soporte', departamento: 'Support', cargo: 'Technical Lead', estado: 'PRUEBA', email: 'lisa.wong@empresa.com', telefono: '+51 987 654 327', fechaIngreso: '2022-07-19', imagenUrl: 'https://randomuser.me/api/portraits/women/7.jpg' },
    { id: 8, nombre: 'Carlos Mendez', idPersonal: '#AX-7890', sede: 'Central', area: 'Administrativa', departamento: 'Admin', cargo: 'Financial Analyst', estado: 'ACTIVO', email: 'carlos.mendez@empresa.com', telefono: '+51 987 654 328', fechaIngreso: '2020-10-30', imagenUrl: 'https://randomuser.me/api/portraits/men/8.jpg' },
    { id: 9, nombre: 'Anna Kowalski', idPersonal: '#AX-4567', sede: 'North', area: 'Académica', departamento: 'Academic', cargo: 'Research Assistant', estado: 'ACTIVO', email: 'anna.kowalski@empresa.com', telefono: '+51 987 654 329', fechaIngreso: '2023-01-12', imagenUrl: 'https://randomuser.me/api/portraits/women/9.jpg' },
    { id: 10, nombre: 'David Kim', idPersonal: '#AX-8901', sede: 'South', area: 'Soporte', departamento: 'Support', cargo: 'Network Admin', estado: 'SUSPENDIDO', email: 'david.kim@empresa.com', telefono: '+51 987 654 330', fechaIngreso: '2019-05-08', imagenUrl: 'https://randomuser.me/api/portraits/men/10.jpg' }
  ]);
  
  // ==========================================
  // ESTADO DE FILTROS Y PAGINACIÓN
  // ==========================================
  searchTerm: string = '';
  filtroSede: string = '';
  filtroArea: string = '';
  filtroDepartamento: string = '';
  
  paginaActual: number = 1;
  itemsPerPage: number = 5;
  
  // ==========================================
  // ESTADO DE MODALES
  // ==========================================
  modalPersonalOpen: boolean = false;
  modalDetalleOpen: boolean = false;
  editandoPersonal: boolean = false;
  personalSeleccionado: Personal | null = null;
  
  imagenPreviewUrl: string | null = null;
  modoImagenUrl: boolean = true;
  
  personalForm: Personal = {
    id: 0,
    nombre: '',
    idPersonal: '',
    sede: '',
    area: '',
    departamento: '',
    cargo: '',
    estado: 'ACTIVO',
    email: '',
    telefono: '',
    fechaIngreso: '',
    imagenUrl: ''
  };
  
  // ==========================================
  // OPCIONES PARA FILTROS
  // ==========================================
  sedesDisponibles: string[] = ['Central', 'North', 'South'];
  areasDisponibles: string[] = ['Académica', 'Administrativa', 'Soporte'];
  departamentosDisponibles: string[] = ['Academic', 'Admin', 'Support'];
  
  // ==========================================
  // MÉTRICAS
  // ==========================================
  metricas: Metricas = {
    personalTotal: 1284,
    activosHoy: 1156,
    incorporaciones: 42,
    deLicencia: 86,
    crecimiento: 12,
    porcentajeActivos: 90
  };
  
  // ==========================================
  // GETTERS
  // ==========================================
  
  get personalFiltrado(): Personal[] {
    let resultado = this.personal();
    
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      resultado = resultado.filter(p => 
        p.nombre.toLowerCase().includes(term) || 
        p.idPersonal.toLowerCase().includes(term)
      );
    }
    
    if (this.filtroSede) {
      resultado = resultado.filter(p => p.sede === this.filtroSede);
    }
    
    if (this.filtroArea) {
      resultado = resultado.filter(p => p.area === this.filtroArea);
    }
    
    if (this.filtroDepartamento) {
      resultado = resultado.filter(p => p.departamento === this.filtroDepartamento);
    }
    
    return resultado;
  }
  
  get personalPaginado(): Personal[] {
    const start = (this.paginaActual - 1) * this.itemsPerPage;
    const end = start + this.itemsPerPage;
    return this.personalFiltrado.slice(start, end);
  }
  
  get totalPaginas(): number {
    return Math.ceil(this.personalFiltrado.length / this.itemsPerPage);
  }
  
  get inicioRegistro(): number {
    return (this.paginaActual - 1) * this.itemsPerPage + 1;
  }
  
  get finRegistro(): number {
    return Math.min(this.paginaActual * this.itemsPerPage, this.personalFiltrado.length);
  }
  
  // ==========================================
  // MÉTODOS DE IMAGEN
  // ==========================================
  
  abrirSelectorArchivo(): void {
    const input = document.getElementById('fileInput') as HTMLInputElement;
    if (input) {
      input.click();
    }
  }
  
  cambiarModoImagen(modo: boolean): void {
    this.modoImagenUrl = modo;
    if (modo) {
      this.imagenPreviewUrl = this.personalForm.imagenUrl || null;
    }
  }
  
  actualizarPreviewUrl(): void {
    this.imagenPreviewUrl = this.personalForm.imagenUrl || null;
  }
  
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      
      if (file.size > 2 * 1024 * 1024) {
        this.mostrarToast('❌ El archivo no puede superar los 2MB');
        return;
      }
      
      const reader = new FileReader();
      reader.onload = (e) => {
        const base64 = e.target?.result as string;
        this.personalForm.imagenUrl = base64;
        this.imagenPreviewUrl = base64;
      };
      reader.readAsDataURL(file);
    }
  }
  
  // ==========================================
  // MÉTODOS DE FILTRADO
  // ==========================================
  
  filtrarPersonal(): void {
    this.paginaActual = 1;
  }
  
  cambiarPagina(pagina: number): void {
    if (pagina >= 1 && pagina <= this.totalPaginas) {
      this.paginaActual = pagina;
    }
  }
  
  // ==========================================
  // MÉTODOS DE MODALES - AGREGAR/EDITAR
  // ==========================================
  
  abrirModalAgregar(): void {
    this.editandoPersonal = false;
    this.personalForm = {
      id: 0,
      nombre: '',
      idPersonal: '',
      sede: this.sedesDisponibles[0],
      area: this.areasDisponibles[0],
      departamento: this.departamentosDisponibles[0],
      cargo: '',
      estado: 'ACTIVO',
      email: '',
      telefono: '',
      fechaIngreso: new Date().toISOString().split('T')[0],
      imagenUrl: ''
    };
    this.imagenPreviewUrl = null;
    this.modoImagenUrl = true;
    this.modalPersonalOpen = true;
  }
  
  abrirModalEditar(persona: Personal): void {
    this.editandoPersonal = true;
    this.personalForm = { ...persona };
    this.imagenPreviewUrl = persona.imagenUrl || null;
    this.modoImagenUrl = true;
    this.modalPersonalOpen = true;
  }
  
  cerrarModalPersonal(): void {
    this.modalPersonalOpen = false;
    this.imagenPreviewUrl = null;
  }
  
  guardarPersonal(): void {
    if (!this.personalForm.nombre || !this.personalForm.idPersonal) {
      this.mostrarToast('⚠️ Complete los campos obligatorios');
      return;
    }
    
    if (this.editandoPersonal) {
      this.personal.update(list => 
        list.map(p => p.id === this.personalForm.id ? this.personalForm : p)
      );
      this.mostrarToast('✅ Personal actualizado correctamente');
    } else {
      const nuevoId = Math.max(...this.personal().map(p => p.id), 0) + 1;
      const nuevoPersonal = { ...this.personalForm, id: nuevoId };
      this.personal.update(list => [...list, nuevoPersonal]);
      this.mostrarToast('✅ Nuevo miembro agregado correctamente');
      
      this.metricas.personalTotal++;
      this.metricas.incorporaciones++;
    }
    
    this.cerrarModalPersonal();
    this.filtrarPersonal();
  }
  
  // ==========================================
  // MÉTODOS DE MODALES - DETALLE
  // ==========================================
  
  abrirModalDetalle(persona: Personal): void {
    this.personalSeleccionado = persona;
    this.modalDetalleOpen = true;
  }
  
  cerrarModalDetalle(): void {
    this.modalDetalleOpen = false;
    this.personalSeleccionado = null;
  }
  
  editarDesdeDetalle(): void {
    const persona = this.personalSeleccionado;
    if (persona) {
      this.cerrarModalDetalle();
      this.abrirModalEditar(persona);
    }
  }
  
  // ==========================================
  // MÉTODOS DE ACCIÓN
  // ==========================================
  
  eliminarPersonal(id: number): void {
    if (confirm('¿Está seguro de eliminar este registro?')) {
      this.personal.update(list => list.filter(p => p.id !== id));
      this.mostrarToast('🗑️ Personal eliminado correctamente');
      this.metricas.personalTotal--;
    }
  }
  
  // ==========================================
  // MÉTODOS UTILITARIOS
  // ==========================================
  
  private mostrarToast(mensaje: string): void {
    const toast = document.createElement('div');
    toast.className = 'toast-notification';
    toast.innerHTML = `
      <div class="toast-notification__content">
        <i class="bi bi-info-circle-fill me-2"></i>
        <span>${mensaje}</span>
      </div>
    `;
    document.body.appendChild(toast);
    
    setTimeout(() => {
      toast.classList.add('toast-notification--show');
      setTimeout(() => {
        toast.classList.remove('toast-notification--show');
        setTimeout(() => toast.remove(), 300);
      }, 2000);
    }, 10);
  }
}