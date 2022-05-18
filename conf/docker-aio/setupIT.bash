#!/usr/bin/env bash

# do integration-test install and test data setup

cd /opt/dv
unzip dvinstall.zip
cd /opt/dv/testdata
./scripts/deploy/phoenix.dataverse.org/prep
./db.sh
echo "Calling install (bash script) from setupIT.bash..."
./install # modified from phoenix
/opt/payara6/glassfish/bin/asadmin deploy /opt/dv/dvinstall/dataverse.war
echo "Calling post (bash script) from setupIT.bash..."
./post # modified from phoenix

