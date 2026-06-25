package trazzo.back.saasglobal.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";

    @Bean
    @SuppressWarnings("java:S4502")
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(LOGIN_URL, LOGOUT_URL))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(LOGIN_URL, "/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
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
}
