class scoold::elasticsearch {
	
	# ------------ EDIT HERE ---------------------#
	$HEAP_SIZE = "1200M"
	$HEAP_DEV = "200M"
	$eslink = "https://github.com/downloads/elasticsearch/elasticsearch/elasticsearch-0.16.3.zip"
	# --------------------------------------------#
	
	$elasticsearchusr = "elasticsearch"
	$elasticsearchhome = "/home/elasticsearch"	
	$espath = "${elasticsearchhome}/elasticsearch.zip"
	$esdir = "${elasticsearchhome}/elasticsearch"
	$esconf = "${esdir}/config/elasticsearch.yml"	
	$dontstart = "#do-not-start"	 	
	$minmem = "ES_MIN_MEM="
	$maxmem = "ES_MAX_MEM="
		 	
	package { "unzip": }
	
	user { $elasticsearchusr:
		home => $elasticsearchhome,
		managehome => true,
		shell => "/bin/bash"
	}
		
	notify{ "installing elasticsearch...": before => Exec["download-elasticsearch"] }
	 
	exec { 
		"download-elasticsearch":
			command => "sudo -u ${elasticsearchusr} wget -O ${espath} ${eslink}",
			unless => "test -e ${espath}",
			require => User[$elasticsearchusr],
			before => Exec["unzip-elasticsearch"];
		"unzip-elasticsearch":			
			command => "sudo -u ${elasticsearchusr} unzip -qq -d ${elasticsearchhome} ${espath}",
			unless => "test -e ${esdir}",
			require => Package["unzip"],
			before => Exec["rename-elasticsearch"];	
		"rename-elasticsearch":
			command => "mv ${elasticsearchhome}/elasticsearch-* ${esdir}",
			unless => "test -e ${esdir}",
			require => Exec["download-elasticsearch"]					
	}
			
	file { $esconf:
		source => "puppet:///modules/scoold/elasticsearch.yml",
		mode => 700,
		owner => $elasticsearchusr,
		group => $elasticsearchusr,
		require => Exec["rename-elasticsearch"],
		before => Exec["start-elasticsearch"]
	}
	
	if $scoold::inproduction {		
		$cmd1 = "'1,/#${minmem}/ s/#${minmem}.*/${minmem}\"${HEAP_SIZE}\"/'"
		$cmd2 = "'1,/#${maxmem}/ s/#${maxmem}.*/${maxmem}\"${HEAP_SIZE}\"/'"
	}else{
		$cmd1 = "'1,/#${minmem}/ s/#${minmem}.*/${minmem}\"${HEAP_DEV}\"/'"
		$cmd2 = "'1,/#${maxmem}/ s/#${maxmem}.*/${maxmem}\"${HEAP_DEV}\"/'"
	}
	
	exec { "set-env": 
		command => "sed -e ${cmd1} -e ${cmd2} -i.bak ${esdir}/bin/elasticsearch.in.sh",
		require => Exec["rename-elasticsearch"] 
	}
				
	file { ["/var/lib/elasticsearch", "/var/lib/elasticsearch/data", "/var/lib/elasticsearch/work", 
			"/var/run/elasticsearch", "/var/log/elasticsearch", "${esdir}/plugins"]:
		ensure => directory,
	    owner => $elasticsearchusr,
	    group => $elasticsearchusr,
	    recurse => true,
	    mode => 755,
	    require => Exec["rename-elasticsearch"]
	}
	
	file { "/etc/init/${elasticsearchusr}.conf":
		ensure => file,
		source => "puppet:///modules/scoold/elasticsearch.conf",
		owner => root,
		mode => 644,
		before => Exec["start-elasticsearch"]
	}
	
	file { "${esdir}/config/default-mapping.json":
		ensure => file,
		source => "puppet:///modules/scoold/default-mapping.json",
		owner => $elasticsearchusr,
	    group => $elasticsearchusr,
		mode => 700,
		require => Exec["rename-elasticsearch"],
		before => Exec["start-elasticsearch"]
	}
	
	file { "${esdir}/plugins/river-amazonsqs.zip":
		ensure => file,
		source => "puppet:///modules/scoold/river-amazonsqs.zip",
		owner => $elasticsearchusr,
	    group => $elasticsearchusr,
		mode => 700,
		require => Exec["rename-elasticsearch"],
		before => Exec["start-elasticsearch"]
	}
	
	exec { "unzip-river":
		command => "sudo -u ${elasticsearchusr} unzip -qq -d ${esdir}/plugins/river-amazonsqs ${esdir}/plugins/river-amazonsqs.zip",
		unless => "test -e ${esdir}/plugins/river-amazonsqs",
		require => [Package["unzip"], File["${esdir}/plugins/river-amazonsqs.zip"]]
	}
			
	line { "limits.conf1":
		ensure => present,		
		file => "/etc/security/limits.conf",
		line => "${elasticsearchusr}    -       nofile          64000"
	}
	
	line { "limits.conf2":
		ensure => present,		
		file => "/etc/security/limits.conf",
		line => "${elasticsearchusr}    -       memlock         unlimited"
	}
	
	exec{ "stop-elasticsearch":
		command => "stop elasticsearch",
		onlyif => "test -e /var/run/elasticsearch/elasticsearch.pid",
		before => Exec["start-elasticsearch"]
	}
	
	exec{ "start-elasticsearch":
		command => "start elasticsearch",
		unless => "test -e /var/run/elasticsearch/elasticsearch.pid"
	}		
}