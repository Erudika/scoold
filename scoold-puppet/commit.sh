# !/bin/bash

LBNAME="ScooldLB"
REGION="eu-west-1"
MODULESDIR=/usr/share/puppet/modules
MODNAME="scoold"
NODETYPE="unknown"

if [ -n "$1$2" ]; then
	GROUP=$2	
			
	if [ $GROUP = "cassandra" ]; then
		NODETYPE="db"
	elif [ $GROUP = "glassfish" ]; then
		NODETYPE="web"
	elif [ $GROUP = "elasticsearch" ]; then
		NODETYPE="search"
	fi
	
	FILE1="$NODETYPE-instances.txt"
	FILE2="$NODETYPE-hostnames.txt"
	
	if [ "$1" = "init" ]; then				
		### commit to all hosts in group, get info about all web servers
		ec2-describe-instances --region $REGION --filter group-name=$GROUP | egrep ^INSTANCE | awk '{ print $2,$4}' > $FILE1
		cat $FILE1 | awk '{print $2}' > $FILE2 # hostnames only
		echo "done."
	elif [ "$1" = "all" ]; then
		count=1		
		while read i; do	
			instid=$(echo $i | awk '{ print $1 }')
			host=$(echo $i | awk '{ print $2 }')
			
	 		if [ -n "$host" ]; then			
	 			### set node id and set tag
				sed -e "1,/\\\$nodename/ s/\\\$nodename.*/\\\$nodename = \"$NODETYPE$count\"/" -i.bak ./$MODNAME/manifests/init.pp
				rm ./$MODNAME/manifests/*.bak
				
				zip -rq $MODNAME $MODNAME/				
				ec2-create-tags --region $REGION $instid --tag Name="$NODETYPE$count"
				
				### push puppet script
				echo "copying $MODNAME.zip to $NODETYPE$count..."
				scp $MODNAME.zip ubuntu@$host:~/
												
	 			count=$((count+1))
	 		fi		
	 	done < $FILE1			
		echo "done. executing puppet code in parallel."
		### unzip & execute remotely
		pssh/bin/pssh -h $FILE2 -l ubuntu -i "sudo rm -rf $MODULESDIR/$MODNAME; sudo unzip -qq ~/$MODNAME.zip -d $MODULESDIR/; sudo puppet apply -e 'include $MODNAME'"
	fi
fi
