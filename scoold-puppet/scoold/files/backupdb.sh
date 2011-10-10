#!/bin/bash

BUCKET="com.scoold.cassandra"
TIMESTAMP=$(date +%F_%T)
DIR1="/var/lib/cassandra/data/scoold/snapshots"
DIR2="/var/lib/cassandra/data/system/snapshots"
BAK="snapshot-$TIMESTAMP.tar.gz"

if [ -e $DIR1 ] && [ -e $DIR2 ]; then
	# gzip snapshot
	tar czf $BAK $DIR1 $DIR2
	# upload to S3
	s3cmd put $BAK s3://$BUCKET
	# clean up
	rm $BAK
fi