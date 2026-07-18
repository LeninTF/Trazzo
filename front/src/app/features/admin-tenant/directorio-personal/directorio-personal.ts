import { Component, signal, WritableSignal, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { ToastService } from '../../../services/toast.service';
import { ModalService } from '../../../services/modal.service';
import { ApiService } from '../../../api/services/api.service';
import { tenantUserToPersonal } from '../../../api/services/helpers';
import { firstValueFrom } from 'rxjs';
import {
  MiddlewareWebSocketService,
  type MiddlewareFingerprintEnrollProgress,
  type MiddlewareFingerprintEnrollResult,
  type MiddlewareFingerprintQuality,
} from '../../../services/middleware-websocket.service';
import { FingerprintStoreService } from '../../../services/fingerprint-store.service';

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

interface LogEntry {
  timestamp: string;
  message: string;
  type: 'info' | 'success' | 'error' | 'warn';
}

@Component({
  selector: 'app-directorio-personal',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './directorio-personal.html',
  styleUrl: './directorio-personal.css',
})
export class DirectorioPersonal implements OnInit, OnDestroy {

  private readonly toastService = inject(ToastService);
  private readonly modalService = inject(ModalService);
  private readonly api = inject(ApiService);
  private readonly middlewareWs = inject(MiddlewareWebSocketService);
  private readonly fingerprintStore = inject(FingerprintStoreService);
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
  // ENROLLMENT STATE
  // ==========================================
  modalEnrolarOpen: boolean = false;
  personaEnrolar: Personal | null = null;

  readonly enrolConnectionState = this.middlewareWs.connectionState.asReadonly();
  readonly enrolOperationLabel = signal('');
  readonly enrolInProgress = signal(false);
  readonly enrolProgress = signal('');
  readonly enrolFpImageUrl = signal<string | null>(null);
  readonly enrolFpStatus = signal('');
  readonly enrolQuality = signal<MiddlewareFingerprintQuality | null>(null);
  readonly enrolTemplateSize = signal(0);
  readonly enrolTemplatePreview = signal('');
  readonly enrolLogs = signal<LogEntry[]>([]);
  readonly enrolCompleted = signal(false);
  readonly enrolSuccess = signal(false);
  readonly enrolAwaitingReference = signal(false);
  private enrolReferenceData: { templateBase64: string; templateSize: number } | null = null;

  private unsubscribeEnrol: (() => void)[] = [];

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

  ngOnInit(): void {
    this.cargarPersonal();
  }

  ngOnDestroy(): void {
    this.cleanupEnrollmentSubscriptions();
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
        this.mostrarToast('El archivo no puede superar los 2MB');
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
          email: this.personalForm.email?.trim() || '',
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
          email: this.personalForm.email?.trim() || `${this.personalForm.nombre.toLowerCase().replace(/\s+/g, '.')}@colegio.edu.pe`,
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
  // ENROLLMENT MODAL
  // ==========================================

  abrirModalEnrolar(persona: Personal): void {
    this.personaEnrolar = persona;
    this.modalEnrolarOpen = true;
    this.enrolCompleted.set(false);
    this.enrolSuccess.set(false);
    this.enrolOperationLabel.set('');
    this.enrolInProgress.set(false);
    this.enrolAwaitingReference.set(false);
    this.enrolReferenceData = null;
    this.enrolProgress.set('');
    this.enrolFpImageUrl.set(null);
    this.enrolFpStatus.set('Preparando conexión...');
    this.enrolQuality.set(null);
    this.enrolTemplateSize.set(0);
    this.enrolTemplatePreview.set('');
    this.enrolLogs.set([]);

    this.addEnrolLog(`Iniciando enrolamiento para: ${persona.nombre}`, 'info');
    this.addEnrolLog('Conectando al middleware biométrico...', 'info');

    this.middlewareWs.connect();
    this.setupEnrollmentSubscriptions();
  }

  cerrarModalEnrolar(): void {
    this.modalEnrolarOpen = false;
    this.personaEnrolar = null;
    this.enrolAwaitingReference.set(false);
    this.enrolReferenceData = null;
    this.cleanupEnrollmentSubscriptions();
    if (this.enrolInProgress()) {
      this.middlewareWs.send('fingerprint.enroll.cancel');
    }
  }

  private setupEnrollmentSubscriptions(): void {
    this.cleanupEnrollmentSubscriptions();

    this.unsubscribeEnrol.push(
      this.middlewareWs.on('fingerprint.enroll.progress', (msg) => {
        const data = msg as MiddlewareFingerprintEnrollProgress;
        this.enrolProgress.set(`${data.step}/${data.totalSteps}`);
        this.enrolFpStatus.set(data.message || 'Coloca el dedo en el lector...');
        this.enrolOperationLabel.set('Enrolando huella...');
        this.enrolInProgress.set(true);
        this.addEnrolLog(`Progreso: ${data.message}`, 'info');
      })
    );

    this.unsubscribeEnrol.push(
      this.middlewareWs.on('fingerprint.enroll.result', (msg) => {
        const data = msg as MiddlewareFingerprintEnrollResult;
        this.enrolCompleted.set(true);
        this.enrolSuccess.set(data.success);
        this.enrolProgress.set(data.success ? `${data.capturedSamples ?? 0}/3` : '—');
        this.enrolTemplateSize.set(data.registeredTemplateSize);
        this.enrolTemplatePreview.set(data.registeredTemplateBase64
          ? `${data.registeredTemplateBase64.slice(0, 18)}…` : '(cifrado)');
        this.enrolFpStatus.set(data.message || (data.success ? 'Enrolamiento completado.' : 'Enrolamiento fallido.'));
        this.enrolOperationLabel.set(data.success ? 'Completado' : 'Fallido');
        this.enrolInProgress.set(false);

        if (data.success && this.personaEnrolar) {
          const ref = this.enrolReferenceData ?? { templateBase64: '', templateSize: 0 };
          this.fingerprintStore.save({
            userId: this.personaEnrolar.id,
            userName: this.personaEnrolar.nombre,
            userDisplayId: this.personaEnrolar.idPersonal,
            templateBase64: data.registeredTemplateBase64 || '',
            templateSize: data.registeredTemplateSize,
            referenceTemplateBase64: ref.templateBase64,
            referenceTemplateSize: ref.templateSize,
            encryptedTemplate: data.encryptedRegisteredTemplate
              ? { encryptedTemplateBase64: data.encryptedRegisteredTemplate.encryptedTemplateBase64, encryptedAesKeyBase64: data.encryptedRegisteredTemplate.encryptedAesKeyBase64 }
              : null,
            enrolledAt: new Date().toISOString(),
          });
          this.enrolReferenceData = null;
          this.addEnrolLog(`Enrolamiento exitoso para ${this.personaEnrolar.nombre}`, 'success');
          this.addEnrolLog(`Tamaño de template: ${data.registeredTemplateSize} bytes — almacenado localmente`, 'success');
          this.toastService.success(`Huella enrolada para ${this.personaEnrolar.nombre}`);
        } else {
          this.addEnrolLog(`Enrolamiento fallido: ${data.message}`, 'error');
          this.toastService.error(`Error al enrolar: ${data.message}`);
        }
      })
    );

    this.unsubscribeEnrol.push(
      this.middlewareWs.on('fingerprint.capture.result', (msg) => {
        const data = msg as any;
        if (data.fingerprintImageDataUrl) {
          this.enrolFpImageUrl.set(data.fingerprintImageDataUrl);
        }
        if (data.quality) {
          this.enrolQuality.set(data.quality);
        }
        if (this.enrolAwaitingReference()) {
          this.enrolAwaitingReference.set(false);
          if (data.success && data.templateBase64) {
            this.enrolReferenceData = {
              templateBase64: data.templateBase64,
              templateSize: data.templateSize ?? 0,
            };
            this.addEnrolLog('Referencia capturada. Iniciando enrolamiento (3 capturas)...', 'info');
            this.enrolOperationLabel.set('Enrolando huella...');
            this.enrolFpStatus.set('Coloca el dedo para captura 1 de 3...');
            this.middlewareWs.send('fingerprint.enroll.start');
          } else {
            this.addEnrolLog('Fallo captura de referencia. Reintente.', 'error');
            this.enrolFpStatus.set(data.message || 'Error capturando referencia.');
            this.enrolOperationLabel.set('Error');
            this.enrolInProgress.set(false);
            this.enrolCompleted.set(true);
            this.enrolSuccess.set(false);
          }
        }
      })
    );
  }

  private cleanupEnrollmentSubscriptions(): void {
    this.unsubscribeEnrol.forEach(fn => fn());
    this.unsubscribeEnrol = [];
  }

  iniciarEnrolamiento(): void {
    if (this.enrolInProgress()) return;
    this.enrolCompleted.set(false);
    this.enrolSuccess.set(false);
    this.enrolProgress.set('0/3');
    this.enrolFpImageUrl.set(null);
    this.enrolQuality.set(null);
    this.enrolTemplateSize.set(0);
    this.enrolTemplatePreview.set('');
    this.enrolReferenceData = null;
    this.enrolOperationLabel.set('Capturando referencia...');
    this.enrolInProgress.set(true);
    this.enrolAwaitingReference.set(true);
    this.enrolFpStatus.set('Coloca el dedo para captura de referencia...');
    this.addEnrolLog('Capturando huella de referencia (1 de 4)...', 'info');
    this.middlewareWs.send('fingerprint.capture');
  }

  reintentarEnrolamiento(): void {
    if (this.enrolInProgress()) return;
    this.addEnrolLog('Reintentando enrolamiento...', 'info');
    this.iniciarEnrolamiento();
  }

  cancelarEnrolamiento(): void {
    if (!this.enrolInProgress()) return;
    this.enrolAwaitingReference.set(false);
    this.enrolReferenceData = null;
    this.addEnrolLog('Cancelando enrolamiento...', 'warn');
    this.middlewareWs.send('fingerprint.enroll.cancel');
    this.enrolInProgress.set(false);
    this.enrolOperationLabel.set('Cancelado');
    this.enrolFpStatus.set('Enrolamiento cancelado.');
  }

  private addEnrolLog(message: string, type: LogEntry['type'] = 'info'): void {
    const now = new Date();
    const ts = now.toLocaleTimeString('es-PE', { hour12: false });
    this.enrolLogs.update(list => [...list.slice(-99), { timestamp: ts, message, type }]);
  }

  // ==========================================
  // MÉTODOS UTILITARIOS
  // ==========================================

  private mostrarToast(mensaje: string): void {
    this.toastService.info(mensaje);
  }
}
