#!/bin/bash

if [[ $EUID -ne 0 ]]; then
  echo "$0 must be run by a super user.\nInstallation failed!" >&2
  exit 1
fi

if [[ -z ${OUTPUT_VERBOSITY} ]];then OUTPUT_VERBOSITY='1'; fi
if [[ -z ${ZOOKEEPER_ENSEMBLE} ]]; then ZOOKEEPER_ENSEMBLE='localhost:2181'; fi
ENABLE_TLS_ON_SOLR=0
SOLR_INSTALL_DIR='/opt'

_usage() {
  echo "\nUsage: $0 \[ehiv\]"
  echo "\nSupported options:"
  echo "  -e     Zookeeper ensemble string to add to solr.in.sh service configuration script."
  echo "  -h     Print this help message."
  echo "  -i     Solr installation directory. \[${SOLR_INSTALL_DIR}\]"
  echo "  -t     Set solr urlScheme to 'https' in zookeeper."
  echo "  -v     Verbosity of this installation script \(0-3\). \[${OUTPUT_VERBOSITY}\]"
  echo "\n"
}

while getopts :e:h:i:t:v: FLAG; do
  case $FLAG in
    e)
      ZOOKEEPER_ENSEMBLE=$OPTARG
      ;;
    h)  #print help
      _usage
      exit 0
      ;;
    i)  #set option solr install directory "i"
      SOLR_INSTALL_DIR=$OPTARG
      ;;
    t)
      ENABLE_TLS_ON_SOLR=$OPTARG
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

$_IF_TERSE echo "Creating solr Znode using verbosity level: ${OUTPUT_VERBOSITY}"

#### make zkcli.sh executable ####
if [[ -e ${SOLR_INSTALL_DIR}/solr/server/scripts/cloud-scripts/zkcli.sh ]]; then
  chmod +x ${SOLR_INSTALL_DIR}/solr/server/scripts/cloud-scripts/zkcli.sh
else
  echo "zkcli.sh Could NOT be found!\nConfiguration failed!" >&2
  exit 1
fi

#### create solr znode ####
$_IF_VERBOSE ${SOLR_INSTALL_DIR}/solr/server/scripts/cloud-scripts/zkcli.sh -zkhost ${ZOOKEEPER_ENSEMBLE} -cmd makepath /solr
$_IF_TERSE echo "solr znode created"

if [[ ${ENABLE_TLS_ON_SOLR} ]]; then
  $_IF_TERSE echo "Setting the solr urlScheme to 'https' in zookeeper"
  $_IF_VERBOSE ${SOLR_INSTALL_DIR}/solr/server/scripts/cloud-scripts/zkcli.sh -zkhost ${ZOOKEEPER_ENSEMBLE}/solr -cmd clusterprop -name urlScheme -val https
  $_IF_TERSE echo "solr urlScheme set to https"
fi
