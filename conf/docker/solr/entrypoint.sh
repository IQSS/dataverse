#!/bin/bash

if [ "$1" = 'solr' ]; then
    cd /usr/local/solr-4.6.0/example/
    java -jar start.jar 
elif [ "$1" = 'usage' ]; then
    echo  'docker run -d iqss/dataverse-solr solr'
else
    exec "$@"
fi
