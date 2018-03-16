#!/bin/bash
echo "Setting up Solr"
GLASSFISH_USER=glassfish
GLASSFISH_USER_HOME=~glassfish
SOLR_HOME=$GLASSFISH_USER_HOME/solr
su $GLASSFISH_USER -s /bin/sh -c "mkdir $SOLR_HOME"
su $GLASSFISH_USER -s /bin/sh -c "cp /downloads/solr-7.2.1.tgz $SOLR_HOME"
su $GLASSFISH_USER -s /bin/sh -c "cd $SOLR_HOME && tar xfz solr-7.2.1.tgz"
su $GLASSFISH_USER -s /bin/sh -c "cd solr-7.2.1/server/solr"
su $GLASSFISH_USER -s /bin/sh -c "cp -r configsets\_default ."
su $GLASSFISH_USER -s /bin/sh -c "mv _default collection1"
su $GLASSFISH_USER -s /bin/sh -c "cp /conf/solr/7.2.1/schema.xml $SOLR_HOME/solr-7.2.1/server/solr/collection1/conf/schema.xml"
su $GLASSFISH_USER -s /bin/sh -c "cp /conf/solr/7.2.1/solrconfig.xml $SOLR_HOME/solr-7.2.1/server/solr/collection1/conf/solrconfig.xml"
su $GLASSFISH_USER -s /bin/sh -c "cd $SOLR_HOME/solr-7.2.1 && bin/solr start"
su $GLASSFISH_USER -s /bin/sh -c "bin/solr create_core -c collection1 -d server/solr/collection1/conf/"
