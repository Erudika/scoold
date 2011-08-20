class scoold::cassandra {
	
	# ------------ EDIT HERE ---------------------#
	$HEAP_SIZE = "7G"
	$HEAP_NEW = "200M"
	$ver = "0.8.4" 
	$nodeid = 0
	$seedlist = "$HOSTNAME_OF_NODE_0" #replace with hostnames of nodes 0 and 1
	$tokens = ["0", "56713727820156410577229101238628035242", "113427455640312821154458202477256070485"]
	$caslink = "http://www.eu.apache.org/dist/cassandra/${ver}/apache-cassandra-${ver}-bin.tar.gz"
	# --------------------------------------------#
	
	$cassandrausr = "cassandra"
	$cassandrahome = "/home/cassandra"	
	$caspath = "${cassandrahome}/cassandra.tar.gz"
	$casdir = "${cassandrahome}/cassandra"
	$casconf = "${casdir}/conf/cassandra.yaml"	
	$dontstart = "#do-not-start"	 	
	$mhs = "MAX_HEAP_SIZE="
	$hns = "HEAP_NEWSIZE="
	$cname = "cluster_name:"
	$itok = "initial_token:" 
	$abst = "auto_bootstrap:"
	$seeds = "seeds:"
			 	
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
			
#	file { $casconf:
#		source => "puppet:///modules/scoold/cassandra.yaml",		
#		mode => 700,
#		owner => $cassandrausr,
#		group => $cassandrausr,
#		require => Exec["rename-cassandra"],
#		before => Exec["start-cassandra"]
#	}	
	
	$cnamecmd = "'1,/${cname}/ s/${cname}.*/${cname} ${CLUSTER_NAME}/'"
	exec { "set-cluster-name": 
		command => "sed -e ${cnamecmd} -i.bak ${casconf}",
		require => Exec["rename-cassandra"]
	}
	
	if $scoold::inproduction {		
		$cmd1 = "'1,/#${mhs}/ s/#${mhs}.*/${mhs}\"${HEAP_SIZE}\"/'"
		$cmd2 = "'1,/#${hns}/ s/#${hns}.*/${hns}\"${HEAP_NEW}\"/'"
		
		exec { "set-env": 
			command => "sed -e ${cmd1} -e ${cmd2} -i.bak ${casdir}/conf/cassandra-env.sh",
			require => Exec["rename-cassandra"]
		}
	}else{
		#auto calculated and set by cassandra env script
	}
	
	if $nodeid == 0 {
		# first node is the seed node
		exec { "enable-start": 
			command => "sed -e 's/${dontstart}//' ${casconf}",
			require => Exec["set-cluster-name"];
		}
		
		# first node has token 0
		exec { "set-token":
			command => "sed -e '1,/${itok}/ s/${itok}.*/${itok} 0/' -i.bak1 ${casconf}",
			require => Exec["set-cluster-name"];		
		}
		
		# first node is also the munin server
		package { ["munin", "nginx"]: }
		
		file { "/etc/nginx/sites-enabled/default": 
			ensure => absent,
			before => Exec["restart-nginx"]
		}
		
		file { "/etc/nginx/sites-enabled/munin":
			ensure => file,
			source => "puppet:///modules/scoold/munin.nginx.txt",
			owner => root,
			mode => 777,
			require => [Package["munin"], Package["nginx"]],
			before => Exec["restart-nginx"]
		}
		
		exec { "restart-nginx":
			command => "service nginx restart"
		}		
		
		$cmpdir = "/home/${scoold::defuser}/cassandra-munin-plugins"
		$cmd1 = "chmod -R 777 ${cmpdir}"
		$cmd2 = "ln -sf ${cmpdir}/jmx_ /etc/munin/plugins/jvm_memory"
		$cmd3 = "ln -sf ${cmpdir}/jmx_ /etc/munin/plugins/ops_pending"
		 
#		$cmd4 = "ln -sf ${cmpdir}/jmx_ /etc/munin/plugins/ops_pending"
#		$cmd5 = "ln -sf ${cmpdir}/jmx_ /etc/munin/plugins/ops_pending"
#		$cmd6 = "ln -sf ${cmpdir}/jmx_ /etc/munin/plugins/ops_pending"
#		$cmd7 = "ln -sf ${cmpdir}/jmx_ /etc/munin/plugins/ops_pending"
		
		$cmdr = "service munin-node restart"
		
		file { $cmpdir:
			source => "puppet:///modules/scoold/cassandra-munin-plugins",
			recurse => true,
			force => true,
			before => Exec["install-cassandra-munin-plugin"]			
		}
		
		exec { "install-cassandra-munin-plugin":
			command => "$cmd1 && $cmd2 && $cmd3 && $cmdr"			
		}
	}else{		
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
			
	file { ["/var/lib/cassandra", "/var/lib/cassandra/saved_caches", "/var/lib/cassandra/commitlog", 
			"/var/lib/cassandra/data", "/var/log/cassandra"]:
		ensure => directory,
	    owner => $cassandrausr,
	    group => $cassandrausr,
	    recurse => true,
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
	
	exec { "stop-cassandra":
		command => "stop cassandra",
		onlyif => ["test -e ${cassandrahome}/cassandra.pid"],
		before => Exec["start-cassandra"]
	}
	
	exec { "start-cassandra":
		command => "start cassandra",
		unless => ["test -e ${cassandrahome}/cassandra.pid", "grep '${dontstart}' ${casconf} 2>/dev/null"],
		require => Exec["set-cluster-name"]
	}
			
}