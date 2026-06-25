package trazzo.back;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BackApplicationTests {

    @MockitoBean
    DataSource dataSource;

    @Test
    void contextLoads() {
    }

}
