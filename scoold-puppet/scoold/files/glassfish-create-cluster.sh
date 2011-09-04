#!/bin/bash

ASADMIN="sudo -u glassfish /home/glassfish/glassfish/bin/asadmin"
GFHOME=/home/glassfish/glassfish
CLUSTER="scoold"
#ip-10-234-121-3
#ip-10-234-109-36

if [ -n "$1$2" ]; then
    # run on all nodes except DAS
	echo "--- 1 ---"
	$ASADMIN setup-ssh $2
	#ssh ubuntu@$2 "sudo passwd -d glassfish;sudo mv -f /etc/ssh/sshd_config.bak /etc/ssh/sshd_config && sudo service ssh restart"
	
	echo "--- 2 ---"
	$ASADMIN enable-secure-admin
	sudo stop glassfish
	sudo start glassfish
	
	echo "--- 3 ---"
	$ASADMIN create-cluster $CLUSTER
	
	echo "--- 4 ---"
	$ASADMIN create-local-instance --cluster $CLUSTER inst1
	
	echo "--- 5 ---"
	$ASADMIN create-node-ssh --sshuser glassfish --nodehost $2 --installdir $GFHOME node2
	
	echo "--- 6 ---"
	$ASADMIN create-instance --node node2 --cluster $CLUSTER inst2
	
	echo "--- 7 ---"  
	$ASADMIN start-cluster $CLUSTER
fi