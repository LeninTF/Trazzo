package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.saasglobal.application.dto.result.ShopCheckoutResult;
import trazzo.back.saasglobal.application.port.in.ShopCheckoutUseCase;
import trazzo.back.shared.security.SecurityConfig;

@WebMvcTest(ShopCheckoutController.class)
@Import(SecurityConfig.class)
class ShopCheckoutControllerTest {

    private static final String VALID_BODY = """
            {
              "planId": 2,
              "firstName": "Juan",
              "lastNamePaterno": "Perez",
              "lastNameMaterno": "Lopez",
              "documentType": "DNI",
              "documentNumber": "12345678",
              "email": "juan@acme.pe",
              "phone": "999999999",
              "ruc": "20123456789",
              "companyName": "Acme SAC",
              "businessName": "Acme Sociedad Anonima Cerrada",
              "address": "Av. Siempre Viva 123",
              "anotherAdmin": false
            }
            """;

    @Autowired MockMvc mockMvc;
    @MockitoBean ShopCheckoutUseCase shopCheckoutUseCase;

    @Test
    void checkout_returns200_withInitPoint() throws Exception {
        when(shopCheckoutUseCase.checkout(any()))
                .thenReturn(new ShopCheckoutResult("tenant-1", "acme-sac", "https://mp/sandbox-init"));

        mockMvc.perform(post("/shop/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-1"))
                .andExpect(jsonPath("$.subDomain").value("acme-sac"))
                .andExpect(jsonPath("$.initPoint").value("https://mp/sandbox-init"));
    }

    @Test
    void checkout_returns400_whenPlanIdMissing() throws Exception {
        mockMvc.perform(post("/shop/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Juan","lastNamePaterno":"Perez","lastNameMaterno":"Lopez",
                                 "documentType":"DNI","documentNumber":"12345678","email":"juan@acme.pe",
                                 "phone":"999999999","ruc":"20123456789","companyName":"Acme SAC",
                                 "businessName":"Acme SAC"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_returns400_whenEmailInvalid() throws Exception {
        mockMvc.perform(post("/shop/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("juan@acme.pe", "not-an-email")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_returns400_whenPlanNotFound() throws Exception {
        when(shopCheckoutUseCase.checkout(any()))
                .thenThrow(new IllegalArgumentException("Plan not found: 2"));

        mockMvc.perform(post("/shop/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_returns409_whenDocumentAlreadyRegistered() throws Exception {
        when(shopCheckoutUseCase.checkout(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"persons_document_value_key\""));

        mockMvc.perform(post("/shop/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict());
    }
}
