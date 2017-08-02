FROM openjdk:8-jre-alpine
COPY target/uberjar/receptionist.jar /app/receptionist.jar
WORKDIR /app
EXPOSE 8075
CMD ["java", "-jar", "receptionist.jar"]
