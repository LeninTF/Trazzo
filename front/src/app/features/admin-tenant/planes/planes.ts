import { Component, signal, WritableSignal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastService } from '../../../services/toast.service';

interface Factura {
  id: number;
  codigo: string;
  plan: string;
  fecha: Date;
  monto: number;
  estado: 'Pagada' | 'Pendiente' | 'Vencida';
}

interface Plan {
  id: string;
  nombre: string;
  precio: number;
  descripcion: string;
  caracteristicas: string[];
  limiteUsuarios: number;
  limiteAlmacenamiento: number;
}

interface Metricas {
  usuariosActivos: number;
  limiteUsuarios: number;
  almacenamientoUsado: number;
  limiteAlmacenamiento: number;
}

@Component({
  selector: 'app-planes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './planes.html',
  styleUrl: './planes.css',
})
export class Planes {

  private readonly toastService = inject(ToastService);
  
  // ==========================================
  // SIGNALS PARA DATOS REACTIVOS
  // ==========================================
  
  facturas: WritableSignal<Factura[]> = signal<Factura[]>([
    { id: 1, codigo: 'INV 2023-006', plan: 'Plan Starter', fecha: new Date('2023-08-12'), monto: 49.00, estado: 'Pagada' },
    { id: 2, codigo: 'INV 2023-007', plan: 'Plan Professional', fecha: new Date('2023-09-12'), monto: 129.00, estado: 'Pagada' },
    { id: 3, codigo: 'INV 2023-008', plan: 'Plan Professional', fecha: new Date('2023-10-12'), monto: 129.00, estado: 'Pagada' },
    { id: 4, codigo: 'INV 2023-009', plan: 'Plan Professional', fecha: new Date('2023-11-12'), monto: 129.00, estado: 'Pendiente' }
  ]);
  
  planes: WritableSignal<Plan[]> = signal<Plan[]>([
    {
      id: 'starter',
      nombre: 'Starter',
      precio: 49,
      descripcion: 'Para equipos pequeños que empiezan',
      caracteristicas: ['Hasta 100 empleados', 'Reportes básicos PDF', 'Soporte vía email'],
      limiteUsuarios: 100,
      limiteAlmacenamiento: 20
    },
    {
      id: 'professional',
      nombre: 'Professional',
      precio: 129,
      descripcion: 'Gestión avanzada para empresas en crecimiento',
      caracteristicas: ['Hasta 1,000 empleados', 'Analytics Avanzado & Excel', 'Soporte prioritario 24/5', 'Control de Vacaciones & Permisos'],
      limiteUsuarios: 1000,
      limiteAlmacenamiento: 50
    },
    {
      id: 'enterprise',
      nombre: 'Enterprise',
      precio: 299,
      descripcion: 'Personalización total y seguridad avanzada',
      caracteristicas: ['Empleados ilimitados', 'Webhooks', 'SSO & SAML', 'Account Manager dedicado'],
      limiteUsuarios: 10000,
      limiteAlmacenamiento: 200
    }
  ]);
  
  // ==========================================
  // ESTADO DE SELECCIÓN Y MODALES
  // ==========================================
  planActualId: string = 'professional';
  planSeleccionadoId: string | null = null;
  facturaSeleccionadaId: number | null = null;
  
  modalActualizarOpen: boolean = false;
  modalFacturaOpen: boolean = false;
  
  metricas: Metricas = {
    usuariosActivos: 822,
    limiteUsuarios: 1000,
    almacenamientoUsado: 22.5,
    limiteAlmacenamiento: 50
  };
  
  // ==========================================
  // GETTERS (para usar en el template)
  // ==========================================
  
  get planActual(): Plan | undefined {
    return this.planes().find(p => p.id === this.planActualId);
  }
  
  get planSeleccionado(): Plan | undefined {
    if (!this.planSeleccionadoId) return undefined;
    return this.planes().find(p => p.id === this.planSeleccionadoId);
  }
  
  get facturaSeleccionada(): Factura | undefined {
    if (!this.facturaSeleccionadaId) return undefined;
    return this.facturas().find(f => f.id === this.facturaSeleccionadaId);
  }
  
  get porcentajeUsuarios(): number {
    const limite = this.planActual?.limiteUsuarios || this.metricas.limiteUsuarios;
    return Math.round((this.metricas.usuariosActivos / limite) * 100);
  }
  
  get porcentajeAlmacenamiento(): number {
    const limite = this.planActual?.limiteAlmacenamiento || this.metricas.limiteAlmacenamiento;
    return Math.round((this.metricas.almacenamientoUsado / limite) * 100);
  }
  
  // ==========================================
  // MÉTODOS DE SELECCIÓN
  // ==========================================
  
  seleccionarPlan(plan: Plan): void {
    this.planSeleccionadoId = plan.id;
  }
  
  seleccionarFactura(factura: Factura): void {
    this.facturaSeleccionadaId = factura.id;
    this.modalFacturaOpen = true;
  }
  
  // ==========================================
  // MÉTODOS DE MODALES
  // ==========================================
  
  abrirModalActualizarPlan(): void {
    if (!this.planSeleccionadoId) {
      // Si no hay plan seleccionado, usar el siguiente al actual o el primero
      const planesList = this.planes();
      const currentIndex = planesList.findIndex(p => p.id === this.planActualId);
      if (currentIndex + 1 < planesList.length) {
        this.planSeleccionadoId = planesList[currentIndex + 1].id;
      } else {
        this.planSeleccionadoId = planesList[0].id;
      }
    }
    this.modalActualizarOpen = true;
  }
  
  cerrarModalActualizar(): void {
    this.modalActualizarOpen = false;
  }
  
  confirmarActualizarPlan(): void {
    const planSeleccionado = this.planSeleccionado;
    if (planSeleccionado) {
      this.planActualId = planSeleccionado.id;
      this.metricas.limiteUsuarios = planSeleccionado.limiteUsuarios;
      this.metricas.limiteAlmacenamiento = planSeleccionado.limiteAlmacenamiento;
      
      // Agregar nueva factura por el cambio de plan
      const nuevaFactura: Factura = {
        id: Date.now(),
        codigo: `INV ${new Date().getFullYear()}-${String(this.facturas().length + 1).padStart(3, '0')}`,
        plan: `Plan ${planSeleccionado.nombre}`,
        fecha: new Date(),
        monto: planSeleccionado.precio,
        estado: 'Pendiente'
      };
      this.facturas.update(f => [nuevaFactura, ...f]);
      
      this.mostrarToast(` Plan actualizado a ${planSeleccionado.nombre}`);
      this.planSeleccionadoId = null;
      this.cerrarModalActualizar();
    }
  }
  
  cerrarModalFactura(): void {
    this.modalFacturaOpen = false;
    this.facturaSeleccionadaId = null;
  }
  
  // ==========================================
  // MÉTODOS DE ACCIÓN
  // ==========================================
  
  exportarFacturas(): void {
    this.mostrarToast('Exportando facturas...');
  }
  
  descargarFactura(): void {
    this.mostrarToast('Descargando factura...');
  }
  
  // ==========================================
  // MÉTODOS UTILITARIOS
  // ==========================================
  
  private mostrarToast(mensaje: string): void {
    this.toastService.info(mensaje);
  }
}