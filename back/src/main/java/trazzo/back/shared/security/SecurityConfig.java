package trazzo.back.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${spring.h2.console.path:/h2-console}") String h2ConsolePath
    ) throws Exception {
        RequestMatcher h2Console = h2ConsoleRequestMatcher(h2ConsolePath);

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(h2Console).authenticated()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(h2Console))
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.disable())
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                h2Console,
                                new XFrameOptionsHeaderWriter(XFrameOptionsMode.SAMEORIGIN)))
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                new NegatedRequestMatcher(h2Console),
                                new XFrameOptionsHeaderWriter(XFrameOptionsMode.DENY))))
                .httpBasic(Customizer.withDefaults())
                .formLogin(Customizer.withDefaults());

        return http.build();
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
