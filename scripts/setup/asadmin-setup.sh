#!/bin/bash
# This is a setup script for setting up Glassfish 4 to run Dataverse
# The script was tested on Mac OS X.9
# ASSUMPTIONS
# * Script has to run locally (i.e. on the machine that hosts the server)
# * Internet connectivity is assumed, in order to get the postgresql driver.

##
# Default values - Change to suit your machine, or override by providing 
# environment variables.
DEFAULT_GLASSFISH_ROOT=/Applications/NetBeans/glassfish4
DEFAULT_DOMAIN=domain1
DEFAULT_ASADMIN_OPTS=" "


###
# Database values. Update as needed.
# Note: DB_USER "dvnApp" is case-sensitive and later used in "scripts/database/reference_data.sql"
#
DB_PORT=5432
DB_HOST=localhost
DB_NAME=dvndb
DB_USER=dvnApp
DB_PASS=dvnAppPass

# "${VAR+xxx}" for unset vs. empty per http://stackoverflow.com/questions/228544/how-to-tell-if-a-string-is-not-defined-in-a-bash-shell-script/230593#230593

if [ "${DB_NAME_CUSTOM+xxx}" ]
 then
  echo "Default DB_NAME ($DB_NAME) overridden: $DB_NAME_CUSTOM"
  DB_NAME=$DB_NAME_CUSTOM
fi

if [ "${DB_USER_CUSTOM+xxx}" ]
 then
  echo "Default DB_USER ($DB_USER) overridden: $DB_USER_CUSTOM"
  DB_USER=$DB_USER_CUSTOM
fi

if [ "${DB_PASS_CUSTOM+xxx}" ]
 then
  echo "Default DB_PASS ($DB_PASS) overridden: $DB_PASS_CUSTOM"
  DB_PASS=$DB_PASS_CUSTOM
fi

#echo "end"
#exit

## 
# External dependencies
PGSQL_DRIVER_URL=http://jdbc.postgresql.org/download/postgresql-9.3-1100.jdbc41.jar

if [ $SUDO_USER == "vagrant" ]
  then
  echo "We are running in a Vagrant environment."
  cat /etc/redhat-release
  # Choosing all lower case indentifiers for DB_NAME and DB_USER for this reason:
  #
  # Quoting an identifier also makes it case-sensitive, whereas unquoted names
  # are always folded to lower case. For example, the identifiers FOO, foo, and
  # "foo" are considered the same by PostgreSQL, but "Foo" and "FOO" are
  # different from these three and each other. (The folding of unquoted names
  # to lower case in PostgreSQL is incompatible with the SQL standard, which
  # says that unquoted names should be folded to upper case. Thus, foo should
  # be equivalent to "FOO" not "foo" according to the standard. If you want to
  # write portable applications you are advised to always quote a particular
  # name or never quote it.) --
  # http://www.postgresql.org/docs/9.3/static/sql-syntax-lexical.html
  DB_NAME=dataverse_db
  DB_USER=dataverse_app
  DB_PASS=secret
  echo "Installing dependencies via yum"
  yum install -y -q java-1.7.0-openjdk postgresql-server
  rpm -q postgresql-server
  echo "Starting PostgreSQL"
  chkconfig postgresql on
  /sbin/service postgresql initdb
  cp -a /var/lib/pgsql/data/pg_hba.conf /var/lib/pgsql/data/pg_hba.conf.orig
  sed -i -e 's/ident$/trust/' /var/lib/pgsql/data/pg_hba.conf
  /sbin/service postgresql start
  POSTGRES_USER=postgres
  echo "Creating database user $DB_USER"
  su $POSTGRES_USER -s /bin/sh -c "psql -c \"CREATE ROLE \"$DB_USER\" UNENCRYPTED PASSWORD '$DB_PASS' NOSUPERUSER CREATEDB CREATEROLE NOINHERIT LOGIN\""
  #su $POSTGRES_USER -s /bin/sh -c "psql -c '\du'"
  echo "Creating database $DB_NAME"
  su $POSTGRES_USER -s /bin/sh -c "psql -c 'CREATE DATABASE \"$DB_NAME\" WITH OWNER = \"$DB_USER\"'"
  GLASSFISH_USER=glassfish
  echo "Ensuring Unix user '$GLASSFISH_USER' exists"
  useradd $GLASSFISH_USER || :
  GLASSFISH_ZIP=`ls /downloads/glassfish*zip`
  GLASSFISH_USER_HOME=~glassfish
  echo "Copying $GLASSFISH_ZIP to $GLASSFISH_USER_HOME and unzipping"
  su $GLASSFISH_USER -s /bin/sh -c "cp $GLASSFISH_ZIP $GLASSFISH_USER_HOME"
  su $GLASSFISH_USER -s /bin/sh -c "cd $GLASSFISH_USER_HOME && unzip -q $GLASSFISH_ZIP"
  DEFAULT_GLASSFISH_ROOT=$GLASSFISH_USER_HOME/glassfish4
fi


# Set the scripts parameters (if needed)
if [ -z "${GLASSFISH_ROOT+xxx}" ]
 then
  echo setting GLASSFISH_ROOT to $DEFAULT_GLASSFISH_ROOT
  GLASSFISH_ROOT=$DEFAULT_GLASSFISH_ROOT
fi
if [ ! -d "$GLASSFISH_ROOT" ]
  then
    echo Glassfish root '$GLASSFISH_ROOT' does not exist
    exit 1
fi
GLASSFISH_BIN_DIR=$GLASSFISH_ROOT/bin

if [ -z "${DOMAIN+xxx}" ]
  then
    echo setting DOMAIN to $DEFAULT_DOMAIN
    DOMAIN=$DEFAULT_DOMAIN
fi
DOMAIN_DIR=$GLASSFISH_ROOT/glassfish/domains/$DOMAIN
if [ ! -d "$DOMAIN_DIR" ]
  then
    echo Domain directory '$DOMAIN_DIR' does not exist
    exit 2
fi
if [ -z "$ASADMIN_OPTS" ]
 then
  ASADMIN_OPTS=$DEFAULT_ASADMIN_OPTS
fi

echo "Setting up your glassfish4 to support Dataverse"
echo "Glassfish directory: "$GLASSFISH_ROOT
echo "Domain directory:    "$DOMAIN_DIR

###
# getting the postgres driver
DOMAIN_LIB=$DOMAIN_DIR/lib
if ! grep -qs postgres $DOMAIN_LIB/*
  then
    DRIVER_NAME=$(echo $PGSQL_DRIVER_URL | tr / \\n | tail -n1)
    echo Downloading postgresql driver '$DRIVER_NAME'
    wget $PGSQL_DRIVER_URL -O $DOMAIN_LIB/$DRIVER_NAME
  else
    echo postgresql driver already installed.
fi

###
# Move to the glassfish dir
pushd $GLASSFISH_BIN_DIR

###
# take the domain up, if needed.
DOMAIN_DOWN=$(./asadmin list-domains | grep "$DOMAIN " | grep "not running")
if [  $(echo $DOMAIN_DOWN|wc -c) -ne 1  ];
  then
    echo Trying to start domain $DOMAIN up...
    ./asadmin $ASADMIN_OPTS start-domain $DOMAIN
  else
    echo domain running
fi

# avoid OutOfMemoryError: PermGen per http://eugenedvorkin.com/java-lang-outofmemoryerror-permgen-space-error-during-deployment-to-glassfish/
#./asadmin $ASADMIN_OPTS list-jvm-options
./asadmin $ASADMIN_OPTS delete-jvm-options "-XX\:MaxPermSize=192m"
./asadmin $ASADMIN_OPTS create-jvm-options "-XX\:MaxPermSize=512m"
./asadmin $ASADMIN_OPTS create-jvm-options "-XX\:PermSize=512m"
./asadmin $ASADMIN_OPTS delete-jvm-options -Xmx512m
./asadmin $ASADMIN_OPTS create-jvm-options -Xmx1024m

###
# JDBC connection pool
./asadmin $ASADMIN_OPTS create-jdbc-connection-pool --restype javax.sql.DataSource \
                                      --datasourceclassname org.postgresql.ds.PGPoolingDataSource \
                                      --property create=true:User=$DB_USER:PortNumber=$DB_PORT:databaseName=$DB_NAME:password=$DB_PASS:ServerName=$DB_HOST \
                                      dvnDbPool

###
# Create data sources
./asadmin $ASADMIN_OPTS create-jdbc-resource --connectionpoolid dvnDbPool jdbc/VDCNetDS

###
# Set up the data source for the timers
./asadmin $ASADMIN_OPTS set configs.config.server-config.ejb-container.ejb-timer-service.timer-datasource=jdbc/VDCNetDS

###
# Add the necessary JVM options: 
# 
# location of the datafiles directory: 
# (defaults to dataverse/files in the users home directory)
./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.directory=${HOME}/dataverse/files"

# enable comet support
./asadmin $ASADMIN_OPTS set server-config.network-config.protocols.protocol.http-listener-1.http.comet-support-enabled="true"

./asadmin $ASADMIN_OPTS delete-connector-connection-pool --cascade=true jms/__defaultConnectionFactory-Connection-Pool 

# cascade delete takes care of it
#./asadmin $ASADMIN_OPTS delete-connector-resource jms/__defaultConnectionFactory-Connection-Pool

# http://docs.oracle.com/cd/E19798-01/821-1751/gioce/index.html
./asadmin $ASADMIN_OPTS create-connector-connection-pool --steadypoolsize 1 --maxpoolsize 250 --poolresize 2 --maxwait 60000 --raname jmsra --connectiondefinition javax.jms.QueueConnectionFactory jms/IngestQueueConnectionFactoryPool

# http://docs.oracle.com/cd/E18930_01/html/821-2416/abllx.html#giogt
./asadmin $ASADMIN_OPTS create-connector-resource --poolname jms/IngestQueueConnectionFactoryPool --description "ingest connector resource" jms/IngestQueueConnectionFactory

# http://docs.oracle.com/cd/E18930_01/html/821-2416/ablmc.html#giolr
./asadmin $ASADMIN_OPTS create-admin-object --restype javax.jms.Queue --raname jmsra --description "sample administered object" --property Name=DataverseIngest jms/DataverseIngest


#./asadmin $ASADMIN_OPTS create-resource-ref --target Cluster1 jms/IngestQueueConnectionFactory

# created mail configuration: 
# (yes, the mail server is hard-coded; the top-level installer script will be taking care of this)

./asadmin $ASADMIN_OPTS create-javamail-resource --mailhost mail.hmdc.harvard.edu --mailuser "dataversenotify" --fromaddress "do-not-reply@hmdc.harvard.edu" mail/notifyMailSession

###
# Restart
echo Updates done. Restarting...
./asadmin $ASADMIN_OPTS restart-domain $DOMAIN

###
# Clean up
popd

echo "Glassfish setup complete"
date

