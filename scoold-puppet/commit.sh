# Copyright Erudika LLC
# http://erudika.com/
# 
# Permission is hereby granted, free of charge, to any person obtaining
# a copy of this software and associated documentation files (the
# "Software"), to deal in the Software without restriction, including
# without limitation the rights to use, copy, modify, merge, publish,
# distribute, sublicense, and/or sell copies of the Software, and to
# permit persons to whom the Software is furnished to do so, subject to
# the following conditions:
# 
# The above copyright notice and this permission notice shall be
# included in all copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
# LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
# OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
# WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# commit.sh - helper script for committing puppet code to nodes, setting up instances and for executing utility tasks

#!/bin/bash

LBNAME="ScooldLB"
REGION="eu-west-1"
MODULESDIR="/usr/share/puppet/modules"
WEBDIR="../scoold-web/src/main/webapp/WEB-INF"
MODNAME="scoold"
F1SUFFIX="-instances.txt"
F2SUFFIX="-hostnames.txt"
KEYS="keys.txt"
INDEXNAME="ScooldIndex"
ESCONFIG="./$MODNAME/files/elasticsearch.yml"
S3CONFIG="./$MODNAME/files/s3cfg.txt"
PUPPETINIT="./$MODNAME/manifests/init.pp"

function init () {
	GROUP=$1
	FILE1=$2
	FILE2=$3
	ec2-describe-instances --region $REGION --filter group-name=$GROUP --filter "instance-state-name=running" | egrep ^INSTANCE | awk '{ print $2,$4}' > $FILE1
	cat $FILE1 | awk '{print $2}' > $FILE2 # hostnames only
}

function readProp () {
	FILE=$1
	echo $(sed '/^\#/d' $FILE | grep "$2" | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
}

function setProps () {
	nodename=$1
	if [ -e "cassandra$F1SUFFIX" ]; then
		# set seed nodes to be the first two IPs
		dbseeds=$(sed -n 1,3p "cassandra$F1SUFFIX" | awk '{ print $3" " }' | tr -d "\n" | awk '{ print $1","$2","$3 }' | sed 's/,$//g')			
		sed -e "1,/\\\$dbseeds/ s/\\\$dbseeds.*/\\\$dbseeds = \"$dbseeds\"/" -i.bak $PUPPETINIT	
	fi
	sed -e "1,/\\\$nodename/ s/\\\$nodename.*/\\\$nodename = \"$nodename\"/" -i.bak $PUPPETINIT	
	
	ak=$(readProp $KEYS "com.scoold.awsaccesskey")
	sk=$(echo `readProp $KEYS "com.scoold.awssecretkey"` | sed 's/\//\\\//g')
	
	if [ -e $ESCONFIG ]; then		
		sed -e "1,/cloud\.aws\.access_key:/ s/cloud\.aws\.access_key:.*/cloud\.aws\.access_key: $ak/" -i.bak $ESCONFIG
		sed -e "1,/cloud\.aws\.secret_key:/ s/cloud\.aws\.secret_key:.*/cloud\.aws\.secret_key: $sk/" -i.bak $ESCONFIG
	fi
	if [ -e $ESCONFIG ]; then
		sed -e "1,/access_key/ s/access_key.*/access_key = $ak/" -i.bak $S3CONFIG
		sed -e "1,/secret_key/ s/secret_key.*/secret_key = $sk/" -i.bak $S3CONFIG
	fi
}

function unsetProps () {
	sed -e "1,/\\\$dbseeds/ s/\\\$dbseeds.*/\\\$dbseeds = \"\"/" -i.bak $PUPPETINIT
	sed -e "1,/\\\$nodename/ s/\\\$nodename.*/\\\$nodename = \"\"/" -i.bak $PUPPETINIT	
	
	if [ -e $ESCONFIG ]; then
		sed -e "1,/cloud\.aws\.access_key:/ s/cloud\.aws\.access_key:.*/cloud\.aws\.access_key: /" -i.bak $ESCONFIG
		sed -e "1,/cloud\.aws\.secret_key:/ s/cloud\.aws\.secret_key:.*/cloud\.aws\.secret_key: /" -i.bak $ESCONFIG
		sed -e "1,/cloud\.aws\.sqs\.queue_url:/ s/cloud\.aws\.sqs\.queue_url:.*/cloud\.aws\.sqs\.queue_url: /" -i.bak $ESCONFIG
	fi
	if [ -e $ESCONFIG ]; then
		sed -e "1,/access_key/ s/access_key.*/access_key = /" -i.bak $S3CONFIG
		sed -e "1,/secret_key/ s/secret_key.*/secret_key = /" -i.bak $S3CONFIG
	fi
}

function pushConfig () {
	instid=$1
	host=$2
	NODEID=$3
	GROUP=$4
	ZIPNAME=$5
	
	setProps "$GROUP$NODEID"
	### set node TAG
	ec2-create-tags --region $REGION $instid --tag Name="$GROUP$NODEID"					
					
	### push updated puppet script
	echo "copying $MODNAME.zip to $GROUP$NODEID..."
	zip -rq $ZIPNAME.zip $MODNAME/
	scp $ZIPNAME.zip ubuntu@$host:~/
	
	unsetProps
	# if [ $GROUP = "glassfish" ]; then
	# 	scp -C schools.txt ubuntu@$host:~/
	# fi	
}

function cleanupAndExec () {
	ZIPNAME=$1
	FILE2=$2
	host=$3
	cmd="sudo rm -rf $MODULESDIR/$MODNAME; sudo unzip -qq -o ~/$ZIPNAME.zip -d $MODULESDIR/ && sudo puppet apply -e 'include $MODNAME'; rm -rf ~/$ZIPNAME.zip"
	
	### cleanup
	rm ./$MODNAME/manifests/*.bak ./$MODNAME/files/*.bak $ZIPNAME.zip
	
	echo "done. executing puppet code on each node..."
	
	### unzip & execute remotely	
	if [ -n "$host" ]; then
		### execute single
		ssh -n ubuntu@$host "$cmd"
	else
		### execute on all nodes
		pssh/bin/pssh -h $FILE2 -l ubuntu -t 0 -i "$cmd"
	fi
}

if [ -n "$1" ] && [ -n "$2" ]; then
	GROUP=$2
	FILE1="$GROUP$F1SUFFIX"
	FILE2="$GROUP$F2SUFFIX"	
	ZIPNAME="$MODNAME-$GROUP"
	
	if [ "$1" = "init" ]; then
		### get info about all web servers		
		if [ "$GROUP" = "all" ]; then
			for grp in "cassandra" "glassfish" "elasticsearch"; do
				f1="$grp$F1SUFFIX"
				f2="$grp$F2SUFFIX"
				init $grp $f1 $f2
			done			
		else
			init $GROUP $FILE1 $FILE2
		fi		
		echo "done."		
	elif [ "$1" = "all" ]; then						
		count=1	
		while read i; do
			instid=$(echo $i | awk '{ print $1 }')
			host=$(echo $i | awk '{ print $2 }')
			ipaddr=$(echo $i | awk '{ print $3 }')
			
			if [ -n "$host" ]; then
				pushConfig $instid $host $count $GROUP $ZIPNAME
				count=$((count+1))
			fi
	 	done < $FILE1
		
		cleanupAndExec $ZIPNAME $FILE2
	elif [ `expr "$1" : '^node-.*$'` != 0 ]; then
		tag=$(expr "$1" : '^node-\(.*\)$')	
		count=$(expr "$tag" : '^[a-zA-Z]*\([0-9]*\)$')	
		i=$(ec2din --region $REGION --filter "instance-state-name=running" -F "tag-value=$tag" | egrep ^INSTANCE | awk '{ print $2,$4,$15}')
		instid=$(echo $i | awk '{ print $1 }')
		host=$(echo $i | awk '{ print $2 }')
		if [ -n "$host" ]; then
			pushConfig $instid $host $count $GROUP $ZIPNAME
			cleanupAndExec $ZIPNAME $FILE2 $host
		fi		
	elif [ "$1" = "lbadd" ]; then
	 	### register instance with LB
		$AWS_ELB_HOME/bin/elb-register-instances-with-lb $LBNAME --region $REGION --instances $2
	elif [ "$1" = "lbremove" ]; then
	 	### deregister instance from LB
		$AWS_ELB_HOME/bin/elb-deregister-instances-from-lb $LBNAME --region $REGION --instances $2
	elif [ "$1" = "backupdb" ]; then
		### backup snapshot to S3
		db1host=$2
		ssh -n ubuntu@$db1host "sudo monit stop cassandra && sudo -u cassandra /home/cassandra/backupdb.sh $3 && sudo monit start cassandra"
	elif [ "$1" = "restoredb" ]; then
		### load snapshot from S3
		db1host=$2
		ssh -n ubuntu@$db1host "sudo monit stop cassandra && sudo -u cassandra /home/cassandra/restoredb.sh $3 && sudo monit start cassandra"
	elif [ "$1" = "checkdb" ]; then
		### check if db is up and running and ring is OK
		db1host=$2
		ssh -n ubuntu@$db1host "/home/cassandra/cassandra/bin/nodetool -h localhost ring"
	elif [ "$1" = "initdb" ]; then
		### load schema definition from file on db1
		db1host=$2
		ssh -n ubuntu@$db1host "sudo -u cassandra /home/cassandra/cassandra/bin/cassandra-cli -h localhost -f /usr/share/puppet/modules/scoold/files/schema-light.txt"	
	elif [ "$1" = "esindex" ]; then
		### create elasticsearch index
		es1host=$2
		ssh -n ubuntu@$es1host "sudo -u elasticsearch curl -s -XPUT localhost:9200/scoold -d @/home/elasticsearch/elasticsearch/config/index.json"
	elif [ "$1" = "esriver" ]; then
		### create elasticsearch river	
		es1host=$2
		ssh -n ubuntu@$es1host "sudo -u elasticsearch curl -s -XPUT localhost:9200/_river/scoold/_meta -d '{ \"type\" : \"amazonsqs\" }'"
	fi
elif [ "$1" = "createlb" ]; then
	$AWS_ELB_HOME/bin/elb-create-lb $LBNAME --region $REGION --availability-zones "eu-west-1a,eu-west-1b,eu-west-1c" --listener "protocol=http,lb-port=80,instance-port=8080"
	$AWS_ELB_HOME/bin/elb-configure-healthcheck $LBNAME --region $REGION --target "HTTP:8080/" --interval 30 --timeout 3 --unhealthy-threshold 2 --healthy-threshold 2
else
	echo "USAGE: $0 checkdb [host] | initdb [host] | backupdb [host] [file] | restoredb [host] [file] | esindex [host] | esriver [host] | createlb | [ init | all | node-xxx ] group | [ lbadd | lbremove ] instid"
fi