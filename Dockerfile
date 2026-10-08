FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B verify

FROM tomcat:9.0.122-jdk17-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/hr-management.war /usr/local/tomcat/webapps/hr-management.war
EXPOSE 8080
