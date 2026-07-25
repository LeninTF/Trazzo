package trazzo.back.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

class SecurityConfigExtraTest {

    @Test
    void filterChainIsBuilt_whenSecurityConfigIsLoaded() {
        runner().run(context -> {
            assertThat(context).hasSingleBean(SecurityFilterChain.class);
            assertThat(context).doesNotHaveBean(TenantContextGuard.class);
        });
    }

    @Test
    void jwtAuthFilterRegistration_isDisabled() {
        runner().run(context -> {
            FilterRegistrationBean<JwtAuthFilter> registration =
                    context.getBean("jwtAuthFilterRegistration", FilterRegistrationBean.class);
            assertThat(registration.isEnabled()).isFalse();
            assertThat(registration.getFilter()).isInstanceOf(JwtAuthFilter.class);
        });
    }

    @Test
    void authenticationManagerBeanIsExposed() {
        runner().run(context ->
                assertThat(context).hasSingleBean(AuthenticationManager.class));
    }

    @Test
    void passwordEncoderBeanIsBCrypt() {
        runner().run(context -> {
            PasswordEncoder encoder = context.getBean(PasswordEncoder.class);
            assertThat(encoder.matches("raw", encoder.encode("raw"))).isTrue();
        });
    }

    @Test
    void h2ConsoleRequestMatcher_matchesConsolePathAndChildren() {
        var matcher = new H2ConsolePathMatcher("/h2-console");
        assertThat(matcher.matches("/h2-console")).isTrue();
        assertThat(matcher.matches("/h2-console/")).isTrue();
        assertThat(matcher.matches("/h2-console/foo")).isTrue();
        assertThat(matcher.matches("/other")).isFalse();
    }

    @Test
    void h2ConsoleRequestMatcher_normalizesTrailingSlashFromConfig() {
        var matcher = new H2ConsolePathMatcher("/h2-console/");
        assertThat(matcher.matches("/h2-console")).isTrue();
        assertThat(matcher.matches("/h2-console/")).isTrue();
        assertThat(matcher.matches("/h2-console/foo")).isTrue();
    }

    private static WebApplicationContextRunner runner() {
        return new WebApplicationContextRunner()
                .withUserConfiguration(TestApplication.class)
                .withPropertyValues(
                        "app.jwt.secret=" + base64Secret(),
                        "app.encryption.key=" + base64AesKey());
    }

    private static String base64Secret() {
        return Base64.getEncoder()
                .encodeToString(io.jsonwebtoken.Jwts.SIG.HS256.key().build().getEncoded());
    }

    private static String base64AesKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        key[0] = 1;
        return Base64.getEncoder().encodeToString(key);
    }

    static final class H2ConsolePathMatcher {
        private final RequestMatcher matcher;

        @SuppressWarnings("unused")
        H2ConsolePathMatcher(String h2ConsolePath) {
            String normalizedPath = h2ConsolePath.endsWith("/")
                    ? h2ConsolePath.substring(0, h2ConsolePath.length() - 1)
                    : h2ConsolePath;
            matcher = new org.springframework.security.web.util.matcher.OrRequestMatcher(
                    new org.springframework.security.web.util.matcher.AntPathRequestMatcher(normalizedPath),
                    new org.springframework.security.web.util.matcher.AntPathRequestMatcher(normalizedPath + "/**"));
        }

        boolean matches(String path) {
            var request = new org.springframework.mock.web.MockHttpServletRequest("GET", path);
            request.setServletPath(path);
            return matcher.matches(request);
        }
    }

    @SpringBootApplication(
            excludeName = {
                    "org.springframework.modulith.runtime.autoconfigure.SpringModulithRuntimeAutoConfiguration",
                    "org.springframework.modulith.actuator.autoconfigure.ApplicationModulesEndpointConfiguration",
                    "org.springframework.modulith.observability.autoconfigure.ModuleObservabilityAutoConfiguration",
                    "org.springframework.boot.autoconfigure.session.SessionAutoConfiguration",
                    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
                    "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
                    "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
            })
    @Import({SecurityConfig.class, SecurityTestMockConfig.class})
    static class TestApplication {

        @org.springframework.context.annotation.Bean
        org.springframework.jdbc.core.JdbcTemplate jdbcTemplate() {
            return org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class);
        }

        @org.springframework.context.annotation.Bean
        trazzo.back.shared.security.TenantPermissionPort tenantPermissionPort() {
            return org.mockito.Mockito.mock(trazzo.back.shared.security.TenantPermissionPort.class);
        }
    }
}
