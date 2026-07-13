package trazzo.back.shared.security;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigTest {

    @Nested
    @SpringBootTest(classes = TestApplication.class)
    @AutoConfigureMockMvc
    class PublicEndpoints {

        @Autowired MockMvc mockMvc;
        @MockitoBean TokenValidator tokenValidator;
        @MockitoBean UserDetailsService userDetailsService;

        @Test
        void login_isAccessibleWithoutToken() throws Exception {
            // /auth/login is in permitAll(); security passes the request through.
            // Returns 404 in this minimal test context (no real controller loaded), not 401.
            mockMvc.perform(post("/auth/login"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void publicKey_isAccessibleWithoutToken() throws Exception {
            mockMvc.perform(get("/security/public-key"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void errorDispatch_isPermitted_soRealErrorsSurface() throws Exception {
            // /error is permitAll() so Tomcat error re-dispatches reach the error controller
            // with the real status (e.g. 5xx) instead of a misleading 401.
            mockMvc.perform(get("/error"))
                    .andExpect(result ->
                            assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
        }

        @Test
        void publicPlans_isAccessibleWithoutToken() throws Exception {
            // /public/plans backs the marketing site's pricing section (permitAll).
            mockMvc.perform(get("/public/plans"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @SpringBootTest(classes = TestApplication.class)
    @AutoConfigureMockMvc
    class ProtectedViewEndpoints {

        @Autowired MockMvc mockMvc;
        @MockitoBean TokenValidator tokenValidator;
        @MockitoBean UserDetailsService userDetailsService;

        // --- Unauthenticated: must return 401 ---

        @Test
        void shifts_withoutAuth_returns401() throws Exception {
            // GET /corehr/shifts backs "Gestión de Horarios"
            mockMvc.perform(get("/corehr/shifts"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void incidents_withoutAuth_returns401() throws Exception {
            // GET /incidentes backs "Incidencias"
            mockMvc.perform(get("/incidentes"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void users_withoutAuth_returns401() throws Exception {
            // GET /usuarios backs "Directorio del Personal"
            mockMvc.perform(get("/usuarios"))
                    .andExpect(status().isUnauthorized());
        }

        // --- Authenticated: security must pass through (not block with 401) ---

        @Test
        void shifts_withAuth_passedBySecurity() throws Exception {
            // Security allows the request; 404 because no controller is registered
            // in this minimal test context, not because the token was rejected.
            mockMvc.perform(get("/corehr/shifts").with(user("admin@trazzo.pe")))
                    .andExpect(status().isNotFound());
        }

        @Test
        void incidents_withAuth_passedBySecurity() throws Exception {
            mockMvc.perform(get("/incidentes").with(user("admin@trazzo.pe")))
                    .andExpect(status().isNotFound());
        }

        @Test
        void users_withAuth_passedBySecurity() throws Exception {
            mockMvc.perform(get("/usuarios").with(user("admin@trazzo.pe")))
                    .andExpect(status().isNotFound());
        }
    }

    @SpringBootApplication(
            excludeName = {
                    "org.springframework.modulith.runtime.autoconfigure.SpringModulithRuntimeAutoConfiguration",
                    "org.springframework.modulith.actuator.autoconfigure.ApplicationModulesEndpointConfiguration",
                    "org.springframework.boot.autoconfigure.session.SessionAutoConfiguration"
            })
    @Import(SecurityConfig.class)
    static class TestApplication {}
}
