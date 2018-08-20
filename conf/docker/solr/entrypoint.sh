#!/bin/bash

if ! whoami &> /dev/null; then
  if [ -w /etc/passwd ]; then
    echo "${USER_NAME:-default}:x:$(id -u):0:${USER_NAME:-default} user:${HOME}:/sbin/nologin" >> /etc/passwd
  fi
fi

SOLR_DIR=/usr/local/solr/solr-7.3.0

if [ "$1" = 'solr' ]; then

    cp -r $SOLR_DIR/server/solr/configsets/_default $SOLR_DIR/server/solr/collection1
    cp /tmp/schema.xml $SOLR_DIR/server/solr/collection1/conf

    if [ $HOSTNAME = "dataverse-solr-0" ]; then
        echo "I am the master"
        mv /tmp/solrconfig_master.xml $SOLR_DIR/server/solr/collection1/conf/solrconfig.xml
        cp /tmp/solrconfig_slave.xml $SOLR_DIR/server/solr/collection1/conf

    else   
        echo "I am the slave"
        cp /tmp/solrconfig_slave.xml $SOLR_DIR/server/solr/collection1/conf 
        mv $SOLR_DIR/server/solr/collection1/conf/solrconfig_slave.xml $SOLR_DIR/server/solr/collection1/conf/solrconfig.xml
    fi
    cd $SOLR_DIR
    bin/solr start
    bin/solr create_core -c collection1 -d server/solr/collection1/conf
    if [ $HOSTNAME = "dataverse-solr-0" ]; then
        curl 'http://localhost:8983/solr/collection1/replication?command=restore&location=/home/share'
    fi

    sleep infinity
elif [ "$1" = 'usage' ]; then
    echo  'docker run -d iqss/dataverse-solr solr'
else
    exec "$@"
fi
