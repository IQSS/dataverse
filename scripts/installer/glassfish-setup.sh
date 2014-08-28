#!/bin/bash
# YOU (THE HUMAN USER) SHOULD NEVER RUN THIS SCRIPT DIRECTLY!
# It should be run by higher-level installers. 
# The following arguments should be passed to it 
# as environmental variables: 
# (no defaults for these values are provided here!)
#
# glassfish configuration: 
# GLASSFISH_ROOT
# GLASSFISH_DOMAIN
# ASADMIN_OPTS
# MEM_HEAP_SIZE
#
# database configuration: 
# DB_PORT
# DB_HOST
# DB_NAME
# DB_USER
# DB_PASS
#
# Rserve configuration: 
# RSERVE_HOST
# RSERVE_PORT
# RSERVE_USER
# RSERVE_PASS
#
# other local configuration:
# HOST_ADDRESS
# SMTP_SERVER
# FILES_DIR

# The script is going to fail and exit if any of the
# parameters aren't supplied. It is the job of the 
# parent script to set all these env. variables, 
# providing default values, if none are supplied by 
# the user, etc. 

if [ -z "$DB_NAME" ]
 then
  echo "You must specify database name (DB_NAME)."
  echo "PLEASE NOTE THAT YOU (THE HUMAN USER) SHOULD NEVER RUN THIS SCRIPT DIRECTLY!"
  echo "IT SHOULD ONLY BE RUN BY OTHER SCRIPTS."
  exit 1
fi

if [ -z "$DB_PORT" ]
 then
  echo "You must specify database port (DB_NAME)."
  exit 1
fi

if [ -z "$DB_HOST" ]
 then
  echo "You must specify database host (DB_HOST)."
  exit 1
fi

if [ -z "$DB_USER" ]
 then
  echo "You must specify database user (DB_USER)."
  exit 1
fi

if [ -z "$DB_PASS" ]
 then
  echo "You must specify database password (DB_PASS)."
  exit 1
fi

if [ -z "$RSERVE_HOST" ]
 then
  echo "You must specify Rserve host (RSERVE_HOST)."
  exit 1
fi

if [ -z "$RSERVE_PORT" ]
 then
  echo "You must specify Rserve port (RSERVE_PORT)."
  exit 1
fi

if [ -z "$RSERVE_USER" ]
 then
  echo "You must specify Rserve user (RSERVE_USER)."
  exit 1
fi

if [ -z "$RSERVE_PASS" ]
 then
  echo "You must specify Rserve password (RSERVE_PASS)."
  exit 1
fi

if [ -z "$SMTP_SERVER" ]
 then
  echo "You must specify smtp server (SMTP_SERVER)."
  exit 1
fi

if [ -z "$HOST_ADDRESS" ]
 then
  echo "You must specify host address (HOST_ADDRESS)."
  exit 1
fi

if [ -z "$FILES_DIR" ]
 then
  echo "You must specify files directory (FILES_DIR)."
  exit 1
fi

if [ -z "$MEM_HEAP_SIZE" ]
 then
  echo "You must specify the memory heap size for glassfish (MEM_HEAP_SIZE)."
  exit 1
fi

if [ -z "$GLASSFISH_DOMAIN" ]
 then
  echo "You must specify glassfish domain (GLASSFISH_DOMAIN)."
  exit 1
fi

echo "checking glassfish root:"${GLASSFISH_ROOT}

if [ ! -d "$GLASSFISH_ROOT" ]
  then
    echo Glassfish root '$GLASSFISH_ROOT' does not exist
    exit 1
fi
GLASSFISH_BIN_DIR=$GLASSFISH_ROOT/bin

echo "checking glassfish domain:"${GLASSFISH_ROOT}/glassfish/domains/$GLASSFISH_DOMAIN

DOMAIN_DIR=$GLASSFISH_ROOT/glassfish/domains/$GLASSFISH_DOMAIN
if [ ! -d "$DOMAIN_DIR" ]
  then
    echo Domain directory '$DOMAIN_DIR' does not exist
    exit 2
fi

echo "Setting up your glassfish4 to support Dataverse"
echo "Glassfish directory: "$GLASSFISH_ROOT
echo "Domain directory:    "$DOMAIN_DIR

# Move to the glassfish dir
pushd $GLASSFISH_BIN_DIR

###
# take the domain up, if needed.
DOMAIN_DOWN=$(./asadmin list-domains | grep "$DOMAIN " | grep "not running")
if [  $(echo $DOMAIN_DOWN|wc -c) -ne 1  ];
  then
    echo Trying to start domain $GLASSFISH_DOMAIN up...
    ./asadmin $ASADMIN_OPTS start-domain $GLASSFISH_DOMAIN
  else
    echo domain running
fi

# undeploy the app, if running: 

./asadmin $ASADMIN_OPTS undeploy dataverse-4.0

# avoid OutOfMemoryError: PermGen per http://eugenedvorkin.com/java-lang-outofmemoryerror-permgen-space-error-during-deployment-to-glassfish/
#./asadmin $ASADMIN_OPTS list-jvm-options
./asadmin $ASADMIN_OPTS delete-jvm-options "-XX\:MaxPermSize=192m"
./asadmin $ASADMIN_OPTS create-jvm-options "-XX\:MaxPermSize=512m"
./asadmin $ASADMIN_OPTS create-jvm-options "-XX\:PermSize=256m"
./asadmin $ASADMIN_OPTS delete-jvm-options -Xmx512m
./asadmin $ASADMIN_OPTS create-jvm-options "-Xmx${MEM_HEAP_SIZE}m"

###
# JDBC connection pool

# we'll try to delete a pool with this name, if already exists. 
# - in case the database name has changed since the last time it 
# was configured. 
./asadmin $ASADMIN_OPTS delete-jdbc-connection-pool --cascade=true dvnDbPool


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
./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.directory=${FILES_DIR}"
# Rserve-related JVM options: 
./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.rserve.host=${RSERVE_HOST}"
./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.rserve.port=${RSERVE_PORT}"
./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.rserve.user=${RSERVE_USER}"
./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.rserve.password=${RSERVE_PASS}"
# Data Deposit API options
./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.fqdn=${HOST_ADDRESS}"
# password reset token timeout in minutes
./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.auth.password-reset-timeout-in-minutes=60"

# enable comet support
./asadmin $ASADMIN_OPTS set server-config.network-config.protocols.protocol.http-listener-1.http.comet-support-enabled="true"

./asadmin $ASADMIN_OPTS delete-connector-connection-pool --cascade=true jms/__defaultConnectionFactory-Connection-Pool 

# no need to explicitly delete the connector resource for the connection pool deleted in the step 
# above - the cascade delete takes care of it.
#./asadmin $ASADMIN_OPTS delete-connector-resource jms/__defaultConnectionFactory-Connection-Pool

# http://docs.oracle.com/cd/E19798-01/821-1751/gioce/index.html
./asadmin $ASADMIN_OPTS create-connector-connection-pool --steadypoolsize 1 --maxpoolsize 250 --poolresize 2 --maxwait 60000 --raname jmsra --connectiondefinition javax.jms.QueueConnectionFactory jms/IngestQueueConnectionFactoryPool

# http://docs.oracle.com/cd/E18930_01/html/821-2416/abllx.html#giogt
./asadmin $ASADMIN_OPTS create-connector-resource --poolname jms/IngestQueueConnectionFactoryPool --description "ingest connector resource" jms/IngestQueueConnectionFactory

# http://docs.oracle.com/cd/E18930_01/html/821-2416/ablmc.html#giolr
./asadmin $ASADMIN_OPTS create-admin-object --restype javax.jms.Queue --raname jmsra --description "sample administered object" --property Name=DataverseIngest jms/DataverseIngest

# no need to explicitly create the resource reference for the connection factory created above -
# the "create-connector-resource" creates the reference automatically.
#./asadmin $ASADMIN_OPTS create-resource-ref --target Cluster1 jms/IngestQueueConnectionFactory

# created mail configuration: 

./asadmin $ASADMIN_OPTS create-javamail-resource --mailhost "$SMTP_SERVER" --mailuser "dataversenotify" --fromaddress "do-not-reply@${HOST_ADDRESS}" mail/notifyMailSession

# so we can front with apache httpd ( ProxyPass / ajp://localhost:8009/ )
./asadmin $ASADMIN_OPTS create-network-listener --protocol http-listener-1 --listenerport 8009 --jkenabled true jk-connector

###
# Restart
echo Updates done. Restarting...
./asadmin $ASADMIN_OPTS restart-domain $GLASSFISH_DOMAIN

###
# Clean up
popd

echo "Glassfish setup complete"
date

