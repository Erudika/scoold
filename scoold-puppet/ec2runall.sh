#!/bin/bash -e

AMI="ami-1b9fa86f"
TYPE="m1.small"

# run 2 glassfish nodes 
./ec2run.sh glassfish /Users/alexb/Desktop/scoold/scoold-puppet/scoold/files/userdata.sh $TYPE $AMI web1
./ec2run.sh glassfish /Users/alexb/Desktop/scoold/scoold-puppet/scoold/files/userdata.sh $TYPE $AMI web2

# run 3 cassandra nodes
./ec2run.sh cassandra /Users/alexb/Desktop/scoold/scoold-puppet/scoold/files/userdata.sh $TYPE $AMI db1
./ec2run.sh cassandra /Users/alexb/Desktop/scoold/scoold-puppet/scoold/files/userdata.sh $TYPE $AMI db2
./ec2run.sh cassandra /Users/alexb/Desktop/scoold/scoold-puppet/scoold/files/userdata.sh $TYPE $AMI db3

# run 1 elasticsearch nodes
./ec2run.sh elasticsearch /Users/alexb/Desktop/scoold/scoold-puppet/scoold/files/userdata.sh $TYPE $AMI search1
