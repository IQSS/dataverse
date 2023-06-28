#!/bin/sh
if [ ! -d dv/deps ]; then
	mkdir -p dv/deps
fi
wdir=`pwd`

if [ ! -e dv/deps/payara-6.2023.6.zip ]; then
	echo "payara dependency prep"
	wget https://nexus.payara.fish/repository/payara-community/fish/payara/distributions/payara/6.2023.6/payara-6.2023.6.zip  -O dv/deps/payara-6.2023.6.zip
fi

if [ ! -e dv/deps/solr-8.11.1dv.tgz ]; then
	echo "solr dependency prep"
	# schema changes *should* be the only ones...
	cd dv/deps/	
	wget https://archive.apache.org/dist/lucene/solr/8.11.1/solr-8.11.1.tgz -O solr-8.11.1dv.tgz
	cd ../../
fi

