import { Component, signal, WritableSignal, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { ToastService } from '../../../services/toast.service';
import { ModalService } from '../../../services/modal.service';
import { ApiService } from '../../../api/services/api.service';
import { tenantUserToPersonal } from '../../../api/services/helpers';
import { Subscription, firstValueFrom, map, switchMap, timer } from 'rxjs';
import {
  MiddlewareWebSocketService,
  type MiddlewareFingerprintEnrollProgress,
  type MiddlewareFingerprintEnrollResult,
} from '../../../services/middleware-websocket.service';
import type { DeviceProfile } from '../../../api/types';

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

// La sesión de enrolamiento del backend dura 120 s; se espera un poco más por la latencia del agente.
export const ENROLL_POLL_MS = 3000;
export const ENROLL_TIMEOUT_MS = 130_000;

export const DEDOS: ReadonlyArray<{ index: number; label: string }> = [
  { index: 0, label: 'Pulgar derecho' },
  { index: 1, label: 'Índice derecho' },
  { index: 2, label: 'Medio derecho' },
  { index: 3, label: 'Anular derecho' },
  { index: 4, label: 'Meñique derecho' },
  { index: 5, label: 'Pulgar izquierdo' },
  { index: 6, label: 'Índice izquierdo' },
  { index: 7, label: 'Medio izquierdo' },
  { index: 8, label: 'Anular izquierdo' },
  { index: 9, label: 'Meñique izquierdo' },
];

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
  // El panel abre una sesión en el backend; el agente del lector elegido la toma (enroll/pendiente), captura
  // y la completa. El panel confirma consultando las huellas del usuario y, si el agente corre en esta PC,
  // muestra además el progreso que publica por WebSocket.
  modalEnrolarOpen: boolean = false;
  personaEnrolar: Personal | null = null;

  readonly dedos = DEDOS;
  readonly enrolConnectionState = this.middlewareWs.connectionState.asReadonly();
  readonly enrolDevices = signal<DeviceProfile[]>([]);
  readonly enrolDeviceId = signal<number | null>(null);
  readonly enrolFingerIndex = signal(1);
  readonly enrolOperationLabel = signal('');
  readonly enrolInProgress = signal(false);
  readonly enrolProgress = signal('');
  readonly enrolFpStatus = signal('');
  readonly enrolLogs = signal<LogEntry[]>([]);
  readonly enrolCompleted = signal(false);
  readonly enrolSuccess = signal(false);

  private unsubscribeEnrol: (() => void)[] = [];
  private enrolPolling: Subscription | null = null;

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
    this.stopEnrollPolling();
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
    this.stopEnrollPolling();
    this.personaEnrolar = persona;
    this.modalEnrolarOpen = true;
    this.enrolCompleted.set(false);
    this.enrolSuccess.set(false);
    this.enrolOperationLabel.set('');
    this.enrolInProgress.set(false);
    this.enrolProgress.set('');
    this.enrolFpStatus.set('Elige el lector y el dedo, y pulsa «Iniciar enrolamiento».');
    this.enrolLogs.set([]);
    this.enrolDevices.set([]);
    this.enrolDeviceId.set(null);
    this.enrolFingerIndex.set(1);

    this.addEnrolLog(`Enrolamiento de huella para: ${persona.nombre}`, 'info');
    this.api.corehr.listDevices({ state: true, size: 200 }).subscribe({
      next: page => {
        this.enrolDevices.set(page.content);
        this.enrolDeviceId.set(page.content[0]?.id ?? null);
        if (page.content.length === 0) {
          this.enrolFpStatus.set('No hay lectores activos: regístralos en Dispositivos.');
        }
      },
      error: () => this.enrolFpStatus.set('No se pudieron cargar los lectores.'),
    });

    // Opcional: si el agente corre en esta PC, su progreso llega por WebSocket.
    this.middlewareWs.connect();
    this.setupEnrollmentSubscriptions();
  }

  cerrarModalEnrolar(): void {
    this.modalEnrolarOpen = false;
    this.personaEnrolar = null;
    this.stopEnrollPolling();
    this.cleanupEnrollmentSubscriptions();
  }

  onEnrolDeviceChange(value: string): void {
    this.enrolDeviceId.set(value ? Number(value) : null);
  }

  onEnrolFingerChange(value: string): void {
    this.enrolFingerIndex.set(Number(value));
  }

  private setupEnrollmentSubscriptions(): void {
    this.cleanupEnrollmentSubscriptions();

    this.unsubscribeEnrol.push(
      this.middlewareWs.on('fingerprint.enroll.progress', (msg) => {
        const data = msg as MiddlewareFingerprintEnrollProgress;
        if (!this.isOwnBackendEnrollment(data)) return;
        this.enrolProgress.set(`${data.step}/${data.totalSteps}`);
        this.enrolFpStatus.set(data.message || 'Coloca el dedo en el lector...');
        this.enrolOperationLabel.set('Capturando en el lector...');
        this.addEnrolLog(`Lector: ${data.message}`, 'info');
      })
    );

    this.unsubscribeEnrol.push(
      this.middlewareWs.on('fingerprint.enroll.result', (msg) => {
        const data = msg as MiddlewareFingerprintEnrollResult;
        if (!this.isOwnBackendEnrollment(data)) return;
        if (data.success) {
          this.enrolFpStatus.set('Huella capturada. Confirmando con el servidor...');
          this.addEnrolLog('El lector terminó la captura; esperando la confirmación del servidor.', 'info');
        } else {
          this.terminarEnrolamiento(false, data.message || 'El lector no pudo completar el enrolamiento.');
        }
      })
    );
  }

  // Solo cuenta lo que el agente publica para la sesión abierta de esta persona y este dedo.
  private isOwnBackendEnrollment(data: { source?: string; tenantUserId?: number; fingerIndex?: number }): boolean {
    return this.enrolInProgress()
      && data.source === 'backend'
      && data.tenantUserId === this.personaEnrolar?.id
      && (data.fingerIndex === undefined || data.fingerIndex === this.enrolFingerIndex());
  }

  private cleanupEnrollmentSubscriptions(): void {
    this.unsubscribeEnrol.forEach(fn => fn());
    this.unsubscribeEnrol = [];
  }

  iniciarEnrolamiento(): void {
    const persona = this.personaEnrolar;
    if (this.enrolInProgress() || !persona) return;
    const deviceId = this.enrolDeviceId();
    if (deviceId == null) {
      this.toastService.error('Elige el lector en el que se va a enrolar la huella.');
      return;
    }
    const fingerIndex = this.enrolFingerIndex();
    this.enrolCompleted.set(false);
    this.enrolSuccess.set(false);
    this.enrolProgress.set('0/3');
    this.enrolInProgress.set(true);
    this.enrolOperationLabel.set('Abriendo sesión...');
    this.enrolFpStatus.set('Abriendo la sesión de enrolamiento en el servidor...');

    // Las huellas que ya tenía: la nueva es la que aparezca después para ese dedo.
    this.api.corehr.listBiometria({ tenant_user_id: persona.id, activo: true, size: 50 }).pipe(
      switchMap(before => this.api.corehr.initEnroll({ tenant_user_id: persona.id, device_id: deviceId, finger_index: fingerIndex })
        .pipe(map(session => ({ knownIds: new Set(before.content.map(b => b.id)), session })))),
    ).subscribe({
      next: ({ knownIds, session }) => {
        this.enrolOperationLabel.set('Esperando al lector...');
        this.enrolFpStatus.set(`Ve al lector ${session.device_code} y coloca el dedo cuando lo pida (3 capturas).`);
        this.addEnrolLog(`Sesión abierta en el lector ${session.device_code}.`, 'info');
        this.esperarHuella(persona, fingerIndex, knownIds);
      },
      error: err => this.terminarEnrolamiento(false, err?.error?.message ?? 'No se pudo iniciar el enrolamiento.'),
    });
  }

  // El servidor es la fuente de verdad: la huella está enrolada cuando aparece activa para ese dedo.
  private esperarHuella(persona: Personal, fingerIndex: number, knownIds: Set<number>): void {
    this.stopEnrollPolling();
    const startedAt = Date.now();
    this.enrolPolling = timer(ENROLL_POLL_MS, ENROLL_POLL_MS).pipe(
      switchMap(() => this.api.corehr.listBiometria({ tenant_user_id: persona.id, activo: true, size: 50 })),
    ).subscribe({
      next: page => {
        const enrolled = page.content.find(b => b.finger_index === fingerIndex && !knownIds.has(b.id));
        if (enrolled) {
          this.terminarEnrolamiento(true, `Huella enrolada en el lector ${enrolled.device_code ?? ''}`.trim() + '.');
        } else if (Date.now() - startedAt >= ENROLL_TIMEOUT_MS) {
          this.terminarEnrolamiento(false, 'El lector no completó el enrolamiento a tiempo. Vuelve a intentarlo.');
        }
      },
      error: () => this.terminarEnrolamiento(false, 'Se perdió la conexión con el servidor durante el enrolamiento.'),
    });
  }

  private terminarEnrolamiento(success: boolean, message: string): void {
    this.stopEnrollPolling();
    this.enrolInProgress.set(false);
    this.enrolCompleted.set(true);
    this.enrolSuccess.set(success);
    this.enrolOperationLabel.set(success ? 'Completado' : 'Fallido');
    this.enrolFpStatus.set(message);
    if (success) {
      this.enrolProgress.set('3/3');
      this.addEnrolLog(message, 'success');
      this.toastService.success(`Huella enrolada para ${this.personaEnrolar?.nombre ?? 'el usuario'}`);
    } else {
      this.addEnrolLog(message, 'error');
      this.toastService.error(`Error al enrolar: ${message}`);
    }
  }

  private stopEnrollPolling(): void {
    this.enrolPolling?.unsubscribe();
    this.enrolPolling = null;
  }

  reintentarEnrolamiento(): void {
    if (this.enrolInProgress()) return;
    this.addEnrolLog('Reintentando enrolamiento...', 'info');
    this.iniciarEnrolamiento();
  }

  // La sesión del servidor no se puede cerrar desde aquí: caduca sola.
  cancelarEnrolamiento(): void {
    if (!this.enrolInProgress()) return;
    this.stopEnrollPolling();
    this.enrolInProgress.set(false);
    this.enrolOperationLabel.set('Cancelado');
    this.enrolFpStatus.set('Enrolamiento cancelado. La sesión del lector caduca sola en unos minutos.');
    this.addEnrolLog('Enrolamiento cancelado en el panel.', 'warn');
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
