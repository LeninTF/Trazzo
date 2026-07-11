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

type Hl = [icon: string, value: string, label: string];
type It = [icon: string, title: string, description: string];

const h = ([icon, value, label]: Hl): Highlight => ({ icon, value, label });
const i = ([icon, title, description]: It): HelpItem => ({ icon, title, description });

const section = (
  id: string, title: string, intro: string, heroIcon: string,
  color: string, gradient: string,
  highlights: Hl[], items: It[],
): HelpSection => ({ id, title, intro, heroIcon, color, gradient, highlights: highlights.map(h), items: items.map(i) });

export const helpContent: HelpSection[] = [
  section(
    'guia-de-uso', 'Guía de uso',
    'Aprende a utilizar Trazzo para gestionar la asistencia y los horarios de tu equipo.',
    'bi-book', '#3b5fc0',
    'linear-gradient(135deg, #eef4ff 0%, #dfe8f9 50%, #e8f0fe 100%)',
    [
      ['bi-people', '3', 'Roles disponibles'],
      ['bi-gear', '12+', 'Módulos funcionales'],
      ['bi-clock', '24/7', 'Monitoreo continuo'],
    ],
    [
      ['bi-person-badge', 'Gestión de equipo', 'Desde el panel de administración podrás agregar, editar y desactivar empleados, asignar horarios personalizados y revisar el desempeño individual de cada miembro.'],
      ['bi-calendar-check', 'Control de asistencia', 'El sistema registra automáticamente los ingresos y salidas. Revisa el historial completo y detecta inasistencias en tiempo real con alertas inteligentes.'],
      ['bi-exclamation-triangle', 'Incidencias', 'Los empleados pueden reportar incidencias con su marcación directamente desde la plataforma. Los administradores reciben notificaciones y pueden resolverlas al instante.'],
      ['bi-gear', 'Configuración avanzada', 'Personaliza reglas de asistencia, horarios por sede, roles de usuario, y políticas de tolerancia según las necesidades específicas de tu organización.'],
    ],
  ),
  section(
    'soporte-tecnico', 'Soporte técnico',
    'Estamos aquí para ayudarte. Elige el canal que prefieras para contactarnos.',
    'bi-headset', '#0ea5e9',
    'linear-gradient(135deg, #ecfeff 0%, #cffafe 50%, #e0f2fe 100%)',
    [
      ['bi-clock', '24h', 'Tiempo de respuesta'],
      ['bi-chat-dots', '3', 'Canales de atención'],
      ['bi-person-check', '98%', 'Satisfacción'],
    ],
    [
      ['bi-envelope', 'Correo electrónico', 'Escribenos a soporte@trazzo.com y te responderemos en un máximo de 24 horas hábiles. Incluye toda la información relevante para agilizar la solución.'],
      ['bi-chat-dots', 'Chat en vivo', 'Disponible de lunes a viernes de 9:00 a 18:00 (UTC-5). Conecta con un agente especializado que te guiará paso a paso en tiempo real.'],
      ['bi-telephone', 'Atención telefónica', 'Comunícate con nuestro centro de soporte al +1 (555) 123-4567 en horario laboral. Nuestro equipo está listo para resolver tus dudas.'],
      ['bi-journal-text', 'Centro de ayuda', 'Consulta nuestra base de conocimiento con tutoriales interactivos, preguntas frecuentes actualizadas y documentación técnica detallada.'],
    ],
  ),
  section(
    'acerca-de-trazzo', 'Acerca de Trazzo',
    'Conoce más sobre nuestra plataforma de gestión de asistencia y horarios.',
    'bi-info-circle', '#8b5cf6',
    'linear-gradient(135deg, #f5f3ff 0%, #ede9fe 50%, #eef2ff 100%)',
    [
      ['bi-rocket-takeoff', '2024', 'Año de fundación'],
      ['bi-building', '150+', 'Empresas activas'],
      ['bi-people', '10K+', 'Usuarios gestionados'],
    ],
    [
      ['bi-rocket-takeoff', 'Nuestra misión', 'Simplificar la gestión del talento humano mediante herramientas inteligentes que automatizan el control de asistencia, la planificación de horarios y la detección de incidencias en tiempo real.'],
      ['bi-eye', 'Visión', 'Ser la plataforma líder en Latinoamérica para la gestión de asistencia laboral, reconocida por su facilidad de uso, precisión y capacidad de adaptación a cualquier tipo de organización.'],
      ['bi-people', 'El equipo', 'Trazzo es desarrollado por un equipo multidisciplinario de ingenieros, diseñadores y expertos en recursos humanos comprometidos con transformar la manera en que las empresas gestionan su capital humano.'],
      ['bi-shield-check', 'Versión', 'Trazzo v1.0.0 — Versión estable actual. Cada release incluye mejoras de rendimiento, nuevas funcionalidades y optimizaciones basadas en el feedback de nuestros usuarios.'],
    ],
  ),
];
