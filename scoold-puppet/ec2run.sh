#!/bin/bash -e

# Example: 
# ./ec2run.sh cassandra /Users/alexb/Desktop/scoold/scoold-puppet/scoold/files/userdata.sh m1.small ami-1b9fa86f db1
#
# Ubuntu 11.04 Natty		32 bit			64 bit
# ---------------------------------------------------------
# EBS boot					ami-359ea941  	ami-379ea943
# instance-store 			ami-1b9fa86f  	ami-619ea915


if [ -z "$1$2$3$4$5" ]; then
	echo "USAGE:"    
    echo "    $0 group datafile instancetype ami [name]"
else
	GROUP=$1
	DATAFILE=$2
	REGION="eu-west-1"
	TYPE=$3
	AMI=$4
	SSHKEY="alexb-pubkey"
	NAME="instance"
	if [ -n "$5" ]; then
		NAME=$5
	fi

	# create the instance and capture the instance id
	#echo "Launching $NAME..."
	instanceid=$(ec2-run-instances -g $GROUP --user-data-file $DATAFILE --region $REGION -k $SSHKEY -t $TYPE $AMI | egrep ^INSTANCE | cut -f2)
	if [ -z "$instanceid" ]; then
	    echo "ERROR: it failed!";
	    exit;
	else
	    echo "$NAME [$instanceid] is booting up..."
	fi    

	# wait for the instance to be fully operational
	while host=$(ec2-describe-instances --region $REGION "$instanceid" | egrep ^INSTANCE | cut -f4) && test -z $host; do sleep 3; done
	echo "$NAME [$instanceid] ==> $host"

	# echo -n "Verifying ssh connection to box..."
	# while ssh -o StrictHostKeyChecking=no -q ubuntu@$host true && test; do echo -n .; sleep 1; done
	# echo ""
fi





