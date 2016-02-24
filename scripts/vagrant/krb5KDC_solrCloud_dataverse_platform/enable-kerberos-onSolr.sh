#!/bin/bash

if [[ $EUID -ne 0 ]]; then
  echo "$0 must be run by a super user.\nInstallation failed!" >&2
  exit 1
fi

if [[ -z ${OUTPUT_VERBOSITY} ]]; then OUTPUT_VERBOSITY='1'; fi
if [[ -z ${KEYTAB_PATH} ]]; then KEYTAB_PATH="/var/solr/solr.keytab"; fi
if [[ -z ${SOLR_PRINCIPAL_FIRST} ]]; then SOLR_PRINCIPAL_FIRST="HTTP"; fi
if [[ -z ${ZOOKEEPER_PRINCIPAL_FIRST} ]]; then ZOOKEEPER_PRINCIPAL_FIRST="zookeeper"; fi
if [[ -z ${JAAS_CLIENT_CONF_PATH} ]]; then JAAS_CLIENT_CONF_PATH="/var/solr/jaas-client.conf"; fi

_usage() {
  echo "Usage: $0 [chikpv]"
  echo "Supported options:"
  echo "  -c     jaas_client.conf path [${JAAS_CLIENT_CONF_PATH}]."
  echo "  -h     Print this help message."
  echo "  -i     Host (second) component of kerberos principal."
  echo "  -k     Path to the solr principles keytab file."
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
      SOLR_PRINCIPAL_FIRST=$OPTARG
      ;;
    v)  #set output verbosity level "v"
      OUTPUT_VERBOSITY=$OPTARG
      ;;
    x)  #solr server hostname/IP "x"
      SOLR_HOST=$OPTARG
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

$_IF_TERSE echo "Enabling Kerberos authentication of the solr service using verbosity level: ${OUTPUT_VERBOSITY}"

if [[ ( ! -e $KEYTAB_PATH ) ]]; then 
  echo "Unable to access kerberos keytab: $KEYTAB_PATH" >&2
  echo "Configuration failed!" >&2
  return 1
fi

#### Set solr JAAS client kerberos configurations ####
## working under the assumption that the solr.keytab includes both the solr(HTTP) and zookeeper principals ##
$_IF_INFO echo "Adding Client (zookeeper) and solrClient (solr) settings to ${JAAS_CLIENT_CONF_PATH}"
echo "Client {
  com.sun.security.auth.module.Krb5LoginModule required;
  useKeyTab=true;
  keyTab=\"${KEYTAB_PATH}\";
  storeKey=true;
  useTicketCache=true;
  debug=true;
  principal=\”${ZOOKEEPER_PRINCIPAL_FIRST}/${PRINCIPAL_HOST}\”;
};

solrClient {
  com.sun.security.auth.module.Krb5LoginModule required;
  useKeyTab=true;
  keyTab=\"${KEYTAB_PATH}\";
  storeKey=true;
  useTicketCache=true;
  debug=true;
  principal=\”${SOLR_PRINCIPAL_FIRST}/${PRINCIPAL_HOST}\”;
};
" > ${JAAS_CLIENT_CONF_PATH}


#### Set solr.in.sh to use JAAS client configurations ####
$_IF_INFO echo "Configuring /etc/default/solr.in.sh to use ${JAAS_CLIENT_CONF_PATH}"
echo "SOLR_AUTHENTICATION_CLIENT_CONFIGURER=org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer
SOLR_AUTHENTICATION_OPTS=\"-Djava.security.auth.login.config=${JAAS_CLIENT_CONF_PATH} -Dsolr.kerberos.cookie.domain=${SOLR_HOST} -Dsolr.kerberos.cookie.portaware=true -Dsolr.kerberos.principal=${SOLR_PRINCIPAL_FIRST}/${PRINCIPAL_HOST} -Dsolr.kerberos.keytab=${KEYTAB_PATH}
" > /etc/default/solr.in.sh


$_IF_TERSE echo "solr kerberos configured"
$_IF_INFO echo "Please restart the solr service to fully enable kerberos authentication"