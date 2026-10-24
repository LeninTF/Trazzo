# AGENTS.md

Convenciones y comandos para trabajar en este repositorio. El backend es un
monolito modular con enfoque hexagonal (Spring Boot), el frontend es Angular y
el agente de los lectores biométricos es .NET.

## Estructura

- `back/` — Backend Spring Boot (Java 21, Maven wrapper). Módulos `audit`,
  `corehr`, `incidents`, `organization`, `reports`, `saasglobal` y `shared`,
  hexagonales: `domain` / `application` (ports + usecases) / `infrastructure`
  (adapters in web, out persistence/messaging). PostgreSQL con un schema por
  tenant: las migraciones del schema maestro viven en `db/master` y las de cada
  tenant en `db/tenant/migration` (el baseline es `db/tenant/V1__tenant_db.sql`).
- `front/` — Frontend Angular 21 (signals, standalone components, pnpm).
- `middleware/` — Agente biométrico .NET 10 para Windows (lector ZK9500) y su
  instalador WiX. Ver `middleware/README.md`.
- Contrato API en la raíz del repo: `openapi.yaml`, `openapi-organization.yaml`,
  `openapi-reports.yaml`, `openapi-saas.yaml` y `apicontractoAudit.yaml`.
- `DEPLOY.md` — Runbook de despliegue y operación (compose, secretos, claves,
  lectores, respaldo).

## Backend (Spring Boot, Java 21)

Ejecutar desde `back/`:

```bash
./mvnw test                    # unitarios, slice tests e integración (Testcontainers: requiere Docker)
./mvnw -q -DskipTests compile  # solo compilar (verificación rápida de tipos Java)
./mvnw -q -DskipTests package  # empaquetar
```

Notas:

- Los tests usan JUnit 5 + Mockito. Los slice tests (`@WebMvcTest`) de
  controladores del tenant se anotan con `@TenantSecuritySlice`: activa
  `@EnableMethodSecurity`, registra el bean `authz`, simula
  `TenantUserIdResolver` y deja como usuario a un administrador del tenant.
  Para probar otro usuario: `TenantSecurityContexts.actingAsTenantUserWith(...)`
  o `actingAsPlainUser()`. `@EnableMethodSecurity` puesta en una
  meta-anotación no se aplica; por eso `TenantSecuritySlice` importa una
  `@TestConfiguration` anidada.
- `TenantEndpointAuthorizationCoverageTest` falla si un handler de un
  controlador del tenant no tiene `@PreAuthorize` (en el método o en la
  clase). Las rutas públicas están en su `PUBLIC_PATHS`.
- `OpenApiContractTest` compara método + ruta de cada handler con las
  operaciones de los 5 YAML: falla si un endpoint no está documentado o si un
  YAML documenta uno que no existe.
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
pnpm start   # ng serve; /api va al backend en localhost:8080 (proxy.conf.json)
```

Notas:

- `mock.interceptor.ts` no está registrado en la app (`main.ts` solo usa
  `authInterceptor`): `pnpm start` necesita el backend. El mock se ejercita
  desde `mock.interceptor.spec.ts` y solo responde en `isDevMode()`. Cuando se
  añade validación backend que el mock simula, reflejarla también ahí (e.g.
  `motivo_rechazo` requerido al DENEGAR, las reglas del presign).
- `IncidentProfile` y los demás tipos viven en `src/app/api/types.ts`.
- Los specs del componente `admin-tenant/incidencias` mockean `ApiService` y
  `ToastService`. El flujo de rechazo ahora es: click "Rechazar" → abre form
  inline → escribir motivo → click "Confirmar rechazo".
- En producción nginx (`front/nginx.conf`) envía una CSP con
  `script-src 'self'`: nada de `<script>` inline ni atributos `on*=`; por eso
  `inlineCritical` está desactivado en `angular.json`. Un `add_header` dentro
  de un `location` anula todos los del `server`: las cabeceras se definen solo
  en el `server` (el `Cache-Control` por ruta sale del `map`).

## Convenciones de código

- Backend: sin comentarios innecesarios. Lombok `@Getter`/`@RequiredArgsConstructor`
  en records y servicios. DTOs web como `record` con `@JsonProperty` en snake_case.
  Excepción heredada: casi todos los DTO de `saasglobal` (incluido el login),
  `organization` y `reports`, y el `publicKey` de `/security/public-key` que
  lee el agente, van en camelCase. El front y el agente dependen de esos
  nombres: no se renombran sin migrarlos a la vez. Los YAML describen los
  nombres reales.
- Frontend: signals (`signal()`/`computed()`) en componentes. Imports sólo los
  necesarios del módulo forms si se usa `[(ngModel)]`; aquí se prefiere signal
  binding `(input)` + `signal.set()` para evitar `FormsModule` cuando es viable.

## Autorización en el tenant

- Cada handler de un controlador del tenant lleva `@PreAuthorize` con el bean
  `authz` (`TenantAuthorization`): `@authz.any('modulo.permiso', 'modulo.*')`,
  `@authz.admin()`, los grupos `canViewStaff()`, `canViewStructure()`,
  `canViewSchedules()`, `canViewAllAttendance()` y `approver()`, e
  `@authz.isSelf(#id)` para lo que solo puede tocar su dueño.
  `administrador_tenant` pasa siempre.
- Las autoridades del JWT son `ROLE_<rol>` más los códigos de permiso del
  usuario. En Java, `TenantAuthorization.currentTenantUserId()` devuelve el
  `tenant_user` de quien llama (`Optional` vacío si no tiene).
- Asistencia e incidencias muestran solo lo propio (`scope=SELF`) a quien no
  tiene permiso para ver lo de todos; eso lo decide el controlador, no basta
  con `@PreAuthorize`.
- `TenantContextGuard` (después de `UsernamePasswordAuthenticationFilter`, solo
  con PostgreSQL) responde 403 a quien está autenticado sin schema de tenant
  (p. ej. un admin SaaS) en una ruta del tenant; sin autenticación sigue y la
  cadena responde 401.
- Al cambiar un `@PreAuthorize`, actualizar la nota "**Permisos requeridos:**"
  de esa operación en su YAML: es la única parte normativa de las
  descripciones.

## Agente biométrico (token de dispositivo)

- El agente se autentica con `Authorization: Bearer trzd_…`, un token por
  lector emitido desde el panel (el backend guarda solo su SHA-256).
  `DeviceTokenAuthFilter` (antes de `JwtAuthFilter`) lo acepta únicamente en
  `POST /asistencia/marcar`, `POST /asistencia/sync`,
  `GET /corehr/biometria/enroll/pendiente`,
  `POST /corehr/biometria/enroll/completar` y `GET /security/public-key`, y
  crea un `DeviceAgentPrincipal` con `ROLE_AGENT`.
- Esos handlers usan `DeviceAgentPrincipal.AGENT_OR_ATTENDANCE_OPERATOR` en
  `@PreAuthorize`: el agente o un JWT con
  `control-asistencia.operador-asistencia`.
- El cifrado del payload biométrico está en la sección "Criptografia
  biometrica (normativa)" de `openapi.yaml`.

## Contrato API

Cualquier cambio en DTOs/paths HTTP debe reflejarse en el YAML que corresponda
(raíz del repo) para mantener la consistencia backend ↔ frontend ↔
documentación. `OpenApiContractTest` detecta los paths que faltan o sobran;
los campos de los DTO no se comprueban solos. `.42c/conf.yaml` registra los 5
YAML para la extensión 42Crunch (sus informes, en `.42c/scan/`, no se
versionan).

## Storage de evidencias (Cloudflare R2)

Flujo de subida y descarga de archivos de evidencia en buckets privados de R2:

- **Subida (presigned PUT)**: el frontend pide una URL presigned al backend
  (`GET /storage/presigned-url?fileName=&contentType=&incident_id=`), luego
  hace `PUT` directo a R2 con el `File`, y finalmente crea la evidencia con
  `POST /incidentes/{id}/evidencias` enviando `file_key` (objectKey devuelto
  por el endpoint presigned), `file_name`, `mime_type` y `file_size`.
- **Reglas del presign**: `incident_id` es obligatorio; del `fileName` queda
  solo el nombre (vacío, `.` o `..` → 400); el `contentType` debe estar en la
  lista blanca (400); quien llama necesita un `tenant_user` (403), la
  incidencia debe existir (404) y ser suya, salvo que sea aprobador (403).
- **Descarga (proxy backend)**: el backend nunca expone el objectKey ni la URL
  pública de R2. El `IncidentEvidenceResult.downloadUrl` contiene una ruta
  relativa `GET /incidentes/{incId}/evidencias/{evId}/descarga` que valida
  permisos sobre el incidente + evidence_id + tenant y streamea el archivo
  desde R2 vía `FileStoragePort.downloadFile(objectKey)`. El frontend usa
  `downloadWithAuth(downloadUrl)` (`src/app/shared/storage/download.ts`), que
  hace `fetch` con `Authorization: Bearer <token>` y construye un Blob link.
- **Persistencia**: la columna persistida en `incidencia_evidencia` es
  `file_key VARCHAR(500) NOT NULL` (objectKey R2, formato
  `evidences/{tenantUserId}/{incidentId}/{uuid}/{fileName}`, donde
  `tenantUserId` es el `tenant_user` que sube). La columna legacy
  `file_url` fue eliminada en `V1__tenant_db.sql` (no hay rows históricos en
  prod/tenant_demo). `download_url` **no** se persiste: lo fabrica
  `EvidenceService.toResult` a partir de los IDs del incidente y evidence.
- **`FileStoragePort`** (`shared/application/port/out`): `uploadFile`,
  `downloadFile`, `buildPublicUrl`, `generatePresignedPutUrl`.
  Implementaciones: `CloudflareR2StorageAdapter` (producción) y
  `LocalStorageStub` (fallback que lanza `StorageException` si no hay R2
  configurado — útil para tests de contexto Spring sin contrato S3).
- **Validación MIME**: lista blanca en `IncidentEvidenceSpec.ALLOWED_MIME_TYPES`
  (PDF, PNG, JPEG, DOC, DOCX, MP4, QuickTime). El DTO `CreateEvidenceRequest`
  usa `@Pattern` con la misma regex (validación de input en capa web); el
  dominio no vuelve a validar (para no romper `restore()` con rows históricos).
  El frontend replica la whitelist + detección por magic bytes en
  `src/app/shared/storage/file-validator.ts` (`validateFile`) — el test de
  spoofing depende de la regla MZ (`0x4d 0x5a 0x90 0x00` →
  `application/x-msdownload`) que debe estar en `MAGIC_BYTE_RULES`.
- **Configuración**: `cloudflare.r2.*` en `application.properties` y
  `application-local.properties` (env-driven: `${CLOUDFLARE_R2_*}`). En tests
  se usan valores dummy definidos en `src/test/resources/application.properties`.
- **Mock interceptor (frontend)**: en `isDevMode()` intercepta
  `presigned-url` (aplica las reglas de `incident_id`, MIME y nombre, y
  devuelve un `object_key` con prefijo `evidences/42/{incidentId}/{uuid}/{fileName}`),
  `POST /incidentes/{id}/evidencias` (construye `download_url` relativa) y
  `GET .../descarga` (devuelve un `Blob`).

## Despliegue

`DEPLOY.md` es el runbook. En los `docker-compose*.yml` el backend recibe una
lista explícita de variables, no el `.env` entero. Una clave sin valor se toma
del `--env-file` si está definida; si no, el contenedor no la recibe y rige el
default de `application.properties`. Por eso las opcionales van comentadas en
`.env.example`: una línea `KEY=` vacía pasaría una cadena vacía.
