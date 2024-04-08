FROM maven:3-eclipse-temurin-21-alpine AS build

RUN mkdir -p /scoold
RUN curl -Ls https://github.com/Erudika/scoold/archive/master.tar.gz | tar -xz -C /scoold
RUN cd /scoold/scoold-master && mvn -q -DskipTests=true clean package

FROM adoptopenjdk/openjdk11:alpine-jre

ENV BOOT_SLEEP=0 \
    JAVA_OPTS=""

COPY --from=build /scoold/scoold-master/target/scoold-*.jar /scoold/scoold.jar

WORKDIR /scoold

EXPOSE 8000

CMD sleep $BOOT_SLEEP && \
	java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar scoold.jar
