#!/bin/bash

if [[ $EUID -ne 0 ]]; then
  echo "$0 must be run by a super user.\nInstallation failed!" >&2
  exit 1
fi

if [[ -z ${OUTPUT_VERBOSITY} ]]; then OUTPUT_VERBOSITY='1'; fi
if [[ -z ${PRINCIPAL_PASSWORD} ]]; then PRINCIPAL_PASSWORD='password'; fi

_usage() {
  echo "\nUsage: $0 \[hiv\]"
  echo "\nSupported options:"
  echo "  -h     Print this help message."
  echo "  -i     Host (second) component of kerberos principal."
  echo "  -p     Primary (first) component of kerberos principal."
  echo "  -v     Verbosity of this installation script \(0-3\). \[${OUTPUT_VERBOSITY}\]"
  echo "  -w     Password for this principal."
  echo "\n"
}

while getopts :i:p:v:w:h FLAG; do
  case $FLAG in
    h)  #print help
      _usage
      exit 0
      ;;
    i)
      PRINCIPAL_HOST=$OPTARG
      ;;
    p)
      PRINCIPAL_FIRST=$OPTARG
      ;;
    v)  #set output verbosity level "v"
      OUTPUT_VERBOSITY=$OPTARG
      ;;
    w)
      PRINCIPAL_PASSWORD=$OPTARG
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

$_IF_TERSE echo "Creating principal ${PRINCIPAL_FIRST}/${PRINCIPAL_HOST}"
$_IF_VERBOSE kadmin.local -q "addprinc -pw ${PRINCIPAL_PASSWORD} ${PRINCIPAL_FIRST}/${PRINCIPAL_HOST}"
$_IF_TERSE echo "Creating principal HTTP/${PRINCIPAL_HOST}"
$_IF_VERBOSE kadmin.local -q "addprinc -pw ${PRINCIPAL_PASSWORD} HTTP/${PRINCIPAL_HOST}"

$_IF_TERSE echo "Generating keytab /vagrant/${PRINCIPAL_FIRST}-${PRINCIPAL_HOST}.keytab"
$_IF_VERBOSE kadmin.local -q "xst -norandkey -k /vagrant/${PRINCIPAL_FIRST}-${PRINCIPAL_HOST}.keytab ${PRINCIPAL_FIRST}/${PRINCIPAL_HOST} HTTP/${PRINCIPAL_HOST}"

