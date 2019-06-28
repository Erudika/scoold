#!/bin/bash
set -e -x

# Lightsail/DigitalOcean installer script for Ubuntu
VERSION="1.32.1"
PORT="8000"
WORKDIR="/home/ubuntu"
JARURL="https://github.com/Erudika/scoold/releases/download/${VERSION}/scoold-${VERSION}.jar"
sfile="/etc/systemd/system/scoold.service"

apt-get update && apt-get install -y wget openjdk-11-jre &&
wget -O scoold-${VERSION}.jar ${JARURL} && \
mv scoold-${VERSION}.jar $WORKDIR && \
chown ubuntu:ubuntu ${WORKDIR}/scoold-${VERSION}.jar && \
chmod +x ${WORKDIR}/scoold-${VERSION}.jar

touch $sfile
cat << EOF > $sfile
[Unit]
Description=Scoold
After=syslog.target
[Service]
WorkingDirectory=${WORKDIR}
SyslogIdentifier=Scoold
ExecStart=/bin/bash -c "java -jar -Dserver.port=${PORT} -Dconfig.file=${WORKDIR}/application.conf scoold-*.jar"
User=ubuntu
[Install]
WantedBy=multi-user.target
EOF

iptables -t nat -A PREROUTING -p tcp -m tcp --dport 80 -j REDIRECT --to-port ${PORT} && \
iptables -t nat -A OUTPUT -p tcp --dport 80 -o lo -j REDIRECT --to-port ${PORT}

systemctl enable scoold.service && \
systemctl start scoold.service
