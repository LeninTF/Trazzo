package trazzo.back.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter.XFrameOptionsMode;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";

    @Bean
    @SuppressWarnings("java:S4502")
    SecurityFilterChain filterChain(
            HttpSecurity http,
            @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled,
            @Value("${spring.h2.console.path:/h2-console}") String h2ConsolePath
    ) throws Exception {
        RequestMatcher h2Console = h2ConsoleRequestMatcher(h2ConsolePath);

        http
            .csrf(csrf -> {
                csrf.ignoringRequestMatchers(LOGIN_URL, LOGOUT_URL);
                if (h2ConsoleEnabled) {
                    csrf.ignoringRequestMatchers(h2Console);
                }
            })
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(LOGIN_URL, "/actuator/health").permitAll();
                if (h2ConsoleEnabled) {
                    auth.requestMatchers(h2Console).authenticated();
                }
                auth.anyRequest().authenticated();
            })
            .headers(headers -> configureHeaders(headers, h2ConsoleEnabled, h2Console))
            .formLogin(form -> form
                .loginProcessingUrl(LOGIN_URL)
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler((req, res, authentication) -> res.setStatus(200))
                .failureHandler((req, res, ex) -> res.setStatus(401))
            )
            .logout(logout -> logout
                .logoutUrl(LOGOUT_URL)
                .logoutSuccessHandler((req, res, authentication) -> res.setStatus(200))
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> res.setStatus(401))
            );
        return http.build();
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
            .frameOptions(frameOptions -> frameOptions.disable())
            .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                    h2Console,
                    new XFrameOptionsHeaderWriter(XFrameOptionsMode.SAMEORIGIN)))
            .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                    new NegatedRequestMatcher(h2Console),
                    new XFrameOptionsHeaderWriter(XFrameOptionsMode.DENY)));
    }

    private RequestMatcher h2ConsoleRequestMatcher(String h2ConsolePath) {
        String normalizedPath = h2ConsolePath.endsWith("/")
                ? h2ConsolePath.substring(0, h2ConsolePath.length() - 1)
                : h2ConsolePath;

        return new OrRequestMatcher(
                new AntPathRequestMatcher(normalizedPath),
                new AntPathRequestMatcher(normalizedPath + "/**"));
    }
}
