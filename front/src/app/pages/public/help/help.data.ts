export interface HelpSection {
  id: string;
  title: string;
  intro: string;
  items: HelpItem[];
}

export interface HelpItem {
  icon: string;
  title: string;
  description: string;
}

export const helpContent: HelpSection[] = [
  {
    id: 'guia-de-uso',
    title: 'Guía de uso',
    intro: 'Aprende a utilizar Trazzo para gestionar la asistencia y los horarios de tu equipo.',
    items: [
      {
        icon: 'bi-person-badge',
        title: 'Gestiona tu equipo',
        description: 'Desde el panel de administración podrás agregar, editar y desactivar empleados, asignar horarios y revisar su desempeño.',
      },
      {
        icon: 'bi-calendar-check',
        title: 'Control de asistencia',
        description: 'El sistema registra automáticamente los ingresos y salidas. Revisa el historial de asistencia y detecta inasistencias en tiempo real.',
      },
      {
        icon: 'bi-exclamation-triangle',
        title: 'Incidencias',
        description: 'Los empleados pueden reportar incidencias con su marcación, y los administradores pueden gestionarlas y resolverlas directamente desde la plataforma.',
      },
      {
        icon: 'bi-gear',
        title: 'Configuración',
        description: 'Personaliza las reglas de asistencia, los horarios por sede, y los roles de usuario según las necesidades de tu organización.',
      },
    ],
  },
  {
    id: 'soporte-tecnico',
    title: 'Soporte técnico',
    intro: 'Estamos aquí para ayudarte. Elige el canal que prefieras para contactarnos.',
    items: [
      {
        icon: 'bi-envelope',
        title: 'Correo electrónico',
        description: 'Escribenos a soporte@trazzo.com y te responderemos en un máximo de 24 horas hábiles.',
      },
      {
        icon: 'bi-chat-dots',
        title: 'Chat en vivo',
        description: 'Disponible de lunes a viernes de 9:00 a 18:00 (UTC-5). Inicia una conversación desde el módulo de ayuda en la plataforma.',
      },
      {
        icon: 'bi-telephone',
        title: 'Teléfono',
        description: 'Comunícate con nuestro centro de soporte al +1 (555) 123-4567 en horario laboral.',
      },
      {
        icon: 'bi-journal-text',
        title: 'Centro de ayuda',
        description: 'Consulta nuestra base de conocimiento con tutoriales, preguntas frecuentes y documentación técnica en help.trazzo.com.',
      },
    ],
  },
  {
    id: 'acerca-de-trazzo',
    title: 'Acerca de Trazzo',
    intro: 'Conoce más sobre nuestra plataforma de gestión de asistencia y horarios.',
    items: [
      {
        icon: 'bi-rocket-takeoff',
        title: 'Nuestra misión',
        description: 'Simplificar la gestión del talento humano mediante herramientas inteligentes que automatizan el control de asistencia, la planificación de horarios y la detección de incidencias en tiempo real.',
      },
      {
        icon: 'bi-eye',
        title: 'Visión',
        description: 'Ser la plataforma líder en Latinoamérica para la gestión de asistencia laboral, reconocida por su facilidad de uso, precisión y capacidad de adaptación a cualquier tipo de organización.',
      },
      {
        icon: 'bi-people',
        title: 'El equipo',
        description: 'Trazzo es desarrollado por un equipo multidisciplinario de ingenieros, diseñadores y expertos en recursos humanos comprometidos con transformar la manera en que las empresas gestionan su capital humano.',
      },
      {
        icon: 'bi-shield-check',
        title: 'Versión',
        description: 'Trazzo v1.0.0 — Versión estable actual. Para conocer las novedades de cada release, visita nuestra sección de actualizaciones.',
      },
    ],
  },
];
