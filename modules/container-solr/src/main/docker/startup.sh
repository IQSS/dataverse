#!/bin/bash

mkdir -p /var/solr/data/collection1/
cp -R /opt/solr/server/solr/configsets/_default/conf /var/solr/data/collection1
cp /*.xml /var/solr/data/collection1/conf/

# create collection on startup
echo "name=collection1" > /var/solr/data/collection1/core.properties

solr-foreground
