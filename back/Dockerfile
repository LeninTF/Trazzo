# Etapa 1: Compilación y Extracción (Builder)
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build

# 1. Copiar primero el wrapper de Maven y el POM
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Asegurar permisos de ejecución y eliminar CRLF (checkout Windows)
RUN sed -i 's/\r$//' mvnw && chmod +x ./mvnw

# 2. Descargar las dependencias (optimización de caché de Docker)
RUN ./mvnw dependency:go-offline

# 3. Copiar el código fuente del proyecto
COPY src/ ./src/

# 4. Compilar y empaquetar la aplicación omitiendo los tests
RUN ./mvnw clean package -DskipTests

# 5. Extraer el JAR en capas (Layered JAR) usando jarmode (Spring Boot 3.3+)
WORKDIR /build/extracted
# Sin --launcher, "extract --layers" reescribe el jar de application/ como un jar
# delgado: Main-Class apunta directo a la app y Class-Path referencia "lib/*.jar"
# de forma relativa (no usa BOOT-INF ni el loader de Spring Boot). Por eso no se
# copia la capa spring-boot-loader/: queda vacía en este modo, no hace falta.
RUN java -Djarmode=tools -jar /build/target/app.jar extract --layers --destination .

# Etapa 2: Imagen de Ejecución Ligera (Runtime)
FROM eclipse-temurin:21-jre
WORKDIR /app

# 6. Crear y usar un usuario sin privilegios (Seguridad)
RUN groupadd -r trazzo && useradd -r -g trazzo trazzo

# 7. Copiar capas: dependencies/ trae lib/*.jar, application/ trae el jar delgado (app.jar)
COPY --from=builder /build/extracted/dependencies/ ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/ ./

# Cambiar al usuario sin privilegios
USER trazzo

# Exponer el puerto estándar del backend
EXPOSE 8080

# 8. finalName=app fijado en pom.xml: el nombre no depende de project.version,
# así un bump de versión no puede romper este ENTRYPOINT silenciosamente.
ENTRYPOINT ["java", "-jar", "app.jar"]