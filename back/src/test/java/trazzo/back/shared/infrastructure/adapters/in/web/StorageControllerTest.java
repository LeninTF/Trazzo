package trazzo.back.shared.infrastructure.adapters.in.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.shared.application.port.out.FileStoragePort;

@ExtendWith(SpringExtension.class)
@WebMvcTest(StorageController.class)
@WithMockUser
class StorageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileStoragePort fileStoragePort;

    @Test
    void getPresignedUrlShouldReturnUrlAndObjectKey() throws Exception {
        when(fileStoragePort.generatePresignedPutUrl(anyString(), anyString(), any()))
                .thenReturn("https://r2.example.com/presigned");

        mockMvc.perform(get("/storage/presigned-url")
                        .param("fileName", "photo.jpg")
                        .param("contentType", "image/jpeg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presigned_url").value("https://r2.example.com/presigned"))
                .andExpect(jsonPath("$.object_key").isNotEmpty())
                .andExpect(jsonPath("$.object_key").value(org.hamcrest.Matchers.containsString("photo.jpg")));
    }
}
