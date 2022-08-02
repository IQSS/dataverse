#!/bin/sh
# download necessary dependencies

if [ ! -d dv/deps ]; then
	mkdir -p dv/deps
fi

if [ ! -e dv/deps/payara-5.2021.6.zip ]; then
	echo "payara dependency prep"
	wget https://s3-eu-west-1.amazonaws.com/payara.fish/Payara+Downloads/5.2021.6/payara-5.2021.6.zip  -O dv/deps/payara-5.2021.6.zip
fi

if [ ! -e dv/deps/solr-8.11.1dv.tgz ]; then
	echo "solr dependency prep"	
	wget https://archive.apache.org/dist/lucene/solr/8.11.1/solr-8.11.1.tgz -O dv/deps/solr-8.11.1dv.tgz
fi

# - installing maven using the next script
# if [ ! -e dv/deps/apache-maven-3.8.6-bin.tar.gz ]; then
# 	echo "maven dependency prep"
# 	wget -q https://downloads.apache.org/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz  -O dv/deps/apache-maven-3.8.6-bin.tar.gz
# fi
