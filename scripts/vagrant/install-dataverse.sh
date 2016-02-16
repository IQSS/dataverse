#!/bin/bash

if [[ -z ${OUTPUT_VERBOSITY} ]]; then OUTPUT_VERBOSITY='1'; fi

## OPT_* variables come from /dataverse/scripts/api/bin/dataverse-getopts.sh
. "/dataverse/scripts/api/bin/dataverse-getopts.sh"

#### Set output verbosity ####
## *_CMD and _IF_* command variables are set in /dataverse/scripts/api/bin/util-set-verbosity.sh
if [[ -e "/dataverse/scripts/api/bin/util-set-verbosity.sh" ]]; then
  . "/dataverse/scripts/api/bin/util-set-verbosity.sh"
elif [[ -e "../../api/bin/util-set-verbosity.sh" ]]; then
  . "../../api/bin/util-set-verbosity.sh"
elif [[ -e "./util-set-verbosity.sh" ]]; then
  . "./util-set-verbosity.sh"
else
  CURL_CMD='curl -s'
  WGET_CMD='wget -q'
  YUM_CMD='yum -q'
  MVN_CMP='mvn -q'
fi

$_IF_TERSE echo "Preparing to install dataverse using verbosity level ${OUTPUT_VERBOSITY}"

INSTALL_ARGS="--force -y --hostname $OPT_h --gfdir $OPT_g --mailserver $OPT_m"
if [ -z "$OPT_z" ]; then
  INSTALL_ARGS="$INSTALL_ARGS --solrcollection $OPT_c --solrport $OPT_p --solrhost $OPT_s --solrurlschema $OPT_u"
else
  INSTALL_ARGS="$INSTALL_ARGS --solrcollection $OPT_c --solrzookeeper $OPT_z"
fi

if [ -f /dataverse/pom.xml ]; then
  DATAVERSE_VERSION=`awk -F '[<>]' '$2 == "version" {print $3;exit;}' /dataverse/pom.xml`
  WAR=/dataverse/target/dataverse-${DATAVERSE_VERSION}.war
else
  WAR=/dataverse/target/dataverse*.war
fi

#### Check for dataverse war file ####
$_IF_INFO echo "Checking for dataverse war file"
if [ ! -f $WAR ]; then
  $_IF_VERBOSE echo "${WAR} file not found... building"
  $_IF_VERBOSE echo "Installing nss on CentOS 6 to avoid java.security.KeyException while building war file: https://github.com/IQSS/dataverse/issues/2744"
  $_IF_VERBOSE $YUM_CMD install -y nss
  $_IF_VERBOSE su $SUDO_USER -s /bin/sh -c "cd /dataverse && ${MVN_CMD} package"
else
  $_IF_VERBOSE echo "${WAR} file found"
fi

#### Run installer ####
$_IF_TERSE echo "Running dataverse installer"
$_IF_INFO echo "With arguments: ${INSTALL_ARGS}"
$_IF_VERBOSE pushd /dataverse/scripts/installer
$_IF_TERSE ./install $INSTALL_ARGS