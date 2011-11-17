# !/bin/bash

# Latest build: 
# https://erudika.ci.cloudbees.com/job/scoold/ws/scoold-web/target/scoold-web.war
# https://erudika.ci.cloudbees.com/job/scoold/ws/scoold-web/target/scoold-web/styles/style.css

F1SUFFIX="-instances.txt"
F2SUFFIX="-hostnames.txt"
FILE1="web$F1SUFFIX"
FILE2="web$F2SUFFIX"
JAUTH="jenkins-auth.txt"
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
CSSDIR="/Users/alexb/Desktop/scoold/scoold-web/target/scoold-web/styles"
JSDIR="/Users/alexb/Desktop/scoold/scoold-web/target/scoold-web/scripts"
IMGDIR="/Users/alexb/Desktop/scoold/scoold-web/target/scoold-web/images"
SUMSFILE="checksums.txt"
PATHSFILE="filepaths.txt"
JARPATH="../scoold-invalidator/target/scoold-invalidator-all.jar"

function updateJacssi () {
	echo "Deploying javascript, css and images to CDN..."
	### copy javascript, css, images to S3				
	if [ -d $CSSDIR ] && [ -d $JSDIR ] && [ -d $IMGDIR ]; then
		dirlist="$CSSDIR/*min.css $CSSDIR/pictos* $JSDIR/*min.js $JSDIR/*.htc $IMGDIR/*.gif $IMGDIR/*.png"
		rm -rf $SUMSFILE $PATHSFILE &> /dev/null
		for file in `ls -d1 $dirlist`; do
			if [ -f "$file" ]; then
				name=$(expr $file : '.*/\(.*\)$')
				echo "$name $(md5 -q $file)" >> $SUMSFILE
				echo "$name $file" >> $PATHSFILE
			fi
		done
		
		if [ -n "$1" ] && [ "$1" = "invalidateall" ]; then
			SUMSFILE="all"
			PATHSFILE=""
		fi
	
		# upload to S3
		#s3cmd --reduced-redundancy --acl-public --force put $dirlist $SUMSFILE s3://$BUCKET
		java -jar $JARPATH $SUMSFILE $PATHSFILE
	fi
}

if [ -n "$1" ] && [ -n "$2" ] && [ "$1" != "updatejacssi" ] && [ "$1" != "cmd" ]; then
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
			if [ -e $JAUTH ]; then
				AUTH="-u $(cat $JAUTH)"
			fi
			FETCHWAR="curl -s $AUTH -o $WARPATH $WAR"
		else
			APPNAME=$(expr $FILENAME : '\(.*\)\.war$')
			FETCHWAR="echo -n ''"
			pssh/bin/pscp -h $FILE2 -l ubuntu $1 $WARPATH
		fi

		### STEP 1: deploy .war file to all servers	(enabled if deployed for the first time, disabled otherwise)
		pssh/bin/pssh -h $FILE2 -l ubuntu -t 0 -i "$FETCHWAR && chmod 755 $WARPATH && $ASADMIN deploy --enabled=$ENABLED $CONTEXT --name $APPNAME $WARPATH && rm $WARPATH"

		OLDAPP=""
		isok=false
		dbhosts=$(cat "db$F1SUFFIX" | awk '{ print $3"," }' | tr -d "\n" | sed 's/,$//g')
		eshost=$(head -n 1 "search$F2SUFFIX")
		production="true"
		prefix="com.scoold"
		count=1		
		while read i; do
			if [ -n "$i" ]; then
				instid=$(echo $i | awk '{ print $1 }')
				host=$(echo $i | awk '{ print $2 }')			
				
				### STEP 2: set system properties
				ssh -n ubuntu@$host "$ASADMIN create-system-properties $prefix.workerid=$count:$prefix.production=\"$production\":$prefix.dbhosts=\"$dbhosts\":$prefix.eshost=\"$eshost\""
				
				if [ "$ENABLED" = "false" ] && [ -n "$host" ]; then					
					if [ -z "$OLDAPP" ]; then
						OLDAPP=$(ssh -n ubuntu@$host "$ASADMIN list-applications --type web --long | grep enabled | awk '{ print \$1 }'")
					fi

					### STEP 3: deregister each instance from the LB, consecutively 
					$AWS_ELB_HOME/bin/elb-deregister-instances-from-lb $LBNAME --region $REGION --quiet --instances $instid
					sleep 10
					
					### STEP 4: disable old deployed application and enable new application
					ssh -n ubuntu@$host "$ASADMIN disable $OLDAPP && $ASADMIN enable $APPNAME"

					if [ $isok = false ]; then
						### STEP 5: TEST, TEST, TEST! then continue
						echo "TEST NOW! => $host"
						read -p "'ok' to confirm > " response < /dev/tty
					fi
					
					if [ "$response" = "ok" ] || [ $isok = true ]; then
					    echo "OK! Undeploying old application..."
						ssh -n ubuntu@$host "$ASADMIN undeploy $OLDAPP"
						isok=true
					else
						### STEP 5.1 REVERT back to old application
						echo "NOT OK! Reverting back to old application..."
						ssh -n ubuntu@$host "$ASADMIN disable $APPNAME && $ASADMIN enable $OLDAPP"
						isok=false
					fi			
				fi					
				count=$((count+1))
				### STEP 6: register application back with the LB
				$AWS_ELB_HOME/bin/elb-register-instances-with-lb $LBNAME --region $REGION --quiet --instances $instid
				sleep 6
			fi
		done < $FILE1
		updateJacssi
		echo ""
		echo "---------------------------- DONE ---------------------------------"	
	fi
elif [ "$1" = "updatejacssi" ]; then
	updateJacssi $2
elif [ "$1" = "cmd" ]; then
	pssh/bin/pssh -h $FILE2 -l ubuntu -t 0 -i "$ASADMIN $2"
else
	echo "USAGE:  $0 (war | updatejacssi [invalidateall])  [context | enabled]"
fi

