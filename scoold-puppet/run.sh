#!/bin/bash -e

# Example: 
# ./ec2run.sh cassandra /Users/alexb/Desktop/scoold/scoold-puppet/scoold/files/userdata.sh m1.small ami-1b9fa86f db1
#
# Ubuntu 11.04 Natty		32 bit			64 bit
# ---------------------------------------------------------
# EBS boot					ami-359ea941  	ami-379ea943
# instance-store 			ami-1b9fa86f  	ami-619ea915

AMI=$2  
TYPE="m1.small"
#TYPE="t1.micro"
REGION="eu-west-1"
PRICE="0.060"
DATAFILE=$1
SSHKEY="alexb-pubkey"
NDB=3
NWEB=2
NSEARCH=1

function ec2req () {
	GROUP=$1
	N=$2	
	if [ -z "$2" ]; then
		if [ $GROUP = "cassandra" ]; then
			N=$NDB
		elif [ $GROUP = "glassfish" ]; then
			N=$NWEB
		elif [ $GROUP = "elasticsearch" ]; then
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

if [ -n "$1$2$3" ]; then
	case $3 in
	    glassfish 		) ec2req $3 $4 $5;;
	    cassandra 		) ec2req $3 $4 $5;;
	    elasticsearch 	) ec2req $3 $4 $5;;
	    all 			) ec2req "glassfish" $4 $5
	    	    		  ec2req "cassandra" $4 $5
	    	     		  ec2req "elasticsearch" $4 $5;;
	esac	
else
	echo "USAGE:  $0 datafile ami group/all [size] [nospot]"
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