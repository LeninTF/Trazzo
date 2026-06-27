import { Component, signal, WritableSignal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { ToastService } from '../../../services/toast.service';
import { ModalService } from '../../../services/modal.service';
import { ApiService } from '../../../api/services/api.service';
import { tenantUserToPersonal } from '../../../api/services/helpers';
import { firstValueFrom } from 'rxjs';

export interface Personal {
  id: number;
  nombre: string;
  idPersonal: string;
  sede: string;
  area: string;
  departamento: string;
  cargo: string;
  estado: 'ACTIVO' | 'PRUEBA' | 'SUSPENDIDO' | 'LICENCIA';
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
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './directorio-personal.html',
  styleUrl: './directorio-personal.css',
})
export class DirectorioPersonal implements OnInit {

  private readonly toastService = inject(ToastService);
  private readonly modalService = inject(ModalService);
  private readonly api = inject(ApiService);
  readonly loading = signal(false);
  readonly error = signal('');

  // ==========================================
  // DATOS PRINCIPALES
  // ==========================================
  
  personal: WritableSignal<Personal[]> = signal<Personal[]>([]);
  
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
  sedesDisponibles: string[] = [];
  areasDisponibles: string[] = [];
  departamentosDisponibles: string[] = [];
  
  // ==========================================
  // MÉTRICAS
  // ==========================================
  metricas: Metricas = {
    personalTotal: 0,
    activosHoy: 0,
    incorporaciones: 0,
    deLicencia: 0,
    crecimiento: 0,
    porcentajeActivos: 0
  };

  async ngOnInit(): Promise<void> {
    await this.cargarPersonal();
  }

  async cargarPersonal(): Promise<void> {
    this.loading.set(true);
    try {
      const res = await firstValueFrom(this.api.users.list({ size: 100 }));
      const items = res.content.map(tenantUserToPersonal) as Personal[];
      this.personal.set(items);
      this.sedesDisponibles = [...new Set(items.map(p => p.sede).filter(Boolean))];
      this.areasDisponibles = [...new Set(items.map(p => p.area).filter(Boolean))];
      this.departamentosDisponibles = [...new Set(items.map(p => p.departamento).filter(Boolean))];
      this.metricas = {
        personalTotal: res.totalElements,
        activosHoy: items.filter(p => p.estado === 'ACTIVO').length,
        incorporaciones: 0,
        deLicencia: items.filter(p => p.estado === 'LICENCIA').length,
        crecimiento: 0,
        porcentajeActivos: res.totalElements > 0 ? Math.round((items.filter(p => p.estado === 'ACTIVO').length / res.totalElements) * 100) : 0,
      };
    } catch {
      this.error.set('Error al cargar personal. Verifica tu conexión e intenta nuevamente.');
    } finally {
      this.loading.set(false);
    }
  }
  
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
  
  async guardarPersonal(): Promise<void> {
    if (!this.personalForm.nombre || !this.personalForm.idPersonal) {
      this.mostrarToast('Complete los campos obligatorios');
      return;
    }
    
    try {
      if (this.editandoPersonal) {
        await firstValueFrom(this.api.users.patch(this.personalForm.id, {
          name: this.personalForm.nombre.split(' ')[0] ?? '',
          father_surname: this.personalForm.nombre.split(' ')[1] ?? '',
          email: this.personalForm.email ?? '',
          phone: this.personalForm.telefono ?? null,
        }));
        this.mostrarToast('Personal actualizado correctamente');
      } else {
        await firstValueFrom(this.api.users.create({
          document_type: 'DNI',
          document_value: String(Date.now()).slice(-8),
          name: this.personalForm.nombre.split(' ')[0] ?? '',
          father_surname: this.personalForm.nombre.split(' ')[1] ?? '',
          mother_surname: '',
          email: this.personalForm.email ?? `${this.personalForm.nombre.toLowerCase().replace(/\s+/g, '.')}@colegio.edu.pe`,
          phone: this.personalForm.telefono ?? null,
          role_id: 5,
        }));
        this.mostrarToast('Nuevo miembro agregado correctamente');
      }
      await this.cargarPersonal();
    } catch {
      this.mostrarToast('Error al guardar');
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
  
  async eliminarPersonal(id: number): Promise<void> {
    try {
      await firstValueFrom(this.api.users.delete(id));
      await this.cargarPersonal();
      this.mostrarToast('Personal eliminado correctamente');
    } catch {
      this.mostrarToast('Error al eliminar');
    }
  }
  
  // ==========================================
  // MÉTODOS UTILITARIOS
  // ==========================================
  
  private mostrarToast(mensaje: string): void {
    this.toastService.info(mensaje);
  }
}