package trazzo.back.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Contrato backend ↔ OpenAPI (AUD-16): toda operación de un {@code @RestController} está documentada en alguno
 * de los YAML de la raíz del repositorio, y ninguno documenta operaciones que el backend no tiene.
 * Se compara {@code MÉTODO /ruta} con los parámetros de ruta normalizados ({@code /usuarios/{}}).
 */
class OpenApiContractTest {

    static final List<String> SPECS = List.of("openapi.yaml", "openapi-organization.yaml", "openapi-reports.yaml",
            "openapi-saas.yaml", "apicontractoAudit.yaml");
    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete");

    @Test
    void everyBackendOperationIsDocumentedAndEveryDocumentedOperationExists() throws IOException {
        Set<String> implemented = implementedOperations();
        Set<String> documented = documentedOperations(repositoryRoot());

        Set<String> undocumented = new TreeSet<>(implemented);
        undocumented.removeAll(documented);
        Set<String> phantom = new TreeSet<>(documented);
        phantom.removeAll(implemented);

        assertThat(implemented).as("operaciones del backend encontradas").hasSizeGreaterThan(150);
        assertSoftly(softly -> {
            softly.assertThat(undocumented).as("operaciones del backend sin documentar en OpenAPI").isEmpty();
            softly.assertThat(phantom).as("operaciones documentadas que el backend no tiene").isEmpty();
        });
    }

    static Set<String> implementedOperations() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<String> operations = new TreeSet<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents("trazzo.back")) {
            // Los controladores de apoyo de otros tests también están en el classpath.
            if (candidate instanceof ScannedGenericBeanDefinition scanned
                    && scanned.getResource().getDescription().contains("test-classes")) {
                continue;
            }
            Class<?> controller = ClassUtils.resolveClassName(candidate.getBeanClassName(),
                    OpenApiContractTest.class.getClassLoader());
            RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
            List<String> bases = classMapping == null || classMapping.path().length == 0
                    ? List.of("") : Arrays.asList(classMapping.path());
            for (Method method : controller.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) {
                    continue;
                }
                List<String> paths = mapping.path().length == 0 ? List.of("") : Arrays.asList(mapping.path());
                for (RequestMethod http : mapping.method()) {
                    for (String base : bases) {
                        for (String path : paths) {
                            operations.add(http.name() + " " + normalize(base + path));
                        }
                    }
                }
            }
        }
        return operations;
    }

    @SuppressWarnings("unchecked")
    static Set<String> documentedOperations(Path root) throws IOException {
        Set<String> operations = new TreeSet<>();
        for (String spec : SPECS) {
            Map<String, Object> document;
            try (Reader reader = Files.newBufferedReader(root.resolve(spec))) {
                document = new Yaml().load(reader);
            }
            var paths = (Map<String, Map<String, Object>>) document.getOrDefault("paths", Map.of());
            paths.forEach((path, item) -> item.keySet().stream()
                    .filter(HTTP_METHODS::contains)
                    .forEach(method -> operations.add(method.toUpperCase(Locale.ROOT) + " " + normalize(path))));
        }
        return operations;
    }

    static Path repositoryRoot() {
        // Maven ejecuta los tests desde back/; desde un IDE puede ser la raíz.
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.exists(dir.resolve("openapi.yaml")) && Files.isDirectory(dir.resolve("back"))) {
                return dir;
            }
        }
        throw new IllegalStateException("No se encontró la raíz del repositorio (openapi.yaml junto a back/)");
    }

    private static String normalize(String path) {
        String normalized = path.replaceAll("\\{[^}]*}", "{}");
        return normalized.length() > 1 && normalized.endsWith("/")
                ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
