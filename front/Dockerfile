# Etapa 1: Compilación (Builder)
FROM node:24-alpine AS build
WORKDIR /app

# Habilitar pnpm via corepack
RUN corepack enable pnpm

# 1. Copiar los archivos de dependencias primero para aprovechar la caché de capas de Docker
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml .npmrc ./

# 2. Instalar dependencias de forma limpia
RUN pnpm install --frozen-lockfile --ignore-scripts

# 2.1 Reconstruir explícitamente solo los binarios nativos requeridos por Angular
RUN pnpm rebuild esbuild @parcel/watcher lmdb msgpackr-extract

# 3. Copiar el resto del código fuente del frontend
COPY . .

# 4 . Compilar la aplicación Angular para producción
RUN pnpm run build --configuration=production

# Etapa 2: Servidor Web Ligero (Runtime)
FROM nginx:alpine

# 6. Eliminar los archivos estáticos por defecto de Nginx
RUN rm -rf /usr/share/nginx/html/*

# 7. Copiar los archivos compilados desde la etapa de build
COPY --from=build /app/dist/front/browser /usr/share/nginx/html

# 8. Copiar la configuración personalizada de Nginx para soportar el enrutamiento de la SPA
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 9. Crear directorios de caché con permisos para el usuario no-root
RUN mkdir -p /var/cache/nginx/client_temp \
             /var/cache/nginx/proxy_temp \
             /var/cache/nginx/fastcgi_temp \
             /var/cache/nginx/uwsgi_temp \
             /var/cache/nginx/scgi_temp && \
    chown -R nginx:nginx /var/cache/nginx && \
    chmod -R 755 /var/cache/nginx && \
    mkdir -p /var/run/nginx /var/log/nginx && \
    chown -R nginx:nginx /var/run/nginx /var/log/nginx

# 10. Eliminar la directiva "user" del nginx.conf por defecto para evitar warning al correr como no-root
RUN sed -i '/^user /s/^/#/' /etc/nginx/nginx.conf

# Cambiar a usuario no-root para seguridad
USER nginx

# Exponer el puerto 80
EXPOSE 80

# Iniciar Nginx
ENTRYPOINT ["nginx", "-g", "daemon off;"]
