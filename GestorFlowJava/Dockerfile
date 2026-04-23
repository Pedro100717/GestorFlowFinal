# ESTÁGIO 1: Compilação (O Operário)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
# Copia o pom e saca as dependências (isto usa cache para ser mais rápido)
COPY pom.xml .
RUN mvn dependency:go-offline
# Copia o código e compila ignorando os testes (para ser rápido no deploy)
COPY src ./src
RUN mvn clean package -DskipTests

# ESTÁGIO 2: Produção (O Servidor Leve e Seguro)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 🛡️ FASE 4: Criar um utilizador e grupo sem privilégios de root
RUN addgroup -S gestorflow_group && adduser -S gestorflow_user -G gestorflow_group

# Copia APENAS o ficheiro .jar gerado no estágio anterior e dá a posse ao novo utilizador
COPY --chown=gestorflow_user:gestorflow_group --from=build /app/target/*.jar app.jar

# 🛡️ FASE 4: Mudar para o utilizador seguro antes de arrancar a app
USER gestorflow_user

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]