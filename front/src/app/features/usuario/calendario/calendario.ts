import { Component, signal } from '@angular/core';

interface Turno {
  id: string;
  nombre: string;
  inicio: string;
  fin: string;
  color: string;
  ubicacion: string;
}

interface Feriado {
  fecha: string;
  nombre: string;
}

interface DiaInfo {
  fecha: Date;
  numero: number;
  nombre: string;
  esHoy: boolean;
  esMesActual: boolean;
  turno: Turno | null;
  feriado: Feriado | null;
}

interface DiaSemana {
  nombre: string;
  fecha: string;
  numero: number;
  turno: Turno | null;
  feriado: Feriado | null;
}

@Component({
  selector: 'app-calendario',
  imports: [],
  templateUrl: './calendario.html',
  styleUrl: './calendario.css',
})
export class Calendario {
  vista = signal<'mensual' | 'semanal'>('mensual');
  fechaActual = signal(new Date(2026, 9, 1));

  readonly turnos: Turno[] = [
    { id: 'manana', nombre: 'Turno Mañana', inicio: '06:00', fin: '14:00', color: '#3B82F6', ubicacion: 'Planta Sur - Línea A' },
    { id: 'tarde', nombre: 'Turno Tarde', inicio: '14:00', fin: '22:00', color: '#10B981', ubicacion: 'Planta Norte - Línea B' },
    { id: 'noche', nombre: 'Turno Noche', inicio: '22:00', fin: '06:00', color: '#8B5CF6', ubicacion: 'Edificio Central - Piso 3' },
  ];

  readonly feriados = signal<Feriado[]>([
    { fecha: '2026-10-05', nombre: 'Día del Logro' },
    { fecha: '2026-10-12', nombre: 'Combate de Angamos' },
    { fecha: '2026-10-31', nombre: 'Halloween (no laborable)' },
  ]);

  readonly asignaciones: Record<string, string> = {
    '2026-10-01': 'manana',
    '2026-10-02': 'manana',
    '2026-10-03': 'manana',
    '2026-10-06': 'manana',
    '2026-10-07': 'manana',
    '2026-10-08': 'manana',
    '2026-10-09': 'manana',
    '2026-10-10': 'manana',
    '2026-10-13': 'tarde',
    '2026-10-14': 'tarde',
    '2026-10-15': 'tarde',
    '2026-10-16': 'tarde',
    '2026-10-17': 'tarde',
    '2026-10-20': 'tarde',
    '2026-10-21': 'tarde',
    '2026-10-22': 'tarde',
    '2026-10-23': 'tarde',
    '2026-10-24': 'tarde',
    '2026-10-27': 'manana',
    '2026-10-28': 'manana',
    '2026-10-29': 'manana',
    '2026-10-30': 'manana',
  };

  readonly DIAS = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];
  readonly DIAS_LARGOS = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
  readonly MESES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Setiembre', 'Octubre', 'Noviembre', 'Diciembre'];

  get anio(): number { return this.fechaActual().getFullYear(); }
  get mes(): number { return this.fechaActual().getMonth(); }

  get etiquetaMes(): string {
    return `${this.MESES[this.mes]} ${this.anio}`;
  }

  get turnoActual(): string {
    const hoy = this.formatearFecha(new Date());
    const id = this.asignaciones[hoy];
    const t = this.turnos.find(x => x.id === id);
    return t ? t.nombre : '—';
  }

  get proximaRotacion(): string {
    const dias = this.calendario.filter((d: DiaInfo) => d.turno && d.fecha > new Date());
    if (dias.length >= 1) {
      const prox = dias[0];
      const dia = this.DIAS_LARGOS[prox.fecha.getDay()];
      return `${dia} ${prox.numero} — ${prox.turno!.nombre}`;
    }
    return 'Sin rotación programada';
  }

  get horasSemanales(): number {
    let total = 0;
    const semana = this.diasSemana;
    for (const d of semana) {
      if (d.turno) {
        const [h1, m1] = d.turno.inicio.split(':').map(Number);
        const [h2, m2] = d.turno.fin.split(':').map(Number);
        total += (h2 - h1) + (m2 - m1) / 60;
      }
    }
    return Math.round(total * 10) / 10;
  }

  get extrasAcumuladas(): string {
    const base = 40;
    const extra = Math.max(0, this.horasSemanales - base);
    return extra.toFixed(1);
  }

  get proximoFeriado(): Feriado | null {
    const hoy = new Date();
    const ordenados = [...this.feriados()].sort((a, b) => new Date(a.fecha).getTime() - new Date(b.fecha).getTime());
    return ordenados.find(f => new Date(f.fecha) >= hoy) ?? null;
  }

  formatearFecha(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  cambiarMes(delta: number): void {
    const d = new Date(this.fechaActual());
    d.setMonth(d.getMonth() + delta);
    this.fechaActual.set(d);
  }

  irHoy(): void {
    this.fechaActual.set(new Date());
  }

  cambiarVista(v: 'mensual' | 'semanal'): void {
    this.vista.set(v);
  }

  get calendario(): DiaInfo[] {
    const año = this.anio;
    const mes = this.mes;
    const primerDia = new Date(año, mes, 1);
    const ultimoDia = new Date(año, mes + 1, 0);
    const inicioSemana = primerDia.getDay();
    const dias: DiaInfo[] = [];
    const hoyStr = this.formatearFecha(new Date());

    this.agregarDiasPrevios(dias, año, mes, inicioSemana, hoyStr);
    this.agregarDiasDelMes(dias, año, mes, ultimoDia.getDate(), hoyStr);
    this.agregarDiasRestantes(dias, año, mes);

    return dias;
  }

  private agregarDiasPrevios(dias: DiaInfo[], año: number, mes: number, inicioSemana: number, hoyStr: string): void {
    const diaPrevio = new Date(año, mes, 0);
    for (let i = inicioSemana - 1; i >= 0; i--) {
      const d = new Date(año, mes - 1, diaPrevio.getDate() - i);
      dias.push({
        fecha: d,
        numero: d.getDate(),
        nombre: this.DIAS[d.getDay()],
        esHoy: this.formatearFecha(d) === hoyStr,
        esMesActual: false,
        turno: null,
        feriado: null,
      });
    }
  }

  private agregarDiasDelMes(dias: DiaInfo[], año: number, mes: number, totalDias: number, hoyStr: string): void {
    for (let i = 1; i <= totalDias; i++) {
      const d = new Date(año, mes, i);
      const fechaStr = this.formatearFecha(d);
      const feriado = this.feriados().find(f => f.fecha === fechaStr) ?? null;
      const turnoId = this.asignaciones[fechaStr];
      const turno = this.turnos.find(t => t.id === turnoId) ?? null;
      dias.push({
        fecha: d,
        numero: i,
        nombre: this.DIAS[d.getDay()],
        esHoy: fechaStr === hoyStr,
        esMesActual: true,
        turno: feriado ? null : turno,
        feriado,
      });
    }
  }

  private agregarDiasRestantes(dias: DiaInfo[], año: number, mes: number): void {
    const restantes = 42 - dias.length;
    for (let i = 1; i <= restantes; i++) {
      const d = new Date(año, mes + 1, i);
      dias.push({
        fecha: d,
        numero: i,
        nombre: this.DIAS[d.getDay()],
        esHoy: false,
        esMesActual: false,
        turno: null,
        feriado: null,
      });
    }
  }

  get diasSemana(): DiaSemana[] {
    const hoy = new Date();
    const inicio = new Date(hoy);
    inicio.setDate(hoy.getDate() - hoy.getDay() + 1);
    const dias: DiaSemana[] = [];
    for (let i = 0; i < 5; i++) {
      const d = new Date(inicio);
      d.setDate(inicio.getDate() + i);
      const fechaStr = this.formatearFecha(d);
      const feriado = this.feriados().find(f => f.fecha === fechaStr) ?? null;
      const turnoId = this.asignaciones[fechaStr];
      const turno = this.turnos.find(t => t.id === turnoId) ?? null;
      dias.push({
        nombre: this.DIAS_LARGOS[d.getDay()].substring(0, 3).toUpperCase(),
        fecha: `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`,
        numero: d.getDate(),
        turno,
        feriado,
      });
    }
    return dias;
  }

  descargarPDF(): void {
    window.print();
  }
}