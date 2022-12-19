#!/bin/bash
set -e -x

# Lightsail/DigitalOcean installer script for Ubuntu
VERSION="1.51.3"
PORT="8000"
WORKDIR="/home/ubuntu"
JARURL="https://github.com/Erudika/scoold/releases/download/${VERSION}/scoold-${VERSION}.jar"
sfile="/etc/systemd/system/scoold.service"

apt-get update && apt-get install -y wget openjdk-11-jre &&
wget -O scoold.jar ${JARURL} && \
mv scoold.jar $WORKDIR && \
chown ubuntu:ubuntu ${WORKDIR}/scoold.jar && \
chmod +x ${WORKDIR}/scoold.jar
touch ${WORKDIR}/application.conf && \
chown ubuntu:ubuntu ${WORKDIR}/application.conf

# Feel free to modify the Scoold configuration here
cat << EOF > ${WORKDIR}/application.conf
scoold.app_name = "Scoold"
scoold.port = 8000
scoold.env = "production"
scoold.host_url = "http://localhost:8000"
scoold.para_endpoint = "https://paraio.com"
scoold.para_access_key = "app:scoold"
scoold.para_secret_key = ""
scoold.admins = "admin@example.com"
EOF

touch $sfile
cat << EOF > $sfile
[Unit]
Description=Scoold
After=syslog.target
StartLimitIntervalSec=30
StartLimitBurst=2
[Service]
WorkingDirectory=${WORKDIR}
SyslogIdentifier=Scoold
ExecStart=java -jar -Dconfig.file=application.conf scoold.jar
User=ubuntu
Restart=on-failure
RestartSec=1s
[Install]
WantedBy=multi-user.target
EOF

# This is optional. These rules might interfere with other web server configurations like nginx and certbot.
#iptables -t nat -A PREROUTING -p tcp -m tcp --dport 80 -j REDIRECT --to-port ${PORT} && \
#iptables -t nat -A OUTPUT -p tcp --dport 80 -o lo -j REDIRECT --to-port ${PORT}

systemctl enable scoold.service && \
systemctl start scoold.service
