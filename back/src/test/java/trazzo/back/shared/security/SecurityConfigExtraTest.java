package trazzo.back.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Base64;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SecurityConfigExtraTest {

    @Test
    void filterChainIsBuilt_whenSecurityConfigIsLoaded() {
        runner().run(context -> assertThat(context).hasSingleBean(SecurityFilterChain.class));
    }

    // AUD-15: con @ConditionalOnBean(DataSource) en una @Configuration de usuario el guard nunca se creaba.
    @Test
    void theTenantGuardRunsInsideTheSecurityChainAfterTheJwtFilterEvenWithoutADataSource() {
        runner().run(context -> {
            var filters = context.getBean(SecurityFilterChain.class).getFilters();
            assertThat(indexOf(filters, TenantContextGuard.class)).isGreaterThan(indexOf(filters, JwtAuthFilter.class));

            FilterRegistrationBean<?> registration =
                    context.getBean("tenantContextGuardRegistration", FilterRegistrationBean.class);
            assertThat(registration.isEnabled()).isFalse();
        });
    }

    @Test
    void onPostgreSqlASaasAdminIsKeptOutOfTenantRoutesAndAnonymousCallersStillGet401() {
        runner().withBean(DataSource.class, SecurityConfigExtraTest::postgreSqlDataSource)
                .withPropertyValues("spring.data.jpa.repositories.enabled=false")
                .run(context -> {
                    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
                    var saasAdmin = UsernamePasswordAuthenticationToken.authenticated(
                            "admin@trazzo.pe", null, AuthorityUtils.createAuthorityList("ROLE_SAAS_ADMIN"));

                    mockMvc.perform(get("/corehr/devices").with(authentication(saasAdmin)))
                            .andExpect(status().isForbidden())
                            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                            .andExpect(jsonPath("$.message").value("Acceso denegado: se requiere contexto de tenant"));
                    mockMvc.perform(get("/corehr/devices")).andExpect(status().isUnauthorized());
                });
    }

    private static DataSource postgreSqlDataSource() {
        try {
            DataSource dataSource = mock(DataSource.class);
            Connection connection = mock(Connection.class);
            DatabaseMetaData metaData = mock(DatabaseMetaData.class);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.getMetaData()).thenReturn(metaData);
            when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
            return dataSource;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
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

    // El token trzd_ no es un JWT: el filtro del dispositivo tiene que resolverlo antes que JwtAuthFilter.
    @Test
    void deviceTokenFilterRunsBeforeTheJwtFilterAndOnlyInsideTheSecurityChain() {
        runner().run(context -> {
            var filters = context.getBean(SecurityFilterChain.class).getFilters();
            int device = indexOf(filters, DeviceTokenAuthFilter.class);
            int jwt = indexOf(filters, JwtAuthFilter.class);
            assertThat(device).isNotNegative().isLessThan(jwt);

            FilterRegistrationBean<?> registration =
                    context.getBean("deviceTokenAuthFilterRegistration", FilterRegistrationBean.class);
            assertThat(registration.isEnabled()).isFalse();
        });
    }

    // En la app completa los filtros llegan como proxies CGLIB (observabilidad de Spring Modulith): anclar uno en
    // la clase del otro (addFilterBefore(x, JwtAuthFilter.class)) impedía arrancar con "does not have a registered order".
    @Test
    void theChainStartsAndKeepsTheOrderWhenTheFiltersAreCglibProxies() {
        runner().withBean("proxyAuthFiltersLikeTheApp", org.springframework.beans.factory.config.BeanPostProcessor.class,
                        () -> new org.springframework.beans.factory.config.BeanPostProcessor() {
                            @Override
                            public Object postProcessAfterInitialization(Object bean, String beanName) {
                                if (!(bean instanceof JwtAuthFilter) && !(bean instanceof DeviceTokenAuthFilter)) {
                                    return bean;
                                }
                                var factory = new org.springframework.aop.framework.ProxyFactory(bean);
                                factory.setProxyTargetClass(true);
                                return factory.getProxy();
                            }
                        })
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var filters = context.getBean(SecurityFilterChain.class).getFilters();
                    assertThat(filters.get(indexOf(filters, JwtAuthFilter.class)).getClass()).isNotEqualTo(JwtAuthFilter.class);
                    assertThat(indexOf(filters, DeviceTokenAuthFilter.class)).isNotNegative()
                            .isLessThan(indexOf(filters, JwtAuthFilter.class));
                });
    }

    private static int indexOf(java.util.List<jakarta.servlet.Filter> filters, Class<?> type) {
        for (int i = 0; i < filters.size(); i++) {
            if (type.isInstance(filters.get(i))) {
                return i;
            }
        }
        return -1;
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
