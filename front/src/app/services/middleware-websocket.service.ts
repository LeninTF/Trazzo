import { Injectable, inject, signal } from '@angular/core';
import { ToastService } from './toast.service';

export interface MiddlewareDeviceStatus {
  type: string;
  success: boolean;
  isSdkAvailable: boolean;
  isInitialized: boolean;
  isDeviceOpen: boolean;
  isConnected: boolean;
  deviceCount: number;
  message: string;
  checkedAtUtc: string;
}

export interface MiddlewareHealthResult {
  type: string;
  success: boolean;
  message: string;
}

export interface MiddlewareQueueStatus {
  type: string;
  success: boolean;
  pendingCount: number;
}

export interface MiddlewareDeviceStatusChanged {
  type: 'device.status.changed' | 'device.connecting';
  success: boolean;
  isConnected: boolean;
  message: string;
  waitingSeconds?: number;
}

export type MiddlewareMessage =
  | MiddlewareDeviceStatus
  | MiddlewareHealthResult
  | MiddlewareQueueStatus
  | MiddlewareDeviceStatusChanged
  | { type: string; [key: string]: unknown };

export type ConnectionState = 'connecting' | 'connected' | 'disconnected' | 'reconnecting';

@Injectable({ providedIn: 'root' })
export class MiddlewareWebSocketService {
  private readonly toastService = inject(ToastService);

  readonly connectionState = signal<ConnectionState>('disconnected');
  readonly lastDeviceStatus = signal<MiddlewareDeviceStatus | null>(null);
  readonly lastError = signal<string>('');

  private ws: WebSocket | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 10;
  private baseDelayMs = 1000;
  private maxDelayMs = 30000;
  private destroyed = false;

  private messageHandlers = new Map<string, Set<(msg: MiddlewareMessage) => void>>();

  private url = 'ws://localhost:9001/';

  connect(url?: string): void {
    if (url) this.url = url;
    if (this.ws) this.disconnect();
    this.destroyed = false;
    this.reconnectAttempts = 0;
    this.doConnect();
  }

  disconnect(): void {
    this.destroyed = true;
    this.clearReconnectTimer();
    this.closeWs();
    this.connectionState.set('disconnected');
  }

  on(type: string, handler: (msg: MiddlewareMessage) => void): () => void {
    if (!this.messageHandlers.has(type)) {
      this.messageHandlers.set(type, new Set());
    }
    this.messageHandlers.get(type)!.add(handler);
    return () => this.messageHandlers.get(type)?.delete(handler);
  }

  send(type: string, payload?: Record<string, unknown>): void {
    if (this.ws?.readyState !== WebSocket.OPEN) {
      this.toastService.error('Middleware no conectado');
      return;
    }
    this.ws.send(JSON.stringify(payload ? { type, ...payload } : { type }));
  }

  private doConnect(): void {
    if (this.destroyed) return;
    this.connectionState.set(this.reconnectAttempts > 0 ? 'reconnecting' : 'connecting');
    this.lastError.set('');

    try {
      this.ws = new WebSocket(this.url);
    } catch (err) {
      this.handleError(`Error al crear WebSocket: ${err}`);
      return;
    }

    this.ws.onopen = () => {
      this.reconnectAttempts = 0;
      this.connectionState.set('connected');
      this.lastError.set('');
    };

    this.ws.onmessage = (event) => {
      try {
        const msg: MiddlewareMessage = JSON.parse(event.data);
        this.dispatch(msg);
      } catch {
        // ignore invalid JSON
      }
    };

    this.ws.onclose = () => {
      if (this.destroyed) return;
      this.connectionState.set('disconnected');
      this.scheduleReconnect();
    };

    this.ws.onerror = () => {
      // onclose will fire after this
    };
  }

  private dispatch(msg: MiddlewareMessage): void {
    if (msg.type === 'device.status') {
      this.lastDeviceStatus.set(msg as MiddlewareDeviceStatus);
    }

    const handlers = this.messageHandlers.get(msg.type);
    if (handlers) {
      for (const handler of handlers) {
        try { handler(msg); } catch { /* ignore handler error */ }
      }
    }

    const wildcardHandlers = this.messageHandlers.get('*');
    if (wildcardHandlers) {
      for (const handler of wildcardHandlers) {
        try { handler(msg); } catch { /* ignore handler error */ }
      }
    }
  }

  private handleError(msg: string): void {
    this.lastError.set(msg);
    this.connectionState.set('disconnected');
    this.scheduleReconnect();
  }

  private scheduleReconnect(): void {
    if (this.destroyed) return;
    this.clearReconnectTimer();

    this.reconnectAttempts++;
    if (this.reconnectAttempts > this.maxReconnectAttempts) {
      this.lastError.set('No se pudo conectar al middleware después de varios intentos');
      return;
    }

    const delay = Math.min(
      this.baseDelayMs * Math.pow(2, this.reconnectAttempts - 1),
      this.maxDelayMs
    );

    this.reconnectTimer = setTimeout(() => this.doConnect(), delay);
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  private closeWs(): void {
    if (this.ws) {
      this.ws.onopen = null;
      this.ws.onmessage = null;
      this.ws.onclose = null;
      this.ws.onerror = null;
      if (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING) {
        this.ws.close();
      }
      this.ws = null;
    }
  }
}
