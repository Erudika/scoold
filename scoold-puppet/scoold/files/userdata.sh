# Init script for our Ubuntu servers
# 
# Installs git, puppet and creates a new puppet repo with a 
# post-receive hook which triggers a puppet run. 

#!/bin/bash -ex
export DEBIAN_FRONTEND=noninteractive

# SUN JDK 
sed -ire '/natty partner/ s/^#//' /etc/apt/sources.list
apt-get update && apt-get -y upgrade
echo 'sun-java6-jdk shared/accepted-sun-dlj-v1-1 boolean true' | debconf-set-selections
apt-get -y install sun-java6-jdk

apt-get -y install git
apt-get -y install puppet

# create a git server
GIT_HOME=/home/git
GIT_REPO=puppet.git
MOD_DIR=/usr/share/puppet/modules/

adduser --system --shell /bin/bash --gecos 'git version control' --group --disabled-password --home $GIT_HOME git
echo "git  ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

mkdir $GIT_HOME/.ssh
cp /home/ubuntu/.ssh/authorized_keys $GIT_HOME/.ssh/
chown -R git:git $GIT_HOME/.ssh
chmod 700 $GIT_HOME/.ssh
chmod 600 $GIT_HOME/.ssh/*

sudo -u git mkdir $GIT_HOME/$GIT_REPO
sudo -u git mkdir $GIT_HOME/puppet
mkdir /usr/share/puppet
mkdir $MOD_DIR 

sudo -u git git --git-dir $GIT_HOME/$GIT_REPO --bare init
sudo -u git touch $GIT_HOME/$GIT_REPO/hooks/post-receive
chmod 755 $GIT_HOME/$GIT_REPO/hooks/post-receive

echo "#!/bin/bash
sudo rm -rf $MOD_DIR/.gitignore $MOD_DIR/.git $MOD_DIR/*
sudo git clone --no-hardlinks $GIT_HOME/$GIT_REPO $MOD_DIR
sudo puppet apply -e 'include scoold'" >> $GIT_HOME/$GIT_REPO/hooks/post-receive

# add hostname alias
HOSTNAME=web1
sudo hostname $HOSTNAME
sudo echo "127.0.2.2	$HOSTNAME.localdomain	$HOSTNAME"


