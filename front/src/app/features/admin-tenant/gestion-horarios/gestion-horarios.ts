import { Component, ViewChild, signal } from '@angular/core';
import { TurnosComponent } from './turnos/turnos';
import { AsignacionComponent } from './asignacion/asignacion';
import { FeriadosComponent } from './feriados/feriados';

@Component({
  selector: 'app-gestion-horarios',
  imports: [TurnosComponent, AsignacionComponent, FeriadosComponent],
  templateUrl: './gestion-horarios.html',
  styleUrl: './gestion-horarios.css',
})
export class GestionHorarios {
  readonly loading = signal(false);
  readonly error = signal('');

  @ViewChild(TurnosComponent) turnosComponent!: TurnosComponent;

  activeSection: string = 'turnos';

  sectionMeta: Record<string, { title: string; subtitle: string; icon: string }> = {
    turnos: {
      title: 'Turnos',
      subtitle: 'Administra los turnos y franjas horarias de la institución.',
      icon: 'bi-clock-history',
    },
    asignacion: {
      title: 'Asignación',
      subtitle: 'Asigna trabajadores a turnos y horarios específicos.',
      icon: 'bi-person-check',
    },
    feriados: {
      title: 'Feriados',
      subtitle: 'Gestiona los días feriados nacionales e institucionales.',
      icon: 'bi-calendar-event',
    },
  };

  get currentSection(): { title: string; subtitle: string; icon: string } {
    return this.sectionMeta[this.activeSection];
  }

  nuevoTurno(): void {
    this.activeSection = 'turnos';
    setTimeout(() => this.turnosComponent?.openNewTurnoModal(), 50);
  }


}
