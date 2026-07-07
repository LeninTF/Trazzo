package trazzo.back.shared.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.mockito.Mockito.mock;

@TestConfiguration(proxyBeanMethods = false)
public class SecurityTestMockConfig {

    @Bean
    @Primary
    TokenValidator tokenValidator() {
        return mock(TokenValidator.class);
    }

    @Bean
    UserDetailsService userDetailsService() {
        return mock(UserDetailsService.class);
    }
}
