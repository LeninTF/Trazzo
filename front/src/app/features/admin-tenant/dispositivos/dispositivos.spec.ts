import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Dispositivos } from './dispositivos';
import { CorehrService } from '../../../api/services/corehr.service';
import { OrgService } from '../../../api/services/org.service';
import { AuthService } from '../../../api/services/auth.service';
import { API_BASE_URL } from '../../../api/services/helpers';
import { ToastService } from '../../../services/toast.service';
import type { AgentTokenResponse, DeviceProfile } from '../../../api/types';

describe('Dispositivos', () => {
  let component: Dispositivos;
  let fixture: ComponentFixture<Dispositivos>;

  const device = (id: number, code: string, state = true): DeviceProfile => ({
    id, code, name: `Lector ${id}`, branch_id: 1, branch_name: 'Sede Central',
    ip: null, puerto: null, ubicacion: 'Ingreso', state, created_at: '2026-10-01T08:00:00',
  });

  const issued: AgentTokenResponse = {
    token: 'trzd_secretoQueSoloSeMuestraUnaVez0123456789abcd',
    tenant_id: '11111111-2222-3333-4444-555555555555',
    device_code: 'ZK-001',
    created_at: '2026-10-23T09:00:00',
  };

  let mockCorehr: jasmine.SpyObj<CorehrService>;
  let mockOrg: jasmine.SpyObj<OrgService>;
  let mockToast: jasmine.SpyObj<ToastService>;
  let mockAuth: jasmine.SpyObj<AuthService>;

  // Clave pública test-only; su huella (SHA-256 del SPKI) es la que el agente compara al fijar la clave.
  const PUBLIC_KEY = [
    '-----BEGIN PUBLIC KEY-----',
    'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6kF8CaGA8o0IKiaosXoZ',
    'nmTkqjjDqA0QbBxw7gw0ddmbTyMuIQdyWFO/sUbq/Ao164w+zYL+mqyB7PuaDW4Q',
    'bJdTAcVyoEHmiVf31ykpnmZRMsvyfQgWzAOl9yJ90gbA/HoVjS06S9/rPBBzTnwH',
    '1E6KpOsoiNkyvUCaMX+oa2o3y3EL8O+Gl92GN0KE7a/aBTHH1HpFgkgSDREwhMCl',
    'ICRjDJsLmFt2LkUX5ZDbCN96EV53uIQK568RGdYArn8JsOBdUYA78hiltrpFXZZj',
    'vI7hsCAzT6io0EiWicop0ovDhZzK9KppYQfnzGw6SZ+576UCrlNq8bKD8SYGN9vZ',
    'fwIDAQAB',
    '-----END PUBLIC KEY-----',
  ].join('\n');
  const PUBLIC_KEY_SHA256 = 'ede7b18e96401468935f0bac78512e5f84acd549fb54142aee897fc47119556c';

  // La huella sale de crypto.subtle (promesa nativa, fuera de la zona de Angular).
  async function waitForFingerprint(): Promise<void> {
    for (let i = 0; i < 50 && !component.publicKeyFingerprint(); i++) {
      await new Promise(resolve => setTimeout(resolve, 10));
    }
  }

  const page = <T>(content: T[]) => ({ content, page: 0, size: 200, totalElements: content.length, totalPages: 1 });

  async function render(): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [Dispositivos],
      providers: [
        { provide: CorehrService, useValue: mockCorehr },
        { provide: OrgService, useValue: mockOrg },
        { provide: ToastService, useValue: mockToast },
        { provide: AuthService, useValue: mockAuth },
        { provide: API_BASE_URL, useValue: 'https://trazzo.test/api/v1' },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(Dispositivos);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  const text = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

  beforeEach(() => {
    mockCorehr = jasmine.createSpyObj<CorehrService>('CorehrService',
      ['listDevices', 'createDevice', 'patchDevice', 'issueAgentToken', 'revokeAgentToken']);
    mockOrg = jasmine.createSpyObj<OrgService>('OrgService', ['listBranches']);
    mockToast = jasmine.createSpyObj<ToastService>('ToastService', ['success', 'error', 'info']);
    mockAuth = jasmine.createSpyObj<AuthService>('AuthService', ['getPublicKey']);
    mockAuth.getPublicKey.and.returnValue(of({ publicKey: PUBLIC_KEY, kid: 'rsa-test' }));
    mockCorehr.listDevices.and.returnValue(of(page([device(1, 'ZK-001'), device(2, 'ZK-002', false)])));
    mockOrg.listBranches.and.returnValue(of(page([
      { id: 1, name: 'Sede Central', description: null, state: true, createdAt: '2025-01-01', updatedAt: '2025-01-01' },
    ])) as any);
  });

  it('lista los dispositivos del tenant al abrir', async () => {
    await render();

    expect(mockCorehr.listDevices).toHaveBeenCalled();
    expect(component.devices().map(d => d.code)).toEqual(['ZK-001', 'ZK-002']);
    expect(text()).toContain('ZK-001');
    expect(text()).toContain('ZK-002');
  });

  it('da de alta un dispositivo con los datos del formulario', async () => {
    await render();
    mockCorehr.createDevice.and.returnValue(of(device(3, 'ZK-003')));

    component.newCode.set('ZK-003');
    component.newName.set('Lector patio');
    component.newBranchId.set(1);
    component.newUbicacion.set('Patio');
    component.crearDispositivo();
    fixture.detectChanges();

    expect(mockCorehr.createDevice).toHaveBeenCalledWith({
      code: 'ZK-003', name: 'Lector patio', branch_id: 1, ubicacion: 'Patio',
    });
    expect(component.devices().map(d => d.code)).toContain('ZK-003');
    expect(component.newCode()).toBe('');
    expect(mockToast.success).toHaveBeenCalled();
  });

  it('no da de alta un dispositivo sin código ni sede', async () => {
    await render();

    component.crearDispositivo();

    expect(mockCorehr.createDevice).not.toHaveBeenCalled();
    expect(mockToast.error).toHaveBeenCalled();
  });

  it('muestra el token del agente una sola vez y lo oculta al cerrar', async () => {
    await render();
    mockCorehr.issueAgentToken.and.returnValue(of(issued));

    component.emitirToken(component.devices()[0]);
    fixture.detectChanges();

    expect(mockCorehr.issueAgentToken).toHaveBeenCalledWith(1);
    expect(text()).toContain(issued.token);
    expect(text()).toContain(issued.tenant_id);

    component.cerrarToken();
    fixture.detectChanges();

    expect(component.issuedToken()).toBeNull();
    expect(text()).not.toContain(issued.token);
  });

  it('el comando para el lector lleva tenant, código, backend y huella de la clave, pero no el token', async () => {
    await render();
    mockCorehr.issueAgentToken.and.returnValue(of(issued));

    component.emitirToken(component.devices()[0]);
    await waitForFingerprint();
    fixture.detectChanges();

    const command = component.configureCommand();
    expect(component.publicKeyFingerprint()).toBe(PUBLIC_KEY_SHA256);
    // Comillas simples: PowerShell no expande $(...) dentro de ellas.
    expect(command).toContain(`-TenantId '${issued.tenant_id}'`);
    expect(command).toContain(`-DeviceCode 'ZK-001'`);
    expect(command).toContain(`-BackendBaseUrl 'https://trazzo.test/api/v1'`);
    expect(command).toContain(`-PinnedPublicKeySha256 '${PUBLIC_KEY_SHA256}'`);
    expect(command).not.toContain(issued.token);
    expect(text()).toContain(PUBLIC_KEY_SHA256);
  });

  // El administrador ejecuta el comando como administrador en el lector: un código con $(...) o comillas
  // no debe llegar a él.
  it('no genera el comando si el código del lector o el tenant traen caracteres no permitidos', async () => {
    await render();
    mockCorehr.issueAgentToken.and.returnValue(of({ ...issued, device_code: `ZK"; $(Remove-Item C:\\ -Recurse) #` }));

    component.emitirToken(component.devices()[0]);
    fixture.detectChanges();

    expect(component.configureCommand()).toBe('');
    expect(text()).toContain('configúralo a mano');
  });

  it('revoca el token del agente de un dispositivo', async () => {
    await render();
    mockCorehr.revokeAgentToken.and.returnValue(of(undefined));

    component.revocarToken(component.devices()[0]);

    expect(mockCorehr.revokeAgentToken).toHaveBeenCalledWith(1);
    expect(mockToast.success).toHaveBeenCalled();
  });

  it('desactiva un dispositivo', async () => {
    await render();
    mockCorehr.patchDevice.and.returnValue(of(device(1, 'ZK-001', false)));

    component.desactivar(component.devices()[0]);

    expect(mockCorehr.patchDevice).toHaveBeenCalledWith(1, { state: false });
    expect(component.devices()[0].state).toBeFalse();
  });

  it('informa si la API no responde al cargar', async () => {
    mockCorehr.listDevices.and.returnValue(throwError(() => new Error('500')));

    await render();

    expect(component.error()).toBeTruthy();
    expect(text()).toContain(component.error());
  });

  it('informa si no se pudo emitir el token y no muestra ninguno', async () => {
    await render();
    mockCorehr.issueAgentToken.and.returnValue(throwError(() => ({ status: 403 })));

    component.emitirToken(component.devices()[0]);
    fixture.detectChanges();

    expect(component.issuedToken()).toBeNull();
    expect(mockToast.error).toHaveBeenCalled();
  });
});
