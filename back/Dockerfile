# Etapa 1: Compilación y Extracción (Builder)
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build

# 1. Copiar primero el wrapper de Maven y el POM
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Asegurar permisos de ejecución para el wrapper
RUN chmod +x ./mvnw

# 2. Descargar las dependencias (optimización de caché de Docker)
RUN ./mvnw dependency:go-offline

# 3. Copiar el código fuente del proyecto
COPY src/ ./src/

# 4. Compilar y empaquetar la aplicación omitiendo los tests
RUN ./mvnw clean package -DskipTests

# 5. Extraer el JAR en capas (Layered JAR) usando jarmode (Spring Boot 3.3+)
WORKDIR /build/extracted
RUN java -Djarmode=tools -jar /build/target/*.jar extract --launcher --layers --destination .

# Etapa 2: Imagen de Ejecución Ligera (Runtime)
FROM eclipse-temurin:21-jre
WORKDIR /app

# 6. Crear y usar un usuario sin privilegios (Seguridad)
RUN groupadd -r trazzo && useradd -r -g trazzo trazzo

# 7. Copiar las capas extraídas (ordenado por frecuencia de cambio)
COPY --from=builder /build/extracted/dependencies/ ./
COPY --from=builder /build/extracted/spring-boot-loader/ ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/ ./

# Cambiar al usuario sin privilegios
USER trazzo

# Exponer el puerto estándar del backend
EXPOSE 8080

# 8. Punto de entrada utilizando el JarLauncher optimizado
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]