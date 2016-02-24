#!/bin/bash

if [[ $EUID -ne 0 ]]; then
  echo "$0 must be run by a super user.\nInstallation failed!" >&2
  exit 1
fi

if [[ -z ${OUTPUT_VERBOSITY} ]]; then OUTPUT_VERBOSITY='1'; fi
if [[ -z ${KEYTAB_PATH} ]]; then KEYTAB_PATH="/etc/zookeeper/conf/zookeeper.keytab"; fi
if [[ -z ${PRINCIPAL_FIRST} ]]; then PRINCIPAL_FIRST="zookeeper"; fi
if [[ -z ${ZOOKEEPER_JAAS_CLIENT_CONF_PATH} ]]; then ZOOKEEPER_JAAS_CLIENT_CONF_PATH="/etc/zookeeper/conf/jaas-client.conf"; fi

_usage() {
  echo "Usage: $0 [chikpv]"
  echo "Supported options:"
  echo "  -c     jaas_client.conf path [${ZOOKEEPER_JAAS_CLIENT_CONF_PATH}]."
  echo "  -h     Print this help message."
  echo "  -i     Host (second) component of kerberos principal."
  echo "  -k     Path to the keytab file."
  echo "  -p     Primary (first) component of kerberos principal."
  echo "  -v     Verbosity of this installation script (0-3). [${OUTPUT_VERBOSITY}]"
  echo ""
}

while getopts :c:h:i:k:p:v: FLAG; do
  case $FLAG in
    c)
      ZOOKEEPER_JAAS_CLIENT_CONF_PATH=$OPTARG
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
$_IF_TERSE echo "Enabling Kerberos authentication of the zookeeper service using verbosity level: ${OUTPUT_VERBOSITY}"

#### Set zookeeper kerberos configurations ####
if [[ ( ! -e $KEYTAB_PATH ) ]]; then 
  echo "Unable to access kerberos keytab: $KEYTAB_PATH" >&2
  echo "Configuration failed!" >&2
  return 1
else
  $_IF_INFO echo "Adding zookeeper Server JAAS client path to java JVMFLAGS"
  echo "export JVMFLAGS=\"-Djava.security.auth.login.config=${ZOOKEEPER_JAAS_CLIENT_CONF_PATH}\"${newline}" > /etc/zookeeper/conf/java.conf

  $_IF_INFO echo "Adding kerberos Server client configurations to ${ZOOKEEPER_JAAS_CLIENT_CONF_PATH}"
  echo "Server {
  com.sun.security.auth.module.Krb5LoginModule required
  useKeyTab=true
  keyTab=\"${KEYTAB_PATH}\"
  storeKey=true
  doNotPrompt=true
  useTicketCache=false
  debug=true
  principal=\”${PRINCIPAL_FIRST}/${PRINCIPAL_HOST}\”;
}
" > ${ZOOKEEPER_JAAS_CLIENT_CONF_PATH}

  $_IF_INFO echo "Adding SASLAuthenticationProvider to zookeeper server configuration /etc/zookeeper/conf/zoo.cfg"
  echo "authProvider.1=org.apache.zookeeper.server.auth.SASLAuthenticationProvider${newline}jaasLoginRenew=3600000${newline}" > /etc/zookeeper/conf/zoo.cfg
fi


#### restart zookeeper-server ####
$_IF_INFO echo "Restarting the zookeeper-server service"
$_IF_VERBOSE service zookeeper-server restart

$_IF_TERSE echo "Kerberos authentication enabled for zookeeper"