class scoold::sudo {
    file { "/etc/sudoers":
        owner => "root",
        group => "root",
        mode  => 440,
    }    
}