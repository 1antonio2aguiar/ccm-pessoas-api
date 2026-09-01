# ==================================================
# ETAPA 1 - COMPILAÇÃO
# ==================================================

FROM maven:3.9.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copia primeiro o pom para aproveitar o cache
# das dependências do Maven.
COPY pom.xml .

RUN mvn dependency:go-offline -B

# Copia o código-fonte e gera o JAR.
COPY src ./src

RUN mvn clean package -DskipTests -B \
    && cp target/*.jar app.jar


# ==================================================
# ETAPA 2 - EXECUÇÃO
# ==================================================

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Cria um usuário sem privilégios administrativos.
RUN addgroup -S spring \
    && adduser -S spring -G spring

COPY --from=build \
     --chown=spring:spring \
     /app/app.jar \
     app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]