#!/bin/sh
if [ ! -d dv/deps ]; then
	mkdir -p dv/deps
fi
wdir=`pwd`

if [ ! -e dv/deps/payara-5.2020.2.zip ]; then
	echo "payara dependency prep"
	# no more fiddly patching :)
	wget https://github.com/payara/Payara/releases/download/payara-server-5.2020.2/payara-5.2020.2.zip  -O dv/deps/payara-5.2020.2.zip
fi

if [ ! -e dv/deps/solr-7.7.2dv.tgz ]; then
	echo "solr dependency prep"
	# schema changes *should* be the only ones...
	cd dv/deps/
	#wget https://archive.apache.org/dist/lucene/solr/7.3.0/solr-7.3.0.tgz -O solr-7.3.0dv.tgz
	wget https://archive.apache.org/dist/lucene/solr/7.7.2/solr-7.7.2.tgz -O solr-7.7.2dv.tgz
	cd ../../
fi

