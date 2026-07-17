package trazzo.back.saasglobal.application.usecase;

import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import trazzo.back.saasglobal.application.dto.command.CreateTrialTenantCommand;
import trazzo.back.saasglobal.application.dto.command.ShopCheckoutCommand;
import trazzo.back.saasglobal.application.dto.result.ShopCheckoutResult;
import trazzo.back.saasglobal.application.port.in.CreateTrialTenantUseCase;
import trazzo.back.saasglobal.application.port.in.ShopCheckoutUseCase;
import trazzo.back.saasglobal.application.port.out.AppConfigPort;
import trazzo.back.saasglobal.application.port.out.EmailService;
import trazzo.back.saasglobal.application.port.out.HoldingRepositoryPort;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort.PreapprovalCreated;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort.PreapprovalRequest;
import trazzo.back.saasglobal.application.port.out.PasswordHasherPort;
import trazzo.back.saasglobal.application.port.out.PersonRepositoryPort;
import trazzo.back.saasglobal.application.port.out.PlanRepositoryPort;
import trazzo.back.saasglobal.application.port.out.SubscriptionRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantSchemaProvisioningPort;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.exception.TenantValidationException;
import trazzo.back.saasglobal.domain.model.iam.DocumentType;
import trazzo.back.saasglobal.domain.model.iam.Person;
import trazzo.back.saasglobal.domain.model.iam.User;
import trazzo.back.saasglobal.domain.model.multitenancy.Holding;
import trazzo.back.saasglobal.domain.model.multitenancy.HoldingType;
import trazzo.back.saasglobal.domain.model.multitenancy.Plan;
import trazzo.back.saasglobal.domain.model.multitenancy.Subscription;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

/**
 * Orchestrates the /shop self-signup flow: a new customer picks a plan, fills their contact and
 * billing details, and this service provisions a TRIAL tenant immediately (activated later by
 * the Mercado Pago webhook once the payer authorizes the recurring charge), creates the admin
 * login, and starts the Mercado Pago preapproval that the frontend redirects the payer to.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopCheckoutService implements ShopCheckoutUseCase {

    private static final int TEMP_PASSWORD_LENGTH = 16;
    private static final String TEMP_SECRET_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
    private static final int MAX_SUBDOMAIN_BASE_LENGTH = 40;
    private static final int MAX_SUBDOMAIN_SUFFIX_ATTEMPTS = 1000;

    private final PlanRepositoryPort planRepository;
    private final HoldingRepositoryPort holdingRepository;
    private final TenantRepositoryPort tenantRepository;
    private final CreateTrialTenantUseCase createTrialTenantUseCase;
    private final TenantSchemaProvisioningPort schemaProvisioning;
    private final SubscriptionRepositoryPort subscriptionRepository;
    private final PersonRepositoryPort personRepository;
    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final EmailService emailService;
    private final MercadoPagoSubscriptionPort mercadoPagoSubscriptionPort;
    private final AppConfigPort appConfig;
    private final SecureRandom random = new SecureRandom();

    private record AdminUserCreated(Person person, User user, String rawPassword) {}

    /**
     * Deliberately NOT @Transactional: createTrialTenantUseCase.createTrial() provisions the
     * tenant's Postgres schema over a raw, non-Spring-managed connection (see
     * TenantSchemaProvisioningAdapter), so wrapping this method in a transaction cannot roll
     * that back. Everything after createTrial() succeeds (admin user, Mercado Pago preapproval)
     * is instead guarded by an explicit try/catch below that compensates — deletes the admin
     * user/person, the subscription, the tenant row, and drops the schema — so a checkout that
     * never reached Mercado Pago (or that Mercado Pago rejected) leaves no trace, and the
     * customer can retry with the same data instead of hitting a duplicate-document/email 409.
     */
    @Override
    public ShopCheckoutResult checkout(ShopCheckoutCommand cmd) {
        Plan plan = planRepository.findById(requirePlanId(cmd.planId()))
                .filter(Plan::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found or inactive: " + cmd.planId()));

        Holding holding = findOrCreateHolding(cmd.ruc(), cmd.businessName());
        String subDomain = deriveUniqueSubDomain(cmd.companyName());

        var tenantResult = createTrialTenantUseCase.createTrial(
                new CreateTrialTenantCommand(subDomain, plan.getId(), holding.getId(), null, null, null, null));

        try {
            AdminUserCreated admin = createAdminUser(tenantResult.id(), cmd);

            PreapprovalCreated preapproval = mercadoPagoSubscriptionPort.createPreapproval(new PreapprovalRequest(
                    plan.getPrice(),
                    plan.getCurrency(),
                    plan.getBillingPeriod(),
                    cmd.email(),
                    tenantResult.id(),
                    appConfig.frontendUrl() + "/shop/gracias",
                    "Suscripción Trazzo - " + plan.getName()));

            linkPreapprovalToSubscription(tenantResult.id(), preapproval.id());

            // Sent only once the preapproval exists — sending "your account is ready" before
            // this point would be misleading if the checkout then fails and gets rolled back.
            emailService.send(admin.user().getEmail(), "Bienvenido a Trazzo",
                    "Tu cuenta de administrador fue creada. Contraseña temporal: " + admin.rawPassword()
                            + "<br>Deberás cambiarla al iniciar sesión.");

            String redirectUrl = preapproval.sandboxInitPoint() != null
                    ? preapproval.sandboxInitPoint() : preapproval.initPoint();
            return new ShopCheckoutResult(tenantResult.id(), subDomain, redirectUrl);
        } catch (RuntimeException e) {
            compensateFailedCheckout(tenantResult.id());
            throw e;
        }
    }

    private void compensateFailedCheckout(String tenantId) {
        try {
            String schemaName = tenantRepository.findById(tenantId)
                    .map(Tenant::getSettings)
                    .map(TenantSettings::getSchemaName)
                    .orElse(null);
            // findAllByTenantId (not findByTenantId): the latter filters out soft-deleted rows
            // and returns only the first match — either gap would leave a user row behind
            // holding the tenants.tenant_id FK and silently blocking purgeById below.
            userRepository.findAllByTenantId(tenantId)
                    .forEach(user -> personRepository.deleteById(user.getPersonId()));
            subscriptionRepository.deleteByTenantId(tenantId);
            tenantRepository.purgeById(tenantId);
            if (schemaName != null) {
                schemaProvisioning.deprovision(schemaName);
            }
            log.info("Rolled back failed checkout for tenant {}", tenantId);
        } catch (RuntimeException cleanupEx) {
            log.error("Failed to fully roll back checkout for tenant {} — manual cleanup needed", tenantId, cleanupEx);
        }
    }

    private Integer requirePlanId(Integer planId) {
        if (planId == null) {
            throw new IllegalArgumentException("planId is required");
        }
        return planId;
    }

    private Holding findOrCreateHolding(String ruc, String businessName) {
        return holdingRepository.findByTaxId(ruc)
                .orElseGet(() -> holdingRepository.save(Holding.create(ruc, businessName, HoldingType.PRIVADO)));
    }

    private String deriveUniqueSubDomain(String companyName) {
        String base = companyName.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+)|(-+$)", "");
        if (base.isBlank()) {
            throw new TenantValidationException("companyName does not yield a usable subDomain: " + companyName);
        }
        if (base.length() > MAX_SUBDOMAIN_BASE_LENGTH) {
            base = base.substring(0, MAX_SUBDOMAIN_BASE_LENGTH);
        }
        String candidate = base;
        int suffix = 1;
        while (tenantRepository.existsBySubDomain(candidate)) {
            suffix++;
            if (suffix > MAX_SUBDOMAIN_SUFFIX_ATTEMPTS) {
                throw new TenantValidationException(
                        "Could not derive a unique subDomain for companyName: " + companyName);
            }
            candidate = base + "-" + suffix;
        }
        return candidate;
    }

    private AdminUserCreated createAdminUser(String tenantId, ShopCheckoutCommand cmd) {
        boolean useAltAdmin = cmd.anotherAdmin();
        String firstName = useAltAdmin ? cmd.adminFirstName() : cmd.firstName();
        String fatherSurname = useAltAdmin ? cmd.adminLastNamePaterno() : cmd.lastNamePaterno();
        String motherSurname = useAltAdmin ? cmd.adminLastNameMaterno() : cmd.lastNameMaterno();
        String documentType = useAltAdmin ? cmd.adminDocumentType() : cmd.documentType();
        String documentNumber = useAltAdmin ? cmd.adminDocumentNumber() : cmd.documentNumber();
        String email = useAltAdmin ? cmd.adminEmail() : cmd.email();
        String phone = useAltAdmin ? cmd.adminPhone() : cmd.phone();

        Person person = personRepository.save(Person.create(
                null, toDocumentType(documentType), documentNumber, firstName, fatherSurname, motherSurname, null));

        String rawPassword = generateTempPassword();
        String encodedPassword = passwordHasher.hash(rawPassword);
        User user = userRepository.save(User.create(person.getId(), tenantId, email, phone, encodedPassword, true));

        return new AdminUserCreated(person, user, rawPassword);
    }

    private void linkPreapprovalToSubscription(String tenantId, String preapprovalId) {
        Subscription subscription = subscriptionRepository.findActiveByTenantId(tenantId)
                .orElseThrow(() -> new IllegalStateException("No subscription found for newly created tenant: " + tenantId));
        subscription.linkMercadoPago(preapprovalId);
        subscriptionRepository.save(subscription);
    }

    private static DocumentType toDocumentType(String value) {
        if (value == null) {
            return DocumentType.OTRO;
        }
        return switch (value.toUpperCase()) {
            case "DNI" -> DocumentType.DNI;
            case "CE" -> DocumentType.CARNET_DE_EXTRANJERIA;
            case "PAS" -> DocumentType.PASAPORTE;
            default -> DocumentType.OTRO;
        };
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(TEMP_SECRET_ALPHABET.charAt(random.nextInt(TEMP_SECRET_ALPHABET.length())));
        }
        return sb.toString();
    }
}
