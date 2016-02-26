#!/bin/bash

if [[ $EUID -ne 0 ]]; then
  echo "$0 must be run by a super user.\nInstallation failed!" >&2
  exit 1
fi

if [[ -z ${OUTPUT_VERBOSITY} ]]; then OUTPUT_VERBOSITY='1'; fi
if [[ -z ${JAAS_CLIENT_CONF_PATH} ]]; then JAAS_CLIENT_CONF_PATH="/home/glassfish/glassfish4/glassfish/domains/domain1/jaas-client.conf"; fi

_usage() {
  echo "Usage: $0 [chvx]"
  echo "Supported options:"
  echo "  -c     jaas_client.conf path [${JAAS_CLIENT_CONF_PATH}]."
  echo "  -h     Print this help message."
  echo "  -v     Verbosity of this installation script (0-3). [${OUTPUT_VERBOSITY}]"
  echo "  -x     Network accessible Hostname/IP address for the dataverse server."
  echo ""
}

while getopts :c:v:x:h FLAG; do
  case $FLAG in
    c)
      JAAS_CLIENT_CONF_PATH=$OPTARG
      ;;
    h)  #print help
      _usage
      exit 0
      ;;
    v)  #set output verbosity level "v"
      OUTPUT_VERBOSITY=$OPTARG
      ;;
    x)
      DATAVERSE_HOST=$OPTARG
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

newline='
'

$_IF_TERSE echo "Enabling Kerberos authentication of dataverse to solr communication using verbosity level: ${OUTPUT_VERBOSITY}"
$_IF_VERBOSE curl -L -X PUT -d "${JAAS_CLIENT_CONF_PATH}" "http://${DATAVERSE_HOST}:8080/api/admin/settings/:SolrJAASClientConfFile"
$_IF_VERBOSE curl -L -X PUT -d 'yes' "http://${DATAVERSE_HOST}:8080/api/admin/settings/:SolrUsesJAAS"

#### restart the dataverse glassfish domain ####
$_IF_TERSE echo "Restarting the Dataverse domain"
$_IF_VERBOSE /home/glassfish/glassfish4/glassfish//bin/asadmin restart-domain domain1