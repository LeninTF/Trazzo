# Informe de Errores - Backend Trazzo (rama feature/audit-conexion)

---

## BUG #1 [CRITICAL]

### Informe de error: Desajuste entre nombres de tablas JPA y el schema SQL de tenant

**Título:** Las entidades JPA de incidencias usan nombres de tabla en inglés pero `schema.sql` crea tablas en español, causando fallos en producción

**Precondiciones**
- La aplicación debe estar desplegada en un entorno donde `spring.jpa.hibernate.ddl-auto` NO esté en modo `update` (producción, staging).
- El tenant schema script `db/tenant/schema.sql` debe haber sido ejecutado previamente.

**Pasos para reproducir**
1. Desplegar la aplicación en producción (donde `ddl-auto` no está configurado como `update`).
2. Provisionar un nuevo tenant (esto ejecuta `schema.sql` que crea las tablas en español).
3. Intentar crear o listar una incidencia desde el endpoint `POST /api/v1/incidentes` o `GET /api/v1/incidentes`.
4. Observar el error en logs: `Table "incidents" not found`.

**Resultado Actual**
La tabla `schema.sql` crea:
- `incidencias` → JPA busca `incidents`
- `incidencia_types` → JPA busca `incident_types`
- `incidencia_evidencia` → JPA busca `incident_evidences`
- `permisos_incidencia` → JPA busca `incident_permissions`

Las columnas también difieren (ej: SQL usa `incidencia_type_id`, JPA espera `incident_type_id`). Además, el schema SQL carece de columnas `comment` y `rejection_reason` que JPA espera.

En local dev, `ddl-auto=update` crea las tablas inglesas al lado de las españolas, enmascarando el bug.

**Resultado Esperado**
Las entidades JPA (`IncidentEntity.java:15`, `IncidentTypeEntity.java:12`, etc.) deben coincidir con los nombres de tabla y columnas definidos en `schema.sql` (líneas 215-251).

**Archivos afectados:**
- `src/main/java/trazzo/back/incidents/infrastructure/adapters/out/persistence/entity/IncidentEntity.java:15`
- `src/main/java/trazzo/back/incidents/infrastructure/adapters/out/persistence/entity/IncidentTypeEntity.java:12`
- `src/main/java/trazzo/back/incidents/infrastructure/adapters/out/persistence/entity/IncidentEvidenceEntity.java:12`
- `src/main/java/trazzo/back/incidents/infrastructure/adapters/out/persistence/entity/IncidentPermissionEntity.java:13`
- `src/main/resources/db/tenant/schema.sql:215-251`

---

## BUG #2 [CRITICAL]

### Informe de error: Doble prefijo `/api/v1` en StorageController

**Título:** El endpoint de presigned URLs de storage tiene la ruta `/api/v1/api/v1/storage/presigned-url` debido a un prefijo duplicado

**Precondiciones**
- La aplicación debe estar ejecutándose con la propiedad `server.servlet.context-path=/api/v1` (configurada en `application.properties:48`).

**Pasos para reproducir**
1. Iniciar la aplicación.
2. Intentar acceder al endpoint de presigned URL en `/api/v1/storage/presigned-url`.
3. Observar que la ruta correcta real es `/api/v1/api/v1/storage/presigned-url` (error 404 con la ruta esperada).

**Resultado Actual**
`StorageController.java:16` define `@RequestMapping("/api/v1/storage")`, y `application.properties:48` define `server.servlet.context-path=/api/v1`. La URL efectiva resultante es `/api/v1/api/v1/storage/presigned-url`. Todos los demás controllers usan rutas relativas correctas (ej: `@RequestMapping("/incidentes")`, `@RequestMapping("/org/roles")`).

**Resultado Esperado**
`StorageController.java:16` debe usar `@RequestMapping("/storage")` para ser consistente con el resto de controllers y evitar la duplicación del context path.

**Archivos afectados:**
- `src/main/java/trazzo/back/shared/infrastructure/adapters/in/web/StorageController.java:16`
- `src/main/resources/application.properties:48`

---

## BUG #3 [CRITICAL]

### Informe de error: `fileStoragePort.buildPublicUrl()` lanza StorageException en entorno local

**Título:** Listar evidencias de incidencias lanza error 500 en desarrollo local porque `LocalStorageStub` no puede construir URLs públicas de Cloudflare R2

**Precondiciones**
- La aplicación debe ejecutarse en perfil `local` sin las variables de entorno de Cloudflare R2 configuradas.
- Debe existir al menos una incidencia con evidencias adjuntas.

**Pasos para reproducir**
1. Iniciar la aplicación con el perfil `local`.
2. Crear una incidencia con al menos una evidencia adjunta.
3. Listar las incidencias (`GET /api/v1/incidentes`) o listar evidencias de una incidencia (`GET /api/v1/incidentes/{id}/evidencias`).
4. Observar el error 500: `StorageException: Cloudflare R2 is not configured...`.

**Resultado Actual**
`IncidentService.java:155` y `EvidenceService.java:67` llaman a `fileStoragePort.buildPublicUrl()` en cada resultado de evidencia. Cuando R2 no está configurado, el bean `LocalStorageStub` lanza `StorageException` en cada llamada.

**Resultado Esperado**
El `buildPublicUrl()` debe retornar `null` o una URL placeholder cuando R2 no está configurado, en lugar de lanzar una excepción. Alternativamente, la capa de resultado debe tolerar URLs nulas.

**Archivos afectados:**
- `src/main/java/trazzo/back/incidents/application/usecase/IncidentService.java:152-158`
- `src/main/java/trazzo/back/incidents/application/usecase/EvidenceService.java:61-73`

---

## BUG #4 [HIGH]

### Informe de error: Parámetros de filtro `scope`, `sede_id`, `area_id`, `departamento_id` son ignorados silenciosamente

**Título:** El endpoint de listado de incidencias acepta parámetros de filtrado por sede/área/departamento pero nunca los aplica en la consulta

**Precondiciones**
- El usuario debe estar autenticado.
- Deben existir incidencias en la base de datos.

**Pasos para reproducir**
1. Autenticarse en el sistema.
2. Llamar a `GET /api/v1/incidentes?sede_id=123&area_id=456&departamento_id=789`.
3. Observar que se retornan TODAS las incidencias, sin importar los filtros proporcionados.

**Resultado Actual**
`IncidentController.java:35-38` acepta los parámetros `scope`, `sede_id`, `area_id`, `departamento_id` y los pasa a `IncidentService.findAll()`. Sin embargo, `IncidentService.java:66` llama a `incidentRepository.findAll(null, state, tipoId, ...)` — el primer argumento (`tenantUserId`) está hardcodeado a `null`, y los parámetros `scope`, `sedeId`, `areaId`, `departamentoId` nunca se reenvían al repositorio.

**Resultado Esperado**
Los parámetros de filtrado deben ser propagados desde el controller hasta la consulta del repositorio, permitiendo filtrar incidencias por sede, área y departamento.

**Archivos afectados:**
- `src/main/java/trazzo/back/incidents/infrastructure/adapters/in/web/IncidentController.java:35-38`
- `src/main/java/trazzo/back/incidents/application/usecase/IncidentService.java:58-66`

---

## BUG #5 [HIGH]

### Informe de error: Ausencia de `@Transactional` en servicios de organización e incidencias

**Título:** Los métodos de servicios críticos (roles, permisos, incidencias, holdings, planes, features) no están anotados con `@Transactional`, permitiendo escrituras parciales en caso de error

**Precondiciones**
- Cualquier operación de escritura en los módulos afectados.

**Pasos para reproducir**
1. Crear o modificar un rol con permisos asociados.
2. Simular un fallo después del primer `save()` pero antes de completar la operación.
3. Observar que el primer `save()` persiste pero el resto de la operación no se revierte.

**Resultado Actual**
Ningún método de los siguientes servicios tiene `@Transactional`:
- `RoleService.java`, `PermissionService.java`, `UserRoleService.java`, `RolePermissionsService.java`
- `IncidentService.java`, `EvidenceService.java`, `IncidentTypeService.java`
- `HoldingService.java`, `PlanService.java`, `FeatureService.java`

En contraste, `TenantProvisioningService` y `CreateMonthlyClosureService` sí usan `@Transactional` correctamente.

**Resultado Esperado**
Todos los métodos de servicio que realizan múltiples operaciones de persistencia deben estar anotados con `@Transactional` para garantizar la atomicidad.

**Archivos afectados:**
- `src/main/java/trazzo/back/organization/application/usecase/RoleService.java`
- `src/main/java/trazzo/back/organization/application/usecase/PermissionService.java`
- `src/main/java/trazzo/back/organization/application/usecase/UserRoleService.java`
- `src/main/java/trazzo/back/organization/application/usecase/RolePermissionsService.java`
- `src/main/java/trazzo/back/incidents/application/usecase/IncidentService.java`
- `src/main/java/trazzo/back/incidents/application/usecase/EvidenceService.java`
- `src/main/java/trazzo/back/incidents/application/usecase/IncidentTypeService.java`
- `src/main/java/trazzo/back/saasglobal/application/usecase/HoldingService.java`
- `src/main/java/trazzo/back/saasglobal/application/usecase/PlanService.java`
- `src/main/java/trazzo/back/saasglobal/application/usecase/FeatureService.java`

---

## BUG #6 [HIGH]

### Informe de error: Métodos de `NotificationService` son stubs vacíos con dead code

**Título:** Los endpoints `/incidentes/{id}/notificar` y `/incidentes/{id}/justificar` son no-op: no envían notificaciones ni justifican asistencia, y contienen código muerto

**Precondiciones**
- El usuario debe estar autenticado.
- Debe existir una incidencia válida.

**Pasos para reproducir**
1. Autenticarse en el sistema.
2. Enviar `POST /api/v1/incidentes/{id}/notificar` con un body válido.
3. Observar que la respuesta es 202 Accepted pero no ocurre ninguna acción (no se envía notificación).
4. Enviar `POST /api/v1/incidentes/{id}/justificar`.
5. Observar que tampoco ocurre ninguna acción.

**Resultado Actual**
`NotificationService.java:14-19` y `NotificationService.java:22-28` ejecutan `findById()` que lanza `orElseThrow()`, luego verifican `if (incident == null)` — dead code porque `orElseThrow()` nunca retorna null. Los cuerpos de ambos métodos están vacíos.

**Resultado Esperado**
Ambos métodos deben implementar la lógica de notificación y justificación respectivamente, o los endpoints deben ser removidos/marcados como no implementados.

**Archivos afectados:**
- `src/main/java/trazzo/back/incidents/application/usecase/NotificationService.java:14-29`

---

## BUG #7 [HIGH]

### Informe de error: Riesgo de SQL Injection en `TenantSchemaProvisioningAdapter.createDatabaseAndUser()`

**Título:** La contraseña del usuario de base de datos del tenant se inserta sin sanitizar en un statement SQL dinámico

**Precondiciones**
- Se debe ejecutar el flujo de provisioning de un tenant Trial (que usa `provisionExisting()` con credenciales proporcionadas por el usuario).

**Pasos para reproducir**
1. Ejecutar el provisioning de un tenant Trial con una contraseña que contenga comilla simple (ej: `pass'word`).
2. Observar que el SQL generado es: `CREATE USER "user_xxx" WITH ENCRYPTED PASSWORD 'pass'word'` — esto rompe la sintaxis SQL.
3. Si la contraseña se diseña maliciosamente, podría permitir inyección SQL.

**Resultado Actual**
`TenantSchemaProvisioningAdapter.java:92` construye el SQL con concatenación directa:
```java
stmt.execute("CREATE USER \"" + dbUser + "\" WITH ENCRYPTED PASSWORD '" + dbPassword + "'");
```
El `dbUser` se valida con `validateIdentifier()`, pero `dbPassword` no se sanitiza. En el flujo `provisionExisting()`, la contraseña proviene directamente del request de la API.

**Resultado Esperado**
La contraseña debe ser escapada o el provisioning debe usar parámetros preparados. Como alternativa mínima, se debe validar que la contraseña no contenga caracteres peligrosos (`'`, `"`, `;`, `--`).

**Archivos afectados:**
- `src/main/java/trazzo/back/saasglobal/infrastructure/adapters/out/provisioning/TenantSchemaProvisioningAdapter.java:85-97`

---

## BUG #8 [MEDIUM]

### Informe de error: Falta validación `@NotBlank` en `CreateIncidentRequest.tenantUserId`

**Título:** El campo `tenant_user_id` en la creación de incidencias no tiene validación a nivel de DTO, permitiendo la creación de incidencias huérfanas

**Precondiciones**
- El usuario debe estar autenticado.

**Pasos para reproducir**
1. Autenticarse en el sistema.
2. Enviar `POST /api/v1/incidentes` con un body donde `tenant_user_id` sea `null` o `""`.
3. La incidencia se crea exitosamente sin un `tenantUserId` válido.

**Resultado Actual**
`CreateIncidentRequest.java:9` define `tenantUserId` sin `@NotBlank`. Aunque el dominio (`Incident.create()`) rechaza valores vacíos, el mensaje de error es confuso para el consumidor de la API.

**Resultado Esperado**
El campo `tenantUserId` debe tener `@NotBlank @JsonProperty("tenant_user_id")` para validación consistente a nivel de DTO.

**Archivos afectados:**
- `src/main/java/trazzo/back/incidents/infrastructure/adapters/in/web/dto/CreateIncidentRequest.java:9`

---

## BUG #9 [MEDIUM]

### Informe de error: Sin verificación de autorización en cambios de estado de incidencias

**Título:** Cualquier usuario autenticado puede aprobar o denegar cualquier incidencia sin verificación de roles ni propiedad

**Precondiciones**
- Dos usuarios autenticados distintos deben existir en el sistema.

**Pasos para reproducir**
1. Autenticarse como usuario A (empleado regular).
2. Crear una incidencia como usuario A.
3. Cambiar al usuario B (también empleado regular).
4. Enviar `PATCH /api/v1/incidentes/{id}/estado` con `"state": "APROBADO"`.
5. Observar que el usuario B puede aprobar la incidencia del usuario A.

**Resultado Actual**
`IncidentController.java:77-85` no realiza ninguna verificación de rol o propiedad. La configuración de seguridad solo requiere `authenticated()` para `/incidentes/**`.

**Resultado Esperado**
Solo usuarios con rol de administrador o supervisor deben poder aprobar/denegar incidencias. Los empleados solo deberían poder gestionar las propias.

**Archivos afectados:**
- `src/main/java/trazzo/back/incidents/infrastructure/adapters/in/web/IncidentController.java:77-85`
- `src/main/java/trazzo/back/shared/security/SecurityConfig.java:69`

---

## BUG #10 [MEDIUM]

### Informe de error: Consulta de conteo de incidencias ejecuta query completa innecesariamente

**Título:** El método `count()` del repositorio de incidencias ejecuta la query paginada completa con JOINs solo para obtener el total, resultando en dos queries costosas por cada listado

**Precondiciones**
- Deben existir incidencias en la base de datos con filtros activos.

**Pasos para reproducir**
1. Autenticarse en el sistema.
2. Llamar a `GET /api/v1/incidentes?state=PENDIENTE`.
3. Observar en los logs de SQL que se ejecutan dos queries: una para `findAll` y otra para `count` (que internamente ejecuta `findByFilters(..., PageRequest.of(0,1))`).

**Resultado Actual**
`IncidentService.java:66-67` llama a `incidentRepository.findAll()` y luego `incidentRepository.count()` por separado. `IncidentRepositoryAdapter.java:88-90` ejecuta `findByFilters(..., PageRequest.of(0,1)).getTotalElements()` cuando hay filtros — duplicando el trabajo.

**Resultado Esperado**
El conteo debe obtenerse del objeto `Page` retornado por `findAll()` (que ya incluye `getTotalElements()`), eliminando la segunda query.

**Archivos afectados:**
- `src/main/java/trazzo/back/incidents/application/usecase/IncidentService.java:66-67`
- `src/main/java/trazzo/back/incidents/infrastructure/adapters/out/persistence/IncidentRepositoryAdapter.java:84-93`

---

## BUG #11 [MEDIUM]

### Informe de error: Problema N+1 al convertir incidencias a resultados en `toResult()`

**Título:** Cuando una incidencia no tiene tipo pre-cargado (ej: desde `findById()`), `toResult()` ejecuta una query individual por cada incidencia para buscar su tipo

**Precondiciones**
- Deben existir incidencias con diferentes tipos de incidencia.

**Pasos para reproducir**
1. Autenticarse en el sistema.
2. Obtener una incidencia individual vía `GET /api/v1/incidentes/{id}`.
3. Observar en logs que se ejecuta una query adicional para buscar el `IncidentType`.

**Resultado Actual**
`IncidentService.java:137-141` ejecuta `typeRepository.findById()` cuando `incident.getType()` es null. El path `findAll()` llama a `attachTypes()` previamente, pero `findById()` no lo hace. Resultado: 2 queries para una sola incidencia.

**Resultado Esperado**
El método `findById()` del repositorio debe incluir el tipo asociado (JOIN o batch fetch), o `toResult()` no debe hacer queries adicionales.

**Archivos afectados:**
- `src/main/java/trazzo/back/incidents/application/usecase/IncidentService.java:131-142`
- `src/main/java/trazzo/back/incidents/infrastructure/adapters/out/persistence/IncidentRepositoryAdapter.java:44-50`

---

## BUG #12 [MEDIUM]

### Informe de error: Comentario TODO obsoleto y configuración muerta en producción

**Título:** El `DataSeeder` tiene `@Profile("local")` pero `application.properties` de producción tiene un TODO que sugiere riesgo de ejecución en prod

**Precondiciones**
- La aplicación debe ejecutarse con el perfil de producción.

**Pasos para reproducir**
1. Revisar `application.properties:38-40`:
   ```properties
   # TODO: Admin seeder habilitado temporalmente para produccion.
   #       Eliminar o setear trazzo.seed.admin.enabled=false en el proximo push.
   trazzo.seed.admin.enabled=true
   ```
2. Revisar `DataSeeder.java:17`: `@Profile("local")`.
3. Observar que el seeder NUNCA se ejecuta en producción independientemente de la propiedad, porque está restringido al profile `local`.

**Resultado Actual**
La propiedad `trazzo.seed.admin.enabled=true` en producción es dead config que genera confusión. El TODO es obsoleto y engañoso.

**Resultado Esperado**
Eliminar el comentario TODO y la propiedad de producción, ya que el `@Profile("local")` hace que la configuración sea irrelevante.

**Archivos afectados:**
- `src/main/resources/application.properties:38-40`
- `src/main/java/trazzo/back/saasglobal/infrastructure/config/DataSeeder.java:17`

---

## BUG #13 [MEDIUM]

### Informe de error: `ddl-auto=update` en perfil local enmascara desajustes de schema

**Título:** La configuración `spring.jpa.hibernate.ddl-auto=update` en el perfil local permite que Hibernate modifique silenciosamente el esquema, enmascarando el BUG #1

**Precondiciones**
- La aplicación debe ejecutarse con el perfil `local`.

**Pasos para reproducir**
1. Ejecutar la aplicación con el perfil `local`.
2. Observar en los logs que Hibernate crea tablas nuevas (`incidents`, `incident_types`, etc.) además de las existentes (`incidencias`, `incidencia_types`).
3. La aplicación funciona pero con tablas duplicadas y vacías en el esquema español.

**Resultado Actual**
`application-local.properties:34` define `spring.jpa.hibernate.ddl-auto=update`. Esto crea automáticamente las tablas en inglés que JPA espera, pero las tablas en español de `schema.sql` quedan vacías y sin usar.

**Resultado Esperado**
`ddl-auto` debe ser `validate` o `none` para detectar tempranamente los desajustes de schema en lugar de enmascararlos.

**Archivos afectados:**
- `src/main/resources/application-local.properties:34`

---

## BUG #14 [MEDIUM]

### Informe de error: `HoldingType.valueOf()` lanza `IllegalArgumentException` sin manejo adecuado

**Título:** Enviar un tipo de holding inválido (ej: en minúsculas) produce un error 400 con mensaje confuso de Java interno

**Precondiciones**
- El usuario debe tener permisos de administrador para crear/actualizar holdings.

**Pasos para reproducir**
1. Autenticarse como admin.
2. Enviar `POST /api/v1/saas/holdings` con `"type": "publico"` (minúsculas).
3. Observar el error: `IllegalArgumentException: No enum constant trazzo.back...HoldingType.publico`.

**Resultado Actual**
`HoldingService.java:27` y `HoldingService.java:48` llaman a `HoldingType.valueOf(command.type())` sin validar ni manejar la excepción. El `OrgGlobalExceptionHandler` no captura esta excepción porque está en el paquete `saasglobal`, no `organization`.

**Resultado Esperado**
Se debe validar el tipo contra los valores del enum antes de llamar a `valueOf()`, o manejar la excepción con un mensaje claro para el usuario.

**Archivos afectados:**
- `src/main/java/trazzo/back/saasglobal/application/usecase/HoldingService.java:27,48`

---

## BUG #15 [MEDIUM]

### Informe de error: Problema N+1 por carga lazy de evidencias en listado paginado de incidencias

**Título:** El mapeo de incidencias a dominio en el repositorio dispara queries individuales para cargar evidencias de cada incidencia (N+1)

**Precondiciones**
- Deben existir incidencias con evidencias adjuntas.
- Se debe listar un 페이지 con múltiples incidencias.

**Pasos para reproducir**
1. Crear 5 incidencias, cada una con 2 evidencias.
2. Ejecutar `GET /api/v1/incidentes`.
3. Observar en logs que se ejecuta 1 query principal + 5 queries adicionales (una por cada incidencia para cargar sus evidencias).

**Resultado Actual**
`IncidentRepositoryAdapter.java:79-81` mapea cada entidad con `IncidentMapper.toDomain()`, que en `IncidentMapper.java:58-62` llama a `entity.getEvidences()` — una colección `FetchType.LAZY`. Esto dispara una query por cada incidencia.

**Resultado Esperado**
Las evidencias deben cargarse eagerly en la query principal o mediante `@BatchSize` / `JOIN FETCH` para evitar el problema N+1.

**Archivos afectados:**
- `src/main/java/trazzo/back/incidents/infrastructure/adapters/out/persistence/IncidentRepositoryAdapter.java:79-81`
- `src/main/java/trazzo/back/incidents/infrastructure/adapters/out/persistence/mapper/IncidentMapper.java:58-62`

---

## BUG #16 [MEDIUM]

### Informe de error: Actuator endpoints ignorados completamente de la cadena de seguridad

**Título:** Los endpoints de actuator son excluidos del filtro de seguridad con `web.ignoring()`, haciéndolos completamente inseguros si se exponen más endpoints en el futuro

**Precondiciones**
- La aplicación debe ejecutarse con los endpoints de actuator expuestos.

**Pasos para reproducir**
1. Revisar `SecurityConfig.java:92-94`:
   ```java
   return web -> web.ignoring().requestMatchers("/actuator/**");
   ```
2. Revisar `application.properties:53`: `management.endpoints.web.exposure.include=health,info`.
3. Si en el futuro se cambia para exponer `env`, `beans` o `configprops`, estos serían accesibles sin autenticación.

**Resultado Actual**
`web.ignoring()` saca los actuator del filtro de seguridad completamente. Aunque actualmente solo se exponen `health` e `info`, esto es un riesgo latente.

**Resultado Esperado**
Los endpoints de actuator deben pasar por la cadena de seguridad y ser protegidos con roles adecuados en lugar de ser ignorados.

**Archivos afectados:**
- `src/main/java/trazzo/back/shared/security/SecurityConfig.java:91-94`

---

## BUG #17 [LOW]

### Informe de error: Asignación redundante de `incidentTypeId` en `IncidentMapper.toEntity()`

**Título:** El tipo de incidencia se asigna dos veces en el mapper, con la segunda sobreescritura la primera

**Precondiciones**
- Una incidencia con tipo asociado debe ser mapeada a entidad.

**Pasos para reproducir**
1. Revisar `IncidentMapper.java:25`: `entity.setIncidentTypeId(domain.getIncidentTypeId());`
2. Revisar `IncidentMapper.java:33`: `entity.setIncidentTypeId(domain.getType().getId());` — sobreescribe la línea 25.

**Resultado Actual**
Ambas líneas deberían tener el mismo valor, pero la segunda siempre sobreescribe la primera. Si alguna vez difieren, el valor del objeto `type` gana silenciosamente.

**Resultado Esperado**
Eliminar la asignación redundante en la línea 25 o combinar la lógica en un solo bloque.

**Archivos afectados:**
- `src/main/java/trazzo/back/incidents/infrastructure/adapters/out/persistence/mapper/IncidentMapper.java:25,33`

---

## BUG #18 [LOW]

### Informe de error: `EncryptionService` no valida clave nula antes de decodificar Base64

**Título:** Si la variable de entorno `APP_ENCRYPTION_KEY` no está configurada, el constructor de `EncryptionService` lanza un `NullPointerException` sin mensaje claro

**Precondiciones**
- La aplicación debe ejecutarse sin la variable de entorno `APP_ENCRYPTION_KEY`.

**Pasos para reproducir**
1. Ejecutar la aplicación sin definir `APP_ENCRYPTION_KEY`.
2. Observar un `NullPointerException` en `EncryptionService.java:25` al intentar `Base64.getDecoder().decode(null)`.

**Resultado Actual**
`EncryptionService.java:24-25` recibe `base64Key` como null de Spring y llama a `Base64.getDecoder().decode(null)` sin verificar. En contraste, `JwtService` verifica explícitamente null/blank con un mensaje claro.

**Resultado Esperado**
Agregar una verificación de null/blank antes del decode, similar a `JwtService`.

**Archivos afectados:**
- `src/main/java/trazzo/back/shared/security/EncryptionService.java:24-25`

---

## BUG #19 [LOW]

### Informe de error: `IncidentMapper.toEntity()` incluye evidencias soft-deleted en el mapeo

**Título:** Las evidencias marcadas como eliminadas (soft-delete) se mapean y cascade-persisten junto con las activas

**Precondiciones**
- Una incidencia con evidencias soft-deleted debe ser guardada.

**Pasos para reproducir**
1. Crear una incidencia con 2 evidencias.
2. Eliminar una evidencia (soft-delete).
3. Guardar la incidencia nuevamente.
4. Observar que la evidencia eliminada se mapea y persiste en `IncidentMapper.java:36-41`.

**Resultado Actual**
`IncidentMapper.java:36-41` mapea TODAS las evidencias sin filtrar las eliminadas, creando operaciones de UPDATE innecesarias.

**Resultado Esperado**
Filtrar evidencias con `e.isDeleted()` antes de mapear, o al menos documentar por qué se incluyen.

**Archivos afectados:**
- `src/main/java/trazzo/back/incidents/infrastructure/adapters/out/persistence/mapper/IncidentMapper.java:36-41`

---

## BUG #20 [LOW]

### Informe de error: `IncidentService.create()` no valida la existencia de `tenantUserId`

**Título:** Se pueden crear incidencias con un `tenantUserId` inexistente, generando registros huérfanos

**Precondiciones**
- El usuario debe estar autenticado.

**Pasos para reproducir**
1. Autenticarse en el sistema.
2. Enviar `POST /api/v1/incidentes` con un `tenant_user_id` que no corresponda a un tenant user real (ej: `"99999"`).
3. La incidencia se crea exitosamente sin relación a un usuario válido.

**Resultado Actual**
`IncidentService.java:35-50` crea la incidencia sin verificar que `tenantUserId` corresponda a un tenant user existente. `tenantUserPort` solo se usa para lectura en `toResult()`.

**Resultado Esperado**
Validar la existencia del `tenantUserPort` antes de crear la incidencia.

**Archivos afectados:**
- `src/main/java/trazzo/back/incidents/application/usecase/IncidentService.java:35-50`

---

## BUG #21 [LOW]

### Informe de error: `JwtAuthFilter` registrado como `FilterRegistrationBean` deshabilitado

**Título:** El `JwtAuthFilter` se registra con `FilterRegistrationBean.setEnabled(false)`, creando código muerto confuso

**Precondiciones**
- La aplicación debe iniciarse correctamente.

**Pasos para reproducir**
1. Revisar `SecurityConfig.java:44-48`:
   ```java
   FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(jwtAuthFilter);
   registration.setEnabled(false);
   return registration;
   ```
2. El filtro realmente corre porque se agrega al `SecurityFilterChain` en línea 76.
3. El bean de registro deshabilitado no tiene ningún efecto.

**Resultado Actual**
El `FilterRegistrationBean` deshabilitado es dead code que puede confundir a desarrolladores que revisen la configuración de seguridad.

**Resultado Esperado**
Eliminar el bean `jwtAuthFilterRegistration` ya que no tiene función práctica.

**Archivos afectados:**
- `src/main/java/trazzo/back/shared/security/SecurityConfig.java:44-49`

---

## BUG #22 [LOW]

### Informe de error: Inconsistencia entre soft-delete y hard-delete en servicios de saasglobal

**Título:** `HoldingService` y `PlanService` usan soft-delete pero `FeatureService` usa hard-delete, creando comportamiento inconsistente

**Precondiciones**
- Holdings, planes y features deben existir en el sistema.

**Pasos para reproducir**
1. Eliminar un holding: se ejecuta soft-delete (`holding.delete()` + `holdingRepository.save()`).
2. Eliminar un feature: se ejecuta hard-delete (`featureRepository.deleteById(id)`).
3. El feature se elimina permanentemente mientras el holding solo se marca como eliminado.

**Resultado Actual**
- `HoldingService.java:69-74`: soft-delete (set `deletedAt` + save)
- `PlanService`: soft-delete (mismo patrón)
- `FeatureService.java:48-51`: hard-delete (`deleteById`)

**Resultado Esperado**
Unificar la estrategia de eliminación (soft o hard) para todos los servicios, según los requisitos de negocio.

**Archivos afectados:**
- `src/main/java/trazzo/back/saasglobal/application/usecase/HoldingService.java:69-74`
- `src/main/java/trazzo/back/saasglobal/application/usecase/FeatureService.java:48-51`

---

---

# PARTE 2: Auditoría, Login y Separación Master/Tenant

---

## BUG #23 [CRITICAL]

### Informe de error: Módulo de auditoría completo es infraestructura muerta — nunca escribe datos

**Título:** Las 85+ archivos del módulo de auditoría (entities, services, controllers, repositories, DTOs) existen pero NINGUNO escribe registros en las tablas `audit`, `login_history`, `auditoria_sistema`, `sesion` ni `tenant_settings_record`

**Precondiciones**
- La aplicación debe estar ejecutándose.
- Cualquier operación CRUD debe realizarse en el sistema.

**Pasos para reproducir**
1. Ejecutar cualquier operación de escritura (crear, actualizar, eliminar un rol, incidencia, holding, etc.).
2. Consultar `GET /api/v1/audit/logs` → retorna lista vacía.
3. Consultar `GET /api/v1/audit/login-history` → retorna lista vacía.
4. Consultar `GET /api/v1/audit/system-audit` → retorna lista vacía.
5. Consultar `GET /api/v1/audit/sessions` → retorna lista vacía.
6. Buscar en todo el codebase cualquier llamada a `auditLogService.save()`, `loginHistoryService.record()`, `sessionService.create()`, `systemAuditService.create()` → **cero resultados**.

**Resultado Actual**
Todo el módulo de auditoría es una fachada de solo lectura. Los servicios (`AuditLogService`, `LoginHistoryService`, `SessionService`, `SystemAuditService`, `TenantSettingsService`) solo implementan métodos `findAll()` y `findById()`. No existe ningún interceptor, aspecto, AOP ni controller que invoque métodos de escritura. Las tablas están vacías.

La infraestructura incluye:
- 5 domain models (`Audit`, `LogInHistory`, `TenantSettingsRecord`, `SystemAudit`, `Session`)
- 5 JPA entities (`AuditEntity`, `LogInHistoryEntity`, `TenantSettingsRecordEntity`, `SystemAuditEntity`, `SessionEntity`)
- 5 repository adapters
- 5 use case services
- 6 controllers REST
- Mappers, DTOs, puertos de entrada/salida, excepciones personalizadas

**Resultado Esperado**
Debe existir un mecanismo (AOP aspect, Spring interceptor, o event listener) que registre automáticamente cada operación CRUD en las tablas de auditoría.

**Archivos afectados:**
- `src/main/java/trazzo/back/audit/` (módulo completo — 85+ archivos)
- `src/main/resources/db/migration/V1__init_master_db.sql:324-336` (tabla `audit`)
- `src/main/resources/db/migration/V1__init_master_db.sql:237-245` (tabla `login_history`)
- `src/main/resources/db/tenant/schema.sql:304-319` (tabla `auditoria_sistema`)
- `src/main/resources/db/tenant/schema.sql:288-302` (tabla `sesion`)

---

## BUG #24 [CRITICAL]

### Informe de error: Entidades JPA de tenant apuntan a master_db — no existe routing multi-tenant

**Título:** No existe `AbstractRoutingDataSource`, `TenantContext` ni `TenantInterceptor`. Todas las queries JPA de módulos tenant (incidencias, auditoría, asistencia, etc.) se ejecutan contra `master_db` en lugar de la base de datos del tenant

**Precondiciones**
- La aplicación debe ejecutarse con al menos un tenant provisionado.

**Pasos para reproducir**
1. Provisionar un tenant (esto crea una base de datos separada con `schema.sql`).
2. Crear una incidencia o cualquier entidad de tenant.
3. Observar que los datos se almacenan en `master_db`, no en la base del tenant.
4. Buscar en el codebase: `AbstractRoutingDataSource`, `TenantContext`, `TenantInterceptor`, `TenantFilter`, `getCurrentTenant()` → **cero resultados**.

**Resultado Actual**
El `spring.datasource.url` apunta a `master_db`. No hay mecanismo de routing. Las tablas que JPA espera (como `incidents`, `auditoria_sistema`, `sesion`, `attendances`, etc.) se crean automáticamente en `master_db` gracias a `ddl-auto=update` en local, enmascarando el problema. En producción (sin `ddl-auto`), la aplicación fallaría.

La separación solo existe a nivel de provisioning:
- `TenantSchemaProvisioningAdapter` crea la base del tenant con raw JDBC
- `TenantSchemaMigrator` ejecuta migraciones al startup
- Pero en runtime, **nunca se conecta a la base del tenant**

**Resultado Esperado**
Implementar `AbstractRoutingDataSource` con `TenantContext` (ThreadLocal), un `TenantInterceptor` (o `Filter`) que extraiga el tenant del request header/token, y configurar un `DataSource` por cada tenant conocido.

**Archivos afectados:**
- `src/main/resources/application.properties:11-15` (único DataSource)
- No existen: `TenantContext.java`, `TenantInterceptor.java`, `AbstractRoutingDataSource` en todo el proyecto

---

## BUG #25 [CRITICAL]

### Informe de error: `StatusLogin.SUCCES` tiene typo — no coincide con el enum SQL `status_login_enum`

**Título:** El enum Java `StatusLogin` usa `SUCCES` (sin la S final) pero la tabla SQL define `status_login_enum` con el valor `SUCCESS`

**Precondiciones**
- Se debe intentar registrar un intento de login en la tabla `login_history`.

**Pasos para reproducir**
1. Revisar `StatusLogin.java:4`: `SUCCES` (sin segunda S).
2. Revisar la definición de la tabla `login_history` en la migración V1: el enum SQL es `status_login_enum` con valores incluyendo `SUCCESS`.
3. Si se intentara persistir un registro con `StatusLogin.SUCCESS`, fallaría con `IllegalArgumentException: No enum constant...StatusLogin.SUCCESS`.

**Resultado Actual**
`StatusLogin.java:4` define `SUCCES` en lugar de `SUCCESS`. Aunque actualmente no se usa (BUG #23), cuando se implemente la escritura de historial de login, este typo impedirá persistir registros de login exitoso.

**Resultado Esperado**
Corregir `SUCCES` → `SUCCESS` en `StatusLogin.java:4`.

**Archivos afectados:**
- `src/main/java/trazzo/back/audit/domain/model/master/StatusLogin.java:4`

---

## BUG #26 [CRITICAL]

### Informe de error: No se registran intentos de login en `login_history`

**Título:** El endpoint `POST /auth/login` no registra intentos exitosos ni fallidos en la tabla `login_history`, haciendo imposible auditar accesos

**Precondiciones**
- La aplicación debe ejecutarse con la tabla `login_history` creada en master_db.

**Pasos para reproducir**
1. Iniciar sesión con credenciales válidas → verificar que no se inserta registro en `login_history`.
2. Intentar iniciar sesión con contraseña incorrecta → verificar que no se inserta registro.
3. Intentar con email inexistente → verificar que no se inserta registro.
4. Consultar `GET /api/v1/audit/login-history` → retorna lista vacía.

**Resultado Actual**
`AuthController.java:36-65` ejecuta `authenticationManager.authenticate()` y genera el token JWT, pero nunca invoca `LoginHistoryService` para registrar el intento. La tabla `login_history` y todo su módulo de soporte (`LogInHistoryEntity`, `LogInHistoryRepositoryAdapter`, `LoginHistoryService`, `LoginHistoryController`) son dead infrastructure.

**Resultado Esperado**
El `AuthController` debe registrar cada intento de login (exitoso o fallido) en `login_history` con: usuario, email intentado, estado (SUCCESS/FAILED_WRONG_PASSWORD/etc.), IP del cliente, y User-Agent.

**Archivos afectados:**
- `src/main/java/trazzo/back/saasglobal/infrastructure/adapters/in/web/AuthController.java:36-65`

---

## BUG #27 [HIGH]

### Informe de error: Sin manejo de excepciones para el módulo `saasglobal`

**Título:** No existe `@RestControllerAdvice` para el paquete `trazzo.back.saasglobal`. Excepciones como `TenantProvisioningException` no tienen handler dedicado

**Precondiciones**
- Se debe producir un error en el módulo saasglobal (ej: fallo de provisioning).

**Pasos para reproducir**
1. Intentar provisionar un tenant con credenciales inválidas.
2. `TenantProvisioningException` se lanza.
3. El `GlobalExceptionHandler` de incidents (sin scope) podría capturarla genéricamente, o Spring retorna un 500 sin formato JSON consistente.

**Resultado Actual**
`GlobalExceptionHandler.java:12` usa `@RestControllerAdvice` sin `basePackages`, interceptando todas las excepciones. Pero no tiene catch-all para `Exception.class`. Los handlers de audit (`AuditGlobalExceptionHandler`), organization (`OrgGlobalExceptionHandler`) y corehr (`CoreHrGlobalExceptionHandler`) están scoped correctamente. Saasglobal no tiene ninguno.

Cuando una excepción no manejada se produce en un controller de saasglobal, el handler global de incidents intenta manejarla, pero como no tiene catch-all, Spring genera un 500 con el formato por defecto (HTML).

**Resultado Esperado**
Crear un `SaasGlobalExceptionHandler` con `@RestControllerAdvice("trazzo.back.saasglobal")` que maneje `TenantProvisioningException` y otras excepciones del módulo.

**Archivos afectados:**
- No existe: `src/main/java/trazzo/back/saasglobal/infrastructure/adapters/in/web/SaasGlobalExceptionHandler.java`
- `src/main/java/trazzo/back/incidents/infrastructure/adapters/in/web/GlobalExceptionHandler.java:12` (scope global no intencional)

---

## BUG #28 [HIGH]

### Informe de error: `GlobalExceptionHandler` de incidents tiene scope global sin catch-all

**Título:** El handler de excepciones del módulo incidents usa `@RestControllerAdvice` sin `basePackages`, capturando excepciones de TODOS los módulos pero sin tener un handler para `Exception.class`

**Precondiciones**
- Se debe producir una excepción no manejada en cualquier controller.

**Pasos para reproducir**
1. Producir una excepción `RuntimeException` no capturada en un controller de saasglobal o organization.
2. El `GlobalExceptionHandler` la intercepta (por su scope global).
3. No existe `@ExceptionHandler(Exception.class)`, así que Spring genera un HTML 500 por defecto.

**Resultado Actual**
`GlobalExceptionHandler.java:12` sin `basePackages` actúa como handler global pero solo maneja tipos específicos: `IllegalArgumentException`, `IllegalStateException`, `IncidentValidationException`, etc. Para cualquier excepción no listada, no hay respuesta JSON.

En contraste, `AuditGlobalExceptionHandler.java:67-72` sí tiene un catch-all:
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) { ... }
```

**Resultado Esperado**
Agregar `@ExceptionHandler(Exception.class)` al `GlobalExceptionHandler` como catch-all, O acotar su scope con `@RestControllerAdvice(basePackages = "trazzo.back.incidents")`.

**Archivos afectados:**
- `src/main/java/trazzo/back/incidents/infrastructure/adapters/in/web/GlobalExceptionHandler.java:12-66`

---

## BUG #29 [HIGH]

### Informe de error: Tres de cuatro exception handlers no registran logs de errores

**Título:** Solo `AuditGlobalExceptionHandler` loguea excepciones. Los handlers de incidents, organization y corehr no loguean nada, making debugging imposible en producción

**Precondiciones**
- Se debe producir una excepción manejada en los módulos incidents, organization o corehr.

**Pasos para reproducir**
1. Producir un error en cualquier endpoint de organization (ej: crear rol duplicado).
2. Observar que la respuesta JSON se retorna correctamente, pero no hay nada en los logs de la aplicación.
3. Intentar diagnosticar el problema en producción sin logs es imposible.

**Resultado Actual**
- `AuditGlobalExceptionHandler`: SÍ loguea (`log.warn()`, `log.error()`)
- `GlobalExceptionHandler` (incidents): NO loguea
- `OrgGlobalExceptionHandler`: NO loguea
- `CoreHrGlobalExceptionHandler`: NO loguea

**Resultado Esperado**
Todos los exception handlers deben registrar cada excepción manejada con `log.warn()` o `log.error()` según la severidad.

**Archivos afectados:**
- `src/main/java/trazzo/back/incidents/infrastructure/adapters/in/web/GlobalExceptionHandler.java`
- `src/main/java/trazzo/back/organization/infrastructure/adapters/in/web/OrgGlobalExceptionHandler.java`
- `src/main/java/trazzo/back/corehr/infrastructure/adapters/in/web/CoreHrGlobalExceptionHandler.java`

---

## BUG #30 [MEDIUM]

### Informe de error: `Session.java` tiene typo `lasActivityAt` — debería ser `lastActivityAt`

**Título:** El campo `lasActivityAt` en el modelo de dominio `Session` tiene un typo (falta la 't')

**Precondiciones**
- Se debe crear o consultar una sesión de auditoría.

**Pasos para reproducir**
1. Revisar `Session.java:16`: `private LocalDateTime lasActivityAt;`
2. Revisar `Session.java:65,89`: usa `lasActivityAt` consistentemente.
3. Revisar `SessionResult` y otros DTOs: propagan el typo.
4. Revisar `SessionEntity.java:42-43`: usa `lastActivityAt` (correcto).
5. El mapeo entre dominio y entidad tiene inconsistencia de nombres.

**Resultado Actual**
El modelo de dominio usa `lasActivityAt` mientras la entidad JPA y la tabla SQL usan `lastActivityAt`. Aunque el mapper probablemente los conecta correctamente por posición, el nombre incorrecto causa confusión y es propenso a errores.

**Resultado Esperado**
Renombrar `lasActivityAt` → `lastActivityAt` en `Session.java` y todos los archivos que lo referencian.

**Archivos afectados:**
- `src/main/java/trazzo/back/audit/domain/model/tenant/Session.java:16,65,89`

---

## BUG #31 [MEDIUM]

### Informe de error: `Audit.java` y `AuditEntity.java` tienen typo `ipAdress` — debería ser `ipAddress`

**Título:** El campo `ipAdress` (con una sola 'd') aparece en el modelo de dominio y la entidad de auditoría

**Precondiciones**
- Se debe consultar un registro de auditoría.

**Pasos para reproducir**
1. Revisar `Audit.java:13,47`: `ipAdress`.
2. Revisar `AuditEntity.java:41`: `ipAdress` con `@Column(name = "ip_address")`.
3. El campo Java tiene typo pero la columna SQL está correcta.

**Resultado Actual**
El typo `ipAdress` en el modelo Java causa inconsistencia con la columna `ip_address` de la tabla. Aunque JPA mapea por `@Column(name = "ip_address")`, el nombre del campo en Java es confuso.

**Resultado Esperado**
Renombrar `ipAdress` → `ipAddress` en `Audit.java` y `AuditEntity.java`.

**Archivos afectados:**
- `src/main/java/trazzo/back/audit/domain/model/master/Audit.java:13,47`
- `src/main/java/trazzo/back/audit/infrastructure/adapters/out/persistence/entity/AuditEntity.java:41`

---

## BUG #32 [MEDIUM]

### Informe de error: `HttpMethod.java` define `READ` que no es un método HTTP válido

**Título:** El enum `HttpMethod` incluye `READ` que no es un método HTTP estándar (GET, POST, PUT, PATCH, DELETE)

**Precondiciones**
- Se debe registrar una operación de auditoría con el método HTTP.

**Pasos para reproducir**
1. Revisar `HttpMethod.java:8`: `READ` es el quinto valor del enum.
2. Los métodos HTTP estándar son GET, POST, PUT, PATCH, DELETE.
3. `READ` no mapea a ningún verbo HTTP.

**Resultado Actual**
`HttpMethod.java` define: `GET, PUT, POST, DELETE, READ`. `READ` no es un método HTTP válido. Si se usa para una operación de auditoría, no se puede mapear de vuelta a un verbo HTTP real.

**Resultado Esperado**
Eliminar `READ` del enum, o reemplazarlo con `GET` si se pretende representar operaciones de solo lectura.

**Archivos afectados:**
- `src/main/java/trazzo/back/audit/domain/model/tenant/HttpMethod.java:8`

---

## BUG #33 [MEDIUM]

### Informe de error: Sin protección contra fuerza bruta en el login

**Título:** No existe rate limiting, bloqueo de cuenta ni limitación de intentos. Un atacante puede intentar contraseñas indefinidamente

**Precondiciones**
- Un endpoint de login accesible públicamente.

**Pasos para reproducir**
1. Ejecutar un script que envíe miles de requests a `POST /auth/login` con diferentes contraseñas.
2. Observar que no hay bloqueo,限速, ni delay entre intentos.
3. El enum `StatusLogin` tiene `LOCKED_OUT` pero nunca se usa.

**Resultado Actual**
`AuthController.java:36-65` no tiene ninguna limitación. La tabla `login_history` soporta el campo `status` con valor `LOCKED_OUT` pero nunca se escribe ni se verifica. No hay implementación de:
- Rate limiting por IP o email
- Bloqueo temporal de cuenta
- Delay progresivo entre intentos
- Captcha

**Resultado Esperado**
Implementar al menos rate limiting básico (por IP o por email) y bloqueo temporal de cuenta después de N intentos fallidos.

**Archivos afectados:**
- `src/main/java/trazzo/back/saasglobal/infrastructure/adapters/in/web/AuthController.java:36-65`

---

## BUG #34 [MEDIUM]

### Informe de error: Sin mecanismo de refresh token — expiración de JWT sin renovación

**Título:** El login solo retorna `accessToken` (24h). No existe refresh token. Cuando el token expira, el usuario debe re-autenticarse completamente.

**Precondiciones**
- Un usuario autenticado cuyo token JWT está próximo a expirar.

**Pasos para reproducir**
1. Iniciar sesión → obtener `accessToken` con 24h de vida.
2. Esperar a que expire (o simular).
3. Observar que no hay endpoint `POST /auth/refresh` ni `refresh_token` en la respuesta.
4. La tabla `sesion` tiene columna `refresh_token_hash` pero nunca se usa.

**Resultado Actual**
`LoginResponse.java` solo contiene `accessToken` y `tokenType`. No hay refresh token. La tabla `sesion` tiene la columna preparada pero no hay implementación.

**Resultado Esperado**
Implementar flujo de refresh token: generar un refresh token en el login, almacenar su hash en `sesion.refresh_token_hash`, y proveer endpoint `POST /auth/refresh` para renovar el access token.

**Archivos afectados:**
- `src/main/java/trazzo/back/saasglobal/infrastructure/adapters/in/web/AuthController.java:36-65`
- `src/main/java/trazzo/back/saasglobal/infrastructure/adapters/in/web/dto/LoginResponse.java`

---

## BUG #35 [MEDIUM]

### Informe de error: Sin reseteo de contraseña — tabla `metodo_recuperacion` existe sin implementación

**Título:** La tabla `metodo_recuperacion` está definida en la migración V1 pero no hay código que implemente recuperación de contraseña

**Precondiciones**
- Un usuario que olvidó su contraseña.

**Pasos para reproducir**
1. Revisar `V1__init_master_db.sql:247-255`: tabla `metodo_recuperacion` con campos `user_id`, `token_hash`, `expires_at`, `used`.
2. Buscar en el codebase cualquier referencia a `metodo_recuperacion`, `password reset`, `forgot password`, `recovery` → **cero resultados**.

**Resultado Actual**
La tabla existe en el esquema pero no hay:
- Endpoint para solicitar reseteo
- Endpoint para cambiar contraseña con token
- Servicio de envío de emails con token
- Validación de expiración de token

**Resultado Esperado**
Implementar el flujo completo de recuperación de contraseña o eliminar la tabla si no se planea implementar.

**Archivos afectados:**
- `src/main/resources/db/migration/V1__init_master_db.sql:247-255`

---

## BUG #36 [MEDIUM]

### Informe de error: Sin configuración CORS — frontend en otro origin recibirá bloqueo

**Título:** No existe configuración CORS en toda la aplicación. Si el frontend corre en un dominio/puerto diferente, las requests serán bloqueadas por el navegador

**Precondiciones**
- Frontend ejecutándose en un origen diferente al backend (típico en desarrollo: frontend en `localhost:3000`, backend en `localhost:8080`).

**Pasos para reproducir**
1. Ejecutar el frontend en `localhost:3000`.
2. Ejecutar el backend en `localhost:8080`.
3. Hacer una request desde el frontend al backend.
4. Observar error CORS en la consola del navegador.

**Resultado Actual**
No se encontró ningún archivo con: `@CrossOrigin`, `CorsConfiguration`, `addCorsMappings`, `CorsFilter`, ni configuración CORS en `SecurityConfig`. La única posibilidad es que el frontend use un proxy.

**Resultado Esperado**
Configurar CORS en `SecurityConfig` con los orígenes permitidos para desarrollo y producción.

**Archivos afectados:**
- `src/main/java/trazzo/back/shared/security/SecurityConfig.java`

---

## BUG #37 [LOW]

### Informe de error: `AuthController` carga el usuario dos veces en el login

**Título:** El usuario se carga una vez en `authenticate()` y luego nuevamente en `userRepository.findByEmail()`, causando query redundante

**Precondiciones**
- Un login exitoso.

**Pasos para reproducir**
1. Revisar `AuthController.java:38-39`: `authenticationManager.authenticate()` → internamente llama a `UserDetailsServiceImpl.loadUserByUsername()` que consulta `users` + `user_roles_master`.
2. Revisar `AuthController.java:43-44`: `userRepository.findByEmail()` → consulta `users` de nuevo.
3. Se ejecutan 2 queries al `users` table + 1 query a `user_roles_master` + 1 query a `persons` = 4 queries donde 2-3 serían suficientes.

**Resultado Actual**
`UserDetailsServiceImpl` ya carga el usuario completo (con roles). Luego `AuthController` lo vuelve a cargar por separado. El objeto `User` del `userRepository` tiene los roles como campo pero ya se obtuvieron en el `authenticate()`.

**Resultado Esperado**
Reutilizar la información del usuario autenticado en lugar de hacer una segunda query.

**Archivos afectados:**
- `src/main/java/trazzo/back/saasglobal/infrastructure/adapters/in/web/AuthController.java:38-44`

---

## Resumen Final

| Severidad | Cantidad | Áreas principales |
|-----------|----------|-------------------|
| **CRITICAL** | 7 | Auditoría muerta, sin routing multi-tenant, typo en StatusLogin, login history vacío, tablas JPA/SQL, prefijo URL, StorageException |
| **HIGH** | 5 | Sin handler saasglobal, GlobalExceptionHandler sin catch-all, 3 handlers sin logging, filtros ignorados, sin @Transactional |
| **MEDIUM** | 14 | Sin rate limiting, sin refresh token, sin password reset, sin CORS, typos en domain, enum inválido, stubs vacíos, config obsoleta |
| **LOW** | 7 | Query redundante, dead code, mapeos redundantes, inconsistencias delete |
| **TOTAL** | **33** | |
