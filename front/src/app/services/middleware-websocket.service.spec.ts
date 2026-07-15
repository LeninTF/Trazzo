import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MiddlewareWebSocketService } from './middleware-websocket.service';
import { ToastService } from './toast.service';

describe('MiddlewareWebSocketService', () => {
  let service: MiddlewareWebSocketService;
  let mockToast: jasmine.SpyObj<ToastService>;
  let mockWsInstance: {
    readyState: number;
    send: jasmine.Spy;
    close: jasmine.Spy;
    onopen: (() => void) | null;
    onmessage: ((event: any) => void) | null;
    onclose: ((event: any) => void) | null;
    onerror: ((event: any) => void) | null;
  };
  let originalWebSocket: any;

  function createMockWsInstance() {
    mockWsInstance = {
      readyState: 1,
      send: jasmine.createSpy('send'),
      close: jasmine.createSpy('close'),
      onopen: null,
      onmessage: null,
      onclose: null,
      onerror: null,
    };
    return mockWsInstance;
  }

  beforeEach(() => {
    mockToast = jasmine.createSpyObj('ToastService', ['error', 'success', 'info']);
    createMockWsInstance();
    originalWebSocket = (window as any).WebSocket;
    const mockWebSocketCtor = jasmine.createSpy('WebSocket').and.returnValue(mockWsInstance);
    (mockWebSocketCtor as any).OPEN = 1;
    (mockWebSocketCtor as any).CONNECTING = 0;
    (window as any).WebSocket = mockWebSocketCtor;

    TestBed.configureTestingModule({
      providers: [
        { provide: ToastService, useValue: mockToast },
      ],
    });
    service = TestBed.inject(MiddlewareWebSocketService);
  });

  afterEach(() => {
    service.disconnect();
    (window as any).WebSocket = originalWebSocket;
  });

  it('creates the service', () => {
    expect(service).toBeTruthy();
  });

  it('should have initial state disconnected', () => {
    expect(service.connectionState()).toBe('disconnected');
    expect(service.lastDeviceStatus()).toBeNull();
    expect(service.lastError()).toBe('');
  });

  describe('connect', () => {
    it('should create a WebSocket connection', () => {
      service.connect();
      expect((window as any).WebSocket).toHaveBeenCalledWith('ws://localhost:9001/');
      expect(service.connectionState()).toBe('connecting');
    });

    it('should use custom url', () => {
      service.connect('ws://custom:9002/');
      expect((window as any).WebSocket).toHaveBeenCalledWith('ws://custom:9002/');
    });

    it('should disconnect existing connection first', () => {
      service.connect();
      const ws1 = mockWsInstance;
      createMockWsInstance();
      service.connect();
      expect(ws1.close).toHaveBeenCalled();
    });

    it('should set connected state on open', () => {
      service.connect();
      mockWsInstance.onopen!();
      expect(service.connectionState()).toBe('connected');
      expect(service.lastError()).toBe('');
    });

    it('should set disconnected state on close', () => {
      service.connect();
      mockWsInstance.onopen!();
      mockWsInstance.onclose!({} as any);
      expect(service.connectionState()).toBe('disconnected');
    });

    it('should not reconnect if destroyed', () => {
      service.connect();
      service.disconnect();
      expect(mockWsInstance.close).toHaveBeenCalled();
    });
  });

  describe('send', () => {
    it('should send JSON when WebSocket is open', () => {
      service.connect();
      mockWsInstance.onopen!();
      service.send('test.type', { key: 'value' });
      expect(mockWsInstance.send).toHaveBeenCalledWith(JSON.stringify({ type: 'test.type', key: 'value' }));
    });

    it('should send type-only message when no payload', () => {
      service.connect();
      mockWsInstance.onopen!();
      service.send('test.type');
      expect(mockWsInstance.send).toHaveBeenCalledWith(JSON.stringify({ type: 'test.type' }));
    });

    it('should show error toast when not connected', () => {
      service.send('test.type');
      expect(mockToast.error).toHaveBeenCalledWith('Middleware no conectado');
    });
  });

  describe('on/off', () => {
    it('should register handler and return unsubscribe function', () => {
      const handler = jasmine.createSpy('handler');
      const unsub = service.on('test.event', handler);
      service.connect();
      mockWsInstance.onopen!();
      mockWsInstance.onmessage!({ data: JSON.stringify({ type: 'test.event' }) });
      expect(handler).toHaveBeenCalledWith({ type: 'test.event' });
      unsub();
      handler.calls.reset();
      mockWsInstance.onmessage!({ data: JSON.stringify({ type: 'test.event' }) });
      expect(handler).not.toHaveBeenCalled();
    });
  });

  describe('dispatch', () => {
    it('should update lastDeviceStatus for device.status type', () => {
      const msg = { type: 'device.status', success: true, isConnected: true, deviceCount: 1, message: 'ok', checkedAtUtc: '', isSdkAvailable: true, isInitialized: true, isDeviceOpen: true };
      service.connect();
      mockWsInstance.onopen!();
      mockWsInstance.onmessage!({ data: JSON.stringify(msg) });
      expect(service.lastDeviceStatus()).toEqual(msg as any);
    });

    it('should call wildcard handlers', () => {
      const wildcard = jasmine.createSpy('wildcard');
      service.on('*', wildcard);
      service.connect();
      mockWsInstance.onopen!();
      mockWsInstance.onmessage!({ data: JSON.stringify({ type: 'anything' }) });
      expect(wildcard).toHaveBeenCalledWith({ type: 'anything' });
    });

    it('should not throw if a handler throws', () => {
      service.on('test', () => { throw new Error('handler error'); });
      service.connect();
      mockWsInstance.onopen!();
      expect(() => {
        mockWsInstance.onmessage!({ data: JSON.stringify({ type: 'test' }) });
      }).not.toThrow();
    });
  });

  describe('disconnect', () => {
    it('should close WebSocket and set disconnected', () => {
      service.connect();
      mockWsInstance.onopen!();
      service.disconnect();
      expect(mockWsInstance.close).toHaveBeenCalled();
      expect(service.connectionState()).toBe('disconnected');
    });

    it('should nullify ws event handlers before closing', () => {
      service.connect();
      service.disconnect();
      expect(mockWsInstance.onopen).toBeNull();
      expect(mockWsInstance.onmessage).toBeNull();
      expect(mockWsInstance.onclose).toBeNull();
      expect(mockWsInstance.onerror).toBeNull();
    });
  });

  describe('reconnection', () => {
    it('should attempt reconnection on close', fakeAsync(() => {
      service.connect();
      mockWsInstance.onopen!();
      mockWsInstance.onclose!({} as any);
      expect(service.connectionState()).toBe('disconnected');
      tick(2000);
      expect((window as any).WebSocket).toHaveBeenCalledTimes(2);
    }));

    it('should stop reconnecting after max attempts', fakeAsync(() => {
      service.connect();
      mockWsInstance.onopen!();
      for (let i = 0; i < 11; i++) {
        mockWsInstance.onclose!({} as any);
        tick(30000);
      }
      expect(service.lastError()).toBe('No se pudo conectar al middleware después de varios intentos');
    }));

    it('should clear reconnect timer on disconnect', fakeAsync(() => {
      service.connect();
      mockWsInstance.onopen!();
      mockWsInstance.onclose!({} as any);
      service.disconnect();
      tick(5000);
      // should not reconnect because disconnected
      expect(service.connectionState()).toBe('disconnected');
    }));
  });

  describe('error handling', () => {
    it('should handle JSON parse errors gracefully', () => {
      service.connect();
      mockWsInstance.onopen!();
      expect(() => {
        mockWsInstance.onmessage!({ data: 'invalid json' });
      }).not.toThrow();
    });

    it('should handle WebSocket construction error', () => {
      (window as any).WebSocket.and.throwError(new Error('constructor failed'));
      service.connect();
      expect(service.lastError()).toContain('Error al crear WebSocket');
      expect(service.connectionState()).toBe('disconnected');
    });
  });
});
