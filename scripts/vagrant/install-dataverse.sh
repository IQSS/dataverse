#!/usr/bin/env bash

if [ ! -z "$1" ]; then
  MAILSERVER=$1
  MAILSERVER_ARG="--mailserver $MAILSERVER"
fi
WAR=/dataverse/target/dataverse*.war
if [ ! -f $WAR ]; then
  echo "no war file found... building"
  #echo "Installing nss on CentOS 6 to avoid java.security.KeyException while building war file: https://github.com/IQSS/dataverse/issues/2744"
  #yum install -y nss
  su $SUDO_USER -s /bin/sh -c "cd /dataverse && source /etc/profile.d/maven.sh && mvn -q package"
fi
cd /dataverse/scripts/installer

# move any pre-existing `default.config` file out of the way to avoid overwriting
pid=$$
if [ -e default.config ]; then
	cp default.config tmp-${pid}-default.config
fi

# Switch to newer Python-based installer
python3 ./install.py --noninteractive --config_file="default.config"

if [ -e tmp-${pid}-default.config ]; then # if we moved it out, move it back
	mv -f tmp-${pid}-default.config default.config
fi

echo "If "vagrant up" was successful (check output above) Dataverse is running on port 8080 of the Linux machine running within Vagrant, but this port has been forwarded to port 8088 of the computer you ran "vagrant up" on. For this reason you should go to http://localhost:8088 to see the Dataverse app running."

echo "Please also note that the installation script has now started Payara, but has not set up an autostart mechanism for it.\nTherefore, the next time this VM is started, Payara must be started manually.\nSee https://guides.dataverse.org/en/latest/installation/prerequisites.html#launching-payara-on-system-boot for details."
