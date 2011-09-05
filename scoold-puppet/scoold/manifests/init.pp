# Class: scoold
#
# This module manages scoold.com servers
#
class scoold {
	# ------------ EDIT HERE ---------------------#	
	$inproduction = false
	$defuser = "ubuntu"
	$release = "natty"
	
	#-- UPDATED VIA SHELL SCRIPT --#
	$nodename = "web2"
	#------------------------------#	
	
	#### DB ####	
	$dbnodes = ["10.227.94.112", "10.226.226.8", "10.51.94.94"]
	$dbnodeids = { "${dbnodes[0]}" => 0, "${dbnodes[1]}" => 1, "${dbnodes[2]}" => 2 }
	$dbseeds = "\"${dbnodes[0]},${dbnodes[1]}\""
	$ver = "0.8.4"
	$caslink = "http://www.eu.apache.org/dist/cassandra/${ver}/apache-cassandra-${ver}-bin.tar.gz"
	$dbheapsize = "7G" # memory of m1.large
	$dbheapnew = "200M"
	$dbcluster = "scoold"
	
	#### WEB ####
	$gflink = "http://dlc.sun.com.edgesuite.net/glassfish/3.1.1/release/glassfish-3.1.1.zip"	
	$gfcluster = "scoold" 
		 
	#### SEARCH ####
	$esmaster = true
	$eslink = "https://github.com/downloads/elasticsearch/elasticsearch/elasticsearch-0.16.4.zip"
	$esriverlink = "https://s3-eu-west-1.amazonaws.com/com.scoold.files/river-amazonsqs.zip"
	$esport = 9200
	$esheapsize = "1200M"
	$esheapdev = "200M"
	$esindex = "scoold"
	# --------------------------------------------#
	
	Package { ensure => latest}
	User { ensure => present }
	Group { ensure => present }
	File { ensure => present }
	Service { ensure => running }
	Exec { path => ["/bin", "/sbin", "/usr/bin", "/usr/sbin"] }
    stage { "last": require => Stage["main"] }	
	
	case $nodename {
      /^web(\d*)$/: { 
      	class { "scoold::glassfish": stage => "main" }
    	class { "scoold::monit": stage => "last", type => "glassfish" }  	 
      } 
      /^db(\d*)$/: { 
      	class { "scoold::cassandra": stage => "main" }
    	class { "scoold::monit": stage => "last", type => "cassandra" } 
      } 
      /^search(\d*)$/: { 
      	class { "scoold::elasticsearch": stage => "main" }
    	class { "scoold::monit": stage => "last", type => "elasticsearch" } 
      }  
    }   
    	
	file { "/etc/sudoers":
        owner => root,
        group => root,
        mode  => 440,
    }
	
	define line ($file, $line, $ensure = 'present') {
		case $ensure {
			default: {
				err("unknown ensure value ${ensure}")
			}
			present: {
				exec{
					"/bin/echo '${line}' >> '${file}'":
						unless => "/bin/grep -qFx '${line}' '${file}'"
				}
			}
			absent: {
				exec{
					"/usr/bin/perl -ni -e 'print unless /^\\Q${line}\\E\$/' '${file}'":
						onlyif => "/bin/grep -qFx '${line}' '${file}'"
				}
			}
		}
	}		
}
