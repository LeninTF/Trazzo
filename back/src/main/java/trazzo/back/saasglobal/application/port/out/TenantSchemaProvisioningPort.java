package trazzo.back.saasglobal.application.port.out;

import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

public interface TenantSchemaProvisioningPort {

    /**
     * PAID flow: creates a new isolated database + user, runs the tenant schema script,
     * and returns the generated connection settings.
     */
    TenantSettings provisionNew(String tenantId, String subDomain);

    /**
     * TRIAL flow: runs the tenant schema script against an existing database.
     */
    void provisionExisting(TenantSettings settings);

    /**
     * Best-effort cleanup: drops the database and user created by provisionNew().
     * Called as compensation if the master transaction fails after provisioning.
     */
    void deprovision(String dbName, String dbUser);
}
