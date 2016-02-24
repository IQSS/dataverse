#!/bin/bash

if [[ $EUID -ne 0 ]]; then
  echo "$0 must be run by a super user.\nInstallation failed!" >&2
  exit 1
fi

if [[ -z ${OUTPUT_VERBOSITY} ]]; then OUTPUT_VERBOSITY='1'; fi
if [[ -z ${PRINCIPAL_PASSWORD} ]]; then PRINCIPAL_PASSWORD='password'; fi

_usage() {
  echo "\nUsage: $0 \[h,i,p,p2,p3,v\]"
  echo "\nSupported options:"
  echo "  -h     Print this help message."
  echo "  -i     Host (second) component of kerberos principal."
  echo "  -k     Don't add principals. Just generate keytab."
  echo "  -p     Primary (first) component of kerberos principal."
  echo "  -q     Primary (first) component of additional kerberos principal."
  echo "  -r     Primary (first) component of additional kerberos principal."
  echo "  -v     Verbosity of this installation script \(0-3\). \[${OUTPUT_VERBOSITY}\]"
  echo "  -w     Password for this principal."
  echo "\n"
}

while getopts :i:p:q:r:v:w:hk FLAG; do
  case $FLAG in
    h)  #print help
      _usage
      exit 0
      ;;
    i)
      PRINCIPAL_HOST=$OPTARG
      ;;
    k)
      _keytab_only=1
      ;;
    p)
      PRINCIPAL_FIRST=$OPTARG
      ;;
    q)
      PRINCIPAL2_FIRST=$OPTARG
      ;;
    r)
      PRINCIPAL3_FIRST=$OPTARG
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

if [[ -z ${_keytab_only} ]]; then
  $_IF_TERSE echo "Creating principal ${PRINCIPAL_FIRST}/${PRINCIPAL_HOST}"
  $_IF_VERBOSE kadmin.local -q "addprinc -pw ${PRINCIPAL_PASSWORD} ${PRINCIPAL_FIRST}/${PRINCIPAL_HOST}"
fi
_princ_list="${PRINCIPAL_FIRST}/${PRINCIPAL_HOST}"

if [[ -n ${PRINCIPAL2_FIRST} ]]; then
  if [[ -z ${_keytab_only} ]]; then
    $_IF_TERSE echo "Creating principal ${PRINCIPAL2_FIRST}/${PRINCIPAL_HOST}"
    $_IF_VERBOSE kadmin.local -q "addprinc -pw ${PRINCIPAL_PASSWORD} ${PRINCIPAL2_FIRST}/${PRINCIPAL_HOST}"
  fi
  _princ_list+=" ${PRINCIPAL2_FIRST}/${PRINCIPAL_HOST}"
fi

if [[ -n ${PRINCIPAL3_FIRST} ]]; then
  if [[ -z ${_keytab_only} ]]; then
    $_IF_TERSE echo "Creating principal ${PRINCIPAL3_FIRST}/${PRINCIPAL_HOST}"
    $_IF_VERBOSE kadmin.local -q "addprinc -pw ${PRINCIPAL_PASSWORD} ${PRINCIPAL3_FIRST}/${PRINCIPAL_HOST}"
  fi
  _princ_list+=" ${PRINCIPAL3_FIRST}/${PRINCIPAL_HOST}"
fi

$_IF_TERSE echo "Generating keytab /vagrant/${PRINCIPAL_FIRST}-${PRINCIPAL_HOST}.keytab"
$_IF_VERBOSE kadmin.local -q "xst -norandkey -k /vagrant/${PRINCIPAL_FIRST}-${PRINCIPAL_HOST}.keytab ${_princ_list}"

