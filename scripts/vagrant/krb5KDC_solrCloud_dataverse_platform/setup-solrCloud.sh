#!/bin/bash

if [[ $EUID -ne 0 ]]; then
  echo "$0 must be run by a super user.\nInstallation failed!" >&2
  exit 1
fi

if [[ -z ${OUTPUT_VERBOSITY} ]];then OUTPUT_VERBOSITY='1'; fi
if [[ -z ${ZOOKEEPER_ENSEMBLE} ]]; then ZOOKEEPER_ENSEMBLE='localhost:2181'; fi

_usage() {
  echo "\nUsage: $0 \[ehiv\]"
  echo "\nSupported options:"
  echo "  -e     Zookeeper ensemble string to add to solr.in.sh service configuration script."
  echo "  -h     Print this help message."
  echo "  -v     Verbosity of this installation script \(0-3\). \[${OUTPUT_VERBOSITY}\]"
  echo "\n"
}

while getopts :e:h:v: FLAG; do
  case $FLAG in
    e)
      ZOOKEEPER_ENSEMBLE=$OPTARG
      ;;
    h)  #print help
      _usage
      exit 0
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

$_IF_TERSE echo "Enabling solrCloud service using verbosity level: ${OUTPUT_VERBOSITY}"

#### Modify /etc/defaults/solr.in.sh service configuration file #### 
$_IF_INFO echo "Adding ensemble servers to the solr.in.sh configuration file"
$_IF_VERBOSE echo "ZK_HOST=${ZOOKEEPER_ENSEMBLE}/solr" >> /etc/default/solr.in.sh

$_IF_TERSE echo "solrCloud configured"
$_IF_TERSE echo "Please restart the solr service to fully enable solrCloud"