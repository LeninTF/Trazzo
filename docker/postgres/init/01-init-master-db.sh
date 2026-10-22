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

# Las credenciales viajan como variables de psql y format() las cita (%I identificador,
# %L literal): una contrasena con comillas, $ o espacios no rompe el script ni inyecta SQL.
# El heredoc va entre comillas para que el shell no expanda nada dentro del SQL.
psql "$@" -v app_db="$APP_DB_NAME" -v app_user="$APP_DB_USER" -v app_password="$APP_DB_PASSWORD" <<'EOSQL'
SELECT format('CREATE ROLE %I LOGIN', :'app_user')
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = :'app_user')\gexec

SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'app_user', :'app_password')\gexec

SELECT format('CREATE DATABASE %I OWNER %I', :'app_db', :'app_user')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'app_db')\gexec

SELECT format('GRANT ALL PRIVILEGES ON DATABASE %I TO %I', :'app_db', :'app_user')\gexec
SELECT format('ALTER DATABASE %I SET timezone TO %L', :'app_db', 'UTC')\gexec
EOSQL
