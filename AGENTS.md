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

## Storage de evidencias (Cloudflare R2)

Flujo de subida y descarga de archivos de evidencia en buckets privados de R2:

- **Subida (presigned PUT)**: el frontend pide una URL presigned al backend
  (`GET /storage/presigned-url?fileName=&contentType=&incident_id=`), luego
  hace `PUT` directo a R2 con el `File`, y finalmente crea la evidencia con
  `POST /incidentes/{id}/evidencias` enviando `file_key` (objectKey devuelto
  por el endpoint presigned), `file_name`, `mime_type` y `file_size`.
- **Descarga (proxy backend)**: el backend nunca expone el objectKey ni la URL
  pública de R2. El `IncidentEvidenceResult.downloadUrl` contiene una ruta
  relativa `GET /incidentes/{incId}/evidencias/{evId}/descarga` que valida
  permisos sobre el incidente + evidence_id + tenant y streamea el archivo
  desde R2 vía `FileStoragePort.downloadFile(objectKey)`. El frontend usa
  `fetch(downloadUrl, { credentials: 'include' })` y construye un Blob link.
- **Persistencia**: la columna persistida en `incidencia_evidencia` es
  `file_key VARCHAR(500) NOT NULL` (objectKey R2, formato
  `evidences/{tenantId}/{incidentId}/{uuid}/{fileName}`). La columna legacy
  `file_url` fue eliminada en `V1__tenant_db.sql` (no hay rows históricos en
  prod/tenant_demo). `download_url` **no** se persiste: lo fabrica
  `EvidenceService.toResult` a partir de los IDs del incidente y evidence.
- **`FileStoragePort`** (`shared/application/port/out`): `uploadFile`,
  `downloadFile`, `buildPublicUrl`, `presignedPutUrl`. Implementaciones:
  `CloudflareR2StorageAdapter` (producción) y `LocalStorageStub` (fallback
  que lanza `StorageException` si no hay R2 configurado — útil para tests
  de contexto Spring sin contrato S3).
- **Validación MIME**: lista blanca en `IncidentEvidenceSpec.ALLOWED_MIME_TYPES`
  (PDF, PNG, JPEG, DOC, DOCX, MP4, QuickTime). El DTO `CreateEvidenceRequest`
  usa `@Pattern` con la misma regex (validación de input en capa web); el
  dominio no vuelve a validar (para no romper `restore()` con rows históricos).
  El frontend replica la whitelist + detección por magic bytes en
  `src/app/shared/storage/file-validator.ts` (`validateFile`) — el test de
  spoofing depende de la regla MZ (`0x4d 0x5a 0x90 0x00` →
  `application/x-msdownload`) que debe estar en `MAGIC_BYTE_RULES`.
- **Configuración**: `cloudflare.r2.*` en `application-local.properties`
  (env-driven: `${CLOUDFLARE_R2_*}`). En tests se usan valores dummy
  definidos en `src/test/resources/application.properties`.
- **Mock interceptor (frontend)**: en `isDevMode()` intercepta
  `presigned-url` (devuelve `object_key` mockeado con prefijo
  `evidences/42/{incidentId}/uuid/{fileName}`), `POST /incidentes/{id}/evidencias`
  (construye `download_url` relativa) y `GET .../descarga` (devuelve un `Blob`).
