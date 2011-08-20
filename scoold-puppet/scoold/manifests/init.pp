# Class: scoold
#
# This module manages scoold.com servers
#
class scoold {
	# ------------ EDIT HERE ---------------------#	
	$inproduction = false
	$defuser = "ubuntu"
	$release = "natty"
	$nodename = "db3"
	# --------------------------------------------#
	
	Package { ensure => latest}
	User { ensure => present }
	Group { ensure => present }
	File { ensure => present }
	Service { ensure => running }
	Exec { path => ["/bin", "/sbin", "/usr/bin", "/usr/sbin"] }
    stage { "last": require => Stage["main"] }	
	
	case $nodename {
      /^web(\d+)$/: { 
      	class { "scoold::glassfish": stage => "main" }
    	class { "scoold::monit": stage => "last", type => "glassfish" }  	 
      } 
      /^db(\d+)$/: { 
      	class { "scoold::cassandra": stage => "main" }
    	class { "scoold::monit": stage => "last", type => "cassandra" } 
      } 
      /^search(\d+)$/: { 
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
