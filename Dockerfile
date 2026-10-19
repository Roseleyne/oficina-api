FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests


FROM eclipse-temurin:21-jre

WORKDIR /app

# Usuario sem privilegios (boa pratica de seguranca em containers)
RUN groupadd --system app && useradd --system --gid app --no-create-home app

COPY --from=build /app/target/oficina-api-0.0.1-SNAPSHOT.jar app.jar

USER app

EXPOSE 8080

# Respeita os limites de memoria do container (Kubernetes resources.limits)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]