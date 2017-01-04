#!/bin/bash
set -e -x
export DEBIAN_FRONTEND=noninteractive

# JAVA 8
#echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections
echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections
add-apt-repository -y ppa:webupd8team/java

# update + upgrade system
apt-get -y update && apt-get -y upgrade

# install all the core tools
apt-get -y install monit htop dstat unzip wget curl awscli fail2ban

# install jdk
apt-get -y install oracle-java8-installer

# install puppet
#apt-get -y install puppet
# create puppet's modules dir
#mkdir -p /usr/share/puppet/modules

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

# configure monit
sed -e "1,/START=no/ s/START=no/START=yes/" -i.bak /etc/default/monit
sed -e "1,/set -e/ s/set -e/set -e; ulimit -l $maxmem; ulimit -n $maxfiles/" -i.bak /etc/init.d/monit
# service monit restart

# set daily time sync
echo "ntpdate ntp.ubuntu.com" > /etc/cron.daily/ntpdate
chmod 755 /etc/cron.daily/ntpdate
ntpdate ntp.ubuntu.com

# finish
reboot now