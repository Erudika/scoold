# Class: scoold
#
# This module manages scoold.com servers
#
class scoold {
	# ----------------- EDIT HERE ---------------------#	
	$inproduction = false #controls heap size for m1.large and rpc server type
	$defuser = "ubuntu"
		
	#--- AUTO UPDATED - CHANGES WILL BE OVERWRITTEN ---#
	$nodename = ""
	$dbseeds = ""
	#--------------------------------------------------#	
	$group = regsubst($nodename,'^(\w+)(\d+)$','\1')
	
	#### Cassandra ####	
	$casver = "1.1.5"
	$caslink = "http://www.eu.apache.org/dist/cassandra/${casver}/apache-cassandra-${casver}-bin.tar.gz"
	$jnalink = "https://github.com/downloads/twall/jna/jna.jar"
	$dbheapsize = "6G" # memory of m1.large
	$dbheapnew = "200M"
	$dbcluster = "scoold"
	
	#### Glassfish ####
	$gfver = "3.1.2.2"
	$gflink = "http://dlc.sun.com.edgesuite.net/glassfish/${gfver}/release/glassfish-${gfver}.zip"	
	$gfcluster = "scoold" 
		 
	#### Elasticsearch ####
	$esver = "0.19.9"
	$esriverver = "1.2"
	$escloudawsver = "1.9.0"
	$eslink = "https://github.com/downloads/elasticsearch/elasticsearch/elasticsearch-${esver}.zip"
	$escloudawslink = "elasticsearch/elasticsearch-cloud-aws/${escloudawsver}"
	$esriverlink = "aleski/elasticsearch-river-amazonsqs/${esriverver}"
	$esheapsize = "1024M"
	$esheapdev = "256M"
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
    stage { "first": before => Stage["main"] }	
    stage { "last": require => Stage["main"] }	
	
	class { "scoold::${group}": stage => "last"; }

	file { "/etc/monit/monitrc":
		ensure => file,
		source => "puppet:///modules/scoold/monitrc-${group}.txt",
		owner => root,
		mode => 600;
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
