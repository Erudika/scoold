#!/bin/bash 
set -e -x
export DEBIAN_FRONTEND=noninteractive

# enable SUN JDK 
sed -ire '/natty partner/ s/^#//' /etc/apt/sources.list
echo 'sun-java6-jdk shared/accepted-sun-dlj-v1-1 boolean true' | debconf-set-selections
# update + upgrade system
apt-get -y update && apt-get -y upgrade

# install all the core apps
apt-get -y install sun-java6-jdk puppet monit munin-node htop dstat unzip wget curl s3cmd

# allow all machines to see the munin-node
echo "cidr_allow 0.0.0.0/0" >> /etc/munin/munin-node.conf

# create puppet's modules dir
mkdir -p /usr/share/puppet/modules

# fix cron logging
VAR1="*.*;auth,authpriv.none"
VAR2="#cron.*"
sed -e "1,/$VAR1/ s/$VAR1.*/$VAR1,cron\.none -\/var\/log\/syslog/" -i.bak /etc/rsyslog.d/50-default.conf
sed -e "1,/$VAR2/ s/$VAR2.*/cron\.\*				\/var\/log\/cron\.log/" -i.bak /etc/rsyslog.d/50-default.conf
service rsyslog restart