class scoold::elasticsearch {
	
	# ------------ EDIT HERE ---------------------#
	$HEAP_SIZE = "1200M"
	$HEAP_DEV = "200M"
	$PORT = 9200
	$INDEX_NAME = "scoold"
	$eslink = "https://github.com/downloads/elasticsearch/elasticsearch/elasticsearch-0.16.4.zip"
	$MASTER = true
	# --------------------------------------------#
	
	$elasticsearchusr = "elasticsearch"
	$elasticsearchhome = "/home/elasticsearch"	
	$espath = "${elasticsearchhome}/elasticsearch.zip"
	$esdir = "${elasticsearchhome}/elasticsearch"
	$esconf = "${esdir}/config/elasticsearch.yml"	
	$dontstart = "#do-not-start"	 	
	$minmem = "ES_MIN_MEM="
	$maxmem = "ES_MAX_MEM="
		 	
	package { ["unzip", "curl"]: }
	
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
			"/var/log/elasticsearch", "${esdir}/plugins"]:
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
	
	file { "${esdir}/config/index.json":
		ensure => file,
		source => "puppet:///modules/scoold/index.json",
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
		before => [Exec["start-elasticsearch"], Exec["install-river"]]
	}
	
	exec { "install-river":
		command => "sudo -u ${elasticsearchusr} unzip -qq -d ${esdir}/plugins/river-amazonsqs ${esdir}/plugins/river-amazonsqs.zip",
		unless => "test -e ${esdir}/plugins/river-amazonsqs",
		require => [Package["unzip"], Exec["rename-elasticsearch"]],
		before => Exec["start-elasticsearch"]
	}
	
	exec { "install-cloud-plugin":
		command => "sudo -u ${elasticsearchusr} ${esdir}/bin/plugin -install cloud-aws",
		unless => "test -e ${esdir}/plugins/cloud-aws",
		require => [Exec["rename-elasticsearch"], Exec["install-river"]],
		before => Exec["start-elasticsearch"]
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
	
	exec { "stop-elasticsearch":
		command => "stop elasticsearch",
		onlyif => "test -e ${elasticsearchhome}/elasticsearch.pid",
		before => Exec["start-elasticsearch"]
	}
	
	exec { "start-elasticsearch":
		command => "start elasticsearch",
		unless => "test -e ${elasticsearchhome}/elasticsearch.pid"
	}		
	
	exec { "sleep":
		command => "sleep 15",
		require => [Exec["start-elasticsearch"], Package["curl"]],
		before => Exec["create-river"]			
	}
		
	exec { "create-river":
		command => "curl -XPUT '${ipaddress}:${PORT}/_river/${INDEX_NAME}/_meta' -d '{ \"type\" : \"amazonsqs\" }' &> /dev/null",
		onlyif => "test -e ${elasticsearchhome}/elasticsearch.pid",
		require => [Exec["start-elasticsearch"], Package["curl"]],
		before => Exec["create-index"]
	}
	
	exec { "sleep2":
		command => "sleep 5",
		require => [Exec["create-river"], Package["curl"]],
		before => Exec["create-index"]			
	}
	
	exec { "create-index":
		command => "curl -XPUT '${ipaddress}:${PORT}/${INDEX_NAME}' -d @${esdir}/config/index.json &> /dev/null",
		onlyif => "test -e ${elasticsearchhome}/elasticsearch.pid",
		require => [Exec["start-elasticsearch"], Package["curl"]]		
	}	
}