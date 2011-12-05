#!/bin/bash

DIR1="/home/elasticsearch"
DIR2="$DIR1/elasticsearch/plugins"
RIVERFILE="river-amazonsqs"
RIVERLINK="https://s3-eu-west-1.amazonaws.com/com.scoold.elasticsearch/river-amazonsqs.zip"

rm -rf "$DIR2/$RIVERFILE"
curl -s -o "$DIR1/$RIVERFILE.zip" $RIVERLINK
unzip -o -d "$DIR2/$RIVERFILE" "$DIR1/$RIVERFILE.zip"
chmod -R 755 "$DIR2/$RIVERFILE/*"
rm "$DIR1/$RIVERFILE.zip"