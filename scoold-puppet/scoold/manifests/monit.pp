class scoold::monit ($type) {
	
	file { "/etc/monit/monitrc":
		ensure => file,
		source => "puppet:///modules/scoold/monitrc-${type}.txt",
		owner => root,
		mode => 600,
		before => Exec["start-monit"]
	}
	
	exec { "start-monit":
		command => "monit &> /dev/null; monit monitor all &> /dev/null; sed -e '1,/startup=0/ s/startup=0/startup=1/' -i.bak1 /etc/default/monit",
	}
}