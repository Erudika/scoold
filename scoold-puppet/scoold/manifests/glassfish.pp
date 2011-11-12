class scoold::glassfish {
		
	# ------------ EDIT HERE ---------------------#
		
	# --------------------------------------------#
	
	$glassfishusr = "glassfish"
	$glassfishhome = "/home/glassfish"	
	$gfpath = "${glassfishhome}/gf.zip"
	$gfdir = "${glassfishhome}/glassfish"	
	$gfdomain = "${gfdir}/glassfish/domains/domain1"
	$workerid = str2int(regsubst($scoold::nodename,'^(\w+)(\d+)$','\2'))
	
	package { "unzip": }
	 	
	user { $glassfishusr:
		home => $glassfishhome,
		managehome => true,
		shell => "/bin/bash"
	}
	
	exec { "stop-glassfish":
		command => "stop glassfish; rm ${glassfishhome}/glassfish.pid",		
		onlyif => "test -e ${glassfishhome}/glassfish.pid",
		before => User[$glassfishusr]
	}

	if $scoold::upgrade {	
		exec { 
			"download-glassfish":
				command => "sudo -u ${glassfishusr} wget --no-check-certificate -O ${gfpath} ${scoold::gflink}",
				unless => "test -e ${gfpath}",
				require => User[$glassfishusr],
				before => Exec["unzip-glassfish"];
			"remove-old-glassfish":
				command => "rm -rf ${gfdir}",
				onlyif => "test -e ${gfdir}",
				require => Exec["download-glassfish"],
				before => Exec["unzip-glassfish"];
			"unzip-glassfish":
				command => "sudo -u ${glassfishusr} unzip -qq -o -d ${glassfishhome} ${gfpath}",
				unless => "test -e ${gfdir}",
				require => Package["unzip"],
				before => Exec["rename-glassfish"];
			"rename-glassfish":
				command => "mv -f ${glassfishhome}/glassfish* ${gfdir}",
				unless => "test -e ${gfdir}",
				require => Exec["download-glassfish"]					
		}
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
	
	$logconf = file("/usr/share/puppet/modules/scoold/files/rsyslog-glassfish.txt")	

	exec{ 
		"start-glassfish":
			command => "start glassfish",
			unless => "test -e ${glassfishhome}/glassfish.pid";
		"configure-rsyslog":
			command => "echo '${logconf}' | tee -a /etc/rsyslog.conf && service rsyslog restart",
			require => Exec["start-glassfish"];
	}	
}