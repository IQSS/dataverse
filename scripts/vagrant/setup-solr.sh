#!/bin/bash
echo "Setting up Solr"
SOLR_USER=solr
SOLR_HOME=/usr/local/solr
mkdir $SOLR_HOME
chown $SOLR_USER:$SOLR_USER $SOLR_HOME
su $SOLR_USER -s /bin/sh -c "cp /downloads/solr-7.2.1.tgz $SOLR_HOME"
su $SOLR_USER -s /bin/sh -c "cd $SOLR_HOME && tar xfz solr-7.2.1.tgz"
su $SOLR_USER -s /bin/sh -c "cd $SOLR_HOME/solr-7.2.1/server/solr && cp -r configsets/_default . && mv _default collection1"
su $SOLR_USER -s /bin/sh -c "cp /conf/solr/7.2.1/schema.xml $SOLR_HOME/solr-7.2.1/server/solr/collection1/conf/schema.xml"
su $SOLR_USER -s /bin/sh -c "cp /conf/solr/7.2.1/solrconfig.xml $SOLR_HOME/solr-7.2.1/server/solr/collection1/conf/solrconfig.xml"
su $SOLR_USER -s /bin/sh -c "cd $SOLR_HOME/solr-7.2.1 && bin/solr start && bin/solr create_core -c collection1 -d server/solr/collection1/conf/"
