package trazzo.back.shared.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.mockito.Mockito.mock;

/**
 * Auto-applied to every @WebMvcTest slice so JwtAuthFilter can be created.
 * Registered via META-INF/spring/AutoConfigureWebMvc.imports.
 */
@TestConfiguration(proxyBeanMethods = false)
public class SecurityTestMockConfig {

    @Bean
    public JwtService jwtService() {
        return mock(JwtService.class);
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return mock(UserDetailsService.class);
    }
}
