FROM amazoncorretto:21
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} mma-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","/mma-0.0.1-SNAPSHOT.jar"]