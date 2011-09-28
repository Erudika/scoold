#!/bin/bash

export AWS_ACCESS_KEY_ID="AKIAI5WX2PJPYQEPWECQ"
export AWS_SECRET_ACCESS_KEY="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"

BUCKET="com.scoold.cassandra"

if [ -e /var/lib/cassandra/data/scoold/snapshots ]; then
	duplicity full --no-encryption --s3-use-new-style /var/lib/cassandra/data/scoold/snapshots s3+http://$BUCKET
fi

if [ -e /var/lib/cassandra/data/system/snapshots ]; then
	duplicity full --no-encryption --s3-use-new-style /var/lib/cassandra/data/system/snapshots s3+http://$BUCKET
fi

duplicity remove-all-but-n-full 60 --force --no-encryption --s3-use-new-style s3+http://$BUCKET

export AWS_ACCESS_KEY_ID=
export AWS_SECRET_ACCESS_KEY=