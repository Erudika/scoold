#!/bin/bash 
set -e -x
export DEBIAN_FRONTEND=noninteractive

# enable SUN JDK 
sed -ire '/natty partner/ s/^#//' /etc/apt/sources.list
echo 'sun-java6-jdk shared/accepted-sun-dlj-v1-1 boolean true' | debconf-set-selections
# update + upgrade system
apt-get -y update && apt-get -y upgrade

# install Java, Git, Puppet, Munin, htop, dstat, tmux
apt-get -y install sun-java6-jdk git puppet monit munin-node htop dstat tmux

# allow all machines to see the munin-node
echo "cidr_allow 0.0.0.0/0" >> /etc/munin/munin-node.conf

# create a git server
GIT_REPO=/home/ubuntu/puppet.git
MOD_DIR=/usr/share/puppet/modules
HOOK=$GIT_REPO/hooks/post-receive

# create puppet repo 
sudo -u ubuntu mkdir $GIT_REPO
mkdir -p $MOD_DIR
chown -R ubuntu:ubuntu $MOD_DIR/

sudo -u ubuntu git --git-dir $GIT_REPO --bare init
if [ -e "$HOOK" ]; then
	rm $HOOK
fi
sudo -u ubuntu touch $HOOK
chmod 755 $HOOK

echo "#!/bin/bash" >> $HOOK
echo "sudo rm -rf $MOD_DIR/.git" >> $HOOK
echo "sudo rm -rf $MOD_DIR/*" >> $HOOK
echo "sudo -u ubuntu git clone --no-hardlinks $GIT_REPO $MOD_DIR" >> $HOOK
echo "sudo puppet apply -e 'include scoold'" >> $HOOK