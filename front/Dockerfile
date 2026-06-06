# Etapa 1: Compilación (Builder)
FROM node:24-alpine AS build
WORKDIR /app

# Habilitar pnpm via corepack
RUN corepack enable pnpm

# 1. Copiar los archivos de dependencias primero para aprovechar la caché de capas de Docker
COPY package.json pnpm-lock.yaml .npmrc ./

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

# Exponer el puerto 80
EXPOSE 80

# Iniciar Nginx
ENTRYPOINT ["nginx", "-g", "daemon off;"]
