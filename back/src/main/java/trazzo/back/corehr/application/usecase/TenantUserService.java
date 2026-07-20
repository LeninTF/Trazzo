package trazzo.back.corehr.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import trazzo.back.corehr.application.dto.command.CreateTenantUserCommand;
import trazzo.back.corehr.application.dto.command.PatchTenantUserCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.SoftDeleteResult;
import trazzo.back.corehr.application.dto.result.TenantUserProfileResult;
import trazzo.back.corehr.application.port.in.TenantUserUseCase;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.corehr.domain.model.TenantUserState;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class TenantUserService implements TenantUserUseCase {

    private final TenantUserPort tenantUserPort;
    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PaginatedResult<TenantUserProfileResult> findAll(String search, String status, int page, int size, String sort) {
        var projections = tenantUserPort.findAllProfiles(search, status, page, size, sort);
        var total = tenantUserPort.countAllProfiles(search, status);
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        var content = projections.stream().map(this::toResult).toList();
        return new PaginatedResult<>(content, page, size, total, totalPages);
    }

    @Override
    public Optional<TenantUserProfileResult> findById(Long id) {
        return tenantUserPort.findProfileById(id).map(this::toResult);
    }

    @Override
    public TenantUserProfileResult create(CreateTenantUserCommand command) {
        var personId = tenantUserPort.findPersonIdByDocument(command.documentType(), command.documentValue())
                .orElseGet(() -> tenantUserPort.savePerson(
                        command.documentType(), command.documentValue(),
                        command.name(), command.fatherSurname(), command.motherSurname()));

        var tenantUser = User.create(personId, null, command.email(), command.phone(),
                passwordEncoder.encode(generateTemporaryPassword()));
        userRepository.save(tenantUser);

        var tenantUserId = tenantUserPort.saveTenantUser(UUID.fromString(tenantUser.getId()));
        if (command.roleId() != null) {
            tenantUserPort.assignRole(tenantUserId, command.roleId());
        }

        return findById(tenantUserId)
                .orElseThrow(() -> new IllegalStateException("Failed to retrieve created tenant user"));
    }

    @Override
    public TenantUserProfileResult update(Long id, CreateTenantUserCommand command) {
        var projection = tenantUserPort.findProfileById(id)
                .orElseThrow(() -> new IllegalArgumentException("TenantUser not found: " + id));

        if (projection.roleId() != null) {
            tenantUserPort.removeRole(id);
        }
        if (command.roleId() != null) {
            tenantUserPort.assignRole(id, command.roleId());
        }

        return findById(id)
                .orElseThrow(() -> new IllegalStateException("Failed to retrieve updated tenant user"));
    }

    @Override
    public TenantUserProfileResult patch(Long id, PatchTenantUserCommand command) {
        var projection = tenantUserPort.findProfileById(id)
                .orElseThrow(() -> new IllegalArgumentException("TenantUser not found: " + id));

        if (command.name() != null || command.fatherSurname() != null || command.motherSurname() != null) {
            tenantUserPort.updatePerson(
                    projection.personId(),
                    command.name() != null ? command.name() : projection.name(),
                    command.fatherSurname() != null ? command.fatherSurname() : projection.fatherSurname(),
                    command.motherSurname() != null ? command.motherSurname() : projection.motherSurname()
            );
        }

        if (command.estado() != null) {
            tenantUserPort.updateState(id, TenantUserState.valueOf(command.estado()));
        }

        if (command.roleId() != null) {
            tenantUserPort.assignRole(id, command.roleId());
        }

        return findById(id)
                .orElseThrow(() -> new IllegalStateException("Failed to retrieve patched tenant user"));
    }

    @Override
    public SoftDeleteResult delete(Long id) {
        var projection = tenantUserPort.findProfileById(id)
                .orElseThrow(() -> new IllegalArgumentException("TenantUser not found: " + id));
        tenantUserPort.softDelete(id);
        return new SoftDeleteResult(id, "INACTIVO", LocalDateTime.now());
    }

    @Override
    public TenantUserProfileResult assignRole(Long id, String roleId) {
        tenantUserPort.findProfileById(id)
                .orElseThrow(() -> new IllegalArgumentException("TenantUser not found: " + id));
        tenantUserPort.assignRole(id, roleId);
        return findById(id)
                .orElseThrow(() -> new IllegalStateException("Failed to retrieve tenant user after role assignment"));
    }

    @Override
    public void changePassword(Long id, String currentPassword, String newPassword) {
        var projection = tenantUserPort.findProfileById(id)
                .orElseThrow(() -> new IllegalArgumentException("TenantUser not found: " + id));
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password is required");
        }
        var user = userRepository.findByEmail(projection.email())
                .orElseThrow(() -> new IllegalStateException("Master user not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        userRepository.save(User.restore(
                user.getId(), user.getPersonId(), user.getTenantId(), user.getEmail(),
                user.getPhone(), passwordEncoder.encode(newPassword),
                user.getRoles(), user.getPermissionCodes(), false,
                user.getCreatedAt(), LocalDateTime.now(), null
        ));
    }

    @Override
    public Optional<TenantUserProfileResult> findMe(String masterUserId) {
        var all = tenantUserPort.findAllProfiles(null, null, 0, Integer.MAX_VALUE, null);
        return all.stream()
                .filter(p -> p.email() != null)
                .filter(p -> {
                    var userOpt = userRepository.findByEmail(p.email());
                    return userOpt.isPresent() && userOpt.get().getId().equals(masterUserId);
                })
                .findFirst()
                .map(this::toResult);
    }

    @Override
    public TenantUserProfileResult patchMe(String masterUserId, String phone, String imgUrl) {
        var projection = findMe(masterUserId)
                .orElseThrow(() -> new IllegalArgumentException("TenantUser not found for user: " + masterUserId));
        return projection;
    }

    private String generateTemporaryPassword() {
        return UUID.randomUUID().toString().substring(0, 12);
    }

    private TenantUserProfileResult toResult(TenantUserPort.TenantUserProfileProjection p) {
        return new TenantUserProfileResult(
                p.id(),
                p.email(),
                p.phone(),
                p.estado(),
                p.mustChangePassword(),
                p.createdAt(),
                p.updatedAt(),
                new TenantUserProfileResult.PersonaResult(
                        p.personId(),
                        p.documentType(),
                        p.documentValue(),
                        p.name(),
                        p.fatherSurname(),
                        p.motherSurname(),
                        null,
                        null
                ),
                p.roleId() != null
                        ? new TenantUserProfileResult.RoleInfoResult(p.roleId(), p.roleName())
                        : null,
                List.of(),
                List.of(),
                List.of()
        );
    }
}
