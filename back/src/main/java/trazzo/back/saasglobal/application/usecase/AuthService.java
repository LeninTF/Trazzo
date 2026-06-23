package trazzo.back.saasglobal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.saasglobal.application.dto.AuthResponse;
import trazzo.back.saasglobal.application.dto.LoginRequest;
import trazzo.back.saasglobal.domain.model.iam.User;
import trazzo.back.saasglobal.infrastructure.adapters.out.persistence.repository.UserRepository;
import trazzo.back.shared.security.utils.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Lanza BadCredentialsException si las credenciales son incorrectas
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + request.email()));

        Long tenantId = user.getTenant().getId();
        String token = jwtService.generateToken(user, tenantId);

        return new AuthResponse(token, tenantId, user.getName());
    }
}
