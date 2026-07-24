# AGENTS.md

Convenciones y comandos para trabajar en este repositorio. El backend es un
monolito modular con enfoque hexagonal (Spring Boot) y el frontend es Angular.

## Estructura

- `back/` — Backend Spring Boot (Java 21, Maven wrapper). Módulo `incidents`
  hexagonal: `domain` / `application` (ports + usecases) / `infrastructure`
  (adapters in web, out persistence/messaging).
- `front/` — Frontend Angular 21 (signals, standalone components, pnpm).
- `openapi.yaml` — Contrato API en la raíz del repo.

## Backend (Spring Boot, Java 21)

Ejecutar desde `back/`:

```bash
./mvnw test                    # tests unitarios + Whitelist slice tests
./mvnw -q -DskipTests compile  # solo compilar (verificación rápida de tipos Java)
./mvnw -q -DskipTests package  # empaquetar
```

Notas:

- Los tests usan JUnit 5 + Mockito. Los slice tests (`@WebMvcTest`) requieren
  `@EnableMethodSecurity` y auth stub en `SecurityContextHolder`.
- Las excepciones de dominio extienden de JDK base
  (`InvalidIncidentStateException extends IllegalStateException`,
  `IncidentValidationException extends IllegalArgumentException`); los tests
  esperan la superclase JDK, mantén esa jerarquía al añadir nuevas.

## Frontend (Angular 21, pnpm)

Ejecutar desde `front/`:

```bash
pnpm install
pnpm test    # Karma + ChromeHeadless (single run)
pnpm build   # typecheck + build de producción (actúa como verificador de tipos)
pnpm start   # ng serve en modo dev (activa mock interceptor en dev mode)
```

Notas:

- El `mock.interceptor.ts` solo intercepta en `isDevMode()`. Cuando se añade
  validación backend, reflejarla en el mock para que los tests del frontend
  detecten el caso (e.g. `motivo_rechazo` requerido al DENEGAR).
- `IncidentProfile` y los demás tipos viven en `src/app/api/types.ts`.
- Los specs del componente `admin-tenant/incidencias` mockean `ApiService` y
  `ToastService`. El flujo de rechazo ahora es: click "Rechazar" → abre form
  inline → escribir motivo → click "Confirmar rechazo".

## Convenciones de código

- Backend: sin comentarios innecesarios. Lombok `@Getter`/`@RequiredArgsConstructor`
  en records y servicios. DTOs web como `record` con `@JsonProperty` en snake_case.
- Frontend: signals (`signal()`/`computed()`) en componentes. Imports sólo los
  necesarios del módulo forms si se usa `[(ngModel)]`; aquí se prefiere signal
  binding `(input)` + `signal.set()` para evitar `FormsModule` cuando es viable.

## Contrato API

Cualquier cambio en DTOs/paths HTTP debe reflejarse en `openapi.yaml` (raíz del
repo) para mantener la consistencia backend ↔ frontend ↔ documentación.
