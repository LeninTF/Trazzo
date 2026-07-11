package trazzo.back.shared.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

class H2ConsoleSecurityTest {

    @Nested
    @SpringBootTest(classes = TestApplication.class)
    @AutoConfigureMockMvc
    class DefaultConfiguration {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        TokenValidator tokenValidator;

        @MockitoBean
        UserDetailsService userDetailsService;

        @Test
        void h2ConsoleIsDisabledByDefault() throws Exception {
            mockMvc.perform(get("/h2-console/").with(user("developer")))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @SpringBootTest(
            classes = TestApplication.class,
            properties = "spring.h2.console.enabled=true")
    @AutoConfigureMockMvc
    class EnabledConfiguration {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        TokenValidator tokenValidator;

        @MockitoBean
        UserDetailsService userDetailsService;

        @Test
        void h2ConsoleRequiresAuthenticationWhenEnabled() throws Exception {
            mockMvc.perform(get("/h2-console/"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void h2ConsoleUsesSameOriginFramesOnTheConsolePath() throws Exception {
            mockMvc.perform(get("/h2-console/").with(user("developer").roles("ADMIN")))
                    .andExpect(status().isNotFound())
                    .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
        }
    }

    @SpringBootApplication(
            excludeName = {
                    "org.springframework.modulith.runtime.autoconfigure.SpringModulithRuntimeAutoConfiguration",
                    "org.springframework.modulith.actuator.autoconfigure.ApplicationModulesEndpointConfiguration",
                    "org.springframework.boot.autoconfigure.session.SessionAutoConfiguration"
            })
    @Import(SecurityConfig.class)
    static class TestApplication {
    }
}
