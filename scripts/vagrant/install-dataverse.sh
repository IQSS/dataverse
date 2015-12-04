#!/bin/bash
if [ -n "$1" ]; then
  MAILSERVER_ARG="--mailserver $1"
fi
WAR=/dataverse/target/dataverse*.war
if [ ! -f $WAR ]; then
  echo "no war file found... building"
  echo "Installing nss on CentOS 6 to avoid java.security.KeyException while building war file: https://github.com/IQSS/dataverse/issues/2744"
  yum install -y nss
  su $SUDO_USER -s /bin/sh -c "cd /dataverse && mvn package"
fi
cd /dataverse/scripts/installer
./install --hostname localhost $MAILSERVER_ARG --gfdir /home/glassfish/glassfish4 -y --force
