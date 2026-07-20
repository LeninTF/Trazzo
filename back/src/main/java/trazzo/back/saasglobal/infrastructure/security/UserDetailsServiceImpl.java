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

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepositoryPort userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("user not found: " + normalizedEmail));

        List<GrantedAuthority> authorities = new ArrayList<>(user.getRoles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList());

        // Blanket marker granted to any SaaS-role-bearing user, regardless of which specific
        // role: this is the outer path-level gate in SecurityConfig ("/saas/**" requires
        // hasRole("SAAS_ADMIN")). It guarantees a tenant user (zero roles_master roles) can
        // never reach /saas/** even if a specific endpoint's @PreAuthorize was forgotten.
        if (!user.getRoles().isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SAAS_ADMIN"));
        }

        // Unprefixed authority per granted permission code, for hasAuthority(...) checks.
        user.getPermissionCodes().forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));

        return new AuthenticatedUser(
                UUID.fromString(user.getId()),
                user.getEmail(),
                user.getPassword(),
                authorities,
                user.isActive());
    }
}
