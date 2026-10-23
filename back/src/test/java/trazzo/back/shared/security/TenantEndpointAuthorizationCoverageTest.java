package trazzo.back.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Todo handler de tenant declara su regla de acceso: sin {@code @PreAuthorize} lo alcanza cualquier JWT del tenant
 * (AUD-05). Quedan fuera las rutas públicas y las de la consola SaaS, que SecurityConfig limita a SAAS_ADMIN.
 */
class TenantEndpointAuthorizationCoverageTest {

    private static final List<String> SAAS_PREFIXES = List.of("/saas", "/tenants", "/audit");
    private static final Set<String> PUBLIC_PATHS = Set.of("/auth/login", "/security/public-key", "/error", "/requests",
            "/public/plans", "/shop/checkout", "/webhooks/mercadopago");

    @Test
    void everyTenantHandlerDeclaresItsAccessRule() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<String> unguarded = new ArrayList<>();
        int checked = 0;

        for (BeanDefinition candidate : scanner.findCandidateComponents("trazzo.back")) {
            if (fromTestSources(candidate)) {
                continue;
            }
            Class<?> controller = ClassUtils.resolveClassName(candidate.getBeanClassName(), getClass().getClassLoader());
            boolean guardedClass = AnnotatedElementUtils.hasAnnotation(controller, PreAuthorize.class);
            RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
            List<String> bases = classMapping == null || classMapping.path().length == 0
                    ? List.of("") : Arrays.asList(classMapping.path());

            for (Method method : controller.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) {
                    continue;
                }
                List<String> methodPaths = mapping.path().length == 0 ? List.of("") : Arrays.asList(mapping.path());
                List<String> paths = bases.stream().flatMap(base -> methodPaths.stream().map(p -> base + p)).toList();
                if (paths.stream().allMatch(TenantEndpointAuthorizationCoverageTest::outOfScope)) {
                    continue;
                }
                checked++;
                if (!guardedClass && !AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class)) {
                    unguarded.add(Arrays.toString(mapping.method()) + " " + paths + " -> "
                            + controller.getSimpleName() + "." + method.getName());
                }
            }
        }

        assertThat(checked).as("handlers de tenant revisados").isGreaterThan(100);
        assertThat(unguarded).as("handlers de tenant sin @PreAuthorize").isEmpty();
    }

    // Los controladores de apoyo de otros tests (StubController…) también están en el classpath.
    private static boolean fromTestSources(BeanDefinition candidate) {
        return candidate instanceof ScannedGenericBeanDefinition scanned
                && scanned.getResource().getDescription().contains("test-classes");
    }

    private static boolean outOfScope(String path) {
        return PUBLIC_PATHS.contains(path)
                || SAAS_PREFIXES.stream().anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }
}
