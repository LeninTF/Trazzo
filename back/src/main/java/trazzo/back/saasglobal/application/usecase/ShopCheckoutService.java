package trazzo.back.saasglobal.application.usecase;

import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import trazzo.back.saasglobal.application.dto.command.CreateTrialTenantCommand;
import trazzo.back.saasglobal.application.dto.command.ShopCheckoutCommand;
import trazzo.back.saasglobal.application.dto.result.ShopCheckoutResult;
import trazzo.back.saasglobal.application.port.in.CreateTrialTenantUseCase;
import trazzo.back.saasglobal.application.port.in.ShopCheckoutUseCase;
import trazzo.back.saasglobal.application.port.out.EmailService;
import trazzo.back.saasglobal.application.port.out.HoldingRepositoryPort;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort.PreapprovalCreated;
import trazzo.back.saasglobal.application.port.out.MercadoPagoSubscriptionPort.PreapprovalRequest;
import trazzo.back.saasglobal.application.port.out.PersonRepositoryPort;
import trazzo.back.saasglobal.application.port.out.PlanRepositoryPort;
import trazzo.back.saasglobal.application.port.out.SubscriptionRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.exception.TenantValidationException;
import trazzo.back.saasglobal.domain.model.iam.DocumentType;
import trazzo.back.saasglobal.domain.model.iam.Person;
import trazzo.back.saasglobal.domain.model.iam.User;
import trazzo.back.saasglobal.domain.model.multitenancy.Holding;
import trazzo.back.saasglobal.domain.model.multitenancy.HoldingType;
import trazzo.back.saasglobal.domain.model.multitenancy.Plan;
import trazzo.back.saasglobal.domain.model.multitenancy.Subscription;

/**
 * Orchestrates the /shop self-signup flow: a new customer picks a plan, fills their contact and
 * billing details, and this service provisions a TRIAL tenant immediately (activated later by
 * the Mercado Pago webhook once the payer authorizes the recurring charge), creates the admin
 * login, and starts the Mercado Pago preapproval that the frontend redirects the payer to.
 */
@Service
@RequiredArgsConstructor
public class ShopCheckoutService implements ShopCheckoutUseCase {

    private static final int TEMP_PASSWORD_LENGTH = 16;
    private static final String TEMP_SECRET_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
    private static final int MAX_SUBDOMAIN_BASE_LENGTH = 40;

    private final PlanRepositoryPort planRepository;
    private final HoldingRepositoryPort holdingRepository;
    private final TenantRepositoryPort tenantRepository;
    private final CreateTrialTenantUseCase createTrialTenantUseCase;
    private final SubscriptionRepositoryPort subscriptionRepository;
    private final PersonRepositoryPort personRepository;
    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final MercadoPagoSubscriptionPort mercadoPagoSubscriptionPort;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * Deliberately NOT @Transactional: createTrialTenantUseCase.createTrial() provisions the
     * tenant's Postgres schema over a raw, non-Spring-managed connection (see
     * TenantSchemaProvisioningAdapter), so wrapping this method in a transaction cannot roll
     * that back — a later failure (admin user creation, the Mercado Pago call) would instead
     * roll back only the master-DB tenant row while the schema survives, orphaning it and
     * blocking any retry with the same subDomain. createTrial() already manages its own
     * consistency (schema + master row, deprovisioning on internal failure); once it returns
     * successfully the tenant is real and must stay, matching the product decision to create
     * the tenant immediately and activate it later when payment confirms.
     */
    @Override
    public ShopCheckoutResult checkout(ShopCheckoutCommand cmd) {
        Plan plan = planRepository.findById(requirePlanId(cmd.planId()))
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + cmd.planId()));

        Holding holding = findOrCreateHolding(cmd.ruc(), cmd.businessName());
        String subDomain = deriveUniqueSubDomain(cmd.companyName());

        var tenantResult = createTrialTenantUseCase.createTrial(
                new CreateTrialTenantCommand(subDomain, plan.getId(), holding.getId(), null, null, null, null));

        createAdminUser(tenantResult.id(), cmd);

        PreapprovalCreated preapproval = mercadoPagoSubscriptionPort.createPreapproval(new PreapprovalRequest(
                plan.getPrice(),
                plan.getCurrency(),
                plan.getBillingPeriod(),
                cmd.email(),
                tenantResult.id(),
                frontendUrl + "/shop/gracias",
                "Suscripción Trazzo - " + plan.getName()));

        linkPreapprovalToSubscription(tenantResult.id(), preapproval.id());

        String redirectUrl = preapproval.sandboxInitPoint() != null
                ? preapproval.sandboxInitPoint() : preapproval.initPoint();
        return new ShopCheckoutResult(tenantResult.id(), subDomain, redirectUrl);
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
            candidate = base + "-" + suffix;
        }
        return candidate;
    }

    private void createAdminUser(String tenantId, ShopCheckoutCommand cmd) {
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
        String encodedPassword = passwordEncoder.encode(rawPassword);
        User user = userRepository.save(User.create(person.getId(), tenantId, email, phone, encodedPassword, true));

        emailService.send(user.getEmail(), "Bienvenido a Trazzo",
                "Tu cuenta de administrador fue creada. Contraseña temporal: " + rawPassword
                        + "<br>Deberás cambiarla al iniciar sesión.");
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
