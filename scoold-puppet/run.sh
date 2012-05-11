#!/bin/bash -e

# Ubuntu 11.04 Natty		32 bit			64 bit
# ---------------------------------------------------------
# EBS boot					ami-359ea941  	ami-379ea943
# instance-store 			ami-1b9fa86f  	ami-619ea915

# Ubuntu 11.10 Oneiric		32 bit			64 bit
# ---------------------------------------------------------
# EBS boot					ami-65b28011  	ami-61b28015
# instance-store 			ami-dfcdffab  	ami-75b28001
#
# Ubuntu 12.04 LTS - ami-3da89349

TYPE=$1
AMI="ami-3da89349"	# Ubuntu 12.04 LTS 
REGION="eu-west-1"
AZ="eu-west-1a"
PRICE="0.060"
DATAFILE="./scoold/files/userdata.sh"
SSHKEY="alexb-pubkey"
NDB=1
NWEB=1
NSEARCH=1

function ec2req () {
	GROUP=$1
	if [ "$GROUP" = "cassandra" ]; then
		N=$NDB
	elif [ "$GROUP" = "glassfish" ]; then
		N=$NWEB
	elif [ "$GROUP" = "elasticsearch" ]; then
		N=$NSEARCH		
	fi	
	
	if [ -n "$2" ] && [ "$2" != "spot" ]; then
		N=$2
	fi
	
	if [ "$3" = "spot" ] || [ "$2" = "spot" ]; then
		# default - spot request
		ec2-request-spot-instances -n $N -g $GROUP -p $PRICE --user-data-file $DATAFILE --region $REGION -z $AZ -k $SSHKEY -t $TYPE $AMI
	else
		# normal request
		ec2-run-instances -n $N -g $GROUP --user-data-file $DATAFILE --region $REGION -z $AZ -k $SSHKEY -t $TYPE $AMI
	fi	
}

if [ -n "$1" ] && [ -n "$2" ]; then
	case $2 in
	    glassfish 		) ec2req $2 $3 $4;;
	    cassandra 		) ec2req $2 $3 $4;;
	    elasticsearch 	) ec2req $2 $3 $4;;
	    all 			) ec2req "glassfish" $3
	    	    		  ec2req "cassandra" $3
	    	     		  ec2req "elasticsearch" $3;;
	esac	
else
	echo "USAGE:  $0 type (group | all) [size] [nospot]"
fi