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
# multiexec.sh - helper script for executing commands on multiple nodes

#!/bin/bash

F2SUFFIX="-hostnames.txt"
DEFFILE="all.txt"
FILE=$DEFFILE
REGION="eu-west-1"

if [ -n "$1" ] && [ -n "$2" ]; then
	if [ "$1" = "all" ]; then
		for grp in "cassandra" "glassfish" "elasticsearch"; do			
			cat "$grp$F2SUFFIX" >> $FILE			
		done
	elif [ `expr "$1" : '^node-.*$'` != 0 ]; then
		tag=$(expr "$1" : '^node-\(.*\)$')
		i=$(ec2din --region $REGION --filter "instance-state-name=running" -F "tag-value=$tag" | egrep ^INSTANCE | awk '{ print $2,$4,$15}')
		instid=$(echo $i | awk '{ print $1 }')
		host=$(echo $i | awk '{ print $2 }')
		if [ -n "$host" ]; then
			echo "$host" > $FILE
		fi
	else
		FILE="$1$F2SUFFIX"
	fi
	
	pssh/bin/pssh -h $FILE -l ubuntu -t 0 -i $2
	
	if [ -f $DEFFILE ]; then
		rm $DEFFILE
	fi
else
	echo "USAGE:	$0 (all | node-XXX##) 'command'"
fi

