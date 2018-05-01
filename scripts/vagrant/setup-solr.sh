#!/bin/bash
echo "Setting up Solr"
SOLR_USER=solr
SOLR_HOME=/usr/local/solr
mkdir $SOLR_HOME
chown $SOLR_USER:$SOLR_USER $SOLR_HOME
su $SOLR_USER -s /bin/sh -c "cp /downloads/solr-7.3.0.tgz $SOLR_HOME"
su $SOLR_USER -s /bin/sh -c "cd $SOLR_HOME && tar xfz solr-7.3.0.tgz"
su $SOLR_USER -s /bin/sh -c "cd $SOLR_HOME/solr-7.3.0/server/solr && cp -r configsets/_default . && mv _default collection1"
su $SOLR_USER -s /bin/sh -c "cp /conf/solr/7.3.0/schema.xml $SOLR_HOME/solr-7.3.0/server/solr/collection1/conf/schema.xml"
su $SOLR_USER -s /bin/sh -c "cp /conf/solr/7.3.0/solrconfig.xml $SOLR_HOME/solr-7.3.0/server/solr/collection1/conf/solrconfig.xml"
su $SOLR_USER -s /bin/sh -c "cd $SOLR_HOME/solr-7.3.0 && bin/solr start && bin/solr create_core -c collection1 -d server/solr/collection1/conf/"
cp /dataverse/doc/sphinx-guides/source/_static/installation/files/etc/init.d/solr /etc/init.d/solr
chmod 755 /etc/init.d/solr
/etc/init.d/solr stop
/etc/init.d/solr start
chkconfig solr on
