#!/bin/bash

BUCKET="com.scoold.cassandra"
FILE=$1

if [ -n "$1" ]; then
	s3cmd -c /home/cassandra/.s3cfg get s3://$BUCKET/$FILE /home/cassandra/
	if [ -f /home/cassandra/$FILE ]; then
		rm -rf /var/lib/cassandra/data/* /var/lib/cassandra/commitlog/* /var/lib/cassandra/saved_caches/*
		tar -C /var/lib/cassandra/data/ -xzf /home/cassandra/$FILE
		rm /home/cassandra/$FILE
	fi	
else
	echo "USAGE:	$0 file"
fi
