#!/bin/sh
if [ ! -d dv/deps ]; then
	mkdir -p dv/deps
fi
wdir=`pwd`
if [ ! -e dv/deps/glassfish4dv.tgz ]; then
	echo "glassfish dependency prep"
	mkdir -p /tmp/dv-prep/gf
	cd /tmp/dv-prep/gf
	wget http://download.java.net/glassfish/4.1/release/glassfish-4.1.zip
	wget http://search.maven.org/remotecontent?filepath=org/jboss/weld/weld-osgi-bundle/2.2.10.Final/weld-osgi-bundle-2.2.10.Final-glassfish4.jar -O weld-osgi-bundle-2.2.10.Final-glassfish4.jar
	unzip glassfish-4.1.zip
	rm glassfish4/glassfish/modules/weld-osgi-bundle.jar
	mv weld-osgi-bundle-2.2.10.Final-glassfish4.jar glassfish4/glassfish/modules
	tar zcf $wdir/dv/deps/glassfish4dv.tgz glassfish4
	cd $wdir
	# assuming that folks usually have /tmp auto-clean as needed
fi

if [ ! -e dv/deps/solr-7.3.0dv.tgz ]; then
	echo "solr dependency prep"
	# schema changes *should* be the only ones...
	cd dv/deps/
	wget https://archive.apache.org/dist/lucene/solr/7.3.0/solr-7.3.0.tgz -O solr-7.3.0dv.tgz
	cd ../../
fi

