FROM maven:eclipse-temurin AS deps

ENV SCOOLD_HOME=/scoold
WORKDIR ${SCOOLD_HOME}

COPY pom.xml pom.xml
RUN mvn -B dependency:go-offline --fail-never

FROM deps AS build

ENV SCOOLD_HOME=/scoold
WORKDIR ${SCOOLD_HOME}

COPY . .
RUN mvn -B -am -DskipTests=true package && \
    cp target/scoold-*.jar ${SCOOLD_HOME}/scoold.jar

FROM eclipse-temurin:25-jre-alpine

ENV SCOOLD_HOME=/scoold
ENV BOOT_SLEEP=0
ENV JAVA_OPTS=""
WORKDIR ${SCOOLD_HOME}

COPY --from=build ${SCOOLD_HOME}/scoold.jar ./scoold.jar

EXPOSE 8000

ENTRYPOINT ["sh", "-c", "sleep $BOOT_SLEEP && exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar scoold.jar \"$@\"", "scoold"]
CMD []

