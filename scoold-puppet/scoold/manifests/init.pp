# Class: scoold
#
# This module manages scoold.com servers
#
class scoold {
	# ----------------- EDIT HERE ---------------------#	
	$inproduction = false #controls heap size for m2.large
	$defuser = "ubuntu"
	$upgrade = true
		
	#--- AUTO UPDATED - CHANGES WILL BE OVERWRITTEN ---#
	$nodename = "search1"
	$dbseeds = "10.235.90.251,10.224.103.69"
	#--------------------------------------------------#	
	
	#### Cassandra ####	
	$casver = "1.0.1"
	$caslink = "http://www.eu.apache.org/dist/cassandra/${casver}/apache-cassandra-${casver}-bin.tar.gz"
	$jnalink = "http://java.net/projects/jna/sources/svn/content/trunk/jnalib/dist/jna.jar"
	$dbheapsize = "7G" # memory of m1.large
	$dbheapnew = "200M"
	$dbcluster = "scoold"
	
	#### Glassfish ####
	$gflink = "http://dlc.sun.com.edgesuite.net/glassfish/3.1.1/release/glassfish-3.1.1.zip"	
	$gfcluster = "scoold" 
		 
	#### Elasticsearch ####
	$esmaster = true
	$esver = "0.18.2"
	$eslink = "https://github.com/downloads/elasticsearch/elasticsearch/elasticsearch-${esver}.zip"
	$esriverlink = "https://erudika.ci.cloudbees.com/job/scoold/ws/scoold-search/target/river-amazonsqs.zip"
	$esheapsize = "1200M"
	$esheapdev = "200M"
	if inproduction == false {		
		$esheapsize = $esheapdev	
	}
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
