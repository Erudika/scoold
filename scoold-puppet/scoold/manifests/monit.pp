class scoold::monit {

#	check process cassandra with pidfile /var/run/cassandra/cassandra.pid
#  start program = "/sbin/start cassandra" with timeout 60 seconds
#  stop program  = "/sbin/stop cassandra"
#  if failed port 9160 type tcp
#     with timeout 15 seconds
#     then restart
#  if 3 restarts within 5 cycles then timeout
#  group server
	
}