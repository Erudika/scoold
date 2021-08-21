#!/bin/bash

######################
# Become a Certificate Authority
######################

# Generate root certificate
openssl req -x509 -new -nodes -sha256 -days 1024 -newkey rsa:2048 -keyout ScooldRootCA.key -out ScooldRootCA.pem -subj "/C=UK/CN=Scoold-Root-CA"
# Create a Windows-compatible crt file
openssl x509 -outform pem -in ScooldRootCA.pem -out ScooldRootCA.crt

######################
# Create CA-signed certs
######################

NAME=$1 # Use your own domain name
SECRET=$2
# Create a certificate-signing request
openssl req -new -nodes -newkey rsa:2048 -keyout $NAME.key -out $NAME.csr -subj "/C=BG/ST=EU/L=Sofia/O=Erudika/CN=$NAME"
# Create a config file for the extensions
>$NAME.ext cat <<-EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
subjectAltName = @alt_names
[alt_names]
DNS.1 = $NAME # Be sure to include the domain name here because Common Name is not so commonly honoured by itself
#IP.1 = 192.168.0.10 # Optionally, add an IP address (if the connection which you have planned requires it)
EOF
# Create the signed certificate
openssl x509 -req -sha256 -days 1024 -in $NAME.csr -CA ScooldRootCA.pem -CAkey ScooldRootCA.key -CAcreateserial -extfile $NAME.ext -out $NAME.pem
# Create a Windows-compatible crt file
openssl x509 -outform pem -in $NAME.pem -out $NAME.crt
# Clean up
rm $NAME.csr $NAME.ext

######################
# Create Java Keystore
######################
openssl pkcs12 -export -out scoold.p12 -in $NAME.pem -inkey $NAME.key -name scoold -passin pass:$SECRET -passout pass:$SECRET
