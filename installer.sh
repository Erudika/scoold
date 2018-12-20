#!/bin/bash
set -e -x

# Ubuntu/DigitalOcean installer script
VERSION="1.31.1"
PORT="8000"
sfile="/etc/systemd/system/scoold.service"
wget https://github.com/Erudika/scoold/releases/download/${VERSION}/scoold-${VERSION}.jar && \
chown scoold scoold-*.jar && chmod +x scoold-*.jar

touch $sfile
cat << EOF > $sfile
[Unit]
Description=Scoold
After=syslog.target
[Service]
WorkingDirectory=/home/scoold
SyslogIdentifier=Scoold
ExecStart=/bin/bash -c "java -jar -Dserver.port=${PORT} -Dconfig.file=/home/scoold/application.conf scoold-*.jar"
User=scoold
[Install]
WantedBy=multi-user.target
EOF

iptables -t nat -A PREROUTING -p tcp -m tcp --dport 80 -j REDIRECT --to-port ${PORT} && \
iptables -t nat -A OUTPUT -p tcp --dport 80 -o lo -j REDIRECT --to-port ${PORT}

systemctl enable scoold.service && \
systemctl start scoold.service
