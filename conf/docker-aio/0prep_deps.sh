#!/bin/sh
if [ ! -d dv/deps ]; then
	mkdir -p dv/deps
fi
wdir=`pwd`

if [ ! -e dv/deps/payara-5.2022.2.zip ]; then
	echo "payara dependency prep"
	# no more fiddly patching :)
	wget https://s3-eu-west-1.amazonaws.com/payara.fish/Payara+Downloads/5.2022.2/payara-5.2022.2.zip  -O dv/deps/payara-5.2022.2.zip
fi

if [ ! -e dv/deps/solr-8.11.1dv.tgz ]; then
	echo "solr dependency prep"
	# schema changes *should* be the only ones...
	cd dv/deps/	
	wget https://archive.apache.org/dist/lucene/solr/8.11.1/solr-8.11.1.tgz -O solr-8.11.1dv.tgz
	cd ../../
fi
cd ../../
if [ ! -e apache-maven-3.8.5-bin.tar.gz ]; then
	echo "maven dependency prep"
	# cd dv/deps/	
	wget -q https://downloads.apache.org/maven/maven-3/3.8.5/binaries/apache-maven-3.8.5-bin.tar.gz -O apache-maven-3.8.5-bin.tar.gz 
fi
cd conf/docker-aio/