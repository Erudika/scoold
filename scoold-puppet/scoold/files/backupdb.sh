#!/bin/bash

BUCKET="com.scoold.cassandra"
TIMESTAMP=$(date +%F-%H%M%S)
DIR1="/var/lib/cassandra/data/scoold/snapshots"
DIR2="/var/lib/cassandra/data/system/snapshots"
FILE="snapshot-$TIMESTAMP.tar.gz"
if [ -n "$1" ] && [ "$1" != "default" ]; then
	FILE="$1.tar.gz"
fi

/home/cassandra/cassandra/bin/nodetool -h localhost snapshot

if [ -e $DIR1 ] && [ -e $DIR2 ]; then
	rename 's/\d+/scoold/' $DIR1/*
	rename 's/\d+/system/' $DIR2/*
	mv -f $DIR2/system $DIR1/system	
	# gzip snapshot
	tar czf $FILE -C $DIR1 .
	# upload to S3
	s3cmd -c /home/cassandra/.s3cfg put $FILE s3://$BUCKET
	# clean up
	rm $FILE
fi

/home/cassandra/cassandra/bin/nodetool -h localhost clearsnapshot