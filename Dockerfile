FROM maven:3.9-eclipse-temurin-21

WORKDIR /erd-core
COPY . .
RUN mvn clean install -DskipTests

CMD mvn spring-boot:run
