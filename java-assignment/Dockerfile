# ---- build stage ----
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Layer Maven cache separately from source for faster rebuilds
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline -q

COPY src/ src/
RUN ./mvnw -B package -DskipTests -Dnet.bytebuddy.experimental=true

# ---- runtime stage ----
FROM registry.access.redhat.com/ubi8/openjdk-17:1.19
ENV LANGUAGE='en_US:en'

COPY --chown=185 --from=build /app/target/quarkus-app/lib/     /deployments/lib/
COPY --chown=185 --from=build /app/target/quarkus-app/*.jar    /deployments/
COPY --chown=185 --from=build /app/target/quarkus-app/app/     /deployments/app/
COPY --chown=185 --from=build /app/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080
USER 185
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"
ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
