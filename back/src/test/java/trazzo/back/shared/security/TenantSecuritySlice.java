package trazzo.back.shared.security;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Slice web con la autorización real de los endpoints de tenant: {@code @PreAuthorize}, el bean {@code authz} y el
 * resolver de tenant_user simulado. Cada test empieza como administrador del tenant; para otro rol se usa
 * {@link TenantSecurityContexts}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({TenantAuthorization.class, TenantSecuritySlice.MethodSecurity.class})
@MockitoBean(types = TenantUserIdResolver.class)
@ExtendWith(TenantSecurityContexts.TenantAdminByDefault.class)
public @interface TenantSecuritySlice {

    /** Importada aparte: el contexto de test no aplica @EnableMethodSecurity puesta en una meta-anotación. */
    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    class MethodSecurity {
    }
}
