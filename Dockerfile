FROM gradle:7.6.4-jdk8 AS build_stage

WORKDIR /in_build
COPY --chown=gradle:gradle . /in_build
run gradle build --no-daemon


FROM eclipse-temurin:8-jre-ubi9-minimal

WORKDIR /app
COPY --from=build_stage /in_build/build/libs/*.jar /app/
ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-Djava.security.egd=file:/dev/./urandom", "-jar" "AskZgServer.jar"]
