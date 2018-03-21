#!/bin/bash

if [ "$1" = 'solr' ]; then
    cd /usr/local/solr-7.2.1/
    bin/solr start
    bin/solr create_core -c collection1 -d server/solr/collection1/conf
    sleep infinity
elif [ "$1" = 'usage' ]; then
    echo  'docker run -d iqss/dataverse-solr solr'
else
    exec "$@"
fi
