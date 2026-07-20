package trazzo.back.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter.XFrameOptionsMode;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String LOGIN_URL = "/auth/login";
    private static final String PUBLIC_KEY_URL = "/security/public-key";
    private static final String ERROR_URL = "/error";
    // Public marketing-site contact form (RequestController) — unauthenticated by design.
    private static final String SUBMIT_REQUEST_URL = "/requests";
    // Public marketing-site pricing section (PublicPlanController) — unauthenticated by design.
    private static final String PUBLIC_PLANS_URL = "/public/plans";
    // Public self-signup trial (TenantController) — unauthenticated by design.
    private static final String TENANTS_TRIAL_URL = "/tenants/trial";
    // Public self-signup checkout (ShopCheckoutController) — unauthenticated by design.
    private static final String SHOP_CHECKOUT_URL = "/shop/checkout";
    // Mercado Pago webhook receiver — unauthenticated (verified via WebhookSignatureValidator
    // inside the controller instead, since Mercado Pago cannot present a Bearer token).
    private static final String MERCADOPAGO_WEBHOOK_URL = "/webhooks/mercadopago";
    // Load balancers/uptime monitors need this reachable without a token; show-details is
    // separately gated to when-authorized (see application.properties), so an anonymous hit
    // only ever sees a bare UP/DOWN, never DB status or disk space.
    private static final String ACTUATOR_HEALTH_URL = "/actuator/health";

    // CSRF is intentionally disabled: stateless REST API authenticated via JWT Bearer tokens.
    // Cookie-based CSRF attacks do not apply when no session cookies are used.
    @Bean
    JwtAuthFilter jwtAuthFilter(TokenValidator tokenValidator, UserDetailsService userDetailsService) {
        return new JwtAuthFilter(tokenValidator, userDetailsService);
    }

    @Bean
    FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter jwtAuthFilter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(jwtAuthFilter);
        registration.setEnabled(false);
        return registration;
    }

    @SuppressWarnings({"java:S4502", "codeql[java/spring-disabled-csrf-protection]"})
    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthFilter jwtAuthFilter,
            @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled,
            @Value("${spring.h2.console.path:/h2-console}") String h2ConsolePath
    ) throws Exception {
        RequestMatcher h2Console = h2ConsoleRequestMatcher(h2ConsolePath);

        http
            .csrf(csrf -> csrf.disable()) // codeql[java/spring-disabled-csrf-protection] - stateless JWT API, no session cookies
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(LOGIN_URL, PUBLIC_KEY_URL, ERROR_URL, SUBMIT_REQUEST_URL, PUBLIC_PLANS_URL,
                        TENANTS_TRIAL_URL, SHOP_CHECKOUT_URL, MERCADOPAGO_WEBHOOK_URL, ACTUATOR_HEALTH_URL).permitAll();
                if (h2ConsoleEnabled) {
                    auth.requestMatchers(h2Console).hasRole("ADMIN");
                }
                // Any admin SaaS role (not just admin_trazzo) reaches these paths; granular
                // per-permission checks live on individual endpoints via @PreAuthorize.
                // /audit/** was previously uncovered (fell through to the generic authenticated()
                // rule below, reachable by tenant users too) — now correctly admin-gated.
                // /actuator/** (info, and any future exposed endpoint besides health) requires
                // the same admin gate — it used to bypass Spring Security entirely.
                auth.requestMatchers("/saas/**", "/tenants/**", "/audit/**", "/actuator/**").hasRole("SAAS_ADMIN");
                auth.anyRequest().authenticated();
            })
            .headers(headers -> configureHeaders(headers, h2ConsoleEnabled, h2Console))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> res.setStatus(401))
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void configureHeaders(
            HeadersConfigurer<HttpSecurity> headers,
            boolean h2ConsoleEnabled,
            RequestMatcher h2Console
    ) {
        if (!h2ConsoleEnabled) {
            return;
        }

        headers
            .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
            .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                    h2Console,
                    new XFrameOptionsHeaderWriter(XFrameOptionsMode.SAMEORIGIN)))
            .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                    new NegatedRequestMatcher(h2Console),
                    new XFrameOptionsHeaderWriter(XFrameOptionsMode.DENY)));
    }

    @SuppressWarnings("java:S5738")
    private RequestMatcher h2ConsoleRequestMatcher(String h2ConsolePath) {
        String normalizedPath = h2ConsolePath.endsWith("/")
                ? h2ConsolePath.substring(0, h2ConsolePath.length() - 1)
                : h2ConsolePath;
        return new OrRequestMatcher(
                new AntPathRequestMatcher(normalizedPath),
                new AntPathRequestMatcher(normalizedPath + "/**"));
    }
}
