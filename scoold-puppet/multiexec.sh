#!/bin/bash

F2SUFFIX="-hostnames.txt"
FILE="all.txt"

if [ -n "$1" ] && [ -n "$2" ]; then
	if [ "$1" = "all" ]; then
		for grp in "db" "web" "search"; do			
			cat "$grp$F2SUFFIX" >> $FILE			
		done
	else
		FILE="$1$F2SUFFIX"
	fi
	
	pssh/bin/pssh -h $FILE -l ubuntu -t 0 -i $2
	
	if [ "$1" = "all" ]; then
		rm $FILE
	fi
fi

