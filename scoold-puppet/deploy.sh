# !/bin/bash

if [ -n "$1" ] && [ -n "$2" ] && [ -n "$3" ]; then
	FILE1="web-instances.txt"
	FILE2="web-hostnames.txt"
	ASADMIN="sudo -u glassfish /home/glassfish/glassfish/bin/asadmin"
	LBNAME="ScooldLB"
	REGION="eu-west-1"
	WAR=$1
	APPNAME=$2
	CONTEXT=$3
	ENABLED="false"
	FILENAME=$(expr $WAR : '.*/\(.*\)$')
	
	if [ "$4" = "true" ]; then
		ENABLED="true"
	fi	
		
	if [ -f $FILE1 && -f $FILE2 ]; then		
		echo "-------------------  ROLLING UPGRADE SCRIPT -----------------------"
		echo ""
		echo "Deploying '$APPNAME' --> /$CONTEXT [enabled=$ENABLED]"
				
		if [ `expr $1 : '^http.*$'` != 0 ]; then
			AUTH=""
			if [ -e "jenkins.txt" ]; then
				AUTH="-u $(cat jenkins.txt)"
			fi
			FETCHWAR="curl -s $AUTH $WAR > ~/$FILENAME"
		else
			FETCHWAR="echo -n ''"
			pssh/bin/pscp -h $FILE2 -l ubuntu $1 /home/ubuntu/$FILENAME
		fi
		
		### STEP 1: deploy .war file to all servers	(enabled if deployed for the first time, disabled otherwise)
		pssh/bin/pssh -h $FILE2 -l ubuntu -t 0 -i "$FETCHWAR && chmod 755 ~/$FILENAME && $ASADMIN deploy --enabled=$ENABLED --contextroot $CONTEXT --name $APPNAME ~/$FILENAME"
		

		if [ "$ENABLED" = "false" ]; then			
			OLDAPP=""
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
	fi
fi

# git archive command
# git --git-dir=../.git archive --format=zip --prefix=scoold/ -o ~/Desktop/scoold.zip HEAD
