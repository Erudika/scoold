class scoold::java {
	# install Sun JDK
	
	exec { "apt-update":
		command => "sed -ire '/${scoold::release} partner/ s/^#//' /etc/apt/sources.list; apt-get update; echo 'sun-java6-jdk shared/accepted-sun-dlj-v1-1 boolean true' | debconf-set-selections"			
	}
	
	package { 
		["openjdk-6-jdk", "openjdk-6-jre"]: ensure => absent;
		["sun-java6-jdk"]: ensure => present, 
			require => Exec["apt-update"];
	}

	line { "java-home":
		ensure => present,		
		file => "/etc/environment",
		line => "JAVA_HOME=/usr/lib/jvm/java-6-sun",
		require => Package["sun-java6-jdk"]
	}	
}
