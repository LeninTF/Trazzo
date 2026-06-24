package trazzo.back.saasglobal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.RoleMaster;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.AuthResponse;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.LoginRequest;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.RegisterRequest;
import trazzo.back.saasglobal.infrastructure.adapters.out.persistence.entity.RefreshTokenEntity;
import trazzo.back.saasglobal.infrastructure.adapters.out.persistence.entity.UserEntity;
import trazzo.back.shared.security.service.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepositoryPort userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        UserEntity user = UserEntity.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(RoleMaster.ROLE_USER)
                .build();
        UserEntity saved = userRepository.save(user);
        return buildAuthResponse(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        // Revoca tokens previos antes de emitir uno nuevo (rotación)
        refreshTokenService.revokeAllByUser(user.getId());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken) {
        RefreshTokenEntity refreshToken = refreshTokenService.validate(rawRefreshToken);
        UserEntity user = refreshToken.getUser();
        // Rotación: revoca el token usado y emite uno nuevo
        refreshTokenService.revokeByToken(rawRefreshToken);
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeByToken(rawRefreshToken);
    }

    private AuthResponse buildAuthResponse(UserEntity user) {
        String accessToken = jwtService.generateToken(user);
        RefreshTokenEntity refreshToken = refreshTokenService.create(user);
        return new AuthResponse(accessToken, refreshToken.getToken());
    }
}
