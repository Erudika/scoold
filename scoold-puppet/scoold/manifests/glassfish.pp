class scoold::glassfish {
		
	# ------------ EDIT HERE ---------------------#
		
	# --------------------------------------------#
	
	$glassfishusr = "glassfish"
	$glassfishhome = "/home/glassfish"	
	$gfpath = "${glassfishhome}/gf.zip"
	$gfdir = "${glassfishhome}/glassfish"	
	$gfdomain = "${gfdir}/glassfish/domains/domain1"
	$workerid = str2int(regsubst($scoold::nodename,'^(\w+)(\d+)$','\2'))
	
#	$ufwconf = "/etc/ufw/ufw.conf"
#	$ufwrules = "/lib/ufw/user.rules"
#	$ufwbeforerules = "/etc/ufw/before.rules"
#	
#	package { "ufw": 
#		before => File[$ufwconf]
#	}
#		
#	# setup firewall for webserver
#	file { 
#		$ufwconf:
#			mode => 644,
#			owner => root;
#		$ufwrules:
#			ensure => file,
#			mode => 640,
#			owner => root,
#			source => "puppet:///modules/scoold/web.rules";
#		$ufwbeforerules:
#			ensure => file,
#			mode => 640,
#			owner => root,
#			source => "puppet:///modules/scoold/before.rules"				
#	}
#	
#	exec { "enable-ufw":
#		command => "yes | ufw enable",
#		require => [Package["ufw"], File[[$ufwconf, $ufwrules, $ufwbeforerules]]]
#	}

	package { "unzip": }
	 	
	user { $glassfishusr:
		home => $glassfishhome,
		managehome => true,
		shell => "/bin/bash"
	}
		 
	exec { 
		"download-glassfish":
			command => "sudo -u ${glassfishusr} wget --no-check-certificate -O ${gfpath} ${scoold::gflink}",
			unless => "test -e ${gfpath}",
			require => User[$glassfishusr],
			before => Exec["unzip-glassfish"];
		"unzip-glassfish":
			command => "sudo -u ${glassfishusr} unzip -qq -d ${glassfishhome} ${gfpath}",
			unless => "test -e ${gfdir}",
			require => Package["unzip"],
			before => Exec["rename-glassfish"];
		"rename-glassfish":
			command => "mv ${glassfishhome}/glassfish* ${gfdir}",
			unless => "test -e ${gfdir}",
			require => Exec["download-glassfish"]					
	}
	
	file { 
		"${gfdomain}/config/domain.xml":
			ensure => file,
			source => "puppet:///modules/scoold/domain.xml",
			mode => 644,
			owner => $glassfishusr,
			group => $glassfishusr, 
			require => Exec["rename-glassfish"],
			before => Exec["start-glassfish"];
		"${gfdomain}/config/admin-keyfile":
			ensure => file,
			source => "puppet:///modules/scoold/admin-keyfile",
			mode => 644,
			owner => $glassfishusr,
			group => $glassfishusr, 
			require => Exec["rename-glassfish"],
			before => Exec["start-glassfish"];		
		"/etc/init/${glassfishusr}.conf":
			ensure => file,
			source => "puppet:///modules/scoold/glassfish.conf",
			owner => root,
			mode => 644,
			before => Exec["start-glassfish"];
		"${glassfishhome}/gfsec.sh":
			ensure => file,
			source => "puppet:///modules/scoold/gfsec.sh",
			owner => $glassfishusr,
			group => $glassfishusr,
			mode => 744,
			require => Exec["rename-glassfish"]; 
		"${glassfishhome}/.asadminpass":
			ensure => file,
			source => "puppet:///modules/scoold/adminpass.txt",
			owner => $glassfishusr,
			group => $glassfishusr,
			mode => 644,
			require => Exec["rename-glassfish"];
		"${gfdomain}/docroot/index.html":
			ensure => file, 
			source => "puppet:///modules/scoold/index.html",
			owner => $glassfishusr,
			group => $glassfishusr,
			mode => 664,
			require => Exec["rename-glassfish"]
	}
		
	$prop = '\(com\.scoold\.workerid\" value=\)"[0-9]*"'
	$prop1 = '\(com\.scoold\.cassandra\.hosts\" value=\)".*"'
	exec { 
		"set-worker-id": 
			command => "sed -e '1,/${prop}/ s/${prop}/\\1\"${workerid}\"/' -i.bak ${gfdomain}/config/domain.xml",
			require => File["${gfdomain}/config/domain.xml"],
			before => Exec["start-glassfish"];
		"set-db-hosts": 
			command => "sed -e '1,/${prop1}/ s/${prop1}/\\1\"${scoold::dbhosts}\"/' -i.bak ${gfdomain}/config/domain.xml",
			require => File["${gfdomain}/config/domain.xml"],
			before => Exec["start-glassfish"]
	}
		
	exec { "stop-glassfish":
		command => "stop glassfish",		
		onlyif => "test -e ${glassfishhome}/glassfish.pid",
		before => Exec["start-glassfish"]
	}
	
	exec{ "start-glassfish":
		command => "start glassfish",
		unless => "test -e ${glassfishhome}/glassfish.pid",				
	}	
}