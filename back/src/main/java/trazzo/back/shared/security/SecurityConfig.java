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
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
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
                auth.requestMatchers(LOGIN_URL, PUBLIC_KEY_URL, ERROR_URL).permitAll();
                if (h2ConsoleEnabled) {
                    auth.requestMatchers(h2Console).hasRole("ADMIN");
                }
                auth.requestMatchers("/saas/**", "/tenants/**", "/audit/**").hasRole("admin_trazzo");
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

    @Bean
    WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/actuator/**");
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
