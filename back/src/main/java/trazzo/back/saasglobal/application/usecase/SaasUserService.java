package trazzo.back.saasglobal.application.usecase;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import trazzo.back.saasglobal.application.dto.command.AssignSaasUserRolesCommand;
import trazzo.back.saasglobal.application.dto.command.CreateSaasUserCommand;
import trazzo.back.saasglobal.application.dto.command.UpdateSaasUserCommand;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.SaasUserResult;
import trazzo.back.saasglobal.application.port.in.SaasUserUseCase;
import trazzo.back.saasglobal.application.port.out.EmailService;
import trazzo.back.saasglobal.application.port.out.PersonRepositoryPort;
import trazzo.back.saasglobal.application.port.out.RoleMasterRepositoryPort;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.application.port.out.UserRolesMasterRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.DocumentType;
import trazzo.back.saasglobal.domain.model.iam.Person;
import trazzo.back.saasglobal.domain.model.iam.RoleMaster;
import trazzo.back.saasglobal.domain.model.iam.User;

@Service
@RequiredArgsConstructor
public class SaasUserService implements SaasUserUseCase {

    private static final String NOT_FOUND_MSG = "User not found: ";
    private static final int TEMP_PASSWORD_LENGTH = 16;
    private static final String TEMP_SECRET_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";

    private final UserRepositoryPort userRepository;
    private final PersonRepositoryPort personRepository;
    private final UserRolesMasterRepositoryPort userRolesRepository;
    private final RoleMasterRepositoryPort roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    @Override
    public PaginatedResult<SaasUserResult> listAll(String search, int page, int size) {
        List<User> users = userRepository.findAll(search, page, size);
        long total = userRepository.countAll(search);
        List<SaasUserResult> results = users.stream().map(this::toResult).toList();
        return PaginatedResult.of(results, page, size, total);
    }

    @Override
    public SaasUserResult getById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_MSG + id));
        return toResult(user);
    }

    @Override
    public SaasUserResult create(CreateSaasUserCommand command) {
        Person person = personRepository.save(Person.create(
                null, DocumentType.valueOf(command.documentType()), command.documentValue(),
                command.name(), command.fatherSurname(), command.motherSurname(), null));

        boolean generated = command.password() == null || command.password().isBlank();
        String rawPassword = generated ? generateTempPassword() : command.password();
        String encodedPassword = passwordEncoder.encode(rawPassword);

        User user = userRepository.save(User.create(
                person.getId(), null, command.email(), command.phone(), encodedPassword, generated));

        List<Integer> roleIds = command.roleIds() != null ? command.roleIds() : List.of();
        userRolesRepository.replaceForUser(user.getId(), roleIds);

        if (generated) {
            emailService.send(user.getEmail(), "Bienvenido a Trazzo",
                    "Se creó tu cuenta de administrador. Contraseña temporal: " + rawPassword
                            + "<br>Deberás cambiarla al iniciar sesión.");
        }

        return getById(user.getId());
    }

    @Override
    public SaasUserResult update(UpdateSaasUserCommand command) {
        User user = userRepository.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_MSG + command.id()));
        user.updateContact(command.email(), command.phone());
        userRepository.update(user);
        return getById(user.getId());
    }

    @Override
    public void deleteById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_MSG + id));
        user.delete();
        userRepository.update(user);
    }

    @Override
    public SaasUserResult assignRoles(AssignSaasUserRolesCommand command) {
        userRepository.findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_MSG + command.userId()));
        userRolesRepository.replaceForUser(command.userId(), command.roleIds());
        return getById(command.userId());
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(TEMP_SECRET_ALPHABET.charAt(random.nextInt(TEMP_SECRET_ALPHABET.length())));
        }
        return sb.toString();
    }

    private SaasUserResult toResult(User user) {
        SaasUserResult.PersonSummary personSummary = personRepository.findById(user.getPersonId())
                .map(p -> new SaasUserResult.PersonSummary(p.getId(), p.getImgUrl(),
                        p.getDocumentType().name(), p.getDocumentValue(), p.getName(),
                        p.getFatherSurname(), p.getMotherSurname()))
                .orElse(null);

        Map<String, RoleMaster> rolesByName = roleRepository.findAll().stream()
                .collect(Collectors.toMap(RoleMaster::getName, r -> r));
        List<SaasUserResult.RoleTag> roleTags = user.getRoles().stream()
                .map(rolesByName::get)
                .filter(java.util.Objects::nonNull)
                .map(r -> new SaasUserResult.RoleTag(r.getId(), r.getName(), r.getDescription()))
                .toList();

        return new SaasUserResult(user.getId(), user.getEmail(), user.getPhone(),
                user.isMustChangePassword(), user.getCreatedAt(), personSummary, roleTags);
    }
}
