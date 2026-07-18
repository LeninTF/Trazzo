package trazzo.back.saasglobal.domain.model.iam;

import java.util.Set;

/**
 * Fixed catalog of the 15 SaaS-admin permission codes. Must stay in sync with
 * db/migration/V6__saas_rbac_and_invoice_users.sql's permissions_master seed and
 * front/src/app/features/admin-saas/gestion-roles/gestion-roles.ts's modulos array
 * (permission code format is "{moduloId}.{accionId}", matching the frontend's keys
 * verbatim so no translation layer is needed).
 */
public final class PermissionCatalog {

    public static final Set<String> ALL_CODES = Set.of(
            "gestion-tenants.crear",
            "gestion-tenants.editar",
            "gestion-tenants.eliminar",
            "gestion-tenants.activar-suspender",
            "gestion-tenants.configurar-identidad",
            "gestion-tenants.zonas-horarias",
            "gestion-tenants.asignacion-planes",
            "gestion-tenants.tipos-marcacion",
            "billing-suscripciones.gestionar-pagos",
            "billing-suscripciones.historial-facturacion",
            "billing-suscripciones.bloqueo-impago",
            "configuracion-global.modulos-por-plan",
            "monitoreo-sistema.dashboard-global",
            "monitoreo-sistema.logs-sistema",
            "monitoreo-sistema.auditoria-acciones"
    );

    private PermissionCatalog() {
    }
}
