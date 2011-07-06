class scoold::cassandra {
	
	#include scoold::java
	
	# ------------ EDIT HERE ---------------------#
	$HEAP_SIZE = "7G"
	$HEAP_NEW = "200M"
	$nodeid = 0
	$seedlist = "127.0.0.1,127.0.1.1"
	$tokens = ["0", "56713727820156410577229101238628035242", "113427455640312821154458202477256070485"]
	$caslink = "http://www.eu.apache.org/dist/cassandra/0.8.0/apache-cassandra-0.8.0-bin.tar.gz"
	# --------------------------------------------#
	
	$cassandrausr = "cassandra"
	$cassandrahome = "/home/cassandra"	
	$caspath = "${cassandrahome}/cassandra.tar.gz"
	$casdir = "${cassandrahome}/cassandra"
	$casconf = "${casdir}/conf/cassandra.yaml"	
	$dontstart = "#do-not-start"	 	
			 	
	user { $cassandrausr:
		home => $cassandrahome,
		managehome => true,
		shell => "/bin/bash"
	}
		
	notify{ "installing cassandra...": before => Exec["download-cassandra"] }
	 
	exec { 
		"download-cassandra":
			command => "sudo -u ${cassandrausr} wget -O ${caspath} ${caslink}",
			unless => "test -e ${caspath}",
			require => User[$cassandrausr],
			before => Exec["unzip-cassandra"];
		"unzip-cassandra":
			command => "sudo -u ${cassandrausr} tar x -C ${cassandrahome} -f ${caspath}",
			unless => "test -e ${casdir}",
			before => Exec["rename-cassandra"];	
		"rename-cassandra":
			command => "mv ${cassandrahome}/apache-cassandra* ${casdir}",
			unless => "test -e ${casdir}",
			require => Exec["download-cassandra"]					
	}
			
	file { $casconf:
		source => "puppet:///modules/scoold/cassandra.yaml",
		mode => 700,
		owner => $cassandrausr,
		group => $cassandrausr,
		require => Exec["rename-cassandra"],
		before => Exec["start-cassandra"]
	}
	
	if $scoold::inproduction {
		$mhs = "MAX_HEAP_SIZE="
		$hns = "HEAP_NEWSIZE="
		$cmd1 = "'1,/#${mhs}/ s/#${mhs}.*/${mhs}\"${HEAP_SIZE}\"/'"
		$cmd2 = "'1,/#${hns}/ s/#${hns}.*/${hns}\"${HEAP_NEW}\"/'"
		
		exec { "set-env": command => "sed -e ${cmd1} -e ${cmd2} -i.bak ${casdir}/conf/cassandra-env.sh" }
	}
	
	if $nodeid == 0 {
		exec { "enable-start": 
			command => "sed -e 's/${dontstart}//' ${casconf}",
			require => File[$casconf]
		}
	}else{
		$itok = "initial_token:" 
		$abst = "auto_bootstrap:"
		$seeds = "seeds:"
		exec { 
			"disable-start": 
				command => "echo '${dontstart}' >> ${casconf}",
				require => File[$casconf];
			"set-token":
				command => "sed -e '1,/${itok}/ s/${itok}.*/${itok} ${tokens[$nodeid]}/' -i.bak1 ${casconf}",
				require => Exec["disable-start"];
			"set-autobootstrap":
				command => "sed -e '1,/${abst}/ s/${abst}.*/${abst} true/' -i.bak2 ${casconf}",
				require => Exec["set-token"];
			"set-seeds":
				command => "sed -e '1,/${seeds}/ s/${seeds}.*/${seeds} ${seedlist}/' -i.bak3 ${casconf}",
				require => Exec["set-autobootstrap"];				
		}
	}
			
	file { ["/var/lib/cassandra", "/var/lib/cassandra/saved_caches", "/var/run/cassandra",
			"/var/lib/cassandra/commitlog", "/var/lib/cassandra/data", "/var/log/cassandra"]:
		ensure => directory,
	    owner => $cassandrausr,
	    group => $cassandrausr,
	    mode => 755,
	    require => Exec["rename-cassandra"]
	}
	
	file { "/etc/init/${cassandrausr}.conf":
		ensure => file,
		source => "puppet:///modules/scoold/cassandra.conf",
		owner => root,
		mode => 644,
		before => Exec["start-cassandra"]
	}
	
	exec{ "stop-cassandra":
		command => "stop cassandra",
		onlyif => ["test -e /var/run/cassandra/cassandra.pid"],
		before => Exec["start-cassandra"]
	}
	
	exec{ "start-cassandra":
		command => "start cassandra",
		unless => ["test -e /var/run/cassandra/cassandra.pid", "grep '${dontstart}' ${casconf} 2>/dev/null"]
	}
			
}