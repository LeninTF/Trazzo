package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.saasglobal.application.dto.command.CreateTrialTenantCommand;
import trazzo.back.saasglobal.application.dto.result.TenantResultDto;
import trazzo.back.saasglobal.application.port.in.CreateTrialTenantUseCase;
import trazzo.back.shared.security.AuthenticatedUser;
import trazzo.back.shared.security.SecurityConfig;

@WebMvcTest(TenantController.class)
@Import(SecurityConfig.class)
class TenantControllerTest {

    private static final String TRIAL_BODY = """
            {
              "subDomain": "acme",
              "planId": 1,
              "holdingId": 10,
              "logoUrl": "https://cdn.example.com/acme.png",
              "slogan": "Educar es todo",
              "primaryColor": "#112233",
              "secondaryColor": "#445566"
            }
            """;

    @Autowired MockMvc mockMvc;
    @MockitoBean CreateTrialTenantUseCase createTrialTenantUseCase;

    private static UsernamePasswordAuthenticationToken authWithAuthorities(String... authorities) {
        var authUser = new AuthenticatedUser(UUID.randomUUID(), "admin@trazzo.pe", "pass", List.of(), true);
        var grants = List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(authUser, null, grants);
    }

    @Test
    void createTrial_returns201WithBody() throws Exception {
        var result = new TenantResultDto("id-1", "acme", 1, true, null, null);
        when(createTrialTenantUseCase.createTrial(any())).thenReturn(result);

        mockMvc.perform(post("/tenants/trial")
                        .with(authentication(authWithAuthorities("ROLE_SAAS_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRIAL_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("id-1"))
                .andExpect(jsonPath("$.subDomain").value("acme"))
                .andExpect(jsonPath("$.activated").value(true));

        var command = ArgumentCaptor.forClass(CreateTrialTenantCommand.class);
        verify(createTrialTenantUseCase).createTrial(command.capture());
        assertThat(command.getValue()).isEqualTo(new CreateTrialTenantCommand(
                "acme", 1, 10, "https://cdn.example.com/acme.png", "Educar es todo", "#112233", "#445566"));
    }

    @Test
    void createTrial_returns400WhenRequiredFieldMissing() throws Exception {
        mockMvc.perform(post("/tenants/trial")
                        .with(authentication(authWithAuthorities("ROLE_SAAS_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": 1,
                                  "holdingId": 10
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(createTrialTenantUseCase, never()).createTrial(any());
    }

    @Test
    void createTrial_isForbiddenForTenantAdministrator() throws Exception {
        mockMvc.perform(post("/tenants/trial")
                        .with(authentication(authWithAuthorities("ROLE_administrador_tenant", "administrador_tenant")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRIAL_BODY))
                .andExpect(status().isForbidden());

        verify(createTrialTenantUseCase, never()).createTrial(any());
    }

    @Test
    void createTrial_returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/tenants/trial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRIAL_BODY))
                .andExpect(status().isUnauthorized());

        verify(createTrialTenantUseCase, never()).createTrial(any());
    }
}
