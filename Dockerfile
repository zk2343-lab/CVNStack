# AI-assisted.
# Packages the Spring Boot jar as a linux/amd64 image Cloud Run can run.

FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY config config
COPY src src
RUN mvn -B -DskipTests package \
  && find target -maxdepth 1 -type f -name 'cvnstack-*.jar' ! -name '*.original' \
    -exec cp {} /app.jar \; \
  && test -s /app.jar

FROM --platform=linux/amd64 eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app.jar /app/app.jar
RUN groupadd --system cvnstack \
  && useradd --system --gid cvnstack --no-create-home cvnstack \
  && chown cvnstack:cvnstack /app/app.jar
USER cvnstack
ENV PORT=8080
ENV VERIFY_STORES=true
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=5s --start-period=45s --retries=20 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/${PORT:-8080} && printf "GET /health HTTP/1.0\r\nHost: localhost\r\nConnection: close\r\n\r\n" >&3 && grep -q UP <&3'
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
