package trazzo.back;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import trazzo.back.saasglobal.infrastructure.adapters.out.provisioning.TenantSchemaMigrator;

@SpringBootTest
class BackApplicationTests {

    @MockitoBean
    private TenantSchemaMigrator tenantSchemaMigrator;

    @Test
    void contextLoads() {
    }

}
