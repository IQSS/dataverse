#!/bin/bash
# YOU (THE HUMAN USER) SHOULD NEVER RUN THIS SCRIPT DIRECTLY!
# It should be run by higher-level installers. 
# The following arguments should be passed to it 
# as environmental variables: 
# (no defaults for these values are provided here!)
#
# app. server (payara) configuration: 
# the variables used by this script still has the word "glassfish" in them.
# this is kept as legacy, just to avoid unncessary changes.
# GLASSFISH_ROOT
# GLASSFISH_DOMAIN
# ASADMIN_OPTS
# MEM_HEAP_SIZE
# GLASSFISH_REQUEST_TIMEOUT
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
# DOI configuration:
# DOI_USERNAME
# DOI_PASSWORD
# DOI_BASEURL
#
# Base URL the DataCite REST API (Make Data Count, /pids API): 
# DOI_DATACITERESTAPIURL
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

# This script has two big functions: preliminary_setup and final_setup
# In the use of a container, only final_setup is called
function preliminary_setup()
{
    # undeploy the app, if running: 

  ./asadmin $ASADMIN_OPTS undeploy dataverse

  # avoid OutOfMemoryError: PermGen per http://eugenedvorkin.com/java-lang-outofmemoryerror-permgen-space-error-during-deployment-to-glassfish/
  #./asadmin $ASADMIN_OPTS list-jvm-options
  # Note that these JVM options are different for Payara5 and Glassfish4:
  # old Glassfish4 options: (commented out)
  #./asadmin $ASADMIN_OPTS delete-jvm-options "-XX\:MaxPermSize=192m"
  #./asadmin $ASADMIN_OPTS create-jvm-options "-XX\:MaxPermSize=512m"
  #./asadmin $ASADMIN_OPTS create-jvm-options "-XX\:PermSize=256m"
  # payara5 ships with the "-server" option already in domain.xml, so no need:
  #./asadmin $ASADMIN_OPTS delete-jvm-options -client

  # new Payara5 options: (thanks to donsizemore@unc.edu)
  ./asadmin $ASADMIN_OPTS create-jvm-options "-XX\:MaxMetaspaceSize=512m"
  ./asadmin $ASADMIN_OPTS create-jvm-options "-XX\:MetaspaceSize=256m"
  ./asadmin $ASADMIN_OPTS create-jvm-options "-Dfish.payara.classloading.delegate=false"
  ./asadmin $ASADMIN_OPTS create-jvm-options "-XX\:+UseG1GC"
  ./asadmin $ASADMIN_OPTS create-jvm-options "-XX\:+UseStringDeduplication"
  ./asadmin $ASADMIN_OPTS create-jvm-options "-XX\:+DisableExplicitGC"

  # alias passwords
  for alias in "rserve_password_alias ${RSERVE_PASS}" "doi_password_alias ${DOI_PASSWORD}" "dataverse.db.password ${DB_PASS}"
  do
      set -- $alias
      echo "AS_ADMIN_ALIASPASSWORD=$2" > /tmp/$1.txt
      ./asadmin $ASADMIN_OPTS create-password-alias --passwordfile /tmp/$1.txt $1
      rm /tmp/$1.txt
  done

  ###
  # Add the Dataverse-specific JVM options: 
  # 
  # location of the datafiles temp directory: 
  # (defaults to dataverse/files in the users home directory, ${FILES_DIR}/temp if this is set)
  ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.directory=${FILES_DIR}"
  # Backward compatible file store configuration
  ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.file.type=file"
  ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.file.label=file"
  ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.file.directory=${FILES_DIR}"
  # Rserve-related JVM options: 
  ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.rserve.host=${RSERVE_HOST}"
  ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.rserve.port=${RSERVE_PORT}"
  ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.rserve.user=${RSERVE_USER}"
  ./asadmin $ASADMIN_OPTS create-jvm-options '\-Ddataverse.rserve.password=${ALIAS=rserve_password_alias}'
  # The host and url addresses this Dataverse will be using:
  ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.fqdn=${HOST_ADDRESS}"
  ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.siteUrl=http\://\${dataverse.fqdn}\:8080"
  # password reset token timeout in minutes
  ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.auth.password-reset-timeout-in-minutes=60"

  # DataCite DOI Settings
  # (we can no longer offer EZID with their shared test account)
  # jvm-options use colons as separators, escape as literal
  DOI_BASEURL_ESC=`echo $DOI_BASEURL | sed -e 's/:/\\\:/'`
  ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddoi.username=${DOI_USERNAME}"
  ./asadmin $ASADMIN_OPTS create-jvm-options '\-Ddoi.password=${ALIAS=doi_password_alias}'
  ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddoi.baseurlstring=$DOI_BASEURL_ESC"

  # jvm-options use colons as separators, escape as literal
  DOI_DATACITERESTAPIURL_ESC=`echo $DOI_DATACITERESTAPIURL | sed -e 's/:/\\\:/'`
  ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddoi.dataciterestapiurlstring=$DOI_DATACITERESTAPIURL_ESC"

  ./asadmin $ASADMIN_OPTS create-jvm-options "-Ddataverse.timerServer=true"

  # enable comet support
  ./asadmin $ASADMIN_OPTS set server-config.network-config.protocols.protocol.http-listener-1.http.comet-support-enabled="true"

  # bump the http-listener timeout from 900 to 3600
  ./asadmin $ASADMIN_OPTS set server-config.network-config.protocols.protocol.http-listener-1.http.request-timeout-seconds="${GLASSFISH_REQUEST_TIMEOUT}"

  # so we can front with apache httpd ( ProxyPass / ajp://localhost:8009/ )
  ./asadmin $ASADMIN_OPTS create-network-listener --protocol http-listener-1 --listenerport 8009 --jkenabled true jk-connector
}

function final_setup(){
        ./asadmin $ASADMIN_OPTS delete-jvm-options -Xmx512m
        ./asadmin $ASADMIN_OPTS create-jvm-options "-Xmx${MEM_HEAP_SIZE}m"

         # Set up the database connection properties
        ./asadmin $ASADMIN_OPTS create-system-properties "dataverse.db.user=${DB_USER}"
        ./asadmin $ASADMIN_OPTS create-system-properties "dataverse.db.host=${DB_HOST}"
        ./asadmin $ASADMIN_OPTS create-system-properties "dataverse.db.port=${DB_PORT}"
        ./asadmin $ASADMIN_OPTS create-system-properties "dataverse.db.name=${DB_NAME}"

        ./asadmin $ASADMIN_OPTS create-jvm-options "\-Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl"

	### 
	# Mail server setup: 
	# delete any existing mail/notifyMailSession; configure port, if provided:

	./asadmin delete-javamail-resource mail/notifyMailSession

	if [ $SMTP_SERVER_PORT"x" != "x" ]
	then
            ./asadmin $ASADMIN_OPTS create-javamail-resource --mailhost "$SMTP_SERVER" --mailuser "dataversenotify" --fromaddress "do-not-reply@${HOST_ADDRESS}" --property mail.smtp.port="${SMTP_SERVER_PORT}" mail/notifyMailSession
	else
	    ./asadmin $ASADMIN_OPTS create-javamail-resource --mailhost "$SMTP_SERVER" --mailuser "dataversenotify" --fromaddress "do-not-reply@${HOST_ADDRESS}" mail/notifyMailSession
	fi

}

if [ "$DOCKER_BUILD" = "true" ]
  then
    FILES_DIR="/usr/local/payara5/glassfish/domains/domain1/files"
    RSERVE_HOST="localhost"
    RSERVE_PORT="6311"
    RSERVE_USER="rserve"
    RSERVE_PASS="rserve"
    HOST_ADDRESS="localhost\:8080"
    pushd /usr/local/payara5/glassfish/bin/
    ./asadmin start-domain domain1
    preliminary_setup
    chmod -R 777 /usr/local/payara5/
    rm -rf /usr/local/payara5/glassfish/domains/domain1/generated 
    rm -rf /usr/local/payara5/glassfish/domains/domain1/applications
    popd
    exit 0
fi


if [ -z "$DB_NAME" ]
 then
  echo "You must specify database name (DB_NAME)."
  echo "PLEASE NOTE THAT YOU (THE HUMAN USER) SHOULD NEVER RUN THIS SCRIPT DIRECTLY!"
  echo "IT SHOULD ONLY BE RUN BY OTHER SCRIPTS."
  exit 1
fi

if [ -z "$DB_PORT" ]
 then
  echo "You must specify database port (DB_PORT)."
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

echo "Setting up your app. server (Payara5) to support Dataverse"
echo "Payara directory: "$GLASSFISH_ROOT
echo "Domain directory:    "$DOMAIN_DIR

# Move to the payara dir
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


if [ -z "$MY_POD_NAME" ]
 then
    preliminary_setup
    final_setup
 else
    echo $MY_POD_NAME
    if [ $MY_POD_NAME == "dataverse-glassfish-0" ]
      then
        echo "I am in a container so I am doing much less"
            final_setup
    fi
fi

###
# Restart
echo Updates done. Restarting...
# encountered cases where `restart-domain` timed out, but `stop` -> `start` didn't.
./asadmin $ASADMIN_OPTS stop-domain $GLASSFISH_DOMAIN
./asadmin $ASADMIN_OPTS start-domain $GLASSFISH_DOMAIN

###
# Clean up
popd

echo "Payara setup complete"
date

