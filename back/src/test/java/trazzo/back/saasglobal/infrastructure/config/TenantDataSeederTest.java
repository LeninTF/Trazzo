package trazzo.back.saasglobal.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantSchemaProvisioningPort;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

class TenantDataSeederTest {

    private TenantRepositoryPort tenantRepository;
    private TenantSchemaProvisioningPort schemaProvisioning;
    private JdbcTemplate jdbc;
    private PasswordEncoder passwordEncoder;
    private UserRepositoryPort userRepository;
    private TenantDataSeeder seeder;

    @BeforeEach
    void setUp() {
        tenantRepository = mock(TenantRepositoryPort.class);
        schemaProvisioning = mock(TenantSchemaProvisioningPort.class);
        jdbc = mock(JdbcTemplate.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userRepository = mock(UserRepositoryPort.class);
        seeder = new TenantDataSeeder(tenantRepository, schemaProvisioning, jdbc, passwordEncoder, userRepository, "demo");
    }

    @Test
    void constructor_throwsWhenSubDomainBlank() {
        assertThrows(IllegalStateException.class,
                () -> new TenantDataSeeder(tenantRepository, schemaProvisioning, jdbc, passwordEncoder, userRepository, "  "));
    }

    @Test
    void run_whenDemoTenantExistsSkipsSeed() {
        when(tenantRepository.findBySubDomain("demo")).thenReturn(Optional.of(mock(Tenant.class)));

        seeder.run();

        verifyNoInteractions(schemaProvisioning);
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void run_whenPlanAlreadyExistsReusesItAndProvisionsSchema() {
        when(tenantRepository.findBySubDomain("demo")).thenReturn(Optional.empty());
        when(jdbc.queryForList("SELECT id FROM plans WHERE name = ?", Integer.class, "Plan Demo"))
                .thenReturn(List.of(7));
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        mockTenantUserCreation();

        seeder.run();

        verify(jdbc, never()).queryForObject(contains("INSERT INTO plans"), eq(Integer.class), any());
        var settingsCaptor = ArgumentCaptor.forClass(TenantSettings.class);
        verify(schemaProvisioning).provisionExisting(settingsCaptor.capture());
        assertEquals("tenant_demo", settingsCaptor.getValue().getSchemaName());

        var tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        assertEquals("demo", tenantCaptor.getValue().getSubDomain());
        assertEquals(7, tenantCaptor.getValue().getPlanId());
        assertTrue(tenantCaptor.getValue().isActivated());
    }

    @Test
    void run_whenPlanMissingCreatesItThenProvisionsSchema() {
        when(tenantRepository.findBySubDomain("demo")).thenReturn(Optional.empty());
        when(jdbc.queryForList("SELECT id FROM plans WHERE name = ?", Integer.class, "Plan Demo"))
                .thenReturn(List.of());
        when(jdbc.queryForObject(contains("INSERT INTO plans"), eq(Integer.class), eq("Plan Demo")))
                .thenReturn(3);
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        mockTenantUserCreation();

        seeder.run();

        verify(schemaProvisioning).provisionExisting(any(TenantSettings.class));
        var tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        assertEquals(3, tenantCaptor.getValue().getPlanId());
    }

    private void mockTenantUserCreation() {
        when(userRepository.findByEmail("demo@trazzo.pe")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("demo123")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jdbc.queryForObject(eq("SELECT LASTVAL()"), eq(Integer.class))).thenReturn(42);
        when(jdbc.queryForObject(eq("SELECT currval('tenant_user_id_seq')"), eq(Long.class))).thenReturn(1L);
        when(jdbc.queryForObject(eq("SELECT id FROM role WHERE name = 'administrador'"), eq(String.class))).thenReturn("role-uuid");

        when(userRepository.findByEmail("usuario@trazzo.pe")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("usuario123")).thenReturn("encoded-usuario");
        when(jdbc.queryForObject(eq("SELECT id FROM role WHERE name = 'usuario'"), eq(String.class))).thenReturn("usuario-role-uuid");
    }
}
