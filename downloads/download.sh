#!/bin/sh
if [ -z ${QUIETMODE+x} ] || [ $QUIETMODE -ne "" ]; then 
  CURL_CMD='curl -s'
else
  CURL_CMD='curl'
fi

$CURL_CMD -L -O http://download.java.net/glassfish/4.1/release/glassfish-4.1.zip
$CURL_CMD -L -O https://archive.apache.org/dist/lucene/solr/4.6.0/solr-4.6.0.tgz
$CURL_CMD -L -O http://search.maven.org/remotecontent?filepath=org/jboss/weld/weld-osgi-bundle/2.2.10.Final/weld-osgi-bundle-2.2.10.Final-glassfish4.jar
$CURL_CMD -L http://sourceforge.net/projects/schemaspy/files/schemaspy/SchemaSpy%205.0.0/schemaSpy_5.0.0.jar/download > schemaSpy_5.0.0.jar
