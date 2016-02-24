#!/bin/bash

if [[ $EUID -ne 0 ]]; then
  echo "$0 must be run by a super user.\nInstallation failed!" >&2
  exit 1
fi

if [[ -z ${OUTPUT_VERBOSITY} ]];then OUTPUT_VERBOSITY='1'; fi

_usage() {
  echo "\nUsage: $0 \[ehiv\]"
  echo "\nSupported options:"
  echo "  -h     Print this help message."
  echo "  -v     Verbosity of this installation script \(0-3\). \[${OUTPUT_VERBOSITY}\]"
  echo "\n"
}

while getopts :v:h FLAG; do
  case $FLAG in
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

$_IF_TERSE echo "Installing MIT Kerberos Gateway libs using verbosity level: ${OUTPUT_VERBOSITY}"

#### Install krb5-workstation,krb5-libs using yum ####
yum_packages=("krb5-workstation" "krb5-libs")
for yummyPkg in "${yum_packages[@]}"; do
  $_IF_TERSE echo "Installing $yummyPkg"
  $_IF_VERBOSE $YUM_CMD install -y $yummyPkg
done


#### Create krb5.conf configuration file #### 
if [[ ! -e /vagrant/krb5.conf ]]; then
  $_IF_INFO echo "Creating the krb5.conf configuration file"
  echo "[logging]
default = FILE:/var/log/krb5libs.log
kdc = FILE:/var/log/krb5kdc.log
admin_server = FILE:/var/log/kadmind.log

[libdefaults]
default_realm = DATAVERSE.TEST
dns_lookup_realm = false
dns_lookup_kdc = false
ticket_lifetime = 24h
renew_lifetime = 7d
forwardable = true

[realms]
DATAVERSE.TEST = {
kdc = ${KRBKDC_SERVER_IP}
admin_server = ${KRBKDC_SERVER_IP}
default_domain = DATAVERSE.TEST
}

[domain_realm]
" > /vagrant/krb5.conf
fi

$_IF_INFO echo "Installing /etc/krb5.conf"
cp /vagrant/krb5.conf /etc/krb5.conf

$_IF_TERSE echo "kerberos gateway installed"