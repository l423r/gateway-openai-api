FROM eclipse-temurin:17-jdk-jammy
USER root
COPY /build/libs/*.jar /opt/app.jar
EXPOSE 8080 8443
ENTRYPOINT ["java", "-jar", "/opt/app.jar"]