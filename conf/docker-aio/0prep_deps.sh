#!/bin/sh
if [ ! -d dv/deps ]; then
	mkdir -p dv/deps
fi
wdir=`pwd`

if [ ! -e dv/deps/payara-5.2020.6.zip ]; then
	echo "payara dependency prep"
	# no more fiddly patching :)
	wget https://s3-eu-west-1.amazonaws.com/payara.fish/Payara+Downloads/5.2020.6/payara-5.2020.6.zip  -O dv/deps/payara-5.2020.6.zip
fi

if [ ! -e dv/deps/solr-8.8.1dv.tgz ]; then
	echo "solr dependency prep"
	# schema changes *should* be the only ones...
	cd dv/deps/	
	wget https://archive.apache.org/dist/lucene/solr/8.8.1/solr-8.8.1.tgz -O solr-8.8.1dv.tgz
	cd ../../
fi

