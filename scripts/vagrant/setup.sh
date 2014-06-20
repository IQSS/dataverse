#!/bin/bash
echo "Installing dependencies for Dataverse"
echo "Adding Shibboleth yum repo"
cp /dataverse/conf/vagrant/etc/yum.repos.d/shibboleth.repo /etc/yum.repos.d/shibboleth.repo
#yum install -y java-1.7.0-openjdk-devel postgresql-server apache-maven httpd mod_ssl
yum install -y java-1.7.0-openjdk-devel postgresql-server httpd mod_ssl shibboleth
service postgresql initdb
service postgresql stop
cp /dataverse/conf/vagrant/var/lib/pgsql/data/pg_hba.conf /var/lib/pgsql/data/pg_hba.conf
service postgresql start
chkconfig postgresql on
GLASSFISH_USER=glassfish
echo "Ensuring Unix user '$GLASSFISH_USER' exists"
useradd $GLASSFISH_USER || :
GLASSFISH_ZIP=`ls /dataverse/downloads/glassfish*zip`
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
# FIXME: automate this?
#curl 'https://www.testshib.org/cgi-bin/sp2config.cgi?dist=Others&hostname=pdurbin.pagekite.me' > /etc/shibboleth/shibboleth2.xml
#cp /dataverse/conf/vagrant/etc/shibboleth/shibboleth2.xml /etc/shibboleth/shibboleth2.xml
#service shibd restart
#curl -k --sslv3 https://pdurbin.pagekite.me/Shibboleth.sso/Metadata > /downloads/pdurbin.pagekite.me
#service httpd restart
