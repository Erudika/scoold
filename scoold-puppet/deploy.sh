# !/bin/bash

# Latest build: 
# https://erudika.ci.cloudbees.com/job/scoold/ws/scoold-web/target/scoold-web.war
# https://erudika.ci.cloudbees.com/job/scoold/ws/scoold-web/target/scoold-web/styles/style.css

FILE1="web-instances.txt"
FILE2="web-hostnames.txt"
JAUTH="./scoold/files/jenkins-auth.txt"
ASADMIN="sudo -u glassfish /home/glassfish/glassfish/bin/asadmin"
LBNAME="ScooldLB"	
REGION="eu-west-1"
WAR=$1	
CONTEXT=""
ENABLED="false"
FILENAME=$(expr $WAR : '.*/\(.*\)$')
WARPATH="/home/ubuntu/$FILENAME"
VERSION=$(date +%F-%H%M%S)
APPNAME="scoold-web-$VERSION"
BUCKET="com.scoold.files"
CSSDIR="../scoold-web/target/scoold-web/styles"
JSDIR="../scoold-web/target/scoold-web/scripts"
IMGDIR="../scoold-web/target/scoold-web/images"
STYLECSS="https://erudika.ci.cloudbees.com/job/scoold/ws/scoold-web/target/scoold-web/styles/style-min.css"

function uploadJacssi () {
	echo "Deploying javascript, css and images to CDN..."
	### copy javascript, css, images to S3		
	if [ -e $CSSDIR ] && [ -e $JSDIR ] && [ -e $IMGDIR ]; then
		# upload to S3
		AUTH="-u $(cat $JAUTH)"			
		curl -s $AUTH -o $CSSDIR/style-min.css $STYLECSS
		s3cmd --reduced-redundancy --acl-public --force put $CSSDIR/*min.css $CSSDIR/pictos* $JSDIR/*min.js $JSDIR/*.htc $IMGDIR/*.gif $IMGDIR/*.png s3://$BUCKET
	fi
}

if [ -n "$1" ] && [ -n "$2" ]; then
	if [ -n "$2" ] && [ "$2" !=  "true" ] && [ "$2" !=  "false" ]; then
		CONTEXT="--contextroot $2"
	fi

	if [ "$3" = "true" ] || [ "$2" = "true" ]; then
		ENABLED="true"
	fi	

	if [ -f $FILE1 ] && [ -f $FILE2 ]; then		
		echo "-------------------  BEGIN ROLLING UPGRADE  -----------------------"
		echo ""
		echo "Deploying '$APPNAME' [enabled=$ENABLED]"

		if [ `expr $1 : '^http.*$'` != 0 ]; then
			AUTH=""
			if [ -e "$JAUTH" ]; then
				AUTH="-u $(cat $JAUTH)"
			fi
			FETCHWAR="curl -s $AUTH -o $WARPATH $WAR"
		else
			FETCHWAR="echo -n ''"
			pssh/bin/pscp -h $FILE2 -l ubuntu $1 $WARPATH
		fi

		### STEP 1: deploy .war file to all servers	(enabled if deployed for the first time, disabled otherwise)
		pssh/bin/pssh -h $FILE2 -l ubuntu -t 0 -i "$FETCHWAR && chmod 755 $WARPATH && $ASADMIN deploy --enabled=$ENABLED $CONTEXT --name $APPNAME $WARPATH && rm $WARPATH"

		OLDAPP=""
		while read i; do
			if [ -n "$i" ]; then
				instid=$(echo $i | awk '{ print $1 }')
				host=$(echo $i | awk '{ print $2 }')

				if [ "$ENABLED" = "false" ]; then					
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
					    echo "OK! Undeploying old application..."
						ssh -n ubuntu@$host "$ASADMIN undeploy $OLDAPP"
					else
						### STEP 4.1 REVERT back to old application
						echo "NOT OK! Reverting back to old application..."
						ssh -n ubuntu@$host "$ASADMIN disable $APPNAME && $ASADMIN enable $OLDAPP"
					fi			
				fi	
				### STEP 5: register application back with the LB
				$AWS_ELB_HOME/bin/elb-register-instances-with-lb $LBNAME --region $REGION --quiet --instances $instid
			fi
		done < $FILE1
		uploadJacssi
		echo ""
		echo "---------------------------- DONE ---------------------------------"	
	fi
elif [ "$1" = "uploadjacssi" ]; then
	uploadJacssi
else
	echo "USAGE:  $0 (war | uploadjacssi)  [context | enabled]"
fi

