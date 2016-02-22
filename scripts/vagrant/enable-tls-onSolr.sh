#!/bin/bash

if [[ $EUID -ne 0 ]]; then
  echo "$0 must be run by a super user.\nInstallation failed!" >&2
  exit 1
fi

if [[ -z ${OUTPUT_VERBOSITY} ]]; then OUTPUT_VERBOSITY='1'; fi
if [[ -z ${SOLR_IN_SH_PATH} ]]; then SOLR_IN_SH_PATH="/etc/default/solr.in.sh"; fi

_usage() {
  echo "\nUsage: $0 \[hikprstvw\]"
  echo "\nSupported options:"
  echo "  -h     Print this help message."
  echo "  -i     Path to the solr.in.sh init.d service configuration script. \[${SOLR_IN_SH_PATH}\]"
  echo "  -k     Path to the host (java)keystore [.jks] file."
  echo "  -p     Password for the host (java)keystore file."
  echo "  -r     Require client TLS/SSL authentication."
  echo "  -s     Suggest client TLS/SSL authentication."
  echo "  -t     Path to the host (java)truststore [.jks] file."
  echo "  -v     Verbosity of this installation script \(0-3\). \[${OUTPUT_VERBOSITY}\]"
  echo "  -w     Watchword (password) for the host (java)truststore file."
  echo "\n"
}

while getopts :h:i:k:p:t:v:w:rs FLAG; do
  case $FLAG in
    h)  #print help
      _usage
      exit 0
      ;;
    i)
      SOLR_IN_SH_PATH=$OPTARG
      ;;
    k)
      KEYSTORE_PATH=$OPTARG
      ;;
    p)
      KEYSTORE_PASSWORD=$OPTARG
      ;;
    r)
      SSL_NEED_CLIENT_AUTH="true"
      ;;
    s)
      SSL_WANT_CLIENT_AUTH="true"
      ;;
    t)
      TRUSTSTORE_PATH=$OPTARG
      ;;
    v)  #set output verbosity level "v"
      OUTPUT_VERBOSITY=$OPTARG
      ;;
    w)
      TRUSTSTORE_PASSWORD=$OPTARG
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
else
  CURL_CMD='curl'
fi

$_IF_TERSE echo "Enabling TLS/SSL for the solr service using verbosity level: ${OUTPUT_VERBOSITY}"

if [[ ( -z ${KEYSTORE_PATH} ) || ( ! -e $KEYSTORE_PATH ) ]]; then 
  echo "Unable to access keystore: $KEYSTORE_PATH" >&2
  echo "Configuration failed!" >&2
  return 1
else
  SOLR_SSL_SETTINGS="SOLR_SSL_KEY_STORE=${KEYSTORE_PATH}"$'\n'
  if [[ -n ${KEYSTORE_PASSWORD} ]]; then
    SOLR_SSL_SETTINGS+="SOLR_SSL_KEY_STORE_PASSWORD=${KEYSTORE_PASSWORD}"$'\n'
  fi
  if [[ -n ${TRUSTSTORE_PATH} ]]; then
    SOLR_SSL_SETTINGS+="SOLR_SSL_TRUST_STORE=${TRUSTSTORE_PATH}"$'\n'
  fi
  if [[ -n ${TRUSTSTORE_PASSWORD} ]]; then
    SOLR_SSL_SETTINGS+="SOLR_SSL_TRUST_STORE_PASSWORD=${TRUSTSTORE_PASSWORD}"$'\n'
  fi
  if [[ -n ${SSL_NEED_CLIENT_AUTH} ]]; then
    SOLR_SSL_SETTINGS+="SOLR_SSL_NEED_CLIENT_AUTH=true"$'\n'
    SOLR_SSL_SETTINGS+="SOLR_SSL_WANT_CLIENT_AUTH=false"$'\n'
  elif [[ -n ${SSL_WANT_CLIENT_AUTH} ]]; then
    SOLR_SSL_SETTINGS+="SOLR_SSL_NEED_CLIENT_AUTH=false"$'\n'
    SOLR_SSL_SETTINGS+="SOLR_SSL_WANT_CLIENT_AUTH=true"$'\n'
  fi
fi

#### Modify /etc/defaults/solr.in.sh service configuration file #### 
$_IF_INFO echo "Adding TLS/SSL configurations to the solr.in.sh configuration file"
$_IF_VERBOSE echo $SOLR_SSL_SETTINGS >> $SOLR_IN_SH_PATH

$_IF_TERSE echo "solr TLS/SSL configured"
$_IF_INFO echo "Please restart the solr service to fully enable TLS/SSL"