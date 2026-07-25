-- ==============================================================================
-- Default permission catalog and roles for tenant admins ("Gestión de Roles").
-- The frontend's modulos catalog (front/src/app/features/admin-tenant/gestion-roles/
-- gestion-roles.ts) already assumed this fixed set of 35 module.accion permissions
-- and 5 predefined roles existed; it was never actually persisted anywhere.
--
-- TenantSchemaMigrator re-runs every script in db/tenant/migration/ on every app
-- startup for every already-provisioned tenant (no per-tenant migration-history
-- tracking), so every statement here must be safe to execute repeatedly.
-- ==============================================================================

INSERT INTO permissions (id, name, description)
SELECT gen_random_uuid(), seed.name, seed.description
FROM (VALUES
    ('gestion-trabajadores.crear', 'Crear trabajadores'),
    ('gestion-trabajadores.editar', 'Editar trabajadores'),
    ('gestion-trabajadores.eliminar', 'Eliminar trabajadores'),
    ('gestion-trabajadores.asignar-roles', 'Asignar roles a trabajadores'),
    ('estructura-organizacional.crear-editar-areas', 'Crear/editar áreas'),
    ('estructura-organizacional.crear-editar-sedes', 'Crear/editar sedes'),
    ('estructura-organizacional.asignar-personal', 'Asignar personal'),
    ('gestion-horarios.configurar-turnos', 'Configurar turnos'),
    ('gestion-horarios.configurar-tolerancias', 'Configurar tolerancias'),
    ('gestion-horarios.asignacion-masiva', 'Asignación masiva de horarios'),
    ('control-asistencia.corregir-marcaciones', 'Corregir marcaciones'),
    ('control-asistencia.justificar-marcaciones', 'Justificar marcaciones'),
    ('control-asistencia.operador-asistencia', 'Operador de asistencia'),
    ('reportes.exportar-excel', 'Exportar reportes a Excel'),
    ('reportes.exportar-pdf', 'Exportar reportes a PDF'),
    ('permisos-licencias.aprobar-incidencias', 'Aprobar incidencias'),
    ('permisos-licencias.rechazar-incidencias', 'Rechazar incidencias'),
    ('notificaciones.enviar-avisos', 'Enviar avisos'),
    ('notificaciones.configurar-reportes', 'Configurar reportes automáticos'),
    ('configuracion-tenant.gestionar-metodos', 'Gestionar métodos de marcación'),
    ('auditoria-seguridad.acceso-logs', 'Acceso a logs'),
    ('auditoria-seguridad.historial-cambios', 'Historial de cambios'),
    ('asistencia-docente.registrar-entrada-salida', 'Registrar entrada/salida con huella'),
    ('asistencia-docente.ver-historial', 'Ver historial de asistencia'),
    ('asistencia-docente.ver-tardanzas-faltas', 'Ver tardanzas/faltas'),
    ('horarios-docente.ver-turnos', 'Ver turnos asignados'),
    ('horarios-docente.ver-calendario', 'Ver calendario laboral'),
    ('solicitudes.solicitar-correccion', 'Solicitar corrección de marcación'),
    ('solicitudes.ver-estado', 'Ver estado de solicitudes'),
    ('notificaciones-docente.recibir-alertas', 'Recibir alertas'),
    ('notificaciones-docente.ver-cambios-horario', 'Ver cambios de horario'),
    ('perfil.ver-actualizar-datos', 'Ver/actualizar datos personales'),
    ('perfil.cambiar-contrasena', 'Cambiar contraseña'),
    ('incidencias.ver-propias', 'Ver incidencias propias'),
    ('incidencias.crear', 'Crear incidencias'),
    ('incidencias.aprobar-rechazar', 'Aprobar/rechazar incidencias'),
    ('administrador_tenant', 'Acceso completo al panel de administración del tenant')
) AS seed(name, description)
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.name = seed.name);

INSERT INTO role (id, name, description)
SELECT gen_random_uuid(), seed.name, seed.description
FROM (VALUES
    ('administrador', 'El Administrador tiene acceso total a la configuración y seguridad del sistema, incluyendo gestión de trabajadores, horarios, reportes y auditoría.'),
    ('director', 'El Director puede supervisar la estructura organizacional, revisar reportes y aprobar incidencias, con acceso a la mayoría de módulos operativos.'),
    ('coordinador', 'El Coordinador gestiona horarios, control de asistencia y trabajadores a su cargo, con permisos limitados a módulos operativos.'),
    ('recursos-humanos', 'RRHH administra trabajadores, corrige marcaciones, gestiona permisos y licencias, y puede exportar reportes.'),
    ('docente', 'El Docente puede registrar su asistencia, consultar horarios, realizar solicitudes de corrección y gestionar su perfil personal.'),
    ('usuario', 'El Usuario tiene acceso básico a incidencias, historial de asistencia y perfil.')
) AS seed(name, description)
WHERE NOT EXISTS (SELECT 1 FROM role r WHERE r.name = seed.name);

-- (role_name, permission_name) grants, cross-checked 1:1 against the frontend's
-- former PERMISOS_DEFAULT map. role_permissions' PK is (role_id, permission_id),
-- so ON CONFLICT DO NOTHING alone makes this idempotent.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM (VALUES
    ('administrador', 'gestion-trabajadores.crear'),
    ('administrador', 'gestion-trabajadores.editar'),
    ('administrador', 'gestion-trabajadores.eliminar'),
    ('administrador', 'gestion-trabajadores.asignar-roles'),
    ('administrador', 'estructura-organizacional.crear-editar-areas'),
    ('administrador', 'estructura-organizacional.crear-editar-sedes'),
    ('administrador', 'estructura-organizacional.asignar-personal'),
    ('administrador', 'gestion-horarios.configurar-turnos'),
    ('administrador', 'gestion-horarios.configurar-tolerancias'),
    ('administrador', 'gestion-horarios.asignacion-masiva'),
    ('administrador', 'control-asistencia.corregir-marcaciones'),
    ('administrador', 'control-asistencia.justificar-marcaciones'),
    ('administrador', 'control-asistencia.operador-asistencia'),
    ('administrador', 'reportes.exportar-excel'),
    ('administrador', 'reportes.exportar-pdf'),
    ('administrador', 'permisos-licencias.aprobar-incidencias'),
    ('administrador', 'permisos-licencias.rechazar-incidencias'),
    ('administrador', 'notificaciones.enviar-avisos'),
    ('administrador', 'notificaciones.configurar-reportes'),
    ('administrador', 'configuracion-tenant.gestionar-metodos'),
    ('administrador', 'auditoria-seguridad.acceso-logs'),
    ('administrador', 'auditoria-seguridad.historial-cambios'),
    ('administrador', 'incidencias.ver-propias'),
    ('administrador', 'incidencias.crear'),
    ('administrador', 'incidencias.aprobar-rechazar'),
    ('administrador', 'administrador_tenant'),

    ('director', 'gestion-trabajadores.crear'),
    ('director', 'gestion-trabajadores.editar'),
    ('director', 'estructura-organizacional.crear-editar-areas'),
    ('director', 'estructura-organizacional.crear-editar-sedes'),
    ('director', 'estructura-organizacional.asignar-personal'),
    ('director', 'gestion-horarios.configurar-turnos'),
    ('director', 'gestion-horarios.configurar-tolerancias'),
    ('director', 'control-asistencia.corregir-marcaciones'),
    ('director', 'control-asistencia.justificar-marcaciones'),
    ('director', 'reportes.exportar-excel'),
    ('director', 'reportes.exportar-pdf'),
    ('director', 'permisos-licencias.aprobar-incidencias'),
    ('director', 'permisos-licencias.rechazar-incidencias'),
    ('director', 'notificaciones.enviar-avisos'),
    ('director', 'auditoria-seguridad.acceso-logs'),

    ('coordinador', 'gestion-trabajadores.crear'),
    ('coordinador', 'gestion-trabajadores.editar'),
    ('coordinador', 'gestion-horarios.configurar-turnos'),
    ('coordinador', 'gestion-horarios.asignacion-masiva'),
    ('coordinador', 'control-asistencia.corregir-marcaciones'),
    ('coordinador', 'reportes.exportar-excel'),
    ('coordinador', 'notificaciones.enviar-avisos'),

    ('recursos-humanos', 'gestion-trabajadores.crear'),
    ('recursos-humanos', 'gestion-trabajadores.editar'),
    ('recursos-humanos', 'gestion-trabajadores.eliminar'),
    ('recursos-humanos', 'estructura-organizacional.asignar-personal'),
    ('recursos-humanos', 'control-asistencia.corregir-marcaciones'),
    ('recursos-humanos', 'control-asistencia.justificar-marcaciones'),
    ('recursos-humanos', 'reportes.exportar-excel'),
    ('recursos-humanos', 'reportes.exportar-pdf'),
    ('recursos-humanos', 'permisos-licencias.aprobar-incidencias'),
    ('recursos-humanos', 'permisos-licencias.rechazar-incidencias'),
    ('recursos-humanos', 'notificaciones.enviar-avisos'),

    ('docente', 'asistencia-docente.registrar-entrada-salida'),
    ('docente', 'asistencia-docente.ver-historial'),
    ('docente', 'asistencia-docente.ver-tardanzas-faltas'),
    ('docente', 'horarios-docente.ver-turnos'),
    ('docente', 'horarios-docente.ver-calendario'),
    ('docente', 'solicitudes.solicitar-correccion'),
    ('docente', 'solicitudes.ver-estado'),
    ('docente', 'notificaciones-docente.recibir-alertas'),
    ('docente', 'notificaciones-docente.ver-cambios-horario'),
    ('docente', 'perfil.ver-actualizar-datos'),
    ('docente', 'perfil.cambiar-contrasena'),

    ('usuario', 'incidencias.ver-propias'),
    ('usuario', 'incidencias.crear'),
    ('usuario', 'asistencia-docente.ver-historial'),
    ('usuario', 'perfil.ver-actualizar-datos')
) AS grants(role_name, permission_name)
JOIN role r ON r.name = grants.role_name
JOIN permissions p ON p.name = grants.permission_name
ON CONFLICT DO NOTHING;
