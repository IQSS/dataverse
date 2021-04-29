#!/bin/bash
echo "Setting up Solr"
dnf install -qy lsof
SOLR_USER=solr
SOLR_HOME=/usr/local/solr
mkdir $SOLR_HOME
chown $SOLR_USER:$SOLR_USER $SOLR_HOME
su $SOLR_USER -s /bin/sh -c "cp /dataverse/downloads/solr-8.8.1.tgz $SOLR_HOME"
su $SOLR_USER -s /bin/sh -c "cd $SOLR_HOME && tar xfz solr-8.8.1.tgz"
su $SOLR_USER -s /bin/sh -c "cd $SOLR_HOME/solr-8.8.1/server/solr && cp -r configsets/_default . && mv _default collection1"
su $SOLR_USER -s /bin/sh -c "cp /dataverse/conf/solr/8.8.1/schema*.xml $SOLR_HOME/solr-8.8.1/server/solr/collection1/conf/"
su $SOLR_USER -s /bin/sh -c "cp /dataverse/conf/solr/8.8.1/solrconfig.xml $SOLR_HOME/solr-8.8.1/server/solr/collection1/conf/solrconfig.xml"
su $SOLR_USER -s /bin/sh -c "cd $SOLR_HOME/solr-8.8.1 && bin/solr start && bin/solr create_core -c collection1 -d server/solr/collection1/conf/"
cp /dataverse/doc/sphinx-guides/source/_static/installation/files/etc/init.d/solr /etc/init.d/solr
chmod 755 /etc/init.d/solr
/etc/init.d/solr stop
/etc/init.d/solr start
chkconfig solr on
