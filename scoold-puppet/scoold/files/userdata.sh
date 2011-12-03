#!/bin/bash 
set -e -x
export DEBIAN_FRONTEND=noninteractive

# enable SUN JDK 
#sed -ire '/oneiric partner/ s/^#//' /etc/apt/sources.list
add-apt-repository -y ppa:ferramroberto/java
echo 'sun-java6-jdk shared/accepted-sun-dlj-v1-1 boolean true' | debconf-set-selections

# update + upgrade system
apt-get -y update && apt-get -y upgrade

# install all the core apps
apt-get -y install sun-java6-jdk puppet monit htop dstat unzip wget curl s3cmd denyhosts

# create puppet's modules dir
mkdir -p /usr/share/puppet/modules

# disable byobu
sudo -u ubuntu byobu-disable