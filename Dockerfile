FROM maven:3.6-jdk-11-slim AS build

RUN addgroup --system scoold && adduser --system --group scoold && \
	mkdir -p /scoold && \
	chown -R scoold:scoold /scoold

USER scoold

WORKDIR /scoold

RUN curl -Ls https://github.com/Erudika/scoold/archive/master.tar.gz | tar -xz -C /scoold

RUN cd /scoold/scoold-master && mvn -q -DskipTests=true clean package

FROM openjdk:11-jre-slim

ENV BOOT_SLEEP=0 \
    JAVA_OPTS=""

COPY --from=build /scoold/scoold-master/target/scoold-*.jar /scoold/scoold.jar

EXPOSE 8000

CMD sleep $BOOT_SLEEP && \
	java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /scoold/scoold.jar
