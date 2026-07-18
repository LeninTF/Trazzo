package trazzo.back.saasglobal.application.port.out;

import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

public interface TenantSchemaProvisioningPort {

    /**
     * PAID flow: creates the tenant's PostgreSQL schema, runs the tenant schema script
     * against it, and returns the generated settings.
     */
    TenantSettings provisionNew(String tenantId, String subDomain);

    /**
     * TRIAL flow: creates the schema named in the given settings and runs the tenant
     * schema script against it.
     */
    void provisionExisting(TenantSettings settings);

    /**
     * Best-effort cleanup: drops the schema created by provisionNew().
     * Called as compensation if the master transaction fails after provisioning.
     */
    void deprovision(String schemaName);
}
