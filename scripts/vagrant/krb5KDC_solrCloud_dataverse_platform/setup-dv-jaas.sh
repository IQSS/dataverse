#!/bin/bash

if [[ $EUID -ne 0 ]]; then
  echo "$0 must be run by a super user.\nInstallation failed!" >&2
  exit 1
fi

if [[ -z ${OUTPUT_VERBOSITY} ]]; then OUTPUT_VERBOSITY='1'; fi
if [[ -z ${KEYTAB_PATH} ]]; then KEYTAB_PATH="/home/glassfish/glassfish4/glassfish/domains/domain1/dataverse.keytab"; fi
if [[ -z ${PRINCIPAL_FIRST} ]]; then PRINCIPAL_FIRST="HTTP"; fi
if [[ -z ${JAAS_CLIENT_CONF_PATH} ]]; then JAAS_CLIENT_CONF_PATH="/home/glassfish/glassfish4/glassfish/domains/domain1/jaas-client.conf"; fi

_usage() {
  echo "Usage: $0 [chikpv]"
  echo "Supported options:"
  echo "  -c     jaas_client.conf path [${JAAS_CLIENT_CONF_PATH}]."
  echo "  -h     Print this help message."
  echo "  -i     Host (second) component of kerberos principal."
  echo "  -k     Path to the dataverse principles keytab file."
  echo "  -p     Primary (first) component of the solr kerberos principal."
  echo "  -v     Verbosity of this installation script (0-3). [${OUTPUT_VERBOSITY}]"
  echo "  -x     Network accessible Hostname/IP address for solr server."
  echo ""
}

while getopts :c:h:i:k:p:v: FLAG; do
  case $FLAG in
    c)
      JAAS_CLIENT_CONF_PATH=$OPTARG
      ;;
    h)  #print help
      _usage
      exit 0
      ;;
    i)
      PRINCIPAL_HOST=$OPTARG
      ;;
    k)
      KEYTAB_PATH=$OPTARG
      ;;
    p)
      PRINCIPAL_FIRST=$OPTARG
      ;;
    v)  #set output verbosity level "v"
      OUTPUT_VERBOSITY=$OPTARG
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

$_IF_TERSE echo "Configuring Kerberos client authentication for dataverse using verbosity level: ${OUTPUT_VERBOSITY}"

if [[ ( ! -e $KEYTAB_PATH ) ]]; then 
  echo "Unable to access kerberos keytab: $KEYTAB_PATH" >&2
  echo "Configuration failed!" >&2
  exit 1
fi

#### Set dataverse JAAS client kerberos configurations ####
## working under the assumption that the solr.keytab includes both the solr(HTTP) and zookeeper principals ##
$_IF_INFO echo "Adding Client (zookeeper) and solrClient (solr) settings to ${JAAS_CLIENT_CONF_PATH}"
echo "SolrJClient {
  com.sun.security.auth.module.Krb5LoginModule required;
  useKeyTab=true;
  keyTab=\"${KEYTAB_PATH}\";
  storeKey=true;
  useTicketCache=true;
  debug=true;
  principal=\”${PRINCIPAL_FIRST}/${PRINCIPAL_HOST}\”;
};
" > ${JAAS_CLIENT_CONF_PATH}

$_IF_TERSE echo "Dataverse solrj-client kerberos successfully configured"