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
# Spring Boot 3.3+ creates a thin-jar + lib/ structure (not separate spring-boot-loader layer)
RUN java -Djarmode=tools -jar /build/target/*.jar extract --layers --destination .

# Etapa 2: Imagen de Ejecución Ligera (Runtime)
FROM eclipse-temurin:21-jre
WORKDIR /app

# 6. Crear y usar un usuario sin privilegios (Seguridad)
RUN groupadd -r trazzo && useradd -r -g trazzo trazzo

# 7. Copiar capas: la layer "application" contiene thin-jar + lib/ de dependencias
COPY --from=builder /build/extracted/dependencies/ ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/ ./

# Cambiar al usuario sin privilegios
USER trazzo

# Exponer el puerto estándar del backend
EXPOSE 8080

# 8. Spring Boot 3.3+ thin-jar con lib/ adyacente; el loader está embebido en el jar
ENTRYPOINT ["java", "-jar", "back-0.0.1-SNAPSHOT.jar"]