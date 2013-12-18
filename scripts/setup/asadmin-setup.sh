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
DB_PORT=5432
DB_HOST=localhost
DB_NAME=dvndb
DB_USER=dvnapp
DB_PASS=dvnAppPass

## 
# External dependencies
PGSQL_DRIVER_URL=http://jdbc.postgresql.org/download/postgresql-9.3-1100.jdbc41.jar


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
# Restart
echo Updates done. Restarting...
./asadmin $ASADMIN_OPTS restart-domain $DOMAIN

###
# Clean up
popd

echo "Glassfish setup complete"
date

