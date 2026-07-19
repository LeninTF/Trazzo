package trazzo.back.saasglobal.application.port.out;

/** Deployment-specific configuration values the application layer needs but shouldn't read via Spring's @Value directly. */
public interface AppConfigPort {
    /** Base URL of the Angular frontend, used to build Mercado Pago's back_url redirect. */
    String frontendUrl();

    /** Mailbox that receives new marketing-site contact/trial requests. */
    String requestsNotificationEmail();
}
