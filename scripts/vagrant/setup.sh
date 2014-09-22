#!/bin/bash
echo "Installing dependencies for Dataverse"

# Add JQ
echo "Installing jq for the setup scripts"
wget http://stedolan.github.io/jq/download/linux64/jq
chmod +x jq
# this is where EPEL puts it
sudo mv jq /usr/bin/jq

echo "Adding Shibboleth yum repo"
cp /dataverse/conf/vagrant/etc/yum.repos.d/shibboleth.repo /etc/yum.repos.d
cp /dataverse/conf/vagrant/etc/yum.repos.d/epel-apache-maven.repo /etc/yum.repos.d
yum install -y java-1.7.0-openjdk-devel postgresql-server apache-maven httpd mod_ssl shibboleth shibboleth-embedded-ds
service postgresql initdb
service postgresql stop
cp /dataverse/conf/vagrant/var/lib/pgsql/data/pg_hba.conf /var/lib/pgsql/data/pg_hba.conf
service postgresql start
chkconfig postgresql on
GLASSFISH_USER=glassfish
echo "Ensuring Unix user '$GLASSFISH_USER' exists"
useradd $GLASSFISH_USER || :
DOWNLOAD_DIR='/dataverse/downloads'
GLASSFISH_ZIP="$DOWNLOAD_DIR/glassfish-4.0.zip"
SOLR_TGZ="$DOWNLOAD_DIR/solr-4.6.0.tgz"
if [ ! -f $GLASSFISH_ZIP ] || [ ! -f $SOLR_TGZ ]; then
    echo "Couldn't find $GLASSFISH_ZIP or $SOLR_TGZ! Running download script...."
    cd $DOWNLOAD_DIR && ./download.sh && cd
    echo "Done running download script."
fi
GLASSFISH_USER_HOME=~glassfish
GLASSFISH_ROOT=$GLASSFISH_USER_HOME/glassfish4
if [ ! -d $GLASSFISH_ROOT ]; then
  echo "Copying $GLASSFISH_ZIP to $GLASSFISH_USER_HOME and unzipping"
  su $GLASSFISH_USER -s /bin/sh -c "cp $GLASSFISH_ZIP $GLASSFISH_USER_HOME"
  su $GLASSFISH_USER -s /bin/sh -c "cd $GLASSFISH_USER_HOME && unzip -q $GLASSFISH_ZIP"
else
  echo "$GLASSFISH_ROOT already exists"
fi
service shibd start
service httpd stop
cp /dataverse/conf/httpd/conf.d/dataverse.conf /etc/httpd/conf.d/dataverse.conf
service httpd start
curl -k --sslv3 https://pdurbin.pagekite.me/Shibboleth.sso/Metadata > /downloads/pdurbin.pagekite.me
cp -a /etc/shibboleth/shibboleth2.xml /etc/shibboleth/shibboleth2.xml.orig
cp -a /etc/shibboleth/attribute-map.xml /etc/shibboleth/attribute-map.xml.orig
# need more attributes, such as sn, givenName, mail
cp /dataverse/conf/vagrant/etc/shibboleth/attribute-map.xml /etc/shibboleth/attribute-map.xml
# FIXME: automate this?
#curl 'https://www.testshib.org/cgi-bin/sp2config.cgi?dist=Others&hostname=pdurbin.pagekite.me' > /etc/shibboleth/shibboleth2.xml
#cp /dataverse/conf/vagrant/etc/shibboleth/shibboleth2.xml /etc/shibboleth/shibboleth2.xml
#service shibd restart
#curl -k --sslv3 https://pdurbin.pagekite.me/Shibboleth.sso/Metadata > /downloads/pdurbin.pagekite.me
#service httpd restart
