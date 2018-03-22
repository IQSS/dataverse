#!/bin/bash

# FIXME: Don't run Solr out of /tmp!
# Solr is /tmp to avoid AccessDeniedException under Minishift/OpenShift.
SOLR_DIR=/tmp/solr-7.2.1

if [ "$1" = 'solr' ]; then
    cd /tmp
    tar xvfz solr-7.2.1.tgz
    cp -r $SOLR_DIR/server/solr/configsets/_default $SOLR_DIR/server/solr/collection1
    cp /tmp/schema.xml $SOLR_DIR/server/solr/collection1/conf
    cp /tmp/solrconfig.xml $SOLR_DIR/server/solr/collection1/conf
    cd $SOLR_DIR
    bin/solr start
    bin/solr create_core -c collection1 -d server/solr/collection1/conf
    sleep infinity
elif [ "$1" = 'usage' ]; then
    echo  'docker run -d iqss/dataverse-solr solr'
else
    exec "$@"
fi
