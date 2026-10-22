# Remediación de la auditoría general — plan de implementación

> **Para agentes:** SUB-SKILL REQUERIDA: usar superpowers:subagent-driven-development (recomendado) o superpowers:executing-plans para implementar este plan tarea a tarea. Los pasos usan casillas (`- [ ]`) para el seguimiento.

**Objetivo:** dejar funcionando de punta a punta la biometría, el control de acceso, la infraestructura local/prod y el contrato API descritos en la auditoría, con todo lo configurable expuesto como variables de entorno.

**Arquitectura:** seis planes independientes y apilados (A→F), cada uno en su rama y verificable por sí solo. A integra el trabajo D02 ya escrito; B arregla la cadena biométrica (back + middleware + front); C cierra la autorización; D endurece Docker/CI; E alinea OpenAPI y documentación; F limpia deuda menor.

**Stack:** Spring Boot 3.5.14 (Java 21, JUnit 5, Mockito, Testcontainers), Angular 21 (signals, Karma), .NET 10 (xUnit), Docker Compose, GitHub Actions.

**Spec:** [`AUDITORIA_GENERAL.md`](../../../AUDITORIA_GENERAL.md) — §12 (hallazgos AUD-01…30) y §13 (plan de acción).

## Restricciones globales

- Hexagonal: dominio sin dependencias de infraestructura; nueva criptografía detrás de un puerto (`application/port/out`), implementación en `infrastructure/adapters/out`.
- Excepciones nuevas extienden `IllegalArgumentException` / `IllegalStateException` (AGENTS.md).
- DTOs web: `record` + `@JsonProperty` en snake_case. Front: `signal()`/`computed()`, sin `FormsModule` si es viable.
- Todo cambio de DTO o ruta se refleja en OpenAPI; el test de contrato del plan E lo exige a partir de ese plan.
- Configuración nueva: `propiedad=${VARIABLE:default}` en `application*.properties` y documentada en `.env.example` / `.env.local.example` con el formato D02-A: `# obligatoria|opcional - consumidor - formato`.
- Contrato criptográfico normativo: RSA-2048 **OAEP SHA-256 + MGF1-SHA-256**; AES-256-GCM, IV 12 B, tag 16 B, AAD `biometric-identify`; `publicKey` en PEM (SPKI); `captured_at_utc` es UTC, con o sin `Z`.
- Zona horaria de negocio por defecto: `America/Lima`.
- Commits: Conventional Commits (`commitlint.config.mjs`), ámbitos `back`, `front`, `middleware`, `infra`, `ci`, `docs`.
- Al cerrar cada plan: `./mvnw -B test`, `pnpm test`, `pnpm build`, `dotnet test middleware/Trazzo.Middleware.slnx` en verde.

## Review Focus

1. **Reinicio del backend tras enrolar:** marcar después de `docker compose restart` debe seguir reconociendo la huella → test en B3/B4: dos instancias del proveedor con el mismo archivo de clave producen el mismo `kid` y abren la misma plantilla guardada.
2. **Token del lector A usado con el `device_code` del lector B:** 403 → test en B7 (`marcar` y `completar`).
3. **Marcación a las 23:30 en Lima (04:30Z del día siguiente):** `attendance_date` debe ser la fecha local → test en B6.
4. **Usuario con solo los permisos del rol `usuario` llamando a endpoints de administración:** 403 → test reflexivo de cobertura + slice tests en C2.
5. **Filas biométricas antiguas** (`key_ref` nulo, cifradas con una clave efímera ya perdida): se omiten con WARN y se cuentan, sin 500 → test en B4.

---

## Plan A — Integración de D02 (rama `fix/plan-a-integracion-d02`)

### Task 1 (A1): integrar D02-A, D02-B y D02-C

**Archivos:** los de los commits `3de58a2`, `0532bc4`, `4fd5a6c` (ya revisados; `git merge-tree` confirma que combinan sin conflictos).

- [ ] `git cherry-pick 3de58a2 0532bc4 4fd5a6c` (conserva el autor original).
- [ ] `./mvnw -B test` → 0 fallos (incluye `TenantFlywayMigratorIntegrationTest` con Testcontainers; requiere Docker).
- [ ] `docker compose --env-file .env.local -f docker-compose.local.yml up -d --build` desde volúmenes vacíos → los 3 servicios `healthy`, ninguna migración de tenant falla.

### Task 2 (A2): el script de init escapa credenciales (AUD-23)

**Archivos:** modificar `docker/postgres/init/01-init-master-db.sh`.

- [ ] Pasar `APP_DB_USER`, `APP_DB_PASSWORD` y `APP_DB_NAME` como `psql -v` y construir las sentencias con `format('%I', …)` / `format('%L', …)` + `\gexec` (fuera de bloques `DO`).
- [ ] Verificar con un contenedor `postgres:15-alpine` y la contraseña `p'a$s"w` → el rol se crea y `psql -U` con esa contraseña conecta; una segunda ejecución es idempotente.
- [ ] Commit `fix(infra): escapa credenciales en el script de init de Postgres`.

---

## Plan B — Biometría de punta a punta (rama `fix/plan-b-biometria`)

### Task 3 (B1): middleware de `feature/web-cam` en `main` (AUD-03, AUD-04, AUD-14)

**Archivos:** `middleware/**` desde `origin/feature/web-cam` (solo ese directorio; `camera/` y el resto de la rama quedan fuera). Modificar `appsettings.Development.json` (orígenes `http://localhost:4200` y `http://localhost:4000`), `scripts/Configure-Agent.ps1` (dominios por defecto de producción) y el comentario obsoleto de `Contracts/EncryptedPayload.cs`.

- [ ] `git checkout origin/feature/web-cam -- middleware/` y aplicar los ajustes.
- [ ] `dotnet test middleware/Trazzo.Middleware.slnx` → 370 tests en verde.

### Task 4 (B2): puerto criptográfico biométrico con OAEP correcto (AUD-01)

**Archivos:**
- Crear: `corehr/application/port/out/BiometricCryptoPort.java`, `corehr/infrastructure/adapters/out/crypto/HybridBiometricCryptoAdapter.java`.
- Modificar: `MarkAttendanceUseCase.java`, `SourceAfisMatchingAdapter.java`, `CoreHrBeanConfiguration.java`.
- Test: `HybridBiometricCryptoAdapterTest.java` + `src/test/resources/biometric/dotnet-vector.json` (payload generado por el `HybridCryptographyService` de .NET con una clave de prueba).

**Interfaces producidas:**
- `record TransportPayload(String encryptedTemplateBase64, String encryptedAesKeyBase64, String ivBase64, String tagBase64)`
- `byte[] BiometricCryptoPort.openTransport(TransportPayload payload)`: lanza `BiometricDecryptionException extends IllegalArgumentException`.

- [ ] Test que falla: el vector .NET se descifra al template esperado; un tag alterado lanza `BiometricDecryptionException`.
- [ ] Implementar con `OAEPParameterSpec("SHA-256","MGF1",MGF1ParameterSpec.SHA256,PSource.PSpecified.DEFAULT)`, AES/GCM con AAD `biometric-identify`; probar cada clave de descifrado en orden.
- [ ] Sustituir la cripto inline de `MarkAttendanceUseCase` y `SourceAfisMatchingAdapter` por el puerto.

### Task 5 (B3): clave RSA persistente y rotable (AUD-02)

**Archivos:** modificar `RsaKeyProviderAdapter.java`, `CryptoKeyProviderPort.java`, `StartupConfigValidator.java`, `application*.properties`, `back/Dockerfile` (crea `/app/keys` propiedad de `trazzo`), ambos compose (volumen `trazzo-backend-keys:/app/keys`), plantillas `.env*`.

**Propiedades:**
- `app.biometric.rsa.private-key=${APP_BIOMETRIC_RSA_PRIVATE_KEY:}` — PKCS#8 en PEM o Base64.
- `app.biometric.rsa.key-file=${APP_BIOMETRIC_RSA_KEY_FILE:}` — si no existe, se genera y se guarda.
- `app.biometric.rsa.previous-private-keys=${APP_BIOMETRIC_RSA_PREVIOUS_KEYS:}` — lista separada por comas.
- Sin ninguna de las dos primeras: clave efímera con WARN, solo aceptable en tests y local.

**Interfaces:** `CryptoKeyProviderPort.getDecryptionKeys(): List<PrivateKey>` (la actual primero); `kid` = `rsa-` + 16 hex de SHA-256(SPKI).

- [ ] Tests: el mismo `key-file` en dos instancias da el mismo `kid`; una clave anterior sigue descifrando; una clave inválida falla al arrancar con un mensaje que nombra la propiedad; el validador exige `private-key` o `key-file` fuera de `local`.

### Task 6 (B4): re-cifrado de almacenamiento al enrolar (AUD-02 parte 2, AUD-19)

**Archivos:** crear `db/tenant/migration/V5__usuario_biometria_key_ref.sql` (`ADD COLUMN IF NOT EXISTS key_ref VARCHAR(32)`); modificar `UserBiometria` (dominio), `UserBiometriaEntity`, el mapper, `EnrollService.completeEnroll`, `SourceAfisMatchingAdapter`, `BiometricCryptoPort`.

**Propiedad:** `app.biometric.storage-key=${APP_BIOMETRIC_STORAGE_KEY:${app.encryption.key:}}` (Base64 de 32 bytes; el validador la exige fuera de `local`).

**Interfaces producidas:**
- `record StoredTemplate(String ciphertextBase64, String ivBase64, String tagBase64, String keyRef)`
- `StoredTemplate seal(byte[] template)` con AAD `biometric-storage:v1` y `keyRef="storage-v1"`
- `byte[] open(StoredTemplate stored)`: `keyRef` nulo indica un blob de transporte antiguo y se intenta con las claves RSA.

- [ ] `completeEnroll`: descifra el transporte (400 si falla); valida que sea un template SourceAFIS (`new FingerprintTemplate(bytes)`, 400 si no); sella y guarda.
- [ ] *Matching*: recorre todas las plantillas activas en páginas de 1000 (sin tope silencioso); una fila que no abre se omite con WARN y se cuenta.
- [ ] Tests: el *round-trip* `seal`/`open`; una fila legada indescifrable se omite; un payload que no es SourceAFIS → 400.

### Task 7 (B5): hora local del tenant en la asistencia (AUD-07)

**Archivos:** crear `corehr/application/port/out/BusinessTimeZonePort.java` y `corehr/infrastructure/adapters/out/config/PropertyBusinessTimeZoneAdapter.java`; modificar `MarkAttendanceUseCase.java`.

**Propiedad:** `trazzo.attendance.time-zone=${APP_TIMEZONE:America/Lima}`.

- [ ] Tests: `12:55Z` con ingreso a las 08:00 y 5 min de tolerancia → `PUNTUAL`, `check_in` 07:55; `2026-10-23T04:30Z` → `attendance_date=2026-10-22`; una zona inválida falla al arrancar.
- [ ] Convertir `capturedAtUtc` (UTC) a la hora local antes de `registerCheckInAt` / `registerCheckOutAt` y de calcular la fecha.

### Task 8 (B6): sesiones de enrolamiento persistentes (AUD-22)

**Archivos:** crear `db/tenant/migration/V6__enroll_session.sql`; reemplazar el `ConcurrentHashMap` de `EnrollSessionStore` por JDBC (mismas firmas públicas: `save`, `findAndConsume`, `findPendingByDeviceCode`, `existsActiveSession`). `findAndConsume` usa `DELETE … RETURNING` para ser atómico.

- [ ] Tests con `JdbcTemplate` simulado (SQL y parámetros) + uno de integración con Testcontainers: la sesión sobrevive a una nueva instancia del store y se consume una sola vez.

### Task 9 (B7): credencial de dispositivo para el agente (AUD-06)

**Archivos:**
- Crear: `db/master/V10__agent_device_tokens.sql` (`token_hash CHAR(64) PK, tenant_id UUID, schema_name, device_id INT, device_code, created_at, last_used_at, revoked_at`), `shared/security/DeviceTokenAuthFilter.java`, `shared/security/DeviceAgentPrincipal.java`, `shared/security/AgentDeviceTokenRepository.java` (JDBC sobre `rawDataSource`), `corehr/application/usecase/DeviceAgentTokenService.java`.
- Modificar: `SecurityConfig`, `JwtAuthFilter` (no procesa si ya hay autenticación), `DeviceController` (`POST` y `DELETE /corehr/devices/{id}/agent-token`), `AttendanceSyncController` y `UserBiometriaController` (comprobación de `device_code`).

**Interfaces producidas:**
- Token `trzd_` + 43 caracteres Base64url (32 bytes aleatorios); se guarda solo su SHA-256.
- `DeviceAgentPrincipal(UUID tenantId, String schemaName, Long deviceId, String deviceCode)` con autoridad `ROLE_AGENT`.
- Rutas permitidas al agente: `POST /asistencia/marcar`, `POST /asistencia/sync`, `GET /corehr/biometria/enroll/pendiente`, `POST /corehr/biometria/enroll/completar`, `GET /security/public-key`; cualquier otra → 403.
- Respuesta de emisión: `{ "token", "device_code", "created_at" }`. El token se muestra solo una vez y emitir uno nuevo revoca el anterior.

- [ ] Tests: token válido → `TenantContext` del tenant y principal agente; revocado o desconocido → 401; ruta no permitida → 403; `device_code` ajeno en `marcar` o `completar` → 403; un JWT normal sigue funcionando.

### Task 10 (B8): `sync` informa los fallos

**Archivos:** `AttendanceSyncController.java`, `MarcacionSyncResponse.java` (+ `items: [{offline_event_id, status, message}]`).

- [ ] Test: un lote donde ningún elemento se acepta ni se omite → 422 con `items`; un lote mixto → 202 con `items`.

### Task 11 (B9): pantalla de dispositivos en el front

**Archivos:** crear `features/admin-tenant/dispositivos/` (componente standalone con signals: listar, crear y desactivar dispositivos, y generar el token del agente mostrándolo una vez con botón de copiar); ruta `tenant/dispositivos` con `authGuard` + `roleGuard` y entrada en el sidebar; métodos `issueAgentToken` / `revokeAgentToken` en `corehr.service.ts`; tipos en `types.ts`.

- [ ] Specs: lista, alta, emisión de token (el valor se muestra una vez y se oculta al cerrar) y error de API.

### Task 12 (B10): verificación de punta a punta

- [ ] Stack efímero: crear un dispositivo → emitir token → `enroll/iniciar` (JWT de admin) → `pendiente` + `completar` con el token y un template SourceAFIS generado por .NET 3.14 y cifrado como el middleware → reiniciar el backend → `marcar` → 200 `PUNTUAL` con hora local.
- [ ] Actualizar OpenAPI (rutas de token, `items` de `sync`, `key_ref` interno no expuesto), plantillas `.env*` y la documentación del middleware.

---

## Plan C — Control de acceso (rama `fix/plan-c-rbac`)

### Task 13 (C1): bean de autorización `@authz`

**Archivos:** crear `shared/security/TenantAuthorization.java` (bean `authz`) y `shared/security/TenantUserIdResolver.java` (JDBC: `SELECT id FROM tenant_user WHERE master_user_id = ?::uuid AND deleted_at IS NULL`).

**Interfaces:**
- `boolean any(String... permissions)`: `true` si tiene `administrador_tenant` o cualquiera de los permisos.
- `boolean admin()`, `boolean agent()`.
- `Optional<Long> currentTenantUserId()`.
- `boolean isSelf(Long tenantUserId)`.

- [ ] Tests unitarios de cada método con `SecurityContext` simulado.

### Task 14 (C2): matriz permiso → endpoint (AUD-05)

**Archivos:** todos los controladores de tenant (`corehr`, `organization`, `incidents`, `reports`, `TenantBillingController`, `StorageController`).

| Recurso | Lectura | Escritura |
|---|---|---|
| `/usuarios`, `/usuarios/{id}` | `VER_PERSONAL`¹ | crear `gestion-trabajadores.crear`; PUT/PATCH `…editar`; DELETE `…eliminar`; `/rol` `…asignar-roles` |
| `/usuarios/{id}/password` | — | `isSelf(#id)` o `gestion-trabajadores.editar` |
| `/usuarios/me` | `isAuthenticated()` | `isAuthenticated()` |
| `/org/branches` · `/org/areas`, `/org/departments` | `VER_ESTRUCTURA`² | `estructura-organizacional.crear-editar-sedes` · `…crear-editar-areas` |
| `/org/roles`, `/org/permissions` | `gestion-trabajadores.asignar-roles` | `admin()` |
| `/org/users/{id}/roles`, `/org/roles/{id}/users` | `gestion-trabajadores.asignar-roles` | `gestion-trabajadores.asignar-roles` |
| `/org/billing/**` | `admin()` | `admin()` |
| `/corehr/shifts`, `/schedules`, `/tolerancias` | `VER_HORARIOS`³ | `gestion-horarios.configurar-turnos` (tolerancias: `…configurar-tolerancias`) |
| `/corehr/user-schedules` | `VER_HORARIOS` | `gestion-horarios.asignacion-masiva` |
| `/corehr/non-working-days` | `isAuthenticated()` | `gestion-horarios.configurar-turnos` |
| `/corehr/devices` (+ token) | `configuracion-tenant.gestionar-metodos`, `control-asistencia.operador-asistencia` | `configuracion-tenant.gestionar-metodos` |
| `/corehr/biometria` (lista, PATCH, `enroll/iniciar`) | `configuracion-tenant.gestionar-metodos`, `control-asistencia.operador-asistencia`, `gestion-trabajadores.editar` | ídem |
| `enroll/pendiente`, `enroll/completar`, `/asistencia/**` | `agent()` o `control-asistencia.operador-asistencia` | ídem |
| `/corehr/attendance` | alcance SELF forzado salvo `VER_ASISTENCIA_TODOS`⁴ | PATCH `control-asistencia.corregir-marcaciones` |
| `/corehr/tenant-contacts`, `/corehr/usuarios/{id}/departamentos` | `VER_PERSONAL` | `gestion-trabajadores.editar` · `estructura-organizacional.asignar-personal` |
| `/incidentes` (lista, `{id}`, evidencias) | `incidencias.ver-propias` con SELF forzado salvo `APROBADOR`⁵ | crear `incidencias.crear` (solo propias); estado y notificar `APROBADOR`; justificar `APROBADOR` o `control-asistencia.justificar-marcaciones` |
| `/incidentes/tipos` | `isAuthenticated()` | `permisos-licencias.aprobar-incidencias` |
| `/reports/**` | `reportes.exportar-excel`, `reportes.exportar-pdf` | ídem |

¹ `VER_PERSONAL` = `gestion-trabajadores.*`, `estructura-organizacional.asignar-personal`, `control-asistencia.*`, `gestion-horarios.asignacion-masiva`, `reportes.*`, `permisos-licencias.*`, `incidencias.aprobar-rechazar`.
² `VER_ESTRUCTURA` = `VER_PERSONAL` + `estructura-organizacional.*` + `gestion-horarios.*`.
³ `VER_HORARIOS` = `gestion-horarios.*`, `horarios-docente.*`, `control-asistencia.*`, `reportes.*`.
⁴ `VER_ASISTENCIA_TODOS` = `control-asistencia.*`, `reportes.*`, `asistencia-docente.ver-tardanzas-faltas`.
⁵ `APROBADOR` = `incidencias.aprobar-rechazar`, `permisos-licencias.aprobar-incidencias`, `permisos-licencias.rechazar-incidencias`.

`administrador_tenant` siempre pasa (lo resuelve `any`).

- [ ] Test reflexivo `TenantEndpointAuthorizationCoverageTest`: todo *handler* de tenant (fuera de rutas públicas, `/saas`, `/tenants` y `/audit`) tiene `@PreAuthorize`.
- [ ] Slice tests por controlador: el rol `usuario` recibe 403 en escritura de administración; `administrador_tenant` recibe 2xx.

### Task 15 (C3): alcance propio en asistencia, incidencias y contraseña (AUD-08)

**Archivos:** `AttendanceController`, `IncidentController`, `TenantUserController`.

- [ ] Tests: `usuario` sin `scope` → lista solo lo suyo (`scopeAplicado=SELF`); `GET`/`PATCH /incidentes/{id}` ajeno → 403; aprobador ve todo; `usuario` cambia su propia contraseña (204) pero no la de otro (403).

### Task 16 (C4): `TenantContextGuard` activo (AUD-15)

**Archivos:** `SecurityConfig` (bean sin `@ConditionalOnBean`, con `ObjectProvider<DataSource>`, y `FilterRegistrationBean` deshabilitado), `TenantContextGuard` (deja pasar a los anónimos para que respondan 401).

- [ ] Tests: el *filter chain* contiene el guard (`WebApplicationContextRunner`); admin SaaS en una ruta de tenant → 403 JSON; anónimo → 401.

### Task 17 (C5): presign endurecido (AUD-26)

**Archivos:** `StorageController.java`.

- [ ] Tests: `../../x.pdf` → nombre normalizado `x.pdf`; MIME fuera de `IncidentEvidenceSpec.ALLOWED_MIME_TYPES` → 400; `incident_id` inexistente → 404; incidente ajeno sin permiso de aprobador → 403.

### Task 18 (C6): humo de autorización

- [ ] Repetir S9–S12 de la auditoría → 403/403/403/SELF; el front de admin y el de `usuario` cargan todas sus pantallas sin 403.

---

## Plan D — Infraestructura y CI/CD (rama `fix/plan-d-infra-ci`)

### Task 19 (D1): publicación gobernada por CI (AUD-13)

**Archivos:** `.github/workflows/publish-docker.yml` (`workflow_run` de `CI/CD Backend Spring Boot` y `CI/CD Frontend Angular` con `conclusion == success`, `event == push`, `head_branch == main`; checkout de `head_sha`; tags `main` y `sha-<7>`; `concurrency`), `.github/workflows/back-ci.yml` (un solo `./mvnw -B verify` + `sonar:sonar` sin repetir los tests).

- [ ] `actionlint` (imagen Docker `rhysd/actionlint`) sin errores.

### Task 20 (D2): compose sin fugas y versionado del ingreso (AUD-09, AUD-24)

**Archivos:** `docker-compose.yml`, `docker-compose.local.yml`, `docker-compose.dev.yml`, `.env*.example`.

- [ ] `cloudflared` como servicio con `profiles: [tunnel]`, imagen fijada y `TUNNEL_TOKEN`.
- [ ] `openssh-server` con tag fijado; `logging: {driver: json-file, options: {max-size: 10m, max-file: "3"}}` en todos los servicios.
- [ ] El backend recibe una lista `environment:` explícita en lugar de `env_file: .env`, sin la contraseña de superusuario.
- [ ] `dev` publica Postgres en `5434`.
- [ ] `docker compose config -q` de los 3 archivos; `up` limpio del local en verde.

### Task 21 (D3): cabeceras de seguridad en nginx y `.dockerignore` (AUD-20, AUD-28)

**Archivos:** `front/nginx.conf`, `back/.dockerignore`.

- [ ] `X-Content-Type-Options`, `Referrer-Policy`, `X-Frame-Options`, `Permissions-Policy` y HSTS (sin `includeSubDomains`).
- [ ] CSP: `connect-src 'self' ws://localhost:9001 https://*.r2.cloudflarestorage.com`, y el resto según los recursos reales de `index.html`.
- [ ] Verificación con Chrome *headless* contra la imagen construida: 0 violaciones de CSP en consola en login, panel de tenant y panel de usuario.

### Task 22 (D4): runbook de despliegue

**Archivos:** crear `DEPLOY.md`: variables, generación de secretos (`openssl rand -base64 32`, clave RSA con `openssl genpkey`), arranque local y prod, túnel, emisión de tokens de dispositivo y rotación de claves.

---

## Plan E — Contrato y documentación (rama `fix/plan-e-contrato-docs`)

### Task 23 (E1): test de contrato backend ↔ OpenAPI (AUD-16)

**Archivos:** crear `back/src/test/java/trazzo/back/contract/OpenApiContractTest.java` (reflexión sobre `@RestController` + SnakeYAML sobre los 5 YAML de la raíz; compara conjuntos `MÉTODO /ruta` normalizados).

- [ ] El test falla hoy (33 sin documentar / 25 fantasma) y pasa tras E2.

### Task 24 (E2): corregir las especificaciones

**Archivos:** los 5 YAML.

- [ ] `servers` de prod y local.
- [ ] Solo `bearerAuth`: se elimina `tenantHeader`.
- [ ] Añadir las 33 operaciones con esquemas tomados de los DTOs; corregir o eliminar las 25 fantasma.
- [ ] Añadir `tenant_permissions`.
- [ ] Eliminar las menciones a Redis.
- [ ] Definir `iv_base64`/`tag_base64` en `MarcacionRequest`.
- [ ] Sección normativa “Criptografía biométrica”.
- [ ] Seguridad en `apicontractoAudit.yaml`.
- [ ] Validación: el test de E1 en verde + parseo con js-yaml sin `$ref` rotos.

### Task 25 (E3): documentación

**Archivos:** `AGENTS.md`, `front/src/app/api/mock-data-README.md`, `middleware/README.md`, `middleware/docs/README-dev-handoff.md`, `.42c/conf.yaml` (y su línea de `.gitignore`), `AUDITORIA_GENERAL.md` (estado por hallazgo).

---

## Plan F — Higiene (rama `fix/plan-f-higiene`)

### Task 26 (F1): tests *placeholder* y código muerto (AUD-25, AUD-29)

- [ ] Borrar las 10 clases `@Disabled("TODO…")` vacías.
- [ ] Rehabilitar `TenantControllerTest`, dándole su lugar en `saasglobal`.
- [ ] Borrar `CompleteEnrollRequest` (y su test) y el paquete vacío `notifications`.

### Task 27 (F2): avisos NG8102 (AUD-27)

- [ ] Quitar los `??` innecesarios en Facturas y Planes; `pnpm build` sin avisos NG8102.

---

## Fuera de alcance (requiere decisión del equipo)

- Borrar o rebasar ramas remotas ajenas (AUD-21).
- Firma del MSI (requiere certificado) y publicación del manifiesto de auto-update (AUD-30).
- Mover el JWT de `localStorage` a una cookie `HttpOnly` (AUD-20, segunda parte).
