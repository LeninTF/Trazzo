#!/bin/sh
# Idempotente: lo ejecutan tanto docker-entrypoint-initdb.d (primer arranque, por socket)
# como el servicio one-shot trazzo-db-bootstrap (cada `up`, por TCP con PGHOST/PGPASSWORD).
set -eu

APP_DB_NAME="${APP_DB_NAME:-master_db}"
APP_DB_USER="${APP_DB_USER:-trazzo_app}"
APP_DB_PASSWORD="${APP_DB_PASSWORD:?APP_DB_PASSWORD is required}"

# Con PGHOST (bootstrap) se conecta por TCP; sin el (initdb) psql usa el socket local.
set -- -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres
if [ -n "${PGHOST:-}" ]; then
    set -- -h "$PGHOST" "$@"
fi

psql "$@" <<EOSQL
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '${APP_DB_USER}') THEN
        CREATE ROLE ${APP_DB_USER} LOGIN PASSWORD '${APP_DB_PASSWORD}';
    ELSE
        ALTER ROLE ${APP_DB_USER} WITH LOGIN PASSWORD '${APP_DB_PASSWORD}';
    END IF;
END
\$\$;

SELECT 'CREATE DATABASE ${APP_DB_NAME} OWNER ${APP_DB_USER}'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${APP_DB_NAME}')\gexec

GRANT ALL PRIVILEGES ON DATABASE ${APP_DB_NAME} TO ${APP_DB_USER};
ALTER DATABASE ${APP_DB_NAME} SET timezone TO 'UTC';
EOSQL
