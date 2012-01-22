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
		command => "monit stop elasticsearch",
		onlyif => "test -e ${elasticsearchhome}/elasticsearch.pid",
		before => User[$elasticsearchusr];
	}	
	 
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
			command => "rm -rf ${esdir}/plugins/cloud-aws; sudo -u ${elasticsearchusr} ${esdir}/bin/plugin -install cloud-aws",
			require => Exec["rename-elasticsearch"],
			before => Exec["start-elasticsearch"];
		"download-river":
			command => "rm -rf ${esdir}/plugins/river-amazonsqs; sudo -u ${elasticsearchusr} ${esdir}/bin/plugin -install ${scoold::esriverlink}",
			require => Exec["rename-elasticsearch"],
			before => Exec["start-elasticsearch"];
	}

	line { 
		"limits.conf1":
			ensure => present,		
			file => "/etc/security/limits.conf",
			line => "${elasticsearchusr} - nofile 64000",
			require => User[$elasticsearchusr];
		"limits.conf2":
			ensure => present,		
			file => "/etc/security/limits.conf",
			line => "${elasticsearchusr} - memlock unlimited",
			require => User[$elasticsearchusr];
		"serverflag":
			ensure => present,
			file => "${esdir}/bin/elasticsearch.in.sh",
			line => "JAVA_OPTS=\"\$JAVA_OPTS -server\"",
			require => Exec["rename-elasticsearch"],
			before => Exec["start-elasticsearch"];
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
			command => "monit; monit start elasticsearch",
			unless => "test -e ${elasticsearchhome}/elasticsearch.pid";
		"configure-rsyslog":
			command => "echo '${logconf}' | tee -a /etc/rsyslog.conf && service rsyslog restart",
			require => Exec["start-elasticsearch"];
	}	
}