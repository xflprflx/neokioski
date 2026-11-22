# STAGE 1: build
FROM eclipse-temurin:25-jdk AS build
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
# "-DskipTests": pula os testes no build da imagem (já validados pelo job 'unit-tests' da CI/CD).
RUN mvn clean package -Pproduction -DskipTests -B

# STAGE 2: runtime
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
ARG APP_VERSION=dev
ENV APP_VERSION=${APP_VERSION}
ENV TZ=America/Sao_Paulo
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=America/Sao_Paulo", "-jar", "app.jar", "--spring.profiles.active=prod"]