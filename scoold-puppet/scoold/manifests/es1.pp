class scoold::es1 {
	
	  $version = "0.15.2"
	  $xmx = "2048m"
	  $esBasename       = "elasticsearch"
	  $esName           = "${esBasename}-${version}"
	  $esFile           = "${esName}.tar.gz"
	  $esServiceName    = "${esBasename}-servicewrapper"
	  $esServiceFile    = "${esServiceName}.tar.gz"
	  $esPath           = "${ebs1}/usr/local/${esName}"
	  $esPathLink       = "/usr/local/${esBasename}"
	  $esDataPath       = "${ebs1}/var/lib/${esBasename}"
	  $esLibPath        = "${esDataPath}"
	  $esLogPath        = "${ebs1}/var/log/${esBasename}"
	  $esXms            = "256m"
	  $esXmx            = "${xmx}"
	  $cluster          = "${esBasename}"
	  $esTCPPortRange   = "9300-9399"
	  $esHTTPPortRange  = "9200-9299"
	  $esUlimitNofile   = "32000"
	  $esUlimitMemlock  = "unlimited"
	  $esPidpath        = "/var/run"
	  $esPidfile        = "${esPidpath}/${esBasename}.pid"
	  $esJarfile        = "${esName}.jar"
       
	  # Ensure the elasticsearch user is present
	  user { "$esBasename":
		  ensure => "present",
		  comment => "Elasticsearch user created by puppet",
		  managehome => true,
		  shell   => "/bin/false",          
		  require => [Package["sun-java6-jre"], Lvmconfig[$ebs1]],
		  uid => 901
	  }
     
     file { "/etc/security/limits.d/${esBasename}.conf":
		content => template("elasticsearch/elasticsearch.limits.conf.erb"),                                                                                                    
		ensure => present,
		owner => root,
		group => root,
     }
     
#     file { "/etc/init/${esBasename}.conf":
#          content => template("elasticsearch/upstart.elasticsearch.conf.erb"),
#          ensure => present,
#          owner => root,
#          group => root,
#          mode => 644
#     }

     exec { "mkdir-ebs-mongohome":
          path => "/bin:/usr/bin",
          command => "mkdir -p $ebs1/usr/local",
          before => File["$esPath"],
          require => User["$esBasename"]
     }    

     # Make sure we have the application path
     file { "$esPath":
             ensure     => directory,
             require    => User["$esBasename"],
             owner      => "$esBasename",
             group      => "$esBasename", 
             recurse    => true
      }
      
      # Temp location
      file { "/tmp/$esFile":
             source  => "puppet:///elasticsearch/$esFile",
             require => File["$esPath"],
             owner => "$esBasename"
      }
      
      # Remove old files and copy in latest
      exec { "elasticsearch-package":
             path => "/bin:/usr/bin",
             command => "mkdir -p $esPath && tar -xzf /tmp/$esFile -C /tmp && sudo -u$esBasename cp -rf /tmp/$esName/. $esPath/. && rm -rf /tmp/$esBasename*", 
             unless  => "test -f $esPath/bin/elasticsearch",
             require => File["/tmp/$esFile"],
             notify => Service["$esBasename"],
      }

      ## Note: this is a bit hackish, need to stop the old elasticsearch when upgrading
      exec { "stop-elasticsearch-version-change":
           command => "service elasticsearch stop",
           unless => "ps aux | grep ${esName} | grep -v grep",
           onlyif => "ps aux | grep ${esBasename} | grep -v grep",
           require => Exec["elasticsearch-package"],
           notify => Service["$esBasename"]
      }

      # Create link to /usr/local/<esBasename> which will be the current version
      file { "$esPathLink":
           ensure => link,
           target => "$esPath",
           require => Exec["stop-elasticsearch-version-change"] 
           
      }
  
      # Ensure the data path is created
      file { "$esDataPath":
           ensure => directory,
           owner  => "$esBasename",
           group  => "$esBasename",
           require => Exec["elasticsearch-package"],
           recurse => true           
      }

      # Ensure the data path is created
      file { "/var/lib/${esBasename}":
           ensure => link,
           target => "${esDataPath}",
           require => File["$esDataPath"],
      }

      # Ensure the link to the data path is set
      file { "$esPath/data":
           ensure => link,
           force => true,
           target => "$esDataPath",
           require => File["$esDataPath"]
      }
      
      # Symlink config to /etc
      file { "/etc/$esBasename":
             ensure => link,
             target => "$esPathLink/config",
             require => Exec["elasticsearch-package"],
      }

      # Apply config template for search
      file { "$esPath/config/elasticsearch.yml":
             content => template("elasticsearch/elasticsearch.yml.erb"),
             require => File["/etc/$esBasename"]      
      }
      
      # Stage the Service Package
      file { "/tmp/$esServiceFile":
           source => "puppet:///elasticsearch/$esServiceFile",
            require => Exec["elasticsearch-package"]
      }
      
      # Move the service wrapper into place
      exec { "elasticsearch-service":
             path => "/bin:/usr/bin",
             unless => "test -d $esPath/bin/service/lib",
             command => "tar -xzf /tmp/$esServiceFile -C /tmp && mv /tmp/$esServiceName/service $esPath/bin && rm /tmp/$esServiceFile",
             require => [File["/tmp/$esServiceFile"], User["$esBasename"]]
      }

      # Ensure the service is present
      file { "$esPath/bin/service":
           ensure => directory,
           owner  => elasticsearch,
           group  => elasticsearch,
           recurse => true,
           require => Exec["elasticsearch-service"]
      }

      # Set the service config settings
      file { "$esPath/bin/service/elasticsearch.conf":
             content => template("elasticsearch/elasticsearch.conf.erb"),
             require => File["$esPath/bin/service"]
      }
      
      # Add customized startup script (see: http://www.elasticsearch.org/tutorials/2011/02/22/running-elasticsearch-as-a-non-root-user.html)
      file { "$esPath/bin/service/elasticsearch":
             source => "puppet:///elasticsearch/elasticsearch",
             require => File["$esPath/bin/service"]
      }

      # Create startup script
      file { "/etc/init.d/elasticsearch":
             ensure => link,
             target => "$esPath/bin/service/./elasticsearch",
             require => [Exec["stop-elasticsearch-version-change"], File["$esPath/bin/service/elasticsearch"]]
      }
      
      # Ensure logging directory
      file { "$esLogPath":
           owner     => "$esBasename",
           group     => "$esBasename",
           ensure    => directory,
           recurse   => true,
           require   => Exec["elasticsearch-package"],
      }
      
      # Ensure logging link is in place
      file { "/var/log/$esBasename":
           ensure => link,
           target => "$esLogPath",
           require => [File["${esLogPath}"], File["/etc/init.d/$esBasename"]]
      }

      file { "$esPath/logs":
           ensure => link,
           target => "/var/log/$esBasename",
           force => true,
           require => File["/var/log/$esBasename"]
      }
            
      # Ensure the service is running
      service { "$esBasename":
            enable => true,
            ensure => running,
            hasrestart => true,
            require => File["$esPath/logs"]
      }	
}