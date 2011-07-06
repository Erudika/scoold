#!/bin/bash

ASADM=/home/glassfish/glassfish/bin/asadmin
GF_DIR=/home/glassfish/glassfish/glassfish/domains/domain1/config
DNAME="CN=Scoold,O=Erudika,L=Sofia,S=Sofia-Grad,C=Bulgaria"
CERT1=s1as
CERT2=glassfish-instance

if [ "1" == "$1" ]; then
	# STEP 1: change master password
	$ASADM stop-domain domain1
	$ASADM change-master-password --savemasterpassword=true
elif [ "2" == "$1" ]; then
	# STEP 2: change admin password
	$ASADM start-domain domain1
	$ASADM change-admin-password
elif [[ "3" == "$1" && -n "$2" ]]; then
	#cleanup default ssl certs
	keytool -delete -alias s1as -keystore $GF_DIR/keystore.jks -storepass $2
	keytool -delete -alias s1as -keystore $GF_DIR/cacerts.jks -storepass $2
	keytool -delete -alias glassfish-instance -keystore $GF_DIR/keystore.jks -storepass $2
	keytool -delete -alias glassfish-instance -keystore $GF_DIR/cacerts.jks -storepass $2
	
	# STEP 3: generate new SSL certs 
	$ASADM stop-domain domain1
	keytool -keysize 2048 -genkey -alias $CERT1 -keyalg RSA -dname $DNAME -validity 3650 -keypass $2 -storepass $2 -keystore $GF_DIR/keystore.jks
	keytool -keysize 2048 -genkey -alias $CERT2 -keyalg RSA -dname $DNAME -validity 3650 -keypass $2 -storepass $2 -keystore $GF_DIR/keystore.jks
	#keytool -list -keystore $GF_DIR/keystore.jks -storepass $2 
	#export certs
	keytool -export -alias $CERT1 -file $GF_DIR/$CERT1.cert -keystore $GF_DIR/keystore.jks -storepass $2
	keytool -export -alias $CERT2 -file $GF_DIR/$CERT2.cert -keystore $GF_DIR/keystore.jks -storepass $2
	#import certs to local keystore
	keytool -import -alias $CERT1 -file $GF_DIR/$CERT1.cert -keystore $GF_DIR/cacerts.jks -storepass $2
	keytool -import -alias $CERT2 -file $GF_DIR/$CERT2.cert -keystore $GF_DIR/cacerts.jks -storepass $2
	
elif [ "4" == "$1" ]; then
	# STEP 4: enable secure admin
	$ASADM start-domain domain1
	$ASADM set server-config.network-config.protocols.protocol.admin-listener.security-enabled=true
	$ASADM enable-secure-admin
	# --adminalias=scoold-admin --instancealias=scoold-nodes
elif [ "5" == "$1" ]; then
	echo "You must be root to execute this command!"
	#import certs to java keystore
	keytool -delete -alias $CERT1 -keystore /etc/java-6-sun/security/cacerts -storepass "changeit"
	keytool -delete -alias $CERT2 -keystore /etc/java-6-sun/security/cacerts -storepass "changeit"
	
	keytool -import -alias $CERT1 -file $GF_DIR/scoold-admin.cert -keystore /etc/java-6-sun/security/cacerts -storepass "changeit"
	keytool -import -alias $CERT2 -file $GF_DIR/scoold-nodes.cert -keystore /etc/java-6-sun/security/cacerts -storepass "changeit"
fi
