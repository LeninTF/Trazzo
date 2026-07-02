export interface HelpSection {
  id: string;
  title: string;
  intro: string;
  heroIcon: string;
  color: string;
  gradient: string;
  items: HelpItem[];
  highlights?: Highlight[];
}

export interface HelpItem {
  icon: string;
  title: string;
  description: string;
}

export interface Highlight {
  icon: string;
  value: string;
  label: string;
}

export const helpContent: HelpSection[] = [
  {
    id: 'guia-de-uso',
    title: 'Guía de uso',
    intro: 'Aprende a utilizar Trazzo para gestionar la asistencia y los horarios de tu equipo.',
    heroIcon: 'bi-book',
    color: '#3b5fc0',
    gradient: 'linear-gradient(135deg, #eef4ff 0%, #dfe8f9 50%, #e8f0fe 100%)',
    highlights: [
      { icon: 'bi-people', value: '3', label: 'Roles disponibles' },
      { icon: 'bi-gear', value: '12+', label: 'Módulos funcionales' },
      { icon: 'bi-clock', value: '24/7', label: 'Monitoreo continuo' },
    ],
    items: [
      {
        icon: 'bi-person-badge',
        title: 'Gestión de equipo',
        description: 'Desde el panel de administración podrás agregar, editar y desactivar empleados, asignar horarios personalizados y revisar el desempeño individual de cada miembro.',
      },
      {
        icon: 'bi-calendar-check',
        title: 'Control de asistencia',
        description: 'El sistema registra automáticamente los ingresos y salidas. Revisa el historial completo y detecta inasistencias en tiempo real con alertas inteligentes.',
      },
      {
        icon: 'bi-exclamation-triangle',
        title: 'Incidencias',
        description: 'Los empleados pueden reportar incidencias con su marcación directamente desde la plataforma. Los administradores reciben notificaciones y pueden resolverlas al instante.',
      },
      {
        icon: 'bi-gear',
        title: 'Configuración avanzada',
        description: 'Personaliza reglas de asistencia, horarios por sede, roles de usuario, y políticas de tolerancia según las necesidades específicas de tu organización.',
      },
    ],
  },
  {
    id: 'soporte-tecnico',
    title: 'Soporte técnico',
    intro: 'Estamos aquí para ayudarte. Elige el canal que prefieras para contactarnos.',
    heroIcon: 'bi-headset',
    color: '#0ea5e9',
    gradient: 'linear-gradient(135deg, #ecfeff 0%, #cffafe 50%, #e0f2fe 100%)',
    highlights: [
      { icon: 'bi-clock', value: '24h', label: 'Tiempo de respuesta' },
      { icon: 'bi-chat-dots', value: '3', label: 'Canales de atención' },
      { icon: 'bi-person-check', value: '98%', label: 'Satisfacción' },
    ],
    items: [
      {
        icon: 'bi-envelope',
        title: 'Correo electrónico',
        description: 'Escribenos a soporte@trazzo.com y te responderemos en un máximo de 24 horas hábiles. Incluye toda la información relevante para agilizar la solución.',
      },
      {
        icon: 'bi-chat-dots',
        title: 'Chat en vivo',
        description: 'Disponible de lunes a viernes de 9:00 a 18:00 (UTC-5). Conecta con un agente especializado que te guiará paso a paso en tiempo real.',
      },
      {
        icon: 'bi-telephone',
        title: 'Atención telefónica',
        description: 'Comunícate con nuestro centro de soporte al +1 (555) 123-4567 en horario laboral. Nuestro equipo está listo para resolver tus dudas.',
      },
      {
        icon: 'bi-journal-text',
        title: 'Centro de ayuda',
        description: 'Consulta nuestra base de conocimiento con tutoriales interactivos, preguntas frecuentes actualizadas y documentación técnica detallada.',
      },
    ],
  },
  {
    id: 'acerca-de-trazzo',
    title: 'Acerca de Trazzo',
    intro: 'Conoce más sobre nuestra plataforma de gestión de asistencia y horarios.',
    heroIcon: 'bi-info-circle',
    color: '#8b5cf6',
    gradient: 'linear-gradient(135deg, #f5f3ff 0%, #ede9fe 50%, #eef2ff 100%)',
    highlights: [
      { icon: 'bi-rocket-takeoff', value: '2024', label: 'Año de fundación' },
      { icon: 'bi-building', value: '150+', label: 'Empresas activas' },
      { icon: 'bi-people', value: '10K+', label: 'Usuarios gestionados' },
    ],
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
        description: 'Trazzo v1.0.0 — Versión estable actual. Cada release incluye mejoras de rendimiento, nuevas funcionalidades y optimizaciones basadas en el feedback de nuestros usuarios.',
      },
    ],
  },
];
