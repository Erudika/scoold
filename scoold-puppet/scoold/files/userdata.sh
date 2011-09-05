#!/bin/bash 
set -e -x
export DEBIAN_FRONTEND=noninteractive

# enable SUN JDK 
sed -ire '/natty partner/ s/^#//' /etc/apt/sources.list
echo 'sun-java6-jdk shared/accepted-sun-dlj-v1-1 boolean true' | debconf-set-selections
# update + upgrade system
apt-get -y update && apt-get -y upgrade

# install Java, Git, Puppet, Munin, htop, dstat, tmux
apt-get -y install sun-java6-jdk puppet monit munin-node htop dstat tmux unzip #git

# allow all machines to see the munin-node
echo "cidr_allow 0.0.0.0/0" >> /etc/munin/munin-node.conf

# create puppet's modules dir
MOD_DIR=/usr/share/puppet/modules
mkdir -p $MOD_DIR