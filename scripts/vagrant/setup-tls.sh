#!/bin/bash

if [[ $EUID -ne 0 ]]; then
  echo "$0 must be run by a super user.\nInstallation failed!" >&2
  exit 1
fi

if [[ -z ${OUTPUT_VERBOSITY} ]]; then OUTPUT_VERBOSITY='1'; fi
if [[ -z ${HOSTNAME} ]]; then HOSTNAME=`hostname -f`; fi
if [[ -z ${KEYSTORE_PATH} ]]; then KEYSTORE_PATH='/etc/pki/java/dataverse_keystore.jks'; fi
if [[ -z ${KEYSTORE_PASSWORD} ]]; then KEYSTORE_PASSWORD='dataverse'; fi
if [[ -z ${TRUSTSTORE_PASSWORD} ]]; then KEYSTORE_PASSWORD='dataverse'; fi
if [[ -z ${TLS_CERTIFICATE_PATH} ]]; then TLS_CERTIFICATE_PATH='/etc/pki/tls/certs/dataverse_cert.pem'; fi
if [[ -z ${TLS_UNENCRYPTED_KEY_PATH} ]]; then TLS_UNENCRYPTED_KEY_PATH='/etc/pki/tls/private/dataverse.key'; fi
if [[ -z ${TLS_ENCRYPTED_KEY_PATH} ]]; then TLS_ENCRYPTED_KEY_PATH='/etc/pki/tls/private/dataverse.encrypted.key'; fi

_usage() {
  echo "Usage: $0 [ceEhkpvwx]"
  echo "Supported options:"
  echo "  -c     Path (and filename) of the TLS certificate to be created. [${TLS_CERTIFICATE_PATH}]"
  echo "  -e     Path (and filename) of the Non-Encrypted TLS keyfile to be created. [${TLS_UNENCRYPTED_KEY_PATH}]"
  echo "  -E     Path (and filename) of the Encrypted TLS keyfile to be created. [${TLS_ENCRYPTED_KEY_PATH}]"
  echo "  -h     Print this help message."
  echo "  -k     Path (and filename) of java keystore to be created. [${KEYSTORE_PATH}]"
  echo "  -p     Password to use to encrypt all keystores and encrypted keys. [${KEYSTORE_PASSWORD}]"
  echo "  -v     Verbosity of this installation script (0-3). [${OUTPUT_VERBOSITY}]"
  echo "  -w     Watchword (password) for the host (java)truststore file."
  echo "  -x     Network accessible Hostname/IP address for TLS certificate. [${HOSTNAME}]"
  echo ""
}

while getopts :c:e:E:k:p:v:w:x:h FLAG; do
  case $FLAG in
    c) 
      TLS_CERTIFICATE_PATH=$OPTARG
      ;;
    e)
      TLS_UNENCRYPTED_KEY_PATH=$OPTARG
      ;;
    E)
      TLS_ENCRYPTED_KEY_PATH=$OPTARG
      ;;
    h)  #print help
      _usage
      exit 0
      ;;
    k)
      KEYSTORE_PATH=$OPTARG
      ;;
    p)
      KEYSTORE_PASSWORD=$OPTARG
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

#### Check for openssl cmd ####
$_IF_INFO echo "Looking for openssl executable ..."
if [[ -n "$OPENSSL_HOME" ]] && [[ -x "$OPENSSL_HOME/bin/openssl" ]];  then
  $_IF_VERBOSE echo "found openssl executable in OPENSSL_HOME/bin"     
  _openssl="$OPENSSL_HOME/bin/openssl"
elif ( type -p openssl >/dev/null 2>&1 ); then
  $_IF_VERBOSE echo "found openssl executable in PATH" 
  _openssl=openssl
fi

## use keytool -genkeypair to create a self-signed public/private keypair ##
_genkeypair_opt="-keyalg RSA -keysize 2048 -dname 'CN=${HOSTNAME},OU=Dataverse Vagrant Test,O=Dataverse,C=US'"
_keystore_opt="-alias $HOSTNAME -keystore $KEYSTORE_PATH"
_keystore_pass_opt="-storepass $KEYSTORE_PASSWORD -keypass $KEYSTORE_PASSWORD"
$_IF_TERSE echo "Creating self-signed java keystore $KEYSTORE_PATH"
$_IF_VERBOSE eval "$_keytool -genkeypair $_genkeypair_opt $_keystore_opt $_keystore_pass_opt"

## use keytool -export to export the self-signed certificate
_export_opt="-rfc -file $TLS_CERTIFICATE_PATH"
$_IF_TERSE echo "Exporting the self-signed certificate $TLS_CERTIFICATE_PATH"
$_IF_VERBOSE eval "$_keytool -export $_export_opt $_keystore_opt $_keystore_pass_opt" 

## add self-signed certificate to dataverse truststore ##
$_IF_INFO echo "Adding $TLS_CERTIFICATE_PATH to custom dataverse truststore"
$_IF_VERBOSE eval "$_keytool -keystore dataverse_truststore.jks -importcert -alias $HOSTNAME -file $TLS_CERTIFICATE_PATH -storepass $TRUSTSTORE_PASSWORD"

## use keytool to create a dummy pkcs12 keystore to export openssl style keys ##
_importkeystore_opt="-srckeystore $KEYSTORE_PATH -srcstorepass $KEYSTORE_PASSWORD -srckeypass $KEYSTORE_PASSWORD -srcalias $HOSTNAME"
$_IF_INFO echo "Creating temporary pkcs12 keystore to export openssl style keys"
$_IF_VERBOSE eval "$_keytool -importkeystore  $_importkeystore_opt -destkeystore /tmp/tmp.p12 -deststoretype PKCS12 -deststorepass password -destkeypass password"

## use openssl to extract the TLS/SSL private key from the dummy pkcs12 keystore ##
$_IF_TERSE echo "Exporting openssl-style keys $TLS_ENCRYPTED_KEY_PATH and $TLS_UNENCRYPTED_KEY_PATH"
$_IF_VERBOSE eval "$_openssl pkcs12 -in /tmp/tmp.p12 -nocerts -passin pass:password -passout pass:${KEYSTORE_PASSWORD} -out $TLS_ENCRYPTED_KEY_PATH"
$_IF_VERBOSE eval "$_openssl rsa -in $TLS_ENCRYPTED_KEY_PATH -passin pass:${KEYSTORE_PASSWORD} -out $TLS_UNENCRYPTED_KEY_PATH"

$_IF_VERBOSE rm /tmp/tmp.p12

$_IF_TERSE echo "TLS setup complete."
