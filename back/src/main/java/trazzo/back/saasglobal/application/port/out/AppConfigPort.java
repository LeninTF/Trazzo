package trazzo.back.saasglobal.application.port.out;

/** Deployment-specific configuration values the application layer needs but shouldn't read via Spring's @Value directly. */
public interface AppConfigPort {
    /** Base URL of the Angular frontend. */
    String frontendUrl();

    /**
     * Base URL Mercado Pago's hosted checkout redirects the payer back to (back_url); callers append
     * the path of the page that should receive them. Defaults to {@link #frontendUrl()}.
     */
    String mercadoPagoBackUrl();

    /** True when the checkout must redirect to Mercado Pago's sandbox init point instead of the production one. */
    boolean mercadoPagoUseSandbox();

    /** Mailbox that receives new marketing-site contact/trial requests. */
    String requestsNotificationEmail();
}
