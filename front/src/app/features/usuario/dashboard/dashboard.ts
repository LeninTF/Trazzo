import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface ActividadReciente {
  id: number;
  titulo: string;
  descripcion: string;
  fecha: Date;
  tipo: 'entrada' | 'salida' | 'retraso';
  icono: string;
}

interface RegistroAsistencia {
  id: number;
  fecha: Date;
  tipo: 'entrada' | 'salida';
  estado: 'puntual' | 'retraso';
  descripcion: string;
}

interface Usuario {
  nombre: string;
  rol: string;
  email: string;
}

interface Metricas {
  asistenciasPuntuales: number;
  llegadasTarde: number;
  inasistencias: number;
  incidencias: number;  // NUEVO: faltas justificadas
  puntualidad: number;
  horasExtras: number;
}

interface Turno {
  horaInicio: string;
  horaFin: string;
  puesto: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  
  // ==========================================
  // DATOS DEL USUARIO
  // ==========================================
  usuario: Usuario = {
    nombre: 'Roberto',
    rol: 'Docente',
    email: 'roberto@empresa.com'
  };

  fechaActual: Date = new Date();

  // ==========================================
  // TURNO DE HOY
  // ==========================================
  turno: Turno = {
    horaInicio: '08:00',
    horaFin: '13:00',
    puesto: 'Docente'
  };

  // ==========================================
  // MÉTRICAS (con incidencias)
  // ==========================================
  metricas: Metricas = {
    asistenciasPuntuales: 22,
    llegadasTarde: 2,
    inasistencias: 0,
    incidencias: 3,      // NUEVO: faltas justificadas
    puntualidad: 98,
    horasExtras: 4.5
  };

  // ==========================================
  // ACTIVIDADES RECIENTES
  // ==========================================
  actividadesRecientes: ActividadReciente[] = [
    {
      id: 1,
      titulo: 'Entrada Registrada',
      descripcion: 'Registrado puntualmente en la entrada principal.',
      fecha: new Date(),
      tipo: 'entrada',
      icono: 'bi-box-arrow-in-right'
    },
    {
      id: 2,
      titulo: 'Salida Registrada',
      descripcion: 'Jornada finalizada sin incidencias.',
      fecha: new Date(),
      tipo: 'salida',
      icono: 'bi-box-arrow-right'
    },
    {
      id: 3,
      titulo: 'Entrada Registrada',
      descripcion: 'Retraso menor de 2 minutos registrado.',
      fecha: new Date(new Date().setDate(new Date().getDate() - 1)),
      tipo: 'retraso',
      icono: 'bi-exclamation-triangle'
    }
  ];

  // ==========================================
  // ÚLTIMOS REGISTROS (para la columna derecha)
  // ==========================================
  ultimosRegistros: RegistroAsistencia[] = [
    {
      id: 1,
      fecha: new Date(new Date().setDate(new Date().getDate() - 1)),
      tipo: 'entrada',
      estado: 'puntual',
      descripcion: 'Registrado puntualmente en la entrada principal.'
    },
    {
      id: 2,
      fecha: new Date(new Date().setDate(new Date().getDate() - 1)),
      tipo: 'salida',
      estado: 'puntual',
      descripcion: 'Jornada finalizada sin incidencias.'
    },
    {
      id: 3,
      fecha: new Date(2023, 9, 22, 8, 2),
      tipo: 'entrada',
      estado: 'retraso',
      descripcion: 'Retraso menor de 2 minutos registrado.'
    }
  ];

  // ==========================================
  // TODOS LOS REGISTROS (para el modal)
  // ==========================================
  todosLosRegistros: RegistroAsistencia[] = [
    { id: 1, fecha: new Date(2023, 9, 27, 7, 58), tipo: 'entrada', estado: 'puntual', descripcion: 'Registrado puntualmente en la entrada principal.' },
    { id: 2, fecha: new Date(2023, 9, 27, 17, 5), tipo: 'salida', estado: 'puntual', descripcion: 'Jornada finalizada sin incidencias.' },
    { id: 3, fecha: new Date(2023, 9, 22, 8, 2), tipo: 'entrada', estado: 'retraso', descripcion: 'Retraso menor de 2 minutos registrado.' },
    { id: 4, fecha: new Date(2023, 9, 22, 17, 0), tipo: 'salida', estado: 'puntual', descripcion: 'Jornada finalizada sin incidencias.' },
    { id: 5, fecha: new Date(2023, 9, 21, 7, 55), tipo: 'entrada', estado: 'puntual', descripcion: 'Registrado puntualmente en la entrada principal.' },
    { id: 6, fecha: new Date(2023, 9, 21, 17, 10), tipo: 'salida', estado: 'puntual', descripcion: 'Jornada finalizada sin incidencias.' },
    { id: 7, fecha: new Date(2023, 9, 20, 8, 0), tipo: 'entrada', estado: 'puntual', descripcion: 'Registrado puntualmente en la entrada principal.' },
    { id: 8, fecha: new Date(2023, 9, 20, 17, 3), tipo: 'salida', estado: 'puntual', descripcion: 'Jornada finalizada sin incidencias.' }
  ];

  // ==========================================
  // ESTADO DE MODALES
  // ==========================================
  modalHistorialOpen: boolean = false;

  // ==========================================
  // MÉTODOS
  // ==========================================

  verHistorialCompleto(): void {
    this.modalHistorialOpen = true;
  }

  cerrarModalHistorial(): void {
    this.modalHistorialOpen = false;
  }
}