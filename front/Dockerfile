# Etapa 1: Compilación (Builder)
FROM node:24-alpine AS build
WORKDIR /app

# 1. Habilitar pnpm via corepack
RUN corepack enable && corepack prepare pnpm@11.1.2 --activate

# 2. Copiar los archivos de dependencias primero para aprovechar la caché de capas de Docker
COPY package.json pnpm-lock.yaml ./

# 3. Instalar dependencias de forma limpia
RUN pnpm install --frozen-lockfile

# 4. Copiar el resto del código fuente del frontend
COPY . .

# 5. Compilar la aplicación Angular para producción
RUN pnpm run build --configuration=production

# Etapa 2: Servidor Web Ligero (Runtime)
FROM nginx:alpine

# 5. Eliminar los archivos estáticos por defecto de Nginx
RUN rm -rf /usr/share/nginx/html/*

# 6. Copiar los archivos compilados desde la etapa de build
COPY --from=build /app/dist/front/browser /usr/share/nginx/html

# 7. Copiar la configuración personalizada de Nginx para soportar el enrutamiento de la SPA
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Exponer el puerto 80
EXPOSE 80

# Iniciar Nginx
ENTRYPOINT ["nginx", "-g", "daemon off;"]
