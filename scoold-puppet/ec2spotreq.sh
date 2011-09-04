#!/bin/bash -e

#AMI="ami-1b9fa86f" # 32bit inst store
AMI=$2  # 32bit ebs
TYPE="m1.small"
REGION="eu-west-1"
PRICE="0.060"
DATAFILE=$1
SSHKEY="alexb-pubkey"


function spot_db () {
	N=$1
	if [ -z "$1" ]; then
		N=3
	fi
	ec2-request-spot-instances -n $N -g "cassandra" -p $PRICE --user-data-file $DATAFILE --region $REGION -k $SSHKEY -t $TYPE $AMI
}

function spot_web () {
	N=$1
	if [ -z "$1" ]; then
		N=2
	fi
	ec2-request-spot-instances -n $N -g "glassfish" -p $PRICE --user-data-file $DATAFILE --region $REGION -k $SSHKEY -t $TYPE $AMI
}

function spot_search () {
	N=$1
	if [ -z "$1" ]; then
		N=1
	fi
	ec2-request-spot-instances -n $N -g "elasticsearch" -p $PRICE --user-data-file $DATAFILE --region $REGION -k $SSHKEY -t $TYPE $AMI
}

# SPOT REQUEST (set and forget)
if [ -n "$1$2" ]; then
	if [ -n "$3" ]; then		
		case $3 in
		    web		) spot_web $4;;
		    db		) spot_db $4;;
		    search	) spot_search $4;;
		    *		) spot_db $4
					  spot_web $4
					  spot_search $4
		esac		
	fi	
else
	echo "USAGE:  $0 datafile ami [sub group] [size]"
fi
