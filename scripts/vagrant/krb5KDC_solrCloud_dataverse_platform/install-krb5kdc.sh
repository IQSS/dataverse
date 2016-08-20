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
  echo "  -i     IP address for kdc server."
  echo "  -v     Verbosity of this installation script \(0-3\). \[${OUTPUT_VERBOSITY}\]"
  echo "\n"
}

while getopts :i:v:h FLAG; do
  case $FLAG in
    h)  #print help
      _usage
      exit 0
      ;;
    i)
      KRBKDC_SERVER_IP=$OPTARG
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

$_IF_TERSE echo "Installing MIT Kerberos KDC Server using verbosity level: ${OUTPUT_VERBOSITY}"

#### Install krb5-server,krb5-libs, krb5-auth-dialog using yum ####
yum_packages=("krb5-server" "krb5-libs" "krb5-auth-dialog")
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

#### Create kdc.conf configuration file ####
$_IF_INFO echo "Installing the kdc.conf configuration file"
echo "[kdcdefaults]
max_life = 1d
max_renewable_life = 7d
kdc_ports = 88
kdc_tcp_ports = 88

[realms]
DATAVERSE.TEST = {
#master_key_type = aes256-cts
acl_file = /var/kerberos/krb5kdc/kadm5.acl
dict_file = /usr/share/dict/words
admin_keytab = /var/kerberos/krb5kdc/kadm5.keytab
supported_enctypes = aes256-cts:normal aes128-cts:normal des3-hmac-sha1:normal arcfour-hmac:normal des-hmac-sha1:normal des-cbc-md5:normal des-cbc-crc:normal
}
" > /var/kerberos/krb5kdc/kdc.conf
$_IF_INFO echo "Installed to /var/kerberos/krb5kdc/kdc.conf"

#### Install haveged to create randomness in the VM /dev/random ####
$_IF_VERBOSE wget http://download.fedoraproject.org/pub/epel/6/x86_64/epel-release-6-8.noarch.rpm
$_IF_VERBOSE rpm -ivh epel-release-6-8.noarch.rpm
$_IF_VERBOSE yum install -y haveged
haveged -w 1024

#### Initialize the kdb ####
$_IF_INFO echo "Initializing the KDC database"
$_IF_VERBOSE /usr/sbin/kdb5_util -P dataverse create -s

#### Grant admin principles full access ####
$_IF_INFO echo "Setting admin acl in /var/kerberos/krb5kdc/kadm5.acl"
echo '*/admin@DATAVERSE.TEST *' > /var/kerberos/krb5kdc/kadm5.acl

#### start krb5kdc ####
$_IF_INFO echo "Starting the krb5kdc service"
$_IF_VERBOSE service krb5kdc start
$_IF_VERBOSE chkconfig --add krb5kdc
$_IF_VERBOSE chkconfig krb5kdc on

#### start kadmin ####
$_IF_INFO echo "Starting the kadmin service"
$_IF_VERBOSE service kadmin start
$_IF_VERBOSE chkconfig --add kadmin
$_IF_VERBOSE chkconfig kadmin on

$_IF_TERSE echo "kerberos services installed and running"