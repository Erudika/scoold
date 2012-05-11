#!/bin/bash

# https://erudika.ci.cloudbees.com/job/scoold/ws/scoold-web/target/scoold-web/styles/style.css
  
F1SUFFIX="-instances.txt"
F2SUFFIX="-hostnames.txt"
FILE1="glassfish$F1SUFFIX"
FILE2="glassfish$F2SUFFIX"
KEYS="keys.txt"
JAUTH="jenkins-auth.txt"
ASADMIN="sudo -u glassfish /home/glassfish/glassfish/bin/asadmin"
LBNAME="ScooldLB"	
REGION="eu-west-1"
CONTEXT=""
ENABLED="false"
VERSION=$(date +%F-%H%M%S)
APPNAME="scoold-web-$VERSION"
BUCKET="com.scoold.files"
CSSDIR="/Users/alexb/Desktop/scoold/scoold-web/target/scoold-web/styles"
JSDIR="/Users/alexb/Desktop/scoold/scoold-web/target/scoold-web/scripts"
IMGDIR="/Users/alexb/Desktop/scoold/scoold-web/target/scoold-web/images"
JACSSIDIR="/Users/alexb/Desktop/scoold/scoold-web/target/scoold-web/jacssi"
HOMEDIR="/home/ubuntu/"
SUMSFILE="checksums.txt"
PATHSFILE="filepaths.txt"
JARPATH="../scoold-invalidator/target/scoold-invalidator-all.jar"
CIWARPATH="https://erudika.ci.cloudbees.com/job/scoold/ws/scoold-web/target/scoold-web.war"

function readProp () {
	FILE=$1
	echo $(sed '/^\#/d' $FILE | grep "$2" | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
}

AWSACCESSKEY=$(readProp $KEYS "com.scoold.awsaccesskey")
AWSSECRETKEY=$(readProp $KEYS "com.scoold.awssecretkey")
AWSSQSENDPOINT=$(readProp $KEYS "com.scoold.awssqsendpoint")
AWSSQSQUEUEID=$(readProp $KEYS "com.scoold.awssqsqueueid")
FBAPPID=$(readProp $KEYS "com.scoold.fbappid")
FBAPIKEY=$(readProp $KEYS "com.scoold.fbapikey")
FBSECRET=$(readProp $KEYS "com.scoold.fbsecret")
AWSCFDIST=$(readProp $KEYS "com.scoold.awscfdist")

function updateJacssi () {
	echo "Deploying javascript, css and images to CDN..."
	### copy javascript, css, images to S3				
	if [ -d $CSSDIR ] && [ -d $JSDIR ] && [ -d $IMGDIR ]; then
		dirlist="$CSSDIR/*min.css $CSSDIR/pictos* $JSDIR/*min.js $IMGDIR/*.gif $IMGDIR/*.png"
		rm -rf $SUMSFILE $PATHSFILE &> /dev/null
		if [ ! -d $JACSSIDIR ]; then
			mkdir $JACSSIDIR
		fi
		
		for file in `ls -d1 $dirlist`; do
			if [ -f $file ]; then
				name=$(expr "$file" : '.*/\(.*\)$')
				gzip -9 -c $file > $JACSSIDIR/$name
				echo "$name $(md5 -q $file)" >> $SUMSFILE
				echo "$name $JACSSIDIR/$name" >> $PATHSFILE				
			fi
		done
		
		if [ -n "$1" ] && [ "$1" = "invalidateall" ]; then
			SUMSFILE="all"
			PATHSFILE=""
		fi
	
		# upload to S3		
		java -Dawscfdist="$AWSCFDIST" -Dawsaccesskey="$AWSACCESSKEY" -Dawssecretkey="$AWSSECRETKEY" -jar $JARPATH $SUMSFILE $PATHSFILE
		rm -rf $JACSSIDIR
	fi
}

function setProperties () {
	dbhosts=$3
	eshosts=$4
	if [ -z "$3" ]; then
		dbhosts=$(ec2din --region $REGION --filter "instance-state-name=running" -F "tag-value=cassandra1" | egrep ^INSTANCE | awk '{ print $16}')
	fi	
	if [ -z "$4" ]; then
		eshosts=$(ec2din --region $REGION --filter "instance-state-name=running" -F "tag-value=elasticsearch1" | egrep ^INSTANCE | awk '{ print $16}')
	fi
	
	prop1="com.scoold.workerid=$2"
	prop2="com.scoold.production=\"true\""
	prop3="com.scoold.dbhosts=\"$dbhosts\""
	prop4="com.scoold.eshosts=\"$eshosts\""
	
	prop5="com.scoold.awsaccesskey=\"$AWSACCESSKEY\""
	prop6="com.scoold.awssecretkey=\"$AWSSECRETKEY\""
	prop7="com.scoold.awssqsendpoint=\"$(echo $AWSSQSENDPOINT | sed 's/:/\\\:/g')\""
	prop8="com.scoold.awssqsqueueid=\"$AWSSQSQUEUEID\""
	prop9="com.scoold.fbappid=\"$FBAPPID\""
	prop10="com.scoold.fbapikey=\"$FBAPIKEY\""
	prop11="com.scoold.fbsecret=\"$FBSECRET\""
	prop12="com.scoold.awscfdist=\"$AWSCFDIST\""	
	
	ssh -n ubuntu@$1 "$ASADMIN create-system-properties $prop1:$prop2:$prop3:$prop4:$prop5:$prop6:$prop7:$prop8:$prop9:$prop10:$prop11:$prop12"
}

function deployWAR () {
	# params $1=MULTI, $2=WAR, $3=ENABLED, $4=CONTEXT
	if [ -n "$1" ] && [ -n "$2" ] && [ -n "$3" ]; then
		deffile="uploadto.txt"
		hostsfile=$deffile
		
		if [ `expr "$1" : '^glassfish[0-9]*$'` != 0 ]; then
			host=$(ec2din --region $REGION --filter "instance-state-name=running" -F "tag-value=$1" | egrep ^INSTANCE | awk '{ print $4}')
			nodeid=$(expr "$1" : '^glassfish\([0-9]*\)$')
			setProperties $host $nodeid
			echo $host > $hostsfile;
		else
			hostsfile=$FILE2
		fi
		
		if [ "$2" = "jenkins" ]; then
			auth=""
			if [ -e $JAUTH ]; then
				auth="-u $(cat $JAUTH)"
			fi
			filename=$(expr "$CIWARPATH" : '.*/\(.*\)$')
			warpath="$HOMEDIR$filename"
			fetchwar="curl -s $auth -o $warpath $CIWARPATH"
		else
			filename=$(expr "$2" : '.*/\(.*\)$')
			warpath="$HOMEDIR$filename"
			APPNAME=$(expr "$filename" : '\(.*\)\.war$')
			fetchwar="echo -n 'Uploading war file...'"
			pssh/bin/pscp -h $hostsfile -l ubuntu $2 $warpath
			echo -n 'Done.'
		fi
		
		pssh/bin/pssh -h $hostsfile -l ubuntu -t 0 -i "$fetchwar && chmod 755 $warpath && $ASADMIN deploy --enabled=$3 $4 --name $APPNAME $warpath; rm $warpath"
		
		if [ -f $deffile ]; then
			rm $deffile
		fi
	fi	
}

if [ "$1" = "updatejacssi" ]; then
	updateJacssi $2
elif [ "$1" = "setprops" ]; then	
	setProperties $2 $3 $4 $5
elif [ -n "$1" ] && [ -n "$2" ]; then	
	if [ "$1" = "cmd" ]; then
		if [ `expr "$2" : '^glassfish[0-9]*$'` != 0 ] && [ -n "$3" ]; then
			host=$(ec2din --region $REGION --filter "instance-state-name=running" -F "tag-value=$2" | egrep ^INSTANCE | awk '{ print $4}')
			if [ -n "$host" ]; then
				ssh -n ubuntu@$host "$ASADMIN $3"
			fi		
		else
			pssh/bin/pssh -h $FILE2 -l ubuntu -t 0 -i "$ASADMIN $2"		
		fi	
	elif [ `expr "$1" : '^glassfish[0-9]*$'` != 0 ]; then
		if [ -n "$4" ]; then
			CONTEXT="--contextroot $4"
		fi
		deployWAR $1 $2 $3 $CONTEXT
	else
		if [ "$2" !=  "true" ] && [ "$2" !=  "false" ]; then
			CONTEXT="--contextroot $2"
		fi

		if [ "$3" = "true" ] || [ "$2" = "true" ]; then
			ENABLED="true"
		fi	

		if [ -f $FILE1 ] && [ -f $FILE2 ]; then		
			echo "-------------------  BEGIN ROLLING UPGRADE  -----------------------"
			echo ""
			echo "Deploying '$APPNAME' [enabled=$ENABLED]"

			updateJacssi

			### STEP 1: deploy .war file to all servers	(enabled if deployed for the first time, disabled otherwise)			
			deployWAR "all" $1 $ENABLED $CONTEXT

			OLDAPP=""
			isok=false
			dbhosts=$(cat "cassandra$F1SUFFIX" | awk '{ print $3"," }' | tr -d "\n" | sed 's/,$//g')
			eshosts=$(cat "elasticsearch$F1SUFFIX" | awk '{ print $3"," }' | tr -d "\n" | sed 's/,$//g')			
			count=1		
			while read i; do
				if [ -n "$i" ]; then
					instid=$(echo $i | awk '{ print $1 }')
					host=$(echo $i | awk '{ print $2 }')			
					
					if [ "$ENABLED" = "false" ] && [ -n "$host" ]; then					
						if [ -z "$OLDAPP" ]; then
							OLDAPP=$(ssh -n ubuntu@$host "$ASADMIN list-applications --type web --long | grep enabled | awk '{ print \$1 }'")
						fi
						
						### STEP 2: set system properties
						setProperties $host $count $dbhosts $eshosts
						
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
							### STEP 5.1 undeploy old application
						    echo "OK! Undeploying old application..."
							ssh -n ubuntu@$host "$ASADMIN undeploy $OLDAPP"
							isok=true
						else
							### STEP 4.2 REVERT back to old application
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
			echo ""
			echo "---------------------------- DONE ---------------------------------"	
		fi
	fi
else
	echo "USAGE:  $0 [ glassfishXXX war | war  [ enabled context ]] | updatejacssi [invalidateall] | cmd [ glassfishXXX ] gfcommand"
fi

