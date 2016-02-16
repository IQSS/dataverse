#!/bin/bash

if [[ -z ${OUTPUT_VERBOSITY} ]]; then OUTPUT_VERBOSITY='1'; fi

## OPT_* variables come from /dataverse/scripts/api/bin/dataverse-getopts.sh
. "/dataverse/scripts/api/bin/dataverse-getopts.sh"

#### Set output verbosity ####
## *_CMD and _IF_* command variables are set in /dataverse/scripts/api/bin/util-set-verbosity.sh
if [[ -e "/dataverse/scripts/api/bin/util-set-verbosity.sh" ]]; then
  . "/dataverse/scripts/api/bin/util-set-verbosity.sh"
elif [[ -e "../../api/bin/util-set-verbosity.sh" ]]; then
  . "../../api/bin/util-set-verbosity.sh"
elif [[ -e "./util-set-verbosity.sh" ]]; then
  . "./util-set-verbosity.sh"
else
  CURL_CMD='curl -s'
  WGET_CMD='wget -q'
  YUM_CMD='yum -q'
fi

$_IF_TERSE echo "Installing dependencies for Dataverse using verbosity level ${OUTPUT_VERBOSITY}"

#### Install jq to parse/read json
$_IF_VERBOSE 2>&1 type -p jq
if [[ $? ]]; then
  $_IF_TERSE echo "Installing dataverse dependency - jq"
  $_IF_VERBOSE $WGET_CMD http://stedolan.github.io/jq/download/linux64/jq
  $_IF_VERBOSE chmod +x jq
  # this is where EPEL puts it
  $_IF_VERBOSE sudo mv jq /usr/bin/jq
fi

#### Install dependencies using yum
$_IF_INFO echo "Adding Shibboleth yum repo"
$_IF_VERBOSE cp /dataverse/conf/vagrant/etc/yum.repos.d/shibboleth.repo /etc/yum.repos.d
$_IF_VERBOSE cp /dataverse/conf/vagrant/etc/yum.repos.d/epel-apache-maven.repo /etc/yum.repos.d

yum_packages=("java-1.8.0-openjdk-devel" "postgresql-server" "apache-maven" "httpd" "mod_ssl" "shibboleth" "shibboleth-embedded-ds")
for yummyPkg in "${yum_packages[@]}"; do
  $_IF_TERSE echo "Installing $yummyPkg"
  $_IF_VERBOSE $YUM_CMD install -y $yummyPkg
done

#### Configure default java/javac ####
$_IF_VERBOSE type -p alternatives
if [[ $? == 0 ]]; then
  $_IF_INFO echo "Setting alternatives:java and alternatives:javac"
  alternatives --set java /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/bin/java
  alternatives --set javac /usr/lib/jvm/java-1.8.0-openjdk.x86_64/bin/javac
fi
$_IF_VERBOSE echo "Checking java version"
$_IF_VERBOSE java -version
$_IF_VERBOSE echo "Checking javac version"
$_IF_VERBOSE javac -version

#### Configure PostreSQL ####
$_IF_INFO echo "Configuring PostgreSQL"
$_IF_VERBOSE service postgresql initdb
cp /dataverse/conf/vagrant/var/lib/pgsql/data/pg_hba.conf /var/lib/pgsql/data/pg_hba.conf
$_IF_VERBOSE service postgresql restart
$_IF_VERBOSE chkconfig postgresql on


#### Install glassfish ####
GLASSFISH_USER=glassfish
DOWNLOAD_DIR='/dataverse/downloads'
GLASSFISH_ZIP="$DOWNLOAD_DIR/glassfish-4.1.zip"
SOLR_TGZ="$DOWNLOAD_DIR/solr-4.6.0.tgz"
WELD_PATCH="$DOWNLOAD_DIR/weld-osgi-bundle-2.2.10.Final-glassfish4.jar"
GLASSFISH_USER_HOME=~glassfish
GLASSFISH_ROOT=$GLASSFISH_USER_HOME/glassfish4
$_IF_TERSE echo "Installing glassfish-4.1"
$_IF_VERBOSE echo "Ensuring Unix user '$GLASSFISH_USER' exists"
$_IF_VERBOSE useradd $GLASSFISH_USER || :
if [ ! -f $GLASSFISH_ZIP ] || [ ! -f $SOLR_TGZ ]; then
    $_IF_VERBOSE echo "Couldn't find $GLASSFISH_ZIP or $SOLR_TGZ! Running download script...."
    $_IF_VERBOSE pushd $DOWNLOAD_DIR
    $_IF_VERBOSE ./download.sh
    $_IF_VERBOSE popd
    $_IF_VERBOSE echo "Done running download script."
fi
if [ ! -d $GLASSFISH_ROOT ]; then
  $_IF_VERBOSE echo "Copying $GLASSFISH_ZIP to $GLASSFISH_USER_HOME and unzipping"
  $_IF_VERBOSE su $GLASSFISH_USER -s /bin/sh -c "cp $GLASSFISH_ZIP $GLASSFISH_USER_HOME"
  $_IF_VERBOSE su $GLASSFISH_USER -s /bin/sh -c "cd $GLASSFISH_USER_HOME && unzip -q $GLASSFISH_ZIP"
  $_IF_VERBOSE su $GLASSFISH_USER -s /bin/sh -c "mv $GLASSFISH_ROOT/glassfish/modules/weld-osgi-bundle.jar /tmp"
  $_IF_VERBOSE su $GLASSFISH_USER -s /bin/sh -c "cp $WELD_PATCH $GLASSFISH_ROOT/glassfish/modules"
else
  $_IF_VERBOSE echo "$GLASSFISH_ROOT already exists"
fi

#### Configure httpd service
$_IF_INFO echo "Configuring httpd ..."
$_IF_VERBOSE cp /dataverse/conf/httpd/conf.d/dataverse.conf /etc/httpd/conf.d/dataverse.conf
$_IF_VERBOSE mkdir -p /var/www/dataverse/error-documents
$_IF_VERBOSE cp /dataverse/conf/vagrant/var/www/dataverse/error-documents/503.html /var/www/dataverse/error-documents
$_IF_VERBOSE service httpd restart
$_IF_VERBOSE $CURL_CMD -k --sslv3 https://pdurbin.pagekite.me/Shibboleth.sso/Metadata > /tmp/pdurbin.pagekite.me

#### Configure shibboleth service
$_IF_INFO echo "Configuring Shibboleth ..."
$_IF_VERBOSE service shibd start
$_IF_VERBOSE cp -a /etc/shibboleth/shibboleth2.xml /etc/shibboleth/shibboleth2.xml.orig
$_IF_VERBOSE cp -a /etc/shibboleth/attribute-map.xml /etc/shibboleth/attribute-map.xml.orig
# need more attributes, such as sn, givenName, mail
$_IF_VERBOSE cp /dataverse/conf/vagrant/etc/shibboleth/attribute-map.xml /etc/shibboleth/attribute-map.xml



# FIXME: automate this?
#curl 'https://www.testshib.org/cgi-bin/sp2config.cgi?dist=Others&hostname=pdurbin.pagekite.me' > /etc/shibboleth/shibboleth2.xml
#cp /dataverse/conf/vagrant/etc/shibboleth/shibboleth2.xml /etc/shibboleth/shibboleth2.xml
#service shibd restart
#curl -k --sslv3 https://pdurbin.pagekite.me/Shibboleth.sso/Metadata > /downloads/pdurbin.pagekite.me
#service httpd restart
