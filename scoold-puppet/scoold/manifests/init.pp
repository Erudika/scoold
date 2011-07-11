# Class: scoold
#
# This module manages scoold
#
# Parameters:
#
# Actions:
#
# Requires:
#
# Sample Usage:
#
# [Remember: No empty lines between comments and class definition]
class scoold {
	
	Package { ensure => latest}
	User { ensure => present }
	Group { ensure => present }
	File { ensure => present }
	Service { ensure => running }
	Exec { path => ["/bin", "/sbin", "/usr/bin", "/usr/sbin"] }
		
	$inproduction = false
	$defuser = "ubuntu"
	$release = "natty"
	
	include scoold::sudo
	#include scoold::glassfish
	#include scoold::cassandra
	include scoold::elasticsearch
	
	#case $hostname {
    #    jack,jill:      { include hill    } # apply the hill class
    #    humpty,dumpty:  { include wall    } # apply the wall class
    #    default:        { include generic } # apply the generic class
    #}
			
	
#	package { 'openssh-server':
#      ensure => present,
#      before => File['/etc/ssh/sshd_config'],
#    }
#
#    file { '/etc/ssh/sshd_config':
#      ensure => file,
#      mode   => 600,
#      source => '/root/learning-manifests/sshd_config',
#    }
#
#    service { 'sshd':
#      ensure     => running,
#      enable     => true,
#      hasrestart => true,
#      hasstatus  => true,
#      subscribe  => File['/etc/ssh/sshd_config'],
#    }

#	file { '/home/ubuntu/testis.txt':
#      ensure => file,
#      mode   => 600,      
#      source => "puppet:///modules/scoold/kure.txt"
#    }

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
