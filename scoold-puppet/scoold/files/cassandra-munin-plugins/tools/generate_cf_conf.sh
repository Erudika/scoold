#!/bin/bash
#
# Use the standard1_*.conf files as templates to build configurations
# for new keyspaces and column families we wish to monitor
#
# Author: Jim Ancona <jim@anconafamily.com>
#   Date: 17-Dec-2010
#
# Exit here if we don't have two parameters
: ${2?"Usage: $0 KEYSPACE COLUMN-FAMILY"}
#
KEYSPACE=${1}
COLUMNFAMILY=${2}
#
for filename in standard1_*.conf
do
  outfile=${filename/standard1/${COLUMNFAMILY}}
  echo $outfile
	sed -e "s/Keyspace1/$KEYSPACE/g" -e "s/Standard1/$COLUMNFAMILY/g" < $filename > $outfile
done
exit 0
