package trazzo.back.corehr.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.corehr.application.dto.command.CreateTenantContactCommand;
import trazzo.back.corehr.application.dto.command.PatchTenantContactCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.TenantContactResult;
import trazzo.back.corehr.application.port.in.TenantContactUseCase;
import trazzo.back.corehr.domain.exception.InvalidTenantUserException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TenantContactController.class)
@AutoConfigureMockMvc(addFilters = false)
class TenantContactControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TenantContactUseCase tenantContactUseCase;

    private static final LocalDateTime NOW = LocalDateTime.now();

    private static TenantContactResult aResult() {
        var user = new TenantContactResult.TenantUserBasicInfo(
                10L, "Juan", "Perez", "Lopez", "juan@mail.com", "123456789");
        return new TenantContactResult(1L, 10L, "email", user, NOW, NOW, null);
    }

    @Test
    void list_shouldReturn200() throws Exception {
        var paginated = new PaginatedResult<TenantContactResult>(List.of(aResult()), 0, 20, 1, 1);
        when(tenantContactUseCase.findAll(anyInt(), anyInt())).thenReturn(paginated);

        mockMvc.perform(get("/corehr/tenant-contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("email"))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void list_shouldRespectPaginationParams() throws Exception {
        var paginated = new PaginatedResult<TenantContactResult>(List.of(aResult()), 2, 10, 1, 1);
        when(tenantContactUseCase.findAll(eq(2), eq(10))).thenReturn(paginated);

        mockMvc.perform(get("/corehr/tenant-contacts")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        when(tenantContactUseCase.create(any(CreateTenantContactCommand.class))).thenReturn(aResult());

        mockMvc.perform(post("/corehr/tenant-contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenant_user_id": 10, "type": "email"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("email"));
    }

    @Test
    void create_shouldReturn400WhenTenantUserIdIsNull() throws Exception {
        mockMvc.perform(post("/corehr/tenant-contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "email"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenTypeIsBlank() throws Exception {
        mockMvc.perform(post("/corehr/tenant-contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenant_user_id": 10, "type": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn422WhenUseCaseThrowsInvalidTenantUserException() throws Exception {
        when(tenantContactUseCase.create(any(CreateTenantContactCommand.class)))
                .thenThrow(new InvalidTenantUserException("User not found"));

        mockMvc.perform(post("/corehr/tenant-contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenant_user_id": 99, "type": "email"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void patch_shouldReturn200() throws Exception {
        when(tenantContactUseCase.patch(anyLong(), any(PatchTenantContactCommand.class))).thenReturn(aResult());

        mockMvc.perform(patch("/corehr/tenant-contacts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "phone"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("email"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(tenantContactUseCase).deleteById(1L);

        mockMvc.perform(delete("/corehr/tenant-contacts/1"))
                .andExpect(status().isNoContent());
    }
}
