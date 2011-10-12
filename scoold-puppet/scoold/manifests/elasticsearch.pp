class scoold::elasticsearch {
	
	$elasticsearchusr = "elasticsearch"
	$elasticsearchhome = "/home/elasticsearch"	
	$espath = "${elasticsearchhome}/es.zip"
	$esdir = "${elasticsearchhome}/elasticsearch"
	$esconf = "${esdir}/config/elasticsearch.yml"	
	$dontstart = "#do-not-start"	 	
	$minmem = "ES_MIN_MEM="
	$maxmem = "ES_MAX_MEM="
	$nodeid = str2int(regsubst($scoold::nodename,'^(\w+)(\d+)$','\2'))
		 	
	package { ["unzip", "curl"]: }
	
	user { $elasticsearchusr:
		home => $elasticsearchhome,
		managehome => true,
		shell => "/bin/bash" 
	}
	
	exec { "stop-elasticsearch":
		command => "stop elasticsearch && rm ${elasticsearchhome}/elasticsearch.pid",
		onlyif => "test -e ${elasticsearchhome}/elasticsearch.pid",
		before => User[$elasticsearchusr];
	}	
	 
	if $scoold::upgrade {	
		exec { 
			"download-elasticsearch":
				command => "sudo -u ${elasticsearchusr} wget --no-check-certificate -O ${espath} ${scoold::eslink}",
				require => User[$elasticsearchusr],
				before => Exec["unzip-elasticsearch"];
			"remove-old-elasticsearch":
				command => "rm -rf ${esdir}",
				onlyif => "test -e ${esdir}",
				require => Exec["download-elasticsearch"],
				before => Exec["unzip-elasticsearch"];
			"unzip-elasticsearch":			
				command => "sudo -u ${elasticsearchusr} unzip -qq -o -d ${elasticsearchhome} ${espath}",
				require => Package["unzip"],
				before => Exec["rename-elasticsearch"];
			"rename-elasticsearch":
				command => "mv -f ${elasticsearchhome}/elasticsearch-* ${esdir}",
				require => Exec["download-elasticsearch"];
			"set-env": 
				command => "sed -e '1,/#${minmem}/ s/#${minmem}.*/${minmem}\"${scoold::esheapsize}\"/' -e '1,/#${maxmem}/ s/#${maxmem}.*/${maxmem}\"${scoold::esheapsize}\"/' -i.bak ${esdir}/bin/elasticsearch.in.sh",
				require => Exec["rename-elasticsearch"];
			"install-cloud-plugin":
				command => "sudo -u ${elasticsearchusr} ${esdir}/bin/plugin -install cloud-aws",
				require => Exec["rename-elasticsearch"],
				before => Exec["start-elasticsearch"]
		}
		
		line { 
			"limits.conf1":
				ensure => present,		
				file => "/etc/security/limits.conf",
				line => "${elasticsearchusr} - nofile 64000";
			"limits.conf2":
				ensure => present,		
				file => "/etc/security/limits.conf",
				line => "${elasticsearchusr} - memlock unlimited"
		}
	}
	
	if $nodeid == 1 {		
		package { "nginx": }
				
		file { 
			"/etc/nginx/sites-enabled/default": 
				ensure => absent,
				before => Exec["restart-nginx"];
			"/etc/nginx/sites-enabled/eshead":
				ensure => file,
				source => "puppet:///modules/scoold/eshead.nginx.txt",
				owner => root,
				mode => 755,
				require => [Package["nginx"], Exec["rename-gui"]],
				before => Exec["restart-nginx"];
			"${elasticsearchhome}/eshead":
				recurse => true,
				owner => $elasticsearchusr,
				mode => 755,
				require => Exec["rename-gui"];
		}
		
		exec { "restart-nginx":
			command => "service nginx restart"
		}
	
		
		$jauthpath = "/usr/share/puppet/modules/scoold/files/jenkins-auth.txt"
		$jauth = file("${jauthpath}")
		$riverfile = "river-amazonsqs.zip"
		
		exec {
			"download-river":				
				command => "sudo -u ${elasticsearchusr} curl -s -u ${jauth} -o ${elasticsearchhome}/${riverfile} ${scoold::esriverlink} && rm ${jauthpath}",
				before => Exec["install-river"],
				require => User[$elasticsearchusr];
			"install-river":
				command => "sudo -u ${elasticsearchusr} unzip -qq -o -f -d ${esdir}/plugins/ ${elasticsearchhome}/${riverfile} && rm ${elasticsearchhome}/${riverfile}",
				require => [Package["unzip"], Exec["rename-elasticsearch"]],
				before => Exec["start-elasticsearch"];
			"download-gui":
				command => "sudo -u ${elasticsearchusr} wget -q --no-check-certificate -O ${elasticsearchhome}/eshead.zip ${scoold::esguilink}",
				before => Exec["unzip-gui"];
			"unzip-gui":
				command => "sudo -u ${elasticsearchusr} unzip -qq -o -d ${elasticsearchhome}/ ${elasticsearchhome}/eshead.zip",
				require => Package["unzip"],
				before => Exec["rename-gui"];
			"rename-gui":
				command => "rm -rf ${elasticsearchhome}/eshead && sudo -u ${elasticsearchusr} mv -f ${elasticsearchhome}/mobz-* ${elasticsearchhome}/eshead",
				require => User[$elasticsearchusr];
		}		
	}	
			
	file { 
		["/var/lib/elasticsearch", "/var/lib/elasticsearch/data", "/var/lib/elasticsearch/work", 
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
		$esconf:
			source => "puppet:///modules/scoold/elasticsearch.yml",
			mode => 700,
			owner => $elasticsearchusr,
			group => $elasticsearchusr,
			require => Exec["rename-elasticsearch"],
			before => Exec["start-elasticsearch"];		
	}
	
	$logconf = file("/usr/share/puppet/modules/scoold/files/rsyslog-elasticsearch.txt")
	
	exec { 		
		"start-elasticsearch":
			command => "start elasticsearch";
		"sleep":
			command => "sleep 15",
			require => [Exec["start-elasticsearch"], Package["curl"]],
			before => Exec["create-river"];
		"create-river":
			command => "curl -XPUT '${ipaddress}:${scoold::esport}/_river/${scoold::esindex}/_meta' -d '{ \"type\" : \"amazonsqs\" }' &> /dev/null",
			onlyif => "test -e ${elasticsearchhome}/elasticsearch.pid",
			require => [Exec["start-elasticsearch"], Package["curl"]],
			before => Exec["create-index"];
		"sleep2":
			command => "sleep 5",
			require => [Exec["create-river"], Package["curl"]],
			before => Exec["create-index"];
		"create-index":
			command => "curl -XPUT '${ipaddress}:${scoold::esport}/${scoold::esindex}' -d @${esdir}/config/index.json &> /dev/null",
			onlyif => "test -e ${elasticsearchhome}/elasticsearch.pid",
			require => [Exec["start-elasticsearch"], Package["curl"]];
		"configure-rsyslog":
			command => "echo '${logconf}' | tee -a /etc/rsyslog.conf && service rsyslog restart",
			require => Exec["start-elasticsearch"];
	}	
}