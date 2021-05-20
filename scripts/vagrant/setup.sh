#!/bin/bash
echo "Installing dependencies for Dataverse"

# wget seems to be missing in box 'bento/centos-8.2'
dnf install -qy wget

# python3 and psycopg2 for the Dataverse installer
dnf install -qy python3 python3-psycopg2

# JQ
echo "Installing jq for the setup scripts"
dnf install -qy epel-release
dnf install -qy jq

echo "Adding Shibboleth yum repo"
cp /dataverse/conf/vagrant/etc/yum.repos.d/shibboleth.repo /etc/yum.repos.d
# Uncomment this (and other shib stuff below) if you want
# to use Vagrant (and maybe PageKite) to test Shibboleth.
#yum install -y shibboleth shibboleth-embedded-ds

# java configuration et alia
dnf install -qy java-11-openjdk-devel httpd mod_ssl unzip
alternatives --set java /usr/lib/jvm/jre-11-openjdk/bin/java
java -version

# maven included in centos8 requires 1.8.0 - download binary instead
# current version is 3.6.3 - requires newer jacoco.pom.xml
wget -q https://www-us.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
tar xfz apache-maven-3.6.3-bin.tar.gz
mkdir /opt/maven
mv apache-maven-3.6.3/* /opt/maven/
echo "export JAVA_HOME=/usr/lib/jvm/jre-openjdk" > /etc/profile.d/maven.sh
echo "export M2_HOME=/opt/maven" >> /etc/profile.d/maven.sh
echo "export MAVEN_HOME=/opt/maven" >> /etc/profile.d/maven.sh
echo "export PATH=/opt/maven/bin:${PATH}" >> /etc/profile.d/maven.sh
chmod 0755 /etc/profile.d/maven.sh

# disable centos8 postgresql module and install postgresql10-server
# note: postgresql10 because 9.6 isn't backwards compatible with python3-psycopg2
dnf -qy module disable postgresql
dnf install -qy https://download.postgresql.org/pub/repos/yum/reporpms/EL-8-x86_64/pgdg-redhat-repo-latest.noarch.rpm
dnf install -qy postgresql10-server
/usr/pgsql-10/bin/postgresql-10-setup initdb
/usr/bin/systemctl stop postgresql-10
cp /dataverse/conf/vagrant/var/lib/pgsql/data/pg_hba.conf /var/lib/pgsql/10/data/pg_hba.conf
/usr/bin/systemctl start postgresql-10
/usr/bin/systemctl enable postgresql-10

PAYARA_USER=dataverse
echo "Ensuring Unix user '$PAYARA_USER' exists"
useradd $PAYARA_USER || :
SOLR_USER=solr
echo "Ensuring Unix user '$SOLR_USER' exists"
useradd $SOLR_USER || :
DOWNLOAD_DIR='/dataverse/downloads'
PAYARA_ZIP="$DOWNLOAD_DIR/payara-5.2020.6.zip"
SOLR_TGZ="$DOWNLOAD_DIR/solr-8.8.1.tgz"
if [ ! -f $PAYARA_ZIP ] || [ ! -f $SOLR_TGZ ]; then
    echo "Couldn't find $PAYARA_ZIP or $SOLR_TGZ! Running download script...."
    cd $DOWNLOAD_DIR && ./download.sh && cd
    echo "Done running download script."
fi
PAYARA_USER_HOME=~dataverse
PAYARA_ROOT=/usr/local/payara5
if [ ! -d $PAYARA_ROOT ]; then
  echo "Copying $PAYARA_ZIP to $PAYARA_USER_HOME and unzipping"
  su $PAYARA_USER -s /bin/sh -c "cp $PAYARA_ZIP $PAYARA_USER_HOME"
  su $PAYARA_USER -s /bin/sh -c "cd $PAYARA_USER_HOME && unzip -q $PAYARA_ZIP"
  # default.config defaults to /usr/local/payara5 so let's go with that
  rsync -a $PAYARA_USER_HOME/payara5/ $PAYARA_ROOT/
else
  echo "$PAYARA_ROOT already exists"
fi

#service shibd start
/usr/bin/systemctl stop httpd
cp /dataverse/conf/httpd/conf.d/dataverse.conf /etc/httpd/conf.d/dataverse.conf
mkdir -p /var/www/dataverse/error-documents
cp /dataverse/conf/vagrant/var/www/dataverse/error-documents/503.html /var/www/dataverse/error-documents
/usr/bin/systemctl start httpd
#curl -k --sslv3 https://pdurbin.pagekite.me/Shibboleth.sso/Metadata > /tmp/pdurbin.pagekite.me
#cp -a /etc/shibboleth/shibboleth2.xml /etc/shibboleth/shibboleth2.xml.orig
#cp -a /etc/shibboleth/attribute-map.xml /etc/shibboleth/attribute-map.xml.orig
# need more attributes, such as sn, givenName, mail
#cp /dataverse/conf/vagrant/etc/shibboleth/attribute-map.xml /etc/shibboleth/attribute-map.xml
# FIXME: automate this?
#curl 'https://www.testshib.org/cgi-bin/sp2config.cgi?dist=Others&hostname=pdurbin.pagekite.me' > /etc/shibboleth/shibboleth2.xml
#cp /dataverse/conf/vagrant/etc/shibboleth/shibboleth2.xml /etc/shibboleth/shibboleth2.xml
#service shibd restart
#curl -k --sslv3 https://pdurbin.pagekite.me/Shibboleth.sso/Metadata > /downloads/pdurbin.pagekite.me
#service httpd restart

echo "#########################################################################################"
echo "# This is a Vagrant test box, so we're disabling firewalld. 			      #
echo "# Re-enable it with $ sudo systemctl enable firewalld && sudo systemctl start firewalld #"
echo "#########################################################################################"
systemctl disable firewalld
systemctl stop firewalld
