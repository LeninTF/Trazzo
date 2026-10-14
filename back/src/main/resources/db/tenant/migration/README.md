# Migraciones de schema de tenant

Se aplican con Flyway a cada schema de tenant (`TenantFlywayMigrator`), al arrancar la app
(`TenantSchemaMigrator`) y al provisionar un tenant nuevo (justo despues de
`db/tenant/V1__tenant_db.sql`). El historial vive en la tabla `flyway_tenant_history` de cada schema.

## Reglas

- **Inmutables tras el merge.** Flyway guarda el checksum de cada script y rechaza arrancar si cambia
  uno ya aplicado. Para corregir algo, crea una migracion nueva; no edites una existente.
- **Siguiente version libre: `V5`.** Hoy existen `V2`, `V3`, `V3_1` y `V4`. No repitas un numero de
  version (Flyway falla con dos `V3`): para intercalar una migracion usa un numero con punto, como `V3_1`.
- Nombre: `V<version>__<descripcion_en_snake_case>.sql`.
- Los scripts corren con `search_path` apuntando al schema del tenant: usa nombres sin calificar
  y, en consultas a `information_schema`, filtra con `table_schema = current_schema()`.
  Sin ese filtro la consulta ve las tablas de **todos** los tenants y la guarda decide mal.
- Cada script corre en una transaccion; si falla, no deja cambios a medias en ese tenant.
- Escribelos idempotentes igualmente: los schemas anteriores a la tabla de historial se
  "baselinean" en la version 0 y reciben todos los scripts pendientes.
- No uses `${...}` en el SQL: Flyway lo interpreta como placeholder.

## Fallos

`TENANT_MIGRATIONS_FAIL_ON_ERROR` (`trazzo.tenant-migrations.fail-on-error`):

- `false` (default, produccion): el error se registra con su stack trace, el resto de tenants se
  migra y la app arranca. El indicador de salud `tenantMigrations` queda en `OUT_OF_SERVICE`
  hasta el siguiente arranque correcto.
- `true` (local y tests): el primer fallo aborta el arranque.
