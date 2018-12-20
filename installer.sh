#!/bin/bash
set -e -x

# Ubuntu/DigitalOcean installer script
VERSION="1.31.1"
sfile="/etc/systemd/system/scoold.service"
wget https://github.com/Erudika/scoold/releases/download/${VERSION}/scoold-${VERSION}.jar
chown scoold scoold-*.jar
chmod +x scoold-*.jar

touch $sfile
cat << EOF > $sfile
[Unit]
Description=Scoold
After=syslog.target
[Service]
WorkingDirectory=/home/scoold
SyslogIdentifier=Scoold
ExecStart=/bin/bash -c "java -jar -Dserver.port=8000 -Dconfig.file=/home/scoold/application.conf scoold-*.jar"
User=scoold
[Install]
WantedBy=multi-user.target
EOF

systemctl enable scoold.service && \
systemctl start scoold.service
