import { Injectable, signal } from '@angular/core';

export interface EnrolledFingerprint {
  userId: number;
  userName: string;
  userDisplayId: string;
  /** Merged template from 3-capture enrollment (registeredTemplateBase64) */
  templateBase64: string;
  templateSize: number;
  /** Single-capture reference template for size-based matching */
  referenceTemplateBase64: string;
  referenceTemplateSize: number;
  encryptedTemplate: {
    encryptedTemplateBase64: string;
    encryptedAesKeyBase64: string;
  } | null;
  enrolledAt: string;
}

const STORAGE_KEY = 'trazzo_fingerprints';

@Injectable({ providedIn: 'root' })
export class FingerprintStoreService {
  private readonly store = signal<EnrolledFingerprint[]>([]);

  constructor() {
    this.loadFromStorage();
  }

  getAll() {
    return this.store.asReadonly();
  }

  save(enrollment: EnrolledFingerprint): void {
    this.store.update(list => {
      const filtered = list.filter(e => e.userId !== enrollment.userId);
      return [...filtered, enrollment];
    });
    this.persist();
  }

  findByTemplate(capturedSize: number): { match: EnrolledFingerprint | null; reason: string } {
    const list = this.store();
    if (list.length === 0) {
      return { match: null, reason: 'No hay huellas enroladas' };
    }

    const withRef = list.filter(e => e.referenceTemplateSize > 0);
    if (withRef.length === 0) {
      return { match: null, reason: 'Ningún enrolamiento tiene template de referencia. Re-enrole las huellas.' };
    }

    const sorted = withRef
      .map(e => ({ e, diff: Math.abs(e.referenceTemplateSize - capturedSize) }))
      .sort((a, b) => a.diff - b.diff);

    const best = sorted[0];
    if (best.diff <= 220) {
      return { match: best.e, reason: `Coincide por tamaño (diferencia: ${best.diff} bytes)` };
    }

    const refs = withRef.map(e => `${e.userName}:${e.referenceTemplateSize}B`).join(', ');
    return { match: null, reason: `Ninguna coincide. Capturado: ${capturedSize}B. Referencias: ${refs}. Mejor: ${best.e.userName} (dif: ${best.diff}B)` };
  }

  remove(userId: number): void {
    this.store.update(list => list.filter(e => e.userId !== userId));
    this.persist();
  }

  clear(): void {
    this.store.set([]);
    this.persist();
  }

  get count(): number {
    return this.store().length;
  }

  private persist(): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.store()));
    } catch {
      // storage full or unavailable
    }
  }

  private loadFromStorage(): void {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const data = JSON.parse(raw) as EnrolledFingerprint[];
        this.store.set(data.map(e => ({
          ...e,
          referenceTemplateBase64: e.referenceTemplateBase64 ?? '',
          referenceTemplateSize: e.referenceTemplateSize ?? 0,
        })));
      }
    } catch {
      // ignore corrupt data
    }
  }
}
