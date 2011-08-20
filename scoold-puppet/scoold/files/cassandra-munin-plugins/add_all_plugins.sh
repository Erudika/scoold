#!/bin/sh

MUNIN_PLUGIN_DIR=$1
PWD=`pwd`

if [ "$MUNIN_PLUGIN_DIR" = "" ]; then
 echo "Please specify the munin plugin dir" && exit 1
fi

if [ ! -d "$MUNIN_PLUGIN_DIR" ]; then
 echo "$MUNIN_PLUGIN_DIR is not a valid directory" && exit 1
fi

if [ ! -f "jmx_" ]; then
 echo "This script must be run in same dir as jmx_ script" && exit 1
fi

for filename in *.conf
do
  fn=`echo $filename | sed 's/.conf\$//g'`
  echo "ln -sf ${PWD}/jmx_ ${MUNIN_PLUGIN_DIR}/jmx_$fn"
  ln -sf ${PWD}/jmx_ ${MUNIN_PLUGIN_DIR}/jmx_$fn
done


echo "done."