#!/bin/sh
if [ ! -d dv/deps ]; then
	mkdir -p dv/deps
fi
wdir=`pwd`

if [ ! -e dv/deps/payara-6.2022.1.Alpha2.zip ]; then
	echo "payara dependency prep"
	wget https://s3.eu-west-1.amazonaws.com/payara.fish/Payara+Downloads/6.2022.1.Alpha2/payara-6.2022.1.Alpha2.zip -O dv/deps/payara-6.2022.1.Alpha2.zip
fi

if [ ! -e dv/deps/solr-8.11.1dv.tgz ]; then
	echo "solr dependency prep"
	# schema changes *should* be the only ones...
	cd dv/deps/	
	wget https://archive.apache.org/dist/lucene/solr/8.11.1/solr-8.11.1.tgz -O solr-8.11.1dv.tgz
	cd ../../
fi

