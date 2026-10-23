#!/usr/bin/env bash
# Prueba de docker/postgres/init/01-init-master-db.sh (uso: bash docker/postgres/test-init-script.sh) contra postgres:15-alpine real:
#  1) via docker-entrypoint-initdb.d (socket), con una contrasena con ' " $ y espacio
#  2) re-ejecucion por TCP con PGHOST (como trazzo-db-bootstrap): debe ser idempotente
#  3) el rol de la app conecta con esa contrasena y es dueno de la BD; timezone UTC
set -uo pipefail
ROOT=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
NAME=trazzo-init-test-$$
PASS="p'a\$s\"w d"
fail() { echo "FAIL: $*"; docker rm -f "$NAME" >/dev/null 2>&1; exit 1; }

MSYS_NO_PATHCONV=1 docker run -d --name "$NAME" \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=superpw -e POSTGRES_DB=postgres \
  -e APP_DB_NAME=trazzo_it -e APP_DB_USER=trazzo_it_app -e "APP_DB_PASSWORD=$PASS" \
  -v "$ROOT/docker/postgres/init:/docker-entrypoint-initdb.d:ro" \
  postgres:15-alpine >/dev/null || fail "no arranca el contenedor"

# Espera a que el init termine: el servidor definitivo escucha por TCP (el temporal no).
ready=0
for _ in $(seq 1 600); do
  if docker exec "$NAME" pg_isready -h 127.0.0.1 -U postgres -q 2>/dev/null; then ready=1; break; fi
  if [ "$(docker inspect -f '{{.State.Running}}' "$NAME")" != "true" ]; then break; fi
done
[ "$ready" = 1 ] || { docker logs "$NAME" 2>&1 | tail -15; fail "postgres no quedo listo (init fallido)"; }

login() { docker exec -e "PGPASSWORD=$PASS" "$NAME" psql -h 127.0.0.1 -U trazzo_it_app -d trazzo_it -tAc "$1"; }
[ "$(login 'select current_user')" = "trazzo_it_app" ] || fail "el rol de la app no conecta con la contrasena especial"
[ "$(login "select pg_get_userbyid(datdba) from pg_database where datname='trazzo_it'")" = "trazzo_it_app" ] || fail "la BD no pertenece al rol de la app"
[ "$(login 'show timezone')" = "UTC" ] || fail "timezone de la BD distinto de UTC"

# Re-ejecucion por TCP (bootstrap) con la misma contrasena: idempotente. Un shim de psql
# delante del PATH registra su argv, para comprobar que la contrasena nunca va en la linea
# de comandos (visible en ps / /proc/*/cmdline mientras corre).
SHIM_DIR=$(mktemp -d)
printf '#!/bin/sh\nprintf "%%s\\n" "$*" >> /tmp/psql-argv.log\nexec /usr/local/bin/psql "$@"\n' > "$SHIM_DIR/psql"
MSYS_NO_PATHCONV=1 docker exec "$NAME" mkdir -p /tmp/shim
docker cp "$SHIM_DIR/psql" "$NAME:/tmp/shim/psql" >/dev/null && rm -rf "$SHIM_DIR"
MSYS_NO_PATHCONV=1 docker exec "$NAME" chmod +x /tmp/shim/psql
MSYS_NO_PATHCONV=1 docker exec -e PGHOST=127.0.0.1 -e PGPASSWORD=superpw -e POSTGRES_USER=postgres \
  -e APP_DB_NAME=trazzo_it -e APP_DB_USER=trazzo_it_app -e "APP_DB_PASSWORD=$PASS" \
  -e PATH=/tmp/shim:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
  "$NAME" sh /docker-entrypoint-initdb.d/01-init-master-db.sh >/dev/null 2>&1 || fail "la re-ejecucion por TCP falla"
[ "$(login 'select 1')" = "1" ] || fail "tras re-ejecutar, el login falla"
ARGV=$(MSYS_NO_PATHCONV=1 docker exec "$NAME" cat /tmp/psql-argv.log)
[ -n "$ARGV" ] || fail "el shim de psql no se ejecuto"
case "$ARGV" in *"$PASS"*) fail "la contrasena de la app aparece en el argv de psql";; esac

docker rm -f "$NAME" >/dev/null
echo "PASS: init script escapa credenciales, es idempotente y fija UTC"
