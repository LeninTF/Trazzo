package trazzo.back.shared.tenancy;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Builds the application's single physical HikariCP DataSource explicitly (instead of
 * relying on Spring Boot's auto-configured bean) so it can be wrapped in
 * {@link TenantAwareDataSource} and exposed as the {@code @Primary} DataSource that both
 * JPA and JdbcTemplate consumers pick up.
 */
@Configuration
public class TenancyBeanConfiguration {

    @Bean
    @ConfigurationProperties("spring.datasource")
    DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    DataSource rawDataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @Primary
    DataSource dataSource(DataSource rawDataSource) {
        return new TenantAwareDataSource(rawDataSource);
    }
}
