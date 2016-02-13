#!/bin/bash

## OPT_* variables come from /dataverse/scripts/api/bin/dataverse-getopts.sh
. "/dataverse/scripts/api/bin/dataverse-getopts.sh"

if [ -z ${QUIETMODE+x} ] || [ $QUIETMODE -ne "" ]; then 
  YUM_CMD='yum -q'
  MVN_CMD='mvn -q'
else
  YUM_CMD='yum'
  MVN_CMD='mvn'
fi

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

if [ ! -f $WAR ]; then
  echo "${WAR} file not found... building"
  echo "Installing nss on CentOS 6 to avoid java.security.KeyException while building war file: https://github.com/IQSS/dataverse/issues/2744"
  $YUM_CMD install -y nss
  su $SUDO_USER -s /bin/sh -c "cd /dataverse && ${MVN_CMD} package"
fi

cd /dataverse/scripts/installer
./install $INSTALL_ARGS