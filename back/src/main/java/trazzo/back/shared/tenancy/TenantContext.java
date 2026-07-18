package trazzo.back.shared.tenancy;

/**
 * Holds the current request's tenant schema name. Set by {@code JwtAuthFilter} after
 * authentication and must be cleared at the end of the request (Tomcat reuses threads
 * across requests, so a leaked value would leak one tenant's data into another's request).
 */
public final class TenantContext {

    public static final String DEFAULT_SCHEMA = "public";

    private static final ThreadLocal<String> CURRENT_SCHEMA = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(String schema) {
        CURRENT_SCHEMA.set(schema == null || schema.isBlank() ? DEFAULT_SCHEMA : schema);
    }

    public static String get() {
        String schema = CURRENT_SCHEMA.get();
        return schema != null ? schema : DEFAULT_SCHEMA;
    }

    public static void clear() {
        CURRENT_SCHEMA.remove();
    }
}
