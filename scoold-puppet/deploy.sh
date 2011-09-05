# !/bin/bash

if [ -n "$1" ]; then
	FILE=$(expr $1 : '.*/\(.*\)$')
	FILE1="web-instances.txt"
	FILE2="web-hostnames.txt"
	APPNAME=$(expr $FILE : '^\(.*\)\.war$')
	ASADMIN="sudo -u glassfish /home/glassfish/glassfish/bin/asadmin"
	CONTEXT=$APPNAME
	ENABLE="false"
	LBNAME="ScooldLB"
	REGION="eu-west-1"
	
	if [ -n "$2" ]; then
		CONTEXT=$2
		if [ $3 == "true" ]; then
			ENABLE=$3		
		fi
		if [ -n "$4" ]; then
			APPNAME="$APPNAME$4"
		fi
	fi	
		
	if [ -f $FILE1 ] && [ -f $FILE2 ]; then		
		echo "-------------------  ROLLING UPGRADE SCRIPT -----------------------"
		echo ""
		echo "Uploading $FILE as '$APPNAME' --> /$CONTEXT [enabled=$ENABLE]"	
		
		### STEP 1: upload .war file to all servers	(enabled if deployed for the first time, disabled otherwise)
		pssh/bin/pscp -h $FILE2 -l ubuntu $1 /home/ubuntu/$FILE && 
		pssh/bin/pssh -h $FILE2 -l ubuntu -i "chmod 755 ~/$FILE && $ASADMIN deploy --enabled=$ENABLE --contextroot $CONTEXT --name $APPNAME ~/$FILE"

		OLDAPP=""

		if [ $ENABLE = "false" ]; then			
			while read i; do
				if [ -n "$i" ]; then
					instid=$(echo $i | awk '{ print $1 }')
					host=$(echo $i | awk '{ print $2 }')

					if [ -z "$OLDAPP" ]; then
						OLDAPP=$(ssh -n ubuntu@$host "$ASADMIN list-applications --type web --long | grep enabled | awk '{ print \$1 }'")
					fi

					### STEP 2: deregister each instance from the LB, consecutively 
					$AWS_ELB_HOME/bin/elb-deregister-instances-from-lb $LBNAME --region $REGION --quiet --instances $instid

					### STEP 3: disable old deployed application and enable new application
					ssh -n ubuntu@$host "$ASADMIN disable $OLDAPP && $ASADMIN enable $APPNAME"

					### STEP 4: TEST, TEST, TEST! then continue
					echo "TEST NOW! => $host"
					read -p "'ok' to confirm > " response < /dev/tty

					if [ "$response" = "ok" ]; then
					    echo "OK!"
					else
						### STEP 4.1 REVERT back to old application
						echo "NOT OK! Reverting back to old application..."
						ssh -n ubuntu@$host "$ASADMIN disable $APPNAME && $ASADMIN enable $OLDAPP"
					fi

					### STEP 5: register application back with the LB
					$AWS_ELB_HOME/bin/elb-register-instances-with-lb $LBNAME --region $REGION --quiet --instances $instid				
				fi			
			done < $FILE1
		fi
		echo ""
		echo "---------------------------- DONE ---------------------------------"	
	else
		echo "ERROR: hostname files $FILE1, $FILE2 are missing."	
	fi
fi
