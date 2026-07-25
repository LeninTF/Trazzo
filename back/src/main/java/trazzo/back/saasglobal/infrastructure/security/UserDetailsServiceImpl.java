package trazzo.back.saasglobal.infrastructure.security;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.User;
import trazzo.back.shared.security.AuthenticatedUser;
import trazzo.back.shared.security.TenantPermissionPort;
import trazzo.back.shared.tenancy.TenantContext;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepositoryPort userRepository;
    private final TenantPermissionPort tenantPermissionPort;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("user not found: " + normalizedEmail));

        List<GrantedAuthority> authorities = new ArrayList<>(user.getRoles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList());

        if (user.getRoles().contains("admin_trazzo")) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SAAS_ADMIN"));
        }

        user.getPermissionCodes().forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));

        String tenantSchema = TenantContext.get();
        if (tenantSchema != null && !"public".equals(tenantSchema)) {
            tenantPermissionPort.findPermissionCodesByMasterUserId(UUID.fromString(user.getId()))
                    .forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));
        }

        return new AuthenticatedUser(
                UUID.fromString(user.getId()),
                user.getEmail(),
                user.getPassword(),
                authorities,
                user.isActive());
    }
}
