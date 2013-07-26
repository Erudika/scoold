#!/bin/bash 
set -e -x
export DEBIAN_FRONTEND=noninteractive

# enable SUN JDK 6 (manually)
# echo "sun-java6-jdk shared/accepted-sun-dlj-v1-1 select true" | debconf-set-selections
# echo "sun-java6-jre shared/accepted-sun-dlj-v1-1 select true" | debconf-set-selections
# mkdir /usr/lib/jvm
# wget --no-check-certificate -O /usr/lib/jvm/java.bin https://s3-eu-west-1.amazonaws.com/www.scoold.com/jre-6u32-linux-x64.bin
# chmod a+x /usr/lib/jvm/java.bin
# cd /usr/lib/jvm
# /usr/lib/jvm/java.bin 
# ln -s -b /usr/lib/jvm/jre1.6.0_32/bin/java /etc/alternatives/java
# ln -s -b /usr/lib/jvm/jre1.6.0_32/bin/java /usr/bin/java

# JAVA 7
echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections
add-apt-repository -y ppa:webupd8team/java

# update + upgrade system
apt-get -y update && apt-get -y upgrade

# install all the core apps
apt-get -y install puppet monit htop dstat unzip wget curl s3cmd denyhosts oracle-java7-installer

# create puppet's modules dir
mkdir -p /usr/share/puppet/modules

# disable byobu
sudo -u ubuntu byobu-disable

# fix limits.conf
maxmem=$(free | awk '/^Mem:/{print $2}')
maxfiles=64000
echo "session required pam_limits.so" >> /etc/pam.d/common-session
echo "* - memlock $maxmem" >> /etc/security/limits.conf
echo "* - nofile $maxfiles" >> /etc/security/limits.conf

# preserve iptables rules on reboot
echo "pre-up iptables-restore < /etc/iptables.rules" >> /etc/network/interfaces
iptables-save -c > /etc/iptables.rules

# config monit
sed -e "1,/START=no/ s/START=no/START=yes/" -i.bak /etc/default/monit
sed -e "1,/set -e/ s/set -e/set -e; ulimit -l $maxmem; ulimit -n $maxfiles/" -i.bak /etc/init.d/monit
# service monit restart

# set daily time sync
echo "ntpdate ntp.ubuntu.com" > /etc/cron.daily/ntpdate
chmod 755 /etc/cron.daily/ntpdate
ntpdate ntp.ubuntu.com

# finish
reboot now