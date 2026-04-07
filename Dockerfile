# Usa a imagem oficial do Eclipse Temurin com JRE 25 (já que seu JAR foi compilado no Java 25 localmente)
FROM eclipse-temurin:25-jre


# Define o diretório de trabalho dentro do container
WORKDIR /app

# Copia o .jar final para dentro do container
# Esse arquivo será gerado pelo seu comando `mvnw` local e jogado na pasta target/
COPY target/*.jar app.jar

# Expõe a porta principal da aplicação (padrão 8080)
EXPOSE 8080

# Comando para rodar a aplicação quando o container subir (forçando o uso do application-prod.yml)
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
