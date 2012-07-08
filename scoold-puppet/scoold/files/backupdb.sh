#!/bin/bash

BUCKET="com.scoold.cassandra"
TIMESTAMP=$(date +%F-%H%M%S)
DIR="/home/cassandra/backup"
D1="/var/lib/cassandra/data/scoold"
D2="/var/lib/cassandra/data/system"
DIR1="$D1/snapshots"
DIR2="$D2/snapshots"
FILE="snapshot-$TIMESTAMP.tar.gz"
if [ -n "$1" ] && [ "$1" != "default" ]; then
	FILE="$1.tar.gz"
fi

/home/cassandra/cassandra/bin/nodetool -h localhost clearsnapshot
/home/cassandra/cassandra/bin/nodetool -h localhost snapshot -t $TIMESTAMP

mkdir -p $DIR/scoold $DIR/system

for d1 in $D1/*; do
	name=$(expr "$d1" : '.*/\(.*\)$')
	if [ -e "$d1/snapshots/$TIMESTAMP" ]; then
		mv $d1/snapshots/$TIMESTAMP $DIR/scoold/$name
	fi
done

for d2 in $D2/*; do
	name=$(expr "$d2" : '.*/\(.*\)$')
	if [ -e "$d2/snapshots/$TIMESTAMP" ]; then
		mv $d2/snapshots/$TIMESTAMP $DIR/system/$name
	fi
done

# gzip snapshot
tar czf $FILE -C $DIR .
# upload to S3
s3cmd -c /home/cassandra/.s3cfg put $FILE s3://$BUCKET
# clean up
rm -rf $FILE $DIR

/home/cassandra/cassandra/bin/nodetool -h localhost clearsnapshot