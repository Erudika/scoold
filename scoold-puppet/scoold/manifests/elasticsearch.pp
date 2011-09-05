class scoold::elasticsearch {
	
	# ------------ EDIT HERE ---------------------#
	
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
			command => "sudo -u ${elasticsearchusr} wget -O ${espath} ${scoold::eslink}",
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
			require => Exec["download-elasticsearch"];
		"set-env": 
			command => "sed -e ${cmd1} -e ${cmd2} -i.bak ${esdir}/bin/elasticsearch.in.sh",
			require => Exec["rename-elasticsearch"]
		"download-river":
			command => "sudo -u ${elasticsearchusr} wget -O ${esdir}/plugins/river-amazonsqs.zip ${scoold::esriverlink}",
			before => Exec["install-river"];
		"install-river":
			command => "sudo -u ${elasticsearchusr} unzip -qq -o -d ${esdir}/plugins/river-amazonsqs ${esdir}/plugins/river-amazonsqs.zip",
			unless => "test -e ${esdir}/plugins/river-amazonsqs",
			require => [Package["unzip"], Exec["rename-elasticsearch"]],
			before => Exec["start-elasticsearch"];
		"install-cloud-plugin":
			command => "sudo -u ${elasticsearchusr} ${esdir}/bin/plugin -install cloud-aws",
			unless => "test -e ${esdir}/plugins/cloud-aws",
			require => [Exec["rename-elasticsearch"], Exec["install-river"]],
			before => Exec["start-elasticsearch"]
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
		$cmd1 = "'1,/#${minmem}/ s/#${minmem}.*/${minmem}\"${scoold::esheapsize}\"/'"
		$cmd2 = "'1,/#${maxmem}/ s/#${maxmem}.*/${maxmem}\"${scoold::esheapsize}\"/'"
	}else{
		$cmd1 = "'1,/#${minmem}/ s/#${minmem}.*/${minmem}\"${scoold::esheapdev}\"/'"
		$cmd2 = "'1,/#${maxmem}/ s/#${maxmem}.*/${maxmem}\"${scoold::esheapdev}\"/'"
	}
					
	file { ["/var/lib/elasticsearch", "/var/lib/elasticsearch/data", "/var/lib/elasticsearch/work", 
			"/var/log/elasticsearch", "${esdir}/plugins"]:
			ensure => directory,
		    owner => $elasticsearchusr,
		    group => $elasticsearchusr,
		    recurse => true,
		    mode => 755,
		    require => Exec["rename-elasticsearch"];
		"/etc/init/${elasticsearchusr}.conf":
			ensure => file,
			source => "puppet:///modules/scoold/elasticsearch.conf",
			owner => root,
			mode => 644,
			before => Exec["start-elasticsearch"];
		"${esdir}/config/index.json":
			ensure => file,
			source => "puppet:///modules/scoold/index.json",
			owner => $elasticsearchusr,
		    group => $elasticsearchusr,
			mode => 700,
			require => Exec["rename-elasticsearch"],
			before => Exec["start-elasticsearch"];
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
		command => "curl -XPUT '${ipaddress}:${scoold::esport}/_river/${scoold::esindex}/_meta' -d '{ \"type\" : \"amazonsqs\" }' &> /dev/null",
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
		command => "curl -XPUT '${ipaddress}:${scoold::esport}/${scoold::esindex}' -d @${esdir}/config/index.json &> /dev/null",
		onlyif => "test -e ${elasticsearchhome}/elasticsearch.pid",
		require => [Exec["start-elasticsearch"], Package["curl"]]		
	}	
}