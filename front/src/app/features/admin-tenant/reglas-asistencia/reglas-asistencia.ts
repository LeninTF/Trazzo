import { Component, signal } from '@angular/core';

@Component({
  selector: 'app-reglas-asistencia',
  standalone: true,
  imports: [],
  templateUrl: './reglas-asistencia.html',
  styleUrl: './reglas-asistencia.css',
})
export class ReglasAsistencia {
  tolerancia = signal(15);
  redondeo = signal<'estandar' | 'real'>('estandar');
  huellaActivada = signal(true);
  autorizacionPrevia = signal(false);
  topeSemanal = signal(true);
  recargoNocturno = signal(false);

  get toleranciaTexto(): string {
    const v = this.tolerancia();
    if (v === 0) return 'Sin tolerancia — el registro debe ser exacto a la hora de entrada.';
    if (v <= 5) return `Tolerancia muy estricta de ${v} minuto${v === 1 ? '' : 's'}.`;
    if (v <= 15) return `Tolerancia moderada de ${v} minutos — adecuada para entornos flexibles.`;
    if (v <= 30) return `Tolerancia amplia de ${v} minutos — permite cierto margen de retraso.`;
    return `Tolerancia muy amplia de ${v} minutos — puede afectar la puntualidad general.`;
  }

  alternarHuella(): void {
    this.huellaActivada.update(v => !v);
  }

  alternarAutorizacion(): void {
    this.autorizacionPrevia.update(v => !v);
  }

  alternarTope(): void {
    this.topeSemanal.update(v => !v);
  }

  alternarRecargo(): void {
    this.recargoNocturno.update(v => !v);
  }
}
