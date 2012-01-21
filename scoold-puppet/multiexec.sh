#!/bin/bash

F2SUFFIX="-hostnames.txt"
FILE="all.txt"
REGION="eu-west-1"

if [ -n "$1" ] && [ -n "$2" ]; then
	if [ "$1" = "all" ]; then
		for grp in "db" "web" "search"; do			
			cat "$grp$F2SUFFIX" >> $FILE			
		done
	elif [ `expr $1 : '^node-.*$'` != 0 ]; then
		tag=$(expr $1 : '^node-\(.*\)$')
		i=$(ec2din --region $REGION -F "tag-value=$tag" | egrep ^INSTANCE | awk '{ print $2,$4,$15}')
		instid=$(echo $i | awk '{ print $1 }')
		host=$(echo $i | awk '{ print $2 }')
		if [ -n "$host" ]; then
			echo "$host" >> $FILE
		fi
	else
		FILE="$1$F2SUFFIX"
	fi
	
	pssh/bin/pssh -h $FILE -l ubuntu -t 0 -i $2
	
	if [ -f $FILE ]; then
		rm $FILE
	fi
fi

