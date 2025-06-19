FROM maven:3.9.6-eclipse-temurin-21 as build

WORKDIR /rag

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src /rag/src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21

WORKDIR /rag

COPY --from=build /rag/target/rag-0.0.1-SNAPSHOT.jar /rag/rag.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/rag/rag.jar"]
