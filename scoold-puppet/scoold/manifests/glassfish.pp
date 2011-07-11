class scoold::glassfish {
		
	# ------------ EDIT HERE ---------------------#
	$glassfishusr = "glassfish"
	$glassfishhome = "/home/glassfish"	
	$gfpath = "${glassfishhome}/gf.zip"
	$gfdir3 = "${glassfishhome}/glassfish3"
	$gfdir = "${glassfishhome}/glassfish"
	$gflink = "http://download.java.net/glassfish/3.1/release/glassfish-3.1.zip"
	# --------------------------------------------#
	
	$ufwconf = "/etc/ufw/ufw.conf"
	$ufwrules = "/lib/ufw/user.rules"
	$ufwbeforerules = "/etc/ufw/before.rules"
	
	package { "unzip": }
	package { "ufw": 
		before => File[$ufwconf]
	}
	
	# setup firewall for webserver
	file { 
		$ufwconf:
			mode => 644,
			owner => root;
		$ufwrules:
			ensure => file,
			mode => 640,
			owner => root,
			source => "puppet:///modules/scoold/web.rules";
		$ufwbeforerules:
			ensure => file,
			mode => 640,
			owner => root,
			source => "puppet:///modules/scoold/before.rules"				
	}
	
	exec { "enable-ufw":
		command => "yes | ufw enable",
		require => [Package["ufw"], File[[$ufwconf, $ufwrules, $ufwbeforerules]]]
	}
	 	
	user { $glassfishusr:
		home => $glassfishhome,
		managehome => true,
		shell => "/bin/bash"
	}
		
	notify{ "installing glassfish...": before => Exec["download-glassfish"] }
	 
	exec { 
		"download-glassfish":
			command => "sudo -u ${glassfishusr} wget -O ${gfpath} ${gflink}",
			unless => "test -e ${gfpath}",
			require => User[$glassfishusr],
			before => Exec["unzip-glassfish"];
		"unzip-glassfish":
			command => "sudo -u ${glassfishusr} unzip -qq -d ${glassfishhome} ${gfpath}",
			creates => "${gfdir3}",
			unless => "test -e ${gfdir}",
			require => Package["unzip"],
			before => Exec["rename-glassfish"];
		"rename-glassfish":
			command => "mv ${gfdir3} ${gfdir}",
			unless => "test -e ${gfdir}",
			require => Exec["download-glassfish"]					
	}
		
	file { "${gfdir}/glassfish/domains/domain1/config/domain.xml":
		ensure => file,
		source => "puppet:///modules/scoold/domain.xml",
		mode => 644,
		owner => $glassfishusr,
		group => $glassfishusr,
		require => [Exec["unzip-glassfish"], Exec["rename-glassfish"]],
		before => Exec["start-glassfish"]
	}
	
	file { "/etc/init/${glassfishusr}.conf":
		ensure => file,
		source => "puppet:///modules/scoold/glassfish.conf",
		owner => root,
		mode => 644,
		before => Exec["start-glassfish"]
	}
	
	file { "${glassfishhome}/gfsec.sh":
		ensure => file,
		source => "puppet:///modules/scoold/gfsec.sh",
		owner => $glassfishusr,
		group => $glassfishusr,
		mode => 744		
	}	
	
	exec{ "start-glassfish":
		command => "start glassfish"		
	}	
}