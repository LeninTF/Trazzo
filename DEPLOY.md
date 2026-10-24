# Despliegue y operación de Trazzo

Runbook para levantar Trazzo en local y en producción, generar sus secretos, exponerlo, dar de alta los
lectores biométricos y rotar claves. Los comandos son para bash (en Windows, Git Bash) salvo donde se indica
PowerShell. En Git Bash, los comandos que pasan rutas del contenedor como argumento (`docker run -v …:/keys`,
`/out/…`) necesitan `MSYS_NO_PATHCONV=1` delante: si no, Git Bash las convierte en rutas de Windows.

## 1. Qué se despliega

| Servicio (`docker-compose.yml`) | Qué es | Puertos |
|---|---|---|
| `trazzo-db` | PostgreSQL 15 (master + un schema por tenant) | ninguno publicado |
| `trazzo-db-bootstrap` | one-shot: reaplica rol, BD y grants en cada `up` | — |
| `trazzo-db-ssh` | túnel SSH a la BD (solo clave pública) | `2222` en el host |
| `trazzo-backend` | Spring Boot; actuator en `8081` interno | ninguno publicado |
| `trazzo-frontend` | nginx con el front; proxy de `/api/` al backend | ninguno publicado |
| `cloudflared` | Cloudflare Tunnel, perfil `tunnel` | — |

En producción nada web queda publicado en el host: la entrada es el túnel (`cloudflared` → `trazzo-frontend:8080`) o
un proxy propio hacia ese mismo destino. El backend solo se alcanza a través de `/api/` del front.

Las imágenes `ghcr.io/lenintf/trazzo/trazzo-back` y `trazzo-front` las publica CI al pasar el CI de `main`
(`.github/workflows/publish-docker.yml`) con dos tags: `main` (la última) y `sha-<7>` (cada commit).

## 2. Variables y secretos

Las plantillas listan cada variable con `obligatoria|opcional - consumidor - formato`:

- producción: `.env.example` → copiar a `.env` (no se versiona);
- local: `.env.local.example` → copiar a `.env.local` (no se versiona).

Las opcionales se dejan **comentadas**: una variable definida vacía (`VAR=`) llega vacía al backend y anula el
default de `application.properties`. El backend recibe solo las variables que lista su `environment:` en el
compose; añadir una nueva al backend implica añadirla también ahí.

### Generar los secretos

```bash
openssl rand -base64 32   # APP_JWT_SECRET (HMAC-256, >= 32 bytes)
openssl rand -base64 32   # APP_ENCRYPTION_KEY (AES-256, exactamente 32 bytes)
openssl rand -base64 32   # APP_BIOMETRIC_STORAGE_KEY (opcional; vacía = APP_ENCRYPTION_KEY)
openssl rand -base64 24   # POSTGRES_SUPERUSER_PASSWORD y APP_DB_PASSWORD (una distinta para cada una)
```

La **clave RSA biométrica** (transporte de huellas desde los lectores) no hace falta generarla: el compose fija
`APP_BIOMETRIC_RSA_KEY_FILE=/app/keys/biometric-rsa.pem` y el backend la crea en el volumen
`trazzo-backend-keys` la primera vez. Si se prefiere inyectarla (p. ej. desde un gestor de secretos), una clave
nueva en una línea Base64 PKCS#8 para `APP_BIOMETRIC_RSA_PRIVATE_KEY`, que tiene prioridad sobre el archivo:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out biometric-rsa.pem
grep -v '^-----' biometric-rsa.pem | tr -d '\n'   # valor para APP_BIOMETRIC_RSA_PRIVATE_KEY
```

### Lo que impide arrancar en producción

Fuera del perfil `local`, `StartupConfigValidator` detiene el backend si falta alguno de estos (en `local` solo
avisa):

- `APP_JWT_SECRET`;
- `FRONTEND_URL` con `https://`;
- con `MERCADOPAGO_REQUIRED` en `true` (el default): `MERCADOPAGO_ACCESS_TOKEN` y `MERCADOPAGO_WEBHOOK_SECRET`;
- `MERCADOPAGO_USE_SANDBOX` distinto de `true` y `MERCADOPAGO_BACK_URL` (default: `FRONTEND_URL`) con `https://`;
- una clave RSA (`APP_BIOMETRIC_RSA_PRIVATE_KEY` o el archivo, que el compose ya fija);
- una clave de almacenamiento (`APP_BIOMETRIC_STORAGE_KEY` o, en su defecto, `APP_ENCRYPTION_KEY`).

## 3. Local

```bash
cp .env.local.example .env.local      # completar las obligatorias
docker compose --env-file .env.local -f docker-compose.local.yml up -d --build
docker compose --env-file .env.local -f docker-compose.local.yml ps   # postgres-local y backend "healthy"
```

Frontend en <http://localhost:4000>, backend en <http://localhost:8080/api/v1>, Postgres en `localhost:5433`.
El perfil `local` siembra el admin SaaS y el tenant demo con los `TRAZZO_SEED_*` de `.env.local`.

Solo una BD para desarrollar el backend fuera de Docker: `docker compose -f docker-compose.dev.yml up -d`
(Postgres en `localhost:5434`, usuario `trazzo_dev`).

## 4. Producción

### Primera vez

```bash
cp .env.example .env                  # completar las obligatorias (sección 2)
docker compose pull
docker compose up -d                  # sin túnel; con túnel: sección 5
docker compose ps                     # trazzo-db y trazzo-backend "healthy", bootstrap "exited (0)"
docker compose exec trazzo-backend curl -fsS http://localhost:8081/actuator/health
```

En producción no hay seeders: el **primer admin SaaS** se crea a mano, una sola vez, como superusuario de
Postgres. `\prompt` lee los valores sin dejarlos en la línea de comandos ni en el historial (la contraseña sí se
ve en pantalla al escribirla):

```bash
docker compose exec trazzo-db sh -c 'psql -U "$POSTGRES_USER" -d "$APP_DB_NAME"'
```

```sql
\prompt 'DNI: ' dni
\prompt 'Nombre: ' nombre
\prompt 'Apellido paterno: ' apellido_paterno
\prompt 'Apellido materno: ' apellido_materno
\prompt 'Email: ' email
\prompt 'Contrasena: ' pwd
CREATE EXTENSION IF NOT EXISTS pgcrypto;
WITH persona AS (
  INSERT INTO persons (document_type, document_value, name, father_surname, mother_surname)
  VALUES ('DNI', :'dni', :'nombre', :'apellido_paterno', :'apellido_materno')
  RETURNING id
), usuario AS (
  INSERT INTO users (person_id, email, password)
  SELECT persona.id, :'email', crypt(:'pwd', gen_salt('bf', 12)) FROM persona
  RETURNING id
)
INSERT INTO user_roles_master (user_id, roles_master_id)
SELECT usuario.id, r.id FROM usuario JOIN roles_master r ON r.name = 'admin_trazzo';
```

Ese admin entra por la web y crea los tenants desde el panel SaaS.

### Actualizar y volver atrás

`docker compose pull && docker compose up -d` trae la última `main`. Para fijar una versión concreta (o volver a
una anterior) se usa un `docker-compose.override.yml` en el servidor, que compose combina solo y git ignora:

```yaml
services:
  trazzo-backend:
    image: ghcr.io/lenintf/trazzo/trazzo-back:sha-1a2b3c4
  trazzo-frontend:
    image: ghcr.io/lenintf/trazzo/trazzo-front:sha-1a2b3c4
```

Las migraciones de Flyway solo avanzan: volver a una imagen anterior a una migración ya aplicada exige restaurar
el respaldo de la BD de antes de actualizar (sección 8).

## 5. Exposición

**Cloudflare Tunnel.** En Cloudflare Zero Trust se crea un túnel, se copia su token a `TUNNEL_TOKEN` en `.env` y
se le añade un *public hostname* hacia `http://trazzo-frontend:8080`. Se levanta con el perfil:

```bash
docker compose --profile tunnel up -d
```

`FRONTEND_URL` y `MERCADOPAGO_BACK_URL` tienen que ser ese hostname con `https://`.

**SSH a la BD.** `trazzo-db-ssh` publica `2222` con autenticación solo por clave (`SSH_USERNAME`,
`SSH_PUBLIC_KEY`). Conviene limitar ese puerto en el firewall del host. Para llegar a Postgres desde un equipo:

```bash
ssh -p 2222 -N -L 15432:trazzo-db:5432 "$SSH_USERNAME@<host>"
psql -h localhost -p 15432 -U <APP_DB_USER> -d <APP_DB_NAME>
```

## 6. Lectores biométricos

1. En el panel del tenant, **Dispositivos** → nuevo dispositivo: código del lector (`A-Z a-z 0-9 . _ -`), nombre y
   sede.
2. **Emitir token**: el token `trzd_…` aparece **una sola vez**, junto con el comando de configuración y la huella
   SHA-256 de la clave pública del backend. El backend solo guarda su SHA-256; el token queda atado al tenant y a
   ese código de lector, y solo abre los endpoints del agente.
3. En la PC del lector se instala `Trazzo.Biometric.Agent.msi` (cómo generarlo: `middleware/README.md`) y, en
   PowerShell como administrador, en `C:\Program Files\Trazzo\BiometricAgent\`, se ejecuta el comando copiado del
   panel. Pide el token con `Read-Host -AsSecureString` y lo guarda cifrado con DPAPI. Si el front no está en
   `https://trazzosaas.noahtechperu.com`, se añade `-AllowedOrigins "https://<dominio-del-front>"`.
4. Comprobación en esa PC: `http://localhost:9001/` responde `{"type":"health.check.result","success":true}`.

**Revocar**: en Dispositivos, *Revocar token*; el agente recibe 401 al instante. Volver a emitir reemplaza el
token anterior (hay que repetir el paso 3 con el nuevo). Borrar el dispositivo también revoca su token.

## 7. Rotación de claves

| Clave | Qué pasa al cambiarla | Cómo |
|---|---|---|
| `APP_JWT_SECRET` | todas las sesiones caducan (el JWT dura 24 h) | cambiarla en `.env` y `docker compose up -d trazzo-backend` |
| `APP_BIOMETRIC_STORAGE_KEY` | **las huellas enroladas dejan de abrirse** | no hay rotación: cambiarla implica volver a enrolar a todos |
| `APP_ENCRYPTION_KEY` | si no hay `APP_BIOMETRIC_STORAGE_KEY`, lo mismo que la anterior | antes de cambiarla, poner su valor actual en `APP_BIOMETRIC_STORAGE_KEY` |
| clave RSA de transporte | los lectores dejan de aceptar la clave nueva si no la tienen fijada | procedimiento de abajo |
| token de un lector | ese lector deja de autenticar | *Emitir token* de nuevo (sección 6) |
| `APP_DB_PASSWORD` | — | cambiarla en `.env` y `docker compose up -d`: el bootstrap aplica la nueva al rol y el backend se recrea con ella |
| `POSTGRES_SUPERUSER_PASSWORD` | solo se aplica al crear el volumen | `ALTER USER <POSTGRES_USER> PASSWORD '...'` como superusuario y luego actualizar `.env` |
| R2, Mercado Pago, Resend | — | regenerar en su consola, actualizar `.env` y `docker compose up -d trazzo-backend` |

**Clave RSA de transporte.** Las huellas guardadas no dependen de ella (se sellan con la clave de almacenamiento):
solo cifra lo que los lectores envían. Los lectores fijan la huella SHA-256 de la clave pública
(`Security:PinnedPublicKeySha256`, una lista) y la refrescan del backend cada 24 h. Sin cortar el servicio:

1. Generar la clave nueva (sección 2) y calcular su huella:
   `openssl pkey -in biometric-rsa.pem -pubout -outform DER | openssl dgst -sha256 -r`.
2. En cada lector, emitir un token nuevo y ejecutar el comando del panel añadiendo las dos huellas:
   `-PinnedPublicKeySha256 "<huella actual>,<huella nueva>"`.
3. Pasar la clave actual a `APP_BIOMETRIC_RSA_PREVIOUS_KEYS` (Base64 PKCS#8; varias separadas por comas). Si está
   en el volumen: `docker compose exec trazzo-backend sh -c "grep -v '^-----' /app/keys/biometric-rsa.pem | tr -d '\n'"`.
4. Poner la nueva en `APP_BIOMETRIC_RSA_PRIVATE_KEY` y `docker compose up -d trazzo-backend`.
5. Pasadas al menos 24 h y sin eventos offline pendientes en los lectores, quitar la anterior de
   `APP_BIOMETRIC_RSA_PREVIOUS_KEYS` y de las huellas fijadas.

## 8. Respaldo y restauración

Hay que respaldar juntos:

- la BD: `docker compose exec trazzo-db sh -c 'pg_dump -U "$POSTGRES_USER" -Fc "$APP_DB_NAME"' > trazzo-$(date +%F).dump`;
- el volumen `trazzo-backend-keys` (la clave RSA): `docker run --rm -v trazzo_trazzo-backend-keys:/keys -v "$PWD":/out alpine tar czf /out/keys-$(date +%F).tgz -C /keys .`
  (el nombre real del volumen lo da `docker volume ls`);
- `.env`, en un gestor de secretos y nunca en git.

Restaurar la BD: `docker compose exec -T trazzo-db sh -c 'pg_restore -U "$POSTGRES_USER" -d "$APP_DB_NAME" --clean --if-exists' < trazzo-AAAA-MM-DD.dump`.

## 9. Diagnóstico rápido

- `docker compose ps` y `docker compose logs --tail 100 trazzo-backend`: los logs están acotados a 3 × 10 MB por
  servicio.
- `Invalid configuration, refusing to start: …` en el log del backend: falta algo de la lista de la sección 2.
- Un admin SaaS recibe 403 en una pantalla de tenant: es correcto; las rutas de tenant exigen un usuario del
  tenant.
- Un lector no conecta: `http://localhost:9001/` en su PC, token vigente en Dispositivos y huella fijada igual a la
  que muestra el panel.
