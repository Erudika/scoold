# !/bin/bash

LBNAME="ScooldLB"
REGION="eu-west-1"
MODULESDIR="/usr/share/puppet/modules"
WEBDIR="../scoold-web/src/main/webapp/WEB-INF"
MODNAME="scoold"
NODETYPE="unknown"
F1SUFFIX="-instances.txt"
F2SUFFIX="-hostnames.txt"
RIVERFILE="river-amazonsqs"
RIVERLINK="https://s3-eu-west-1.amazonaws.com/com.scoold.elasticsearch/river-amazonsqs.zip"

function init () {
	GROUP=$1
	FILE1=$2
	FILE2=$3
	ec2-describe-instances --region $REGION --filter group-name=$GROUP --filter "instance-state-name=running" | egrep ^INSTANCE | awk '{ print $2,$4,$15}' > $FILE1
	cat $FILE1 | awk '{print $2}' > $FILE2 # hostnames only
}

function getType () {
	NODETYPE=""
	if [ "$1" = "cassandra" ]; then
		NODETYPE="db"
	elif [ "$1" = "glassfish" ]; then
		NODETYPE="web"
	elif [ "$1" = "elasticsearch" ]; then
		NODETYPE="search"
	fi
	echo $NODETYPE
}

function pushConfig () {
	instid=$1
	host=$2
	ipaddr=$3
	count=$4
	NODETYPE=$5
	ZIPNAME=$6
		
	### set node id and set tag
	sed -e "1,/\\\$nodename/ s/\\\$nodename.*/\\\$nodename = \"$NODETYPE$count\"/" -i.bak ./$MODNAME/manifests/init.pp
	
	# skip autotagging and LB regging when running on local virtual machine
	if [ -z "$3" ]; then				
		ec2-create-tags --region $REGION $instid --tag Name="$NODETYPE$count"					
	fi
					
	### push updated puppet script
	echo "copying $MODNAME.zip to $NODETYPE$count..."
	zip -rq $ZIPNAME.zip $MODNAME/
	scp $ZIPNAME.zip ubuntu@$host:~/
	if [ $NODETYPE = "web" ]; then
		scp -C schools.txt ubuntu@$host:~/
	fi	
}

function cleanupAndExec () {
	ZIPNAME=$1
	FILE2=$2
	host=$3
	cmd="sudo rm -rf $MODULESDIR/$MODNAME; sudo unzip -qq -o ~/$ZIPNAME.zip -d $MODULESDIR/ && sudo puppet apply -e 'include $MODNAME'; rm -rf ~/$ZIPNAME.zip"
	
	### cleanup
	rm ./$MODNAME/manifests/*.bak $ZIPNAME.zip
	
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
	NODETYPE=$(getType $GROUP)
	FILE1="$NODETYPE$F1SUFFIX"
	FILE2="$NODETYPE$F2SUFFIX"	
	ZIPNAME="$MODNAME-$NODETYPE"
	
	if [ "$1" = "init" ]; then
		### get info about all web servers		
		if [ "$GROUP" = "all" ]; then
			for grp in "cassandra" "glassfish" "elasticsearch"; do
				ntype=$(getType $grp)
				f1="$ntype$F1SUFFIX"
				f2="$ntype$F2SUFFIX"
				init $grp $f1 $f2
			done			
		else
			init $GROUP $FILE1 $FILE2
		fi	
		echo "done."	
	elif [ "$1" = "all" ]; then	
		if [ -e "db$F1SUFFIX" ]; then
			# set seed nodes to be the first two IPs
			dbseeds=$(sed -n 1,2p "db$F1SUFFIX" | awk '{ print $3" " }' | tr -d "\n" | awk '{ print $1","$2 }' | sed 's/,$//g')			
			sed -e "1,/\\\$dbseeds/ s/\\\$dbseeds.*/\\\$dbseeds = \"$dbseeds\"/" -i.bak ./$MODNAME/manifests/init.pp			
		fi
				
		count=1	
		while read i; do
			instid=$(echo $i | awk '{ print $1 }')
			host=$(echo $i | awk '{ print $2 }')
			ipaddr=$(echo $i | awk '{ print $3 }')
			
			if [ -n "$host" ]; then
				pushConfig $instid $host $ipaddr $count $NODETYPE $ZIPNAME
				count=$((count+1))
			fi
	 	done < $FILE1	
		
		cleanupAndExec $ZIPNAME $FILE2
	elif [ `expr $1 : '^node-.*$'` != 0 ]; then
		tag=$(expr $1 : '^node-\(.*\)$')	
		count=$(expr $tag : '^[a-zA-Z]*\([0-9]*\)$')	
		i=$(ec2din --region $REGION --filter group-name=$GROUP -F "tag-value=$tag" | egrep ^INSTANCE | awk '{ print $2,$4,$15}')
		instid=$(echo $i | awk '{ print $1 }')
		host=$(echo $i | awk '{ print $2 }')
		ipaddr=$(echo $i | awk '{ print $3 }')
		if [ -n "$host" ]; then
			pushConfig $instid $host $ipaddr $count $NODETYPE $ZIPNAME
			cleanupAndExec $ZIPNAME $FILE2 $host
		fi		
	elif [ "$1" = "lbadd" ]; then	
	 	### register instance with LB
		$AWS_ELB_HOME/bin/elb-register-instances-with-lb $LBNAME --region $REGION --instances $2
	elif [ "$1" = "lbremove" ]; then	
	 	### deregister instance from LB
		$AWS_ELB_HOME/bin/elb-deregister-instances-from-lb $LBNAME --region $REGION --instances $2
	elif [ "$1" = "restoredb" ]; then
		### load snapshot from S3 + unpack
		echo "TODO"
	fi
elif [ "$1" = "checkdb" ]; then
	### check if db is up and running and ring is OK
	db1host=$(head -n 1 "db$F2SUFFIX")
	ssh -n ubuntu@$db1host "/home/cassandra/cassandra/bin/nodetool -h localhost ring"
elif [ "$1" = "initdb" ]; then
	### load schema definition from file on db1
	db1host=$(head -n 1 "db$F2SUFFIX")
	ssh -n ubuntu@$db1host "sudo -u cassandra /home/cassandra/cassandra/bin/cassandra-cli -h localhost -f /usr/share/puppet/modules/scoold/files/schema-light.txt"	
elif [ "$1" = "esindex" ]; then
	### create elasticsearch index
	es1host=$(head -n 1 "search$F2SUFFIX")	
	ssh -n ubuntu@$es1host "sudo -u elasticsearch curl -s -XPUT localhost:9200/scoold -d @/home/elasticsearch/elasticsearch/config/index.json"
elif [ "$1" = "esriver" ]; then
	es1host=$(head -n 1 "search$F2SUFFIX")
	### create elasticsearch river
	cmd0="sudo -u elasticsearch rm -rf /home/elasticsearch/elasticsearch/plugins/$RIVERFILE"
	cmd1="sudo -u elasticsearch curl -s -o /home/elasticsearch/$RIVERFILE.zip $RIVERLINK"
	cmd2="sudo -u elasticsearch unzip -o -d /home/elasticsearch/elasticsearch/plugins/$RIVERFILE /home/elasticsearch/$RIVERFILE.zip"
	cmd3="sudo -u elasticsearch chmod -R 755 /home/elasticsearch/elasticsearch/plugins/$RIVERFILE/*"
	cmd4="sudo -u elasticsearch rm /home/elasticsearch/$RIVERFILE.zip"
	cmd5="sudo -u elasticsearch curl -s -XPUT localhost:9200/_river/scoold/_meta -d '{ \"type\" : \"amazonsqs\" }'"
	ssh -n ubuntu@$es1host "$cmd0; $cmd1; $cmd2; $cmd3; $cmd4; $cmd5"
elif [ "$1" = "createlb" ]; then
	$AWS_ELB_HOME/bin/elb-create-lb $LBNAME --region $REGION --availability-zones "eu-west-1a,eu-west-1b,eu-west-1c" --listener "protocol=http,lb-port=80,instance-port=8080"
	$AWS_ELB_HOME/bin/elb-configure-healthcheck $LBNAME --region $REGION --target "HTTP:8080/" --interval 30 --timeout 3 --unhealthy-threshold 2 --healthy-threshold 2
else
	echo "USAGE: $0 checkdb | initdb | inites | [init | all] group"
fi
# rsync --verbose --progress --stats --compress --recursive --times --perms --links --delete --rsync-path="sudo rsync" ~/Desktop/scoold_snapshots/1317214358735/ ubuntu@192.168.113.128:/var/lib/cassandra/data/scoold/