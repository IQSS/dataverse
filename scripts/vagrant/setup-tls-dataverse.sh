#!/bin/bash

if [[ $EUID -ne 0 ]]; then
  echo "$0 must be run by a super user.\nInstallation failed!" >&2
  exit 1
fi

if [[ -z ${OUTPUT_VERBOSITY} ]]; then OUTPUT_VERBOSITY='1'; fi
if [[ -z ${HOSTNAME} ]]; then HOSTNAME=`hostname -f`; fi
if [[ -z ${TRUSTSTORE_PASSWORD} ]]; then KEYSTORE_PASSWORD='dataverse'; fi
if [[ -z ${TLS_CERTIFICATE_PATH} ]]; then TLS_CERTIFICATE_PATH='/etc/pki/tls/certs/dataverse_cert.pem'; fi

while getopts :c:v:w:x: FLAG; do
  case $FLAG in
    c) 
      TLS_CERTIFICATE_PATH=$OPTARG
      ;;
    v)  #set output verbosity level "v"
      OUTPUT_VERBOSITY=$OPTARG
      ;;
    w)
      TRUSTSTORE_PASSWORD=$OPTARG
      ;;
    x)  #solr server hostname/IP "x"
      HOSTNAME=$OPTARG
      ;;
    :)  #valid option requires adjacent argument
      echo "Option $OPTARG requires an adjacent argument" >&2
      exit 1;
      ;;
    *)
      ;;
  esac
done

#### Set output verbosity ####
## *_CMD and _IF_* command variables are set in /dataverse/scripts/api/bin/util-set-verbosity.sh
if [[ -e "/dataverse/scripts/api/bin/util-set-verbosity.sh" ]]; then
  . "/dataverse/scripts/api/bin/util-set-verbosity.sh"
elif [[ -e "../../api/bin/util-set-verbosity.sh" ]]; then
  . "../../api/bin/util-set-verbosity.sh"
elif [[ -e "./util-set-verbosity.sh" ]]; then
  . "./util-set-verbosity.sh"
fi

$_IF_TERSE echo "Preparing to setup TLS/SSL using output verbosity level: ${OUTPUT_VERBOSITY}"

#### Check for java keytool ####
$_IF_INFO echo "Looking for java keytool ..."
if [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/keytool" ]];  then
  $_IF_VERBOSE echo "found keytool executable in JAVA_HOME/bin"     
  _keytool="$JAVA_HOME/bin/keytool"
elif ( type -p keytool >/dev/null 2>&1 ); then
  $_IF_VERBOSE echo "found keytool executable in PATH" 
  _keytool=keytool
fi

## use keytool -export to export the self-signed certificate
_export_opt="-rfc -file $TLS_CERTIFICATE_PATH"
_keystore="-keystore /home/glassfish/glassfish4/glassfish/domains/domain1/config/keystore.jks -storepass changeit"
$_IF_TERSE echo "Exporting the self-signed certificate $TLS_CERTIFICATE_PATH"
$_IF_VERBOSE eval "$_keytool -export $_export_opt $_keystore -alias 'glassfish-instance'" 

## add self-signed certificate to shared /vagrant/dataverse truststore ##
$_IF_TERSE echo "Adding $TLS_CERTIFICATE_PATH to /vagrant/dataverse_truststore"
$_IF_VERBOSE eval "$_keytool -keystore /vagrant/dataverse_truststore.jks -importcert -noprompt -alias $HOSTNAME -file $TLS_CERTIFICATE_PATH -storepass $TRUSTSTORE_PASSWORD"
