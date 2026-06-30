import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastService } from '../../../services/toast.service';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

interface Tenant {
  id: number;
  nombre: string;
  iniciales: string;
  idTenant: string;
  dominio: string;
  industria: string;
  fechaRegistro: Date;
  antiguedad: number;
  plan: 'BASIC' | 'PROFESSIONAL' | 'ENTERPRISE';
  estado: 'Activo' | 'Trial' | 'Suspendido';
  usuarios: number;
  color: string;
}

interface Metricas {
  totalTenants: number;
  activos: number;
  nuevos: number;
  tasaChurn: number;
  crecimiento: number;
  porcentajeActivos: number;
  metaNuevos: number;
  variacionChurn: number;
}

interface ConfiguracionTecnica {
  modeloOrganizativo: 'Sede Unica' | 'Multi-sede';
  biometricoHabilitado: boolean;
  escaneoBarrasHabilitado: boolean;
  almacenamientoUsado: number;
  sedesCreadas: number;
  sedesLimite: number;
}





@Component({
  selector: 'app-tenants',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './tenants.html',
  styleUrl: './tenants.css',
})
export class Tenants {
  readonly loading = signal(false);
  readonly error = signal('');

  private readonly toastService = inject(ToastService);
  
  // ==========================================
  // DATOS PRINCIPALES
  // ==========================================
  
  tenants: Tenant[] = [
    { id: 1, nombre: 'Acme Corp', iniciales: 'AC', idTenant: 'acme-global', dominio: 'acme-global.saas.nexus', industria: 'Tecnología', fechaRegistro: new Date('2023-10-12'), antiguedad: 4, plan: 'ENTERPRISE', estado: 'Activo', usuarios: 2450, color: '#163A96' },
    { id: 2, nombre: 'Stark Nexus', iniciales: 'SN', idTenant: 'stark-industries', dominio: 'stark-industries.saas.nexus', industria: 'Manufactura', fechaRegistro: new Date('2023-11-05'), antiguedad: 3, plan: 'PROFESSIONAL', estado: 'Trial', usuarios: 45, color: '#10B981' },
    { id: 3, nombre: 'Wayne Migital', iniciales: 'WD', idTenant: 'wayne-dig', dominio: 'wayne-dig.saas.nexus', industria: 'Tecnología', fechaRegistro: new Date('2023-09-20'), antiguedad: 5, plan: 'ENTERPRISE', estado: 'Suspendido', usuarios: 890, color: '#6366F1' },
    { id: 4, nombre: 'Globex Corp', iniciales: 'GC', idTenant: 'globex', dominio: 'globex.saas.nexus', industria: 'Comercio', fechaRegistro: new Date('2023-12-01'), antiguedad: 2, plan: 'BASIC', estado: 'Activo', usuarios: 12, color: '#F59E0B' },
    { id: 5, nombre: 'Initech', iniciales: 'IN', idTenant: 'initech', dominio: 'initech.saas.nexus', industria: 'Software', fechaRegistro: new Date('2023-08-15'), antiguedad: 6, plan: 'PROFESSIONAL', estado: 'Activo', usuarios: 320, color: '#EF4444' },
    { id: 6, nombre: 'Umbrella Corp', iniciales: 'UC', idTenant: 'umbrella', dominio: 'umbrella.saas.nexus', industria: 'Biotecnología', fechaRegistro: new Date('2023-07-10'), antiguedad: 7, plan: 'ENTERPRISE', estado: 'Activo', usuarios: 5600, color: '#8B5CF6' },
    { id: 7, nombre: 'Cyberdyne', iniciales: 'CY', idTenant: 'cyberdyne', dominio: 'cyberdyne.saas.nexus', industria: 'Robótica', fechaRegistro: new Date('2023-10-25'), antiguedad: 3, plan: 'PROFESSIONAL', estado: 'Trial', usuarios: 78, color: '#EC4899' },
    { id: 8, nombre: 'Massive Dynamic', iniciales: 'MD', idTenant: 'massive-dynamic', dominio: 'massive-dynamic.saas.nexus', industria: 'Investigación', fechaRegistro: new Date('2023-06-30'), antiguedad: 8, plan: 'ENTERPRISE', estado: 'Activo', usuarios: 1780, color: '#14B8A6' },
    { id: 9, nombre: 'Tyrell Corp', iniciales: 'TY', idTenant: 'tyrell', dominio: 'tyrell.saas.nexus', industria: 'Tecnología', fechaRegistro: new Date('2023-11-18'), antiguedad: 2, plan: 'PROFESSIONAL', estado: 'Suspendido', usuarios: 234, color: '#F97316' },
    { id: 10, nombre: 'Weyland-Yutani', iniciales: 'WY', idTenant: 'weyland', dominio: 'weyland.saas.nexus', industria: 'Aeroespacial', fechaRegistro: new Date('2023-09-05'), antiguedad: 5, plan: 'ENTERPRISE', estado: 'Activo', usuarios: 3450, color: '#06B6D4' }
  ];
  
  // ==========================================
  // ESTADO DE FILTROS Y PAGINACIÓN
  // ==========================================
  searchTerm: string = '';
  filtroPlan: string = '';
  filtroEstado: string = '';
  filtroIndustria: string = '';
  
  paginaActual: number = 1;
  itemsPerPage: number = 10;
  
  // ==========================================
  // ESTADO DE MODALES
  // ==========================================
  modalTenantOpen: boolean = false;
  modalDetalleOpen: boolean = false;
  editandoTenant: boolean = false;
  tenantSeleccionado: Tenant | null = null;
  
  tenantForm: Tenant = {
    id: 0,
    nombre: '',
    iniciales: '',
    idTenant: '',
    dominio: '',
    industria: '',
    fechaRegistro: new Date(),
    antiguedad: 0,
    plan: 'PROFESSIONAL',
    estado: 'Activo',
    usuarios: 0,
    color: '#163A96'
  };
  
  // ==========================================
  // OPCIONES PARA FILTROS
  // ==========================================
  planesDisponibles: string[] = ['BASIC', 'PROFESSIONAL', 'ENTERPRISE'];
  estadosDisponibles: string[] = ['Activo', 'Trial', 'Suspendido'];
  industriasDisponibles: string[] = ['Tecnología', 'Manufactura', 'Comercio', 'Software', 'Biotecnología', 'Robótica', 'Investigación', 'Aeroespacial'];
  
  // ==========================================
  // MÉTRICAS
  // ==========================================
  metricas: Metricas = {
    totalTenants: 1284,
    activos: 1102,
    nuevos: 42,
    tasaChurn: 1.8,
    crecimiento: 12,
    porcentajeActivos: 86,
    metaNuevos: 30,
    variacionChurn: -0.4
  };
  
  // ==========================================
  // GETTERS
  // ==========================================
  
  get tenantsFiltrado(): Tenant[] {
    let resultado = this.tenants;
    
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      resultado = resultado.filter(t => 
        t.nombre.toLowerCase().includes(term) || 
        t.idTenant.toLowerCase().includes(term) ||
        t.dominio.toLowerCase().includes(term)
      );
    }
    
    if (this.filtroPlan) {
      resultado = resultado.filter(t => t.plan === this.filtroPlan);
    }
    
    if (this.filtroEstado) {
      resultado = resultado.filter(t => t.estado === this.filtroEstado);
    }
    
    if (this.filtroIndustria) {
      resultado = resultado.filter(t => t.industria === this.filtroIndustria);
    }
    
    return resultado;
  }
  
  get tenantsPaginado(): Tenant[] {
    const start = (this.paginaActual - 1) * this.itemsPerPage;
    const end = start + this.itemsPerPage;
    return this.tenantsFiltrado.slice(start, end);
  }
  
  get totalPaginas(): number {
    return Math.ceil(this.tenantsFiltrado.length / this.itemsPerPage);
  }
  
  get inicioRegistro(): number {
    return (this.paginaActual - 1) * this.itemsPerPage + 1;
  }
  
  get finRegistro(): number {
    return Math.min(this.paginaActual * this.itemsPerPage, this.tenantsFiltrado.length);
  }
  
  // ==========================================
  // MÉTODOS DE FILTRADO
  // ==========================================
  
  filtrarTenants(): void {
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
    this.editandoTenant = false;
    this.tenantForm = {
      id: 0,
      nombre: '',
      iniciales: '',
      idTenant: '',
      dominio: '',
      industria: this.industriasDisponibles[0],
      fechaRegistro: new Date(),
      antiguedad: 0,
      plan: 'PROFESSIONAL',
      estado: 'Activo',
      usuarios: 0,
      color: '#163A96'
    };
    this.modalTenantOpen = true;
  }
  
  abrirModalEditar(tenant: Tenant): void {
    this.editandoTenant = true;
    this.tenantForm = { ...tenant };
    this.modalTenantOpen = true;
  }
  
  cerrarModalTenant(): void {
    this.modalTenantOpen = false;
  }
  
  guardarTenant(): void {
    if (!this.tenantForm.nombre || !this.tenantForm.idTenant) {
      this.mostrarToast(' Complete los campos obligatorios');
      return;
    }
    
    // Generar iniciales
    this.tenantForm.iniciales = this.tenantForm.nombre.split(' ').map(w => w[0]).join('').toUpperCase().substring(0, 2);
    
    if (this.editandoTenant) {
      this.tenants = this.tenants.map(t => t.id === this.tenantForm.id ? this.tenantForm : t);
      this.mostrarToast(' Tenant actualizado correctamente');
    } else {
      const nuevoId = Math.max(...this.tenants.map(t => t.id), 0) + 1;
      const nuevoTenant = { ...this.tenantForm, id: nuevoId };
      this.tenants = [...this.tenants, nuevoTenant];
      this.mostrarToast(' Nuevo tenant registrado correctamente');
      
      this.metricas.totalTenants++;
      this.metricas.nuevos++;
    }
    
    this.cerrarModalTenant();
    this.filtrarTenants();
  }
  
  // ==========================================
  // MÉTODOS DE MODALES - DETALLE
  // ==========================================
  
  abrirModalDetalle(tenant: Tenant): void {
    this.tenantSeleccionado = tenant;
    this.modalDetalleOpen = true;
  }
  
  cerrarModalDetalle(): void {
    this.modalDetalleOpen = false;
    this.tenantSeleccionado = null;
  }
  
  editarDesdeDetalle(): void {
    const tenant = this.tenantSeleccionado;
    if (tenant) {
      this.cerrarModalDetalle();
      this.abrirModalEditar(tenant);
    }
  }
  
  // ==========================================
  // MÉTODOS DE ACCIÓN
  // ==========================================
  
  eliminarTenant(id: number): void {
    const tenant = this.tenants.find(t => t.id === id);
    if (tenant) {
      this.tenants = this.tenants.filter(t => t.id !== id);
      this.mostrarToast(`"${tenant.nombre}" eliminado correctamente`);
      this.metricas.totalTenants--;
      
      if (tenant.estado === 'Activo') {
        this.metricas.activos--;
      }
    }
  }
  
  // ==========================================
  // MÉTODOS UTILITARIOS
  // ==========================================
  
  private mostrarToast(mensaje: string): void {
    this.toastService.info(mensaje);
  }


// ==========================================
// CONFIGURACIÓN TÉCNICA
// ==========================================
modalConfiguracionOpen: boolean = false;
modalConfirmarSuspensionOpen: boolean = false;
motivoSuspension: string = '';

configuracion: ConfiguracionTecnica = {
  modeloOrganizativo: 'Sede Unica',
  biometricoHabilitado: true,
  escaneoBarrasHabilitado: false,
  almacenamientoUsado: 85,
  sedesCreadas: 3,
  sedesLimite: 10
};

// ==========================================
// MÉTODOS PARA CONFIGURACIÓN TÉCNICA
// ==========================================

abrirModalConfiguracion(tenant: Tenant): void {
  this.tenantSeleccionado = tenant;
  this.modalConfiguracionOpen = true;
  
  // Cargar configuración específica del tenant (simulado)
  // En producción, esto vendría de una API
  this.cargarConfiguracionTenant(tenant.id);
}

cerrarModalConfiguracion(): void {
  this.modalConfiguracionOpen = false;
  this.tenantSeleccionado = null;
}

cargarConfiguracionTenant(tenantId: number): void {
  // Simular carga de configuración por tenant
  // En producción, llamar a API
  if (tenantId === 1) {
    this.configuracion = {
      modeloOrganizativo: 'Multi-sede',
      biometricoHabilitado: true,
      escaneoBarrasHabilitado: true,
      almacenamientoUsado: 85,
      sedesCreadas: 3,
      sedesLimite: 10
    };
  } else {
    this.configuracion = {
      modeloOrganizativo: 'Sede Unica',
      biometricoHabilitado: true,
      escaneoBarrasHabilitado: false,
      almacenamientoUsado: 45,
      sedesCreadas: 1,
      sedesLimite: 10
    };
  }
}

toggleBiometrico(): void {
  this.configuracion.biometricoHabilitado = !this.configuracion.biometricoHabilitado;
  this.mostrarToast(this.configuracion.biometricoHabilitado ? 
    ' Huellero biométrico habilitado' : 
    ' Huellero biométrico deshabilitado');
}

toggleEscaneoBarras(): void {
  this.configuracion.escaneoBarrasHabilitado = !this.configuracion.escaneoBarrasHabilitado;
  this.mostrarToast(this.configuracion.escaneoBarrasHabilitado ? 
    ' Escaneo de barras habilitado' : 
    ' Escaneo de barras deshabilitado');
}

  guardarConfiguracion(): void {
    this.mostrarToast('Configuración guardada correctamente');
    this.cerrarModalConfiguracion();
  }

suspenderTenant(): void {
  this.modalConfirmarSuspensionOpen = true;
}

cerrarModalConfirmarSuspension(): void {
  this.modalConfirmarSuspensionOpen = false;
  this.motivoSuspension = '';
}

confirmarSuspenderTenant(): void {
    if (this.tenantSeleccionado) {
      const tenant = this.tenantSeleccionado;
      tenant.estado = 'Suspendido';
      this.tenants = this.tenants.map(t => 
        t.id === tenant.id ? tenant : t
      );
      
      this.mostrarToast(`"${tenant.nombre}" suspendido correctamente`);
      this.cerrarModalConfirmarSuspension();
      this.cerrarModalConfiguracion();
      
      this.metricas.activos = this.tenants.filter(t => t.estado === 'Activo').length;
    }
  }

  eliminarTenantConfig(): void {
    const tenant = this.tenantSeleccionado;
    if (tenant) {
      this.tenants = this.tenants.filter(t => t.id !== tenant.id);
      this.mostrarToast(`"${tenant.nombre}" eliminado correctamente`);
      this.cerrarModalConfiguracion();
      
      this.metricas.totalTenants = this.tenants.length;
      this.metricas.activos = this.tenants.filter(t => t.estado === 'Activo').length;
    }
  }




}

