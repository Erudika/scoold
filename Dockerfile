FROM openjdk:8-jdk-alpine

RUN apk --update add git openssh maven && \
    rm -rf /var/lib/apt/lists/* && \
    rm /var/cache/apk/*

ENV BOOT_SLEEP=0 \
    JAVA_OPTS=""

RUN addgroup -S scoold && adduser -S -G scoold scoold && \
	mkdir -p /scoold/clone && \
	chown -R scoold:scoold /scoold

USER scoold

WORKDIR /scoold

RUN git clone --depth=1 https://github.com/Erudika/scoold /scoold/clone && \
	cd /scoold/clone && \
	mvn -DskipTests=true clean package && \
	mv target/scoold-*.jar /scoold/ && \
	cd /scoold && rm -rf /scoold/clone && rm -rf ~/.m2

EXPOSE 8000

CMD sleep $BOOT_SLEEP && \
	java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar scoold-*.jar
