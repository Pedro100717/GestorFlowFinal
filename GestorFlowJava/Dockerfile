# ESTÁGIO 1: Compilação (O Operário)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
# Copia o pom e saca as dependências (isto usa cache para ser mais rápido)
COPY pom.xml .
RUN mvn dependency:go-offline
# Copia o código e compila ignorando os testes (para ser rápido no deploy)
COPY src ./src
RUN mvn clean package -DskipTests

# ESTÁGIO 2: Produção (O Servidor Leve)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copia APENAS o ficheiro .jar gerado no estágio anterior
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]