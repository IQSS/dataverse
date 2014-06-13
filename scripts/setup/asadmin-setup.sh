#!/bin/bash
# STOP!
# DO NOT ADD MORE ASADMIN COMMANDS TO THIS SCRIPT!
# IF YOU NEED TO ADD MORE GLASSFISH CONFIG SETTINGS, ADD THEM 
# TO THE ../installer/glassfish-setup.sh SCRIPT. 
# I'M ASSUMING THAT WE'LL WANT TO CONTINUE MAINTAINING THIS SCRIPT, 
# (FOR VAGRANT SETUPS, etc.?); IT SHOULD STILL BE WORKING, BY 
# CALLING THE NEW SCRIPT ABOVE - SO NO NEED TO DUPLICATE THE ASADMIN 
# COMMANDS HERE. 
# FROM NOW ON, ONLY NON-ASADMIN CONFIGURATION SHOULD GO INTO THIS 
# SCRIPT. (which makes the name especially misleading - but I didn't 
# want to change it, in case other scripts are calling it by name!)
#     -Leonid 4.0 beta

# This is a setup script for setting up Glassfish 4 to run Dataverse
# The script was tested on Mac OS X.9
# ASSUMPTIONS
# * Script has to run locally (i.e. on the machine that hosts the server)
# * Internet connectivity is assumed, in order to get the postgresql driver.

##
# Default values - Change to suit your machine.
DEFAULT_GLASSFISH_ROOT=/Applications/NetBeans/glassfish-4.0
DEFAULT_DOMAIN=domain1
DEFAULT_ASADMIN_OPTS=" "

###
# Database values. Update as needed.
# Note: DB_USER "dvnApp" is case-sensitive and later used in "scripts/database/reference_data.sql"
#
DB_PORT=5432; export DB_PORT
DB_HOST=localhost; export DB_HOST
DB_NAME=dvndb; export DB_NAME
DB_USER=dvnApp; export DB_USER
DB_PASS=dvnAppPass; export DB_PASS

###
# Rserve configuration: 
RSERVE_HOST=localhost; export RSERVE_HOST
RSERVE_PORT=6311; export RSERVE_PORT
RSERVE_USER=rserve; export RSERVE_USER
RSERVE_PASS=rserve; export RSERVE_PASS

###
# Other configuration values: 
MEM_HEAP_SIZE=1024; export MEM_HEAP_SIZE
HOST_ADDRESS=localhost; export HOST_ADDRESS
SMTP_SERVER=mail.hmdc.harvard.edu; export SMTP_SERVER
FILES_DIR=${HOME}/dataverse/files; export FILES_DIR

### End of default configuration values.

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

if [ "$SUDO_USER" = "vagrant" ]
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
  echo "Configuring EPEL Maven repo "
  cd /etc/yum.repos.d
  wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo
  cd
  echo "Installing dependencies via yum"
  yum install -y -q java-1.7.0-openjdk-devel postgresql-server apache-maven httpd mod_ssl
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
  su $GLASSFISH_USER -s /bin/sh -c "/scripts/installer/glassfish-setup.sh"
fi


# Set the scripts parameters (if needed)
if [ -z "${GLASSFISH_ROOT+xxx}" ]
 then
  echo setting GLASSFISH_ROOT to $DEFAULT_GLASSFISH_ROOT
  GLASSFISH_ROOT=$DEFAULT_GLASSFISH_ROOT; export GLASSFISH_ROOT
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
    # setting the environmental variable GLASSFISH_DOMAIN, 
    # for the ../installer/glassfish-setup.sh script, that runs 
    # all the required asadmin comands
    GLASSFISH_DOMAIN=$DOMAIN; export GLASSFISH_DOMAIN
fi
DOMAIN_DIR=$GLASSFISH_ROOT/glassfish/domains/$DOMAIN
if [ ! -d "$DOMAIN_DIR" ]
  then
    echo Domain directory '$DOMAIN_DIR' does not exist
    exit 2
fi
if [ -z "$ASADMIN_OPTS" ]
 then
  ASADMIN_OPTS=$DEFAULT_ASADMIN_OPTS; export ASADMIN_OPTS
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

if [ "$SUDO_USER" = "vagrant" ]
  then
  /scripts/installer/glassfish-setup.sh
  echo "Done configuring Vagrant environment"
  exit 0
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

# ONCE AGAIN, ASADMIN COMMANDS BELOW HAVE ALL BEEN MOVED INTO scripts/installer/glassfish-setup.sh

# TODO: diagnostics

###
# Clean up
popd

echo "Glassfish setup complete"
date

