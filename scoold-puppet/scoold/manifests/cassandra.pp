class scoold::cassandra {

	$cassandrausr = "cassandra"
	$cassandrahome = "/home/cassandra"	
	$caspath = "${cassandrahome}/cassandra.tar.gz"
	$casdir = "${cassandrahome}/cassandra"
	$casconf = "${casdir}/conf/cassandra.yaml"	
	$mhs = "MAX_HEAP_SIZE="
	$hns = "HEAP_NEWSIZE="
	$itok = "initial_token:" 
	$cname = "cluster_name:"
	$abst = "auto_bootstrap:"
	$seeds = "seeds:"
	$listenaddr = "listen_address:"
	$rpcaddr = "rpc_address:"
	$concreads = "concurrent_reads:"
	$concwrites = "concurrent_writes:"
	$comptmbps = "compaction_throughput_mb_per_sec:"
	$nodeid = str2int(regsubst($scoold::nodename,'^(\w+)(\d+)$','\2')) - 1
	$tokens = ["0", "56713727820156410577229101238628035242", "113427455640312821154458202477256070485"]
		
	user { $cassandrausr:
		home => $cassandrahome,
		managehome => true,
		shell => "/bin/bash"
	}
	
	exec { "nodetool-drain":
		# ${casdir}/bin/nodetool -h localhost disablegossip && sleep 10 && ${casdir}/bin/nodetool -h localhost disablethrift && 
		command => "${casdir}/bin/nodetool -h localhost drain",
		onlyif => "test -e ${cassandrahome}/cassandra.pid",
		before => Exec["stop-cassandra"]
	}
		
	exec { "stop-cassandra":
		command => "monit stop cassandra",
		onlyif => "test -e ${cassandrahome}/cassandra.pid",
		before => User[$cassandrausr]
	}
	
	exec{			
		"download-cassandra":
			command => "sudo -u ${cassandrausr} wget --no-check-certificate -O ${caspath} ${scoold::caslink}",
			require => User[$cassandrausr],
			before => Exec["unzip-cassandra"];
		"remove-old-cassandra":
			command => "rm -rf ${casdir}",
			onlyif => "test -e ${casdir}",
			require => Exec["download-cassandra"],
			before => Exec["unzip-cassandra"];
		"unzip-cassandra":
			command => "sudo -u ${cassandrausr} tar x -C ${cassandrahome} -f ${caspath}",
			before => Exec["rename-cassandra"];	
		"rename-cassandra":
			command => "mv -f ${cassandrahome}/apache-cassandra* ${casdir}",
			require => Exec["download-cassandra"];
		"set-cluster-name": 
			command => "sed -e '1,/${cname}/ s/${cname}.*/${cname} ${scoold::dbcluster}/' -i.bak ${casconf}",
			require => Exec["rename-cassandra"],
			before => Exec["start-cassandra"];
		"set-token":
			command => "sed -e '1,/${itok}/ s/${itok}.*/${itok} ${tokens[$nodeid]}/' -i.bak ${casconf}",
			require => Exec["rename-cassandra"],
			before => Exec["start-cassandra"];
		"set-seeds":
			command => "sed -e '1,/${seeds}/ s/${seeds}.*/${seeds} \"${scoold::dbseeds}\"/' -i.bak ${casconf}",
			require => Exec["rename-cassandra"],
			before => Exec["start-cassandra"];
		"set-listen-address": 
			command => "sed -e '1,/${listenaddr}/ s/${listenaddr}.*/${listenaddr} ${ipaddress}/' -i.bak ${casconf}",
			require => Exec["rename-cassandra"],
			before => Exec["start-cassandra"];
		"set-rpc-address": 
			command => "sed -e '1,/${rpcaddr}/ s/${rpcaddr}.*/${rpcaddr} 0\\.0\\.0\\.0/' -i.bak ${casconf}",
			require => Exec["rename-cassandra"],
			before => Exec["start-cassandra"];	
		"set-compation-throughput": 
			command => "sed -e '1,/${comptmbps}/ s/${comptmbps}.*/${comptmbps} 1/' -i.bak ${casconf}",
			require => Exec["rename-cassandra"],
			before => Exec["start-cassandra"],
			onlyif => "test '${scoold::inproduction}' = 'false'";
		"set-concurrent-reads": 
			command => "sed -e '1,/${concreads}/ s/${concreads}.*/${concreads} 16/' -i.bak ${casconf}",
			require => Exec["rename-cassandra"],
			before => Exec["start-cassandra"],
			onlyif => "test '${scoold::inproduction}' = 'false'";		
		"set-concurrent-writes": 
			command => "sed -e '1,/${concwrites}/ s/${concwrites}.*/${concwrites} 8/' -i.bak ${casconf}",
			require => Exec["rename-cassandra"],
			before => Exec["start-cassandra"],
			onlyif => "test '${scoold::inproduction}' = 'false'";
		"download-jna":
			command => "sudo -u ${cassandrausr} wget --no-check-certificate -O ${casdir}/lib/jna.jar ${scoold::jnalink}",
			require => Exec["rename-cassandra"],
			before => Exec["start-cassandra"];
	}



	# if not in production JVM MEM values are auto calculated and set by cassandra env script
	if $scoold::inproduction {		
		$cmd1 = "'1,/#${mhs}/ s/#${mhs}.*/${mhs}\"${scoold::dbheapsize}\"/'"
		$cmd2 = "'1,/#${hns}/ s/#${hns}.*/${hns}\"${scoold::dbheapnew}\"/'"

		exec { "set-env": 
			command => "sed -e ${cmd1} -e ${cmd2} -i.bak ${casdir}/conf/cassandra-env.sh",
			require => Exec["rename-cassandra"],
			before => Exec["start-cassandra"]
		}
	}

	line { 
		"limits.conf1":
			ensure => present,		
			file => "/etc/security/limits.conf",
			line => "${cassandrausr} soft memlock unlimited",
			require => User[$cassandrausr];
		"limits.conf2":
			ensure => present,		
			file => "/etc/security/limits.conf",
			line => "${cassandrausr} hard memlock unlimited",
			require => User[$cassandrausr];
	}
	
	file { 			
		"${cassandrahome}/backupdb.sh":
			ensure => file,
			source => "puppet:///modules/scoold/backupdb.sh",
			owner => $cassandrausr,
			mode => 700,
			require => User[$cassandrausr];
		"${cassandrahome}/restoredb.sh":
			ensure => file,
			source => "puppet:///modules/scoold/restoredb.sh",
			owner => $cassandrausr,
			mode => 700,
			require => User[$cassandrausr];
		"${cassandrahome}/.s3cfg":
			ensure => file,
			source => "puppet:///modules/scoold/s3cfg.txt",
			owner => $cassandrausr,
			mode => 600,
			require => User[$cassandrausr];
	}	
	
	if $nodeid == 0 {				
		cron { "snapshot":
			command => "${cassandrahome}/backupdb.sh",
			user => $cassandrausr,
			hour => [10, 22],
			minute => 1,
			require => Exec["start-cassandra"]
		}	
	} else {
		exec { "set-autobootstrap":
			command => "sed -e '1,/${abst}/ s/${abst}.*/${abst} true/' -i.bak ${casconf}",
			require => Exec["set-cluster-name"];	
		}
	}
		
	$repairhour = 6 + $nodeid
	
	cron { "nodetool-repair":
		command => "${casdir}/bin/nodetool -h localhost repair",
		user => $cassandrausr,
		monthday => [1,6,11,16,21,26],
		hour => $repairhour,
		minute => 1,
		require => Exec["start-cassandra"]
	}
	
	file { 
		["/var/lib/cassandra", "/var/lib/cassandra/saved_caches", "/var/lib/cassandra/commitlog", 
			"/var/lib/cassandra/data", "/var/log/cassandra"]:
			ensure => directory,
		    owner => $cassandrausr,
		    group => $cassandrausr,
		    recurse => true,
		    mode => 755,
		    require => Exec["rename-cassandra"];
		"/etc/init/cassandra.conf":
			ensure => file,
			source => "puppet:///modules/scoold/cassandra.conf",
			owner => root,
			mode => 644,
			before => Exec["start-cassandra"];		
	}
		
	$logconf = file("/usr/share/puppet/modules/scoold/files/rsyslog-cassandra.txt")	
		
	exec { 
		"start-cassandra":
			command => "monit start cassandra",
			unless => "test -e ${cassandrahome}/cassandra.pid", 
			require => Exec["set-cluster-name"];
		"configure-rsyslog":
			command => "echo '${logconf}' | tee -a /etc/rsyslog.conf && service rsyslog restart",
			require => Exec["start-cassandra"];
	}			
}