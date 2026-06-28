package trazzo.back;

import javax.sql.DataSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Disabled("Requires all beans to be wired. Enable once full application context is configured.")
class BackApplicationTests {

    // DataSource mock: evita que DataSourceAutoConfiguration intente conectar a PostgreSQL.
    @MockitoBean
    DataSource dataSource;

    // JdbcTemplate mock: JdbcTemplateAutoConfiguration no crea el bean real cuando
    // DataSource es un mock (ConditionalOnSingleCandidate no se satisface con Mockito proxy).
    @MockitoBean
    JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

}
