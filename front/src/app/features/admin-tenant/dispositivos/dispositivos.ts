import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { CorehrService } from '../../../api/services/corehr.service';
import { OrgService } from '../../../api/services/org.service';
import { AuthService } from '../../../api/services/auth.service';
import { API_BASE_URL } from '../../../api/services/helpers';
import { ToastService } from '../../../services/toast.service';
import { publicKeySha256 } from '../../../shared/security/public-key-fingerprint';
import type { AgentTokenResponse, DeviceProfile } from '../../../api/types';

interface SedeOption {
  id: number;
  nombre: string;
}

/**
 * Lectores biométricos del tenant y la credencial de su agente. El token del agente se muestra una sola vez,
 * al emitirlo: solo vive en este componente mientras el panel está abierto y no se guarda en ningún sitio.
 */
@Component({
  selector: 'app-dispositivos',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dispositivos.html',
  styleUrl: './dispositivos.css',
})
export class Dispositivos implements OnInit {
  private readonly corehr = inject(CorehrService);
  private readonly org = inject(OrgService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly apiBase = inject(API_BASE_URL, { optional: true }) ?? '/api/v1';

  readonly loading = signal(false);
  readonly error = signal('');
  readonly devices = signal<DeviceProfile[]>([]);
  readonly sedes = signal<SedeOption[]>([]);

  readonly newCode = signal('');
  readonly newName = signal('');
  readonly newBranchId = signal<number | null>(null);
  readonly newUbicacion = signal('');
  readonly saving = signal(false);

  readonly issuedToken = signal<AgentTokenResponse | null>(null);
  readonly issuing = signal<number | null>(null);
  readonly copied = signal(false);
  readonly publicKeyFingerprint = signal<string | null>(null);

  readonly backendUrl = computed(() => new URL(this.apiBase, window.location.origin).href.replace(/\/$/, ''));

  /** El token se pide aparte (Read-Host seguro) para que no quede en el historial de la consola. */
  readonly configureCommand = computed(() => {
    const issued = this.issuedToken();
    if (!issued) return '';
    const pin = this.publicKeyFingerprint();
    return [
      '.\\Configure-Agent.ps1',
      `-TenantId "${issued.tenant_id}"`,
      `-DeviceCode "${issued.device_code}"`,
      `-BackendBaseUrl "${this.backendUrl()}"`,
      ...(pin ? [`-PinnedPublicKeySha256 "${pin}"`] : []),
      '-AgentTokenSecure (Read-Host "AgentToken" -AsSecureString)',
    ].join(' ');
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      devices: this.corehr.listDevices({ size: 200 }),
      sedes: this.org.listBranches({ size: 200 }),
    }).subscribe({
      next: ({ devices, sedes }) => {
        this.devices.set(devices.content);
        this.sedes.set(sedes.content.map(b => ({ id: b.id, nombre: b.name })));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los dispositivos. Vuelve a intentarlo.');
        this.loading.set(false);
      },
    });
  }

  onBranchChange(value: string): void {
    this.newBranchId.set(value ? Number(value) : null);
  }

  crearDispositivo(): void {
    const code = this.newCode().trim();
    const branchId = this.newBranchId();
    if (!code || branchId == null) {
      this.toast.error('Indica el código del lector y su sede.');
      return;
    }
    this.saving.set(true);
    this.corehr.createDevice({
      code,
      name: this.newName().trim() || null,
      branch_id: branchId,
      ubicacion: this.newUbicacion().trim() || null,
    }).subscribe({
      next: created => {
        this.devices.update(list => [...list, created]);
        this.newCode.set('');
        this.newName.set('');
        this.newUbicacion.set('');
        this.saving.set(false);
        this.toast.success(`Lector ${created.code} registrado. Genera su token para configurar el agente.`);
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(this.apiMessage(err, 'No se pudo registrar el lector.'));
      },
    });
  }

  desactivar(device: DeviceProfile): void {
    this.corehr.patchDevice(device.id, { state: false }).subscribe({
      next: updated => {
        this.devices.update(list => list.map(d => (d.id === updated.id ? updated : d)));
        this.toast.success(`Lector ${device.code} desactivado.`);
      },
      error: err => this.toast.error(this.apiMessage(err, 'No se pudo desactivar el lector.')),
    });
  }

  emitirToken(device: DeviceProfile): void {
    this.issuing.set(device.id);
    this.corehr.issueAgentToken(device.id).subscribe({
      next: issued => {
        this.issuing.set(null);
        this.copied.set(false);
        this.issuedToken.set(issued);
        this.cargarHuellaDeLaClave();
      },
      error: err => {
        this.issuing.set(null);
        this.toast.error(this.apiMessage(err, 'No se pudo generar el token del agente.'));
      },
    });
  }

  revocarToken(device: DeviceProfile): void {
    this.corehr.revokeAgentToken(device.id).subscribe({
      next: () => this.toast.success(`Token del lector ${device.code} revocado: su agente ya no puede marcar.`),
      error: err => this.toast.error(this.apiMessage(err, 'No se pudo revocar el token.')),
    });
  }

  copiar(texto: string): void {
    navigator.clipboard?.writeText(texto)
      .then(() => this.copied.set(true))
      .catch(() => this.toast.error('No se pudo copiar: selecciona el texto y cópialo a mano.'));
  }

  cerrarToken(): void {
    this.issuedToken.set(null);
    this.copied.set(false);
    this.publicKeyFingerprint.set(null);
  }

  private cargarHuellaDeLaClave(): void {
    this.auth.getPublicKey().subscribe({
      next: key => publicKeySha256(key.publicKey).then(fp => this.publicKeyFingerprint.set(fp)),
      error: () => this.publicKeyFingerprint.set(null),
    });
  }

  private apiMessage(err: unknown, fallback: string): string {
    const status = (err as { status?: number })?.status;
    if (status === 403) return 'No tienes permiso para gestionar los lectores.';
    return (err as { error?: { message?: string } })?.error?.message ?? fallback;
  }
}
