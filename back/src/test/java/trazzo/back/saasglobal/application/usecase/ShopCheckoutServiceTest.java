package trazzo.back.saasglobal.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import trazzo.back.saasglobal.application.port.out.AppConfigPort;
import trazzo.back.saasglobal.application.port.out.PasswordHasherPort;
import trazzo.back.saasglobal.application.dto.command.CreateTrialTenantCommand;
import trazzo.back.saasglobal.application.dto.command.ShopCheckoutCommand;
import trazzo.back.saasglobal.application.dto.result.ShopCheckoutResult;
import trazzo.back.saasglobal.application.dto.result.TenantResultDto;
import trazzo.back.saasglobal.application.port.in.CreateTrialTenantUseCase;
import trazzo.back.saasglobal.application.port.out.EmailService;
import trazzo.back.saasglobal.application.port.out.HoldingRepositoryPort;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort.PreapprovalCreated;
import trazzo.back.saasglobal.application.port.out.PersonRepositoryPort;
import trazzo.back.saasglobal.application.port.out.PlanRepositoryPort;
import trazzo.back.saasglobal.application.port.out.SubscriptionRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantSchemaProvisioningPort;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.exception.TenantValidationException;
import trazzo.back.saasglobal.domain.model.iam.Person;
import trazzo.back.saasglobal.domain.model.iam.User;
import trazzo.back.saasglobal.domain.model.multitenancy.Holding;
import trazzo.back.saasglobal.domain.model.multitenancy.HoldingType;
import trazzo.back.saasglobal.domain.model.multitenancy.Plan;
import trazzo.back.saasglobal.domain.model.multitenancy.Subscription;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShopCheckoutServiceTest {

    @Mock PlanRepositoryPort planRepository;
    @Mock HoldingRepositoryPort holdingRepository;
    @Mock TenantRepositoryPort tenantRepository;
    @Mock CreateTrialTenantUseCase createTrialTenantUseCase;
    @Mock TenantSchemaProvisioningPort schemaProvisioning;
    @Mock SubscriptionRepositoryPort subscriptionRepository;
    @Mock PersonRepositoryPort personRepository;
    @Mock UserRepositoryPort userRepository;
    @Mock PasswordHasherPort passwordHasher;
    @Mock AppConfigPort appConfig;
    @Mock EmailService emailService;
    @Mock MercadoPagoSubscriptionPort mercadoPagoSubscriptionPort;
    @InjectMocks ShopCheckoutService service;

    private static Plan plan() {
        return Plan.restore(2, "Plan Demo", new BigDecimal("29.99"), null, "SOLES", "MONTHLY",
                true, LocalDateTime.now(), null, null);
    }

    private static ShopCheckoutCommand command() {
        return new ShopCheckoutCommand(
                2, "Juan", "Perez", "Lopez", "DNI", "12345678", "juan@acme.pe", "999999999",
                "20123456789", "Acme SAC", "Acme Sociedad Anonima Cerrada", "Av. Siempre Viva 123",
                false, null, null, null, null, null, null, null);
    }

    @Test
    void checkout_happyPath_createsTenantAdminAndPreapproval() {
        when(appConfig.frontendUrl()).thenReturn("http://localhost:4200");

        when(planRepository.findById(2)).thenReturn(Optional.of(plan()));
        when(holdingRepository.findByTaxId("20123456789")).thenReturn(Optional.empty());
        when(holdingRepository.save(any())).thenAnswer(inv -> {
            Holding h = inv.getArgument(0);
            return Holding.restore(1, h.getTaxId(), h.getLegalName(), h.getType(), true, LocalDateTime.now(), LocalDateTime.now(), null);
        });
        when(tenantRepository.existsBySubDomain(anyString())).thenReturn(false);
        when(createTrialTenantUseCase.createTrial(any(CreateTrialTenantCommand.class)))
                .thenReturn(new TenantResultDto("tenant-1", "acme-sac", 2, true, LocalDateTime.now(), LocalDateTime.now()));
        when(personRepository.save(any())).thenAnswer(inv -> {
            Person p = inv.getArgument(0);
            return Person.restore(1, null, p.getDocumentType(), p.getDocumentValue(), p.getName(),
                    p.getFatherSurname(), p.getMotherSurname(), null, LocalDateTime.now(), LocalDateTime.now());
        });
        when(passwordHasher.hash(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionRepository.findActiveByTenantId("tenant-1")).thenReturn(Optional.of(
                Subscription.createTrial("tenant-1", 2, BigDecimal.ZERO, LocalDate.now())));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mercadoPagoSubscriptionPort.createPreapproval(any()))
                .thenReturn(new PreapprovalCreated("preapproval-1", "pending", "https://mp/init", "https://mp/sandbox-init"));

        ShopCheckoutResult result = service.checkout(command());

        assertEquals("tenant-1", result.tenantId());
        assertEquals("acme-sac", result.subDomain());
        assertEquals("https://mp/sandbox-init", result.initPoint());
        verify(userRepository).save(any());
        verify(emailService).send(anyString(), anyString(), anyString());
        verify(subscriptionRepository).save(any());
    }

    @Test
    void checkout_reusesExistingHoldingByTaxId() {
        when(appConfig.frontendUrl()).thenReturn("http://localhost:4200");

        when(planRepository.findById(2)).thenReturn(Optional.of(plan()));
        Holding existing = Holding.restore(5, "20123456789", "Acme SAC", HoldingType.PRIVADO, true,
                LocalDateTime.now(), LocalDateTime.now(), null);
        when(holdingRepository.findByTaxId("20123456789")).thenReturn(Optional.of(existing));
        when(tenantRepository.existsBySubDomain(anyString())).thenReturn(false);
        when(createTrialTenantUseCase.createTrial(any(CreateTrialTenantCommand.class)))
                .thenReturn(new TenantResultDto("tenant-1", "acme-sac", 2, true, LocalDateTime.now(), LocalDateTime.now()));
        when(personRepository.save(any())).thenAnswer(inv -> {
            Person p = inv.getArgument(0);
            return Person.restore(1, null, p.getDocumentType(), p.getDocumentValue(), p.getName(),
                    p.getFatherSurname(), p.getMotherSurname(), null, LocalDateTime.now(), LocalDateTime.now());
        });
        when(passwordHasher.hash(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionRepository.findActiveByTenantId("tenant-1")).thenReturn(Optional.of(
                Subscription.createTrial("tenant-1", 2, BigDecimal.ZERO, LocalDate.now())));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mercadoPagoSubscriptionPort.createPreapproval(any()))
                .thenReturn(new PreapprovalCreated("preapproval-1", "pending", "https://mp/init", null));

        service.checkout(command());

        verify(holdingRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void checkout_throwsWhenPlanMissing() {
        when(appConfig.frontendUrl()).thenReturn("http://localhost:4200");
        when(planRepository.findById(2)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.checkout(command()));
    }

    @Test
    void checkout_throwsWhenSubDomainSuffixesExhausted() {
        when(appConfig.frontendUrl()).thenReturn("http://localhost:4200");
        when(planRepository.findById(2)).thenReturn(Optional.of(plan()));
        when(holdingRepository.findByTaxId("20123456789")).thenReturn(Optional.empty());
        when(holdingRepository.save(any())).thenAnswer(inv -> {
            Holding h = inv.getArgument(0);
            return Holding.restore(1, h.getTaxId(), h.getLegalName(), h.getType(), true, LocalDateTime.now(), LocalDateTime.now(), null);
        });
        when(tenantRepository.existsBySubDomain(anyString())).thenReturn(true);

        assertThrows(TenantValidationException.class, () -> service.checkout(command()));

        verify(createTrialTenantUseCase, never()).createTrial(any());
    }

    @Test
    void checkout_throwsWhenPlanInactive() {
        when(appConfig.frontendUrl()).thenReturn("http://localhost:4200");
        Plan inactive = Plan.restore(2, "Plan Demo", new BigDecimal("29.99"), null, "SOLES", "MONTHLY",
                false, LocalDateTime.now(), null, null);
        when(planRepository.findById(2)).thenReturn(Optional.of(inactive));

        assertThrows(IllegalArgumentException.class, () -> service.checkout(command()));
    }

    @Test
    void checkout_usesAlternateAdminWhenAnotherAdminTrue() {
        when(appConfig.frontendUrl()).thenReturn("http://localhost:4200");
        var cmd = new ShopCheckoutCommand(
                2, "Juan", "Perez", "Lopez", "DNI", "12345678", "juan@acme.pe", "999999999",
                "20123456789", "Acme SAC", "Acme Sociedad Anonima Cerrada", "Av. Siempre Viva 123",
                true, "Maria", "Garcia", "Torres", "CE", "CE12345", "maria@acme.pe", "988888888");

        when(planRepository.findById(2)).thenReturn(Optional.of(plan()));
        when(holdingRepository.findByTaxId("20123456789")).thenReturn(Optional.empty());
        when(holdingRepository.save(any())).thenAnswer(inv -> {
            Holding h = inv.getArgument(0);
            return Holding.restore(1, h.getTaxId(), h.getLegalName(), h.getType(), true, LocalDateTime.now(), LocalDateTime.now(), null);
        });
        when(tenantRepository.existsBySubDomain(anyString())).thenReturn(false);
        when(createTrialTenantUseCase.createTrial(any(CreateTrialTenantCommand.class)))
                .thenReturn(new TenantResultDto("tenant-1", "acme-sac", 2, true, LocalDateTime.now(), LocalDateTime.now()));
        when(personRepository.save(any())).thenAnswer(inv -> {
            Person p = inv.getArgument(0);
            return Person.restore(1, null, p.getDocumentType(), p.getDocumentValue(), p.getName(),
                    p.getFatherSurname(), p.getMotherSurname(), null, LocalDateTime.now(), LocalDateTime.now());
        });
        when(passwordHasher.hash(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionRepository.findActiveByTenantId("tenant-1")).thenReturn(Optional.of(
                Subscription.createTrial("tenant-1", 2, BigDecimal.ZERO, LocalDate.now())));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mercadoPagoSubscriptionPort.createPreapproval(any()))
                .thenReturn(new PreapprovalCreated("preapproval-1", "pending", "https://mp/init", null));

        service.checkout(cmd);

        var userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("maria@acme.pe", userCaptor.getValue().getEmail());
    }

    private void wireTenantCreatedUpToAdminUser() {
        when(planRepository.findById(2)).thenReturn(Optional.of(plan()));
        when(holdingRepository.findByTaxId("20123456789")).thenReturn(Optional.empty());
        when(holdingRepository.save(any())).thenAnswer(inv -> {
            Holding h = inv.getArgument(0);
            return Holding.restore(1, h.getTaxId(), h.getLegalName(), h.getType(), true, LocalDateTime.now(), LocalDateTime.now(), null);
        });
        when(tenantRepository.existsBySubDomain(anyString())).thenReturn(false);
        when(createTrialTenantUseCase.createTrial(any(CreateTrialTenantCommand.class)))
                .thenReturn(new TenantResultDto("tenant-1", "acme-sac", 2, true, LocalDateTime.now(), LocalDateTime.now()));
        when(personRepository.save(any())).thenAnswer(inv -> {
            Person p = inv.getArgument(0);
            return Person.restore(1, null, p.getDocumentType(), p.getDocumentValue(), p.getName(),
                    p.getFatherSurname(), p.getMotherSurname(), null, LocalDateTime.now(), LocalDateTime.now());
        });
        when(passwordHasher.hash(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void checkout_rollsBackTenantAndAdminUserWhenPreapprovalCreationFails() {
        when(appConfig.frontendUrl()).thenReturn("http://localhost:4200");
        wireTenantCreatedUpToAdminUser();
        User createdUser = User.create(1, "tenant-1", "juan@acme.pe", "999999999", "encoded", true);
        when(userRepository.findByTenantId("tenant-1")).thenReturn(Optional.of(createdUser));
        Tenant tenantWithSchema = Tenant.restore("tenant-1", null, "acme-sac", 2,
                TenantSettings.of("tenant-1", "tenant_acme_sac"), null, LocalDateTime.now(), null,
                LocalDateTime.now(), LocalDateTime.now(), null);
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenantWithSchema));
        when(mercadoPagoSubscriptionPort.createPreapproval(any()))
                .thenThrow(new RuntimeException("Mercado Pago rejected the request"));

        assertThrows(RuntimeException.class, () -> service.checkout(command()));

        verify(personRepository).deleteById(1);
        verify(subscriptionRepository).deleteByTenantId("tenant-1");
        verify(tenantRepository).purgeById("tenant-1");
        verify(schemaProvisioning).deprovision("tenant_acme_sac");
        verify(emailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void checkout_rollsBackTenantWithoutDeletingPersonWhenAdminUserCreationFails() {
        when(appConfig.frontendUrl()).thenReturn("http://localhost:4200");
        when(planRepository.findById(2)).thenReturn(Optional.of(plan()));
        when(holdingRepository.findByTaxId("20123456789")).thenReturn(Optional.empty());
        when(holdingRepository.save(any())).thenAnswer(inv -> {
            Holding h = inv.getArgument(0);
            return Holding.restore(1, h.getTaxId(), h.getLegalName(), h.getType(), true, LocalDateTime.now(), LocalDateTime.now(), null);
        });
        when(tenantRepository.existsBySubDomain(anyString())).thenReturn(false);
        when(createTrialTenantUseCase.createTrial(any(CreateTrialTenantCommand.class)))
                .thenReturn(new TenantResultDto("tenant-1", "acme-sac", 2, true, LocalDateTime.now(), LocalDateTime.now()));
        when(personRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate document_value"));
        when(userRepository.findByTenantId("tenant-1")).thenReturn(Optional.empty());
        Tenant tenantWithSchema = Tenant.restore("tenant-1", null, "acme-sac", 2,
                TenantSettings.of("tenant-1", "tenant_acme_sac"), null, LocalDateTime.now(), null,
                LocalDateTime.now(), LocalDateTime.now(), null);
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenantWithSchema));

        assertThrows(DataIntegrityViolationException.class, () -> service.checkout(command()));

        verify(personRepository, never()).deleteById(any());
        verify(subscriptionRepository).deleteByTenantId("tenant-1");
        verify(tenantRepository).purgeById("tenant-1");
        verify(schemaProvisioning).deprovision("tenant_acme_sac");
    }
}
