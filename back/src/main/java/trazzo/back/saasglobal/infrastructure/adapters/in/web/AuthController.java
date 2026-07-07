package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.saasglobal.application.port.out.PersonRepositoryPort;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.Person;
import trazzo.back.saasglobal.domain.model.iam.User;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.LoginRequest;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.LoginResponse;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.RoleProfileResponse;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.UsuarioResponse;
import trazzo.back.shared.security.AuthenticatedUser;
import trazzo.back.shared.security.JwtService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepositoryPort userRepository;
    private final PersonRepositoryPort personRepository;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        AuthenticatedUser principal = (AuthenticatedUser) auth.getPrincipal();
        String token = jwtService.generateToken(principal);

        User user = userRepository.findByEmail(principal.email())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + principal.email()));

        Person person = personRepository.findById(user.getPersonId())
                .orElseThrow(() -> new IllegalStateException("Person not found for user: " + user.getId()));

        var roles = user.getRoles().stream()
                .map(RoleProfileResponse::fromRoleName)
                .toList();

        var usuario = new UsuarioResponse(
                person.getId(),
                person.getName(),
                person.getFatherSurname(),
                person.getMotherSurname(),
                user.getEmail(),
                user.isActive() ? "ACTIVO" : "INACTIVO",
                null,
                roles
        );

        return ResponseEntity.ok(new LoginResponse(token, "Bearer", usuario));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "email", authentication.getName(),
                "roles", authentication.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .toList()
        ));
    }
}
