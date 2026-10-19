# one image per service from the jar maven already built (./mvnw -q package -DskipTests), no maven inside docker
# docker build -f docker/Dockerfile.java --build-arg MODULE=core-service .   (compose does this with the "apps" profile)
FROM eclipse-temurin:17-jre
WORKDIR /app
ARG MODULE
# the repackaged spring boot jar only, never the plain *.jar.original
COPY ${MODULE}/target/*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
