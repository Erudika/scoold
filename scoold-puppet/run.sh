#!/bin/bash -e

# Ubuntu 11.04 Natty		32 bit			64 bit
# ---------------------------------------------------------
# EBS boot					ami-359ea941  	ami-379ea943
# instance-store 			ami-1b9fa86f  	ami-619ea915

# Ubuntu 11.10 Oneiric		32 bit			64 bit
# ---------------------------------------------------------
# EBS boot					ami-65b28011  	ami-61b28015
# instance-store 			ami-dfcdffab  	ami-75b28001

ami-dfcdffab

TYPE=$1
AMI=$2
REGION="eu-west-1"
PRICE="0.060"
DATAFILE="./scoold/files/userdata.sh"
SSHKEY="alexb-pubkey"
NDB=3
NWEB=2
NSEARCH=1

function ec2req () {
	GROUP=$1
	N=$2	
	if [ -z "$2" ]; then
		if [ "$GROUP" = "cassandra" ]; then
			N=$NDB
		elif [ "$GROUP" = "glassfish" ]; then
			N=$NWEB
		elif [ "$GROUP" = "elasticsearch" ]; then
			N=$NSEARCH
		fi
	fi
	
	if [ -z "$3" ]; then
		# default - spot request
		ec2-request-spot-instances -n $N -g $GROUP -p $PRICE --user-data-file $DATAFILE --region $REGION -k $SSHKEY -t $TYPE $AMI		
	else
		# normal request
		ec2-run-instances -n $N -g $GROUP --user-data-file $DATAFILE --region $REGION -k $SSHKEY -t $TYPE $AMI
	fi	
}

if [ -n "$1" ] && [ -n "$2" ] && [ -n "$3" ]; then
	case $3 in
	    glassfish 		) ec2req $3 $4 $5;;
	    cassandra 		) ec2req $3 $4 $5;;
	    elasticsearch 	) ec2req $3 $4 $5;;
	    all 			) ec2req "glassfish" $4 $5
	    	    		  ec2req "cassandra" $4 $5
	    	     		  ec2req "elasticsearch" $4 $5;;
	esac	
else
	echo "USAGE:  $0 type ami (group | all) [size] [nospot]"
fi

################
# OLD RUN SCRIPT
################

# instanceid=$(ec2-run-instances -g $GROUP --user-data-file $DATAFILE --region $REGION -k $SSHKEY -t $TYPE $AMI | egrep ^INSTANCE | cut -f2)
# if [ -z "$instanceid" ]; then
#     echo "ERROR: it failed!";
#     exit;
# else
#     echo "$NAME [$instanceid] is booting up..."
# fi    
# 
# # wait for the instance to be fully operational
# while host=$(ec2-describe-instances --region $REGION "$instanceid" | egrep ^INSTANCE | cut -f4) && test -z $host; do sleep 3; done
# echo "$host"

################