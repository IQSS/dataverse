#!/bin/sh
# Creates images and pushes them to Docker Hub.
GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
# Use "conf" directory as context so we can copy schema.xml into Solr image.
docker build -t iqss/dataverse-solr:$GIT_BRANCH -f solr/Dockerfile ../../conf
docker push iqss/dataverse-solr:$GIT_BRANCH
# TODO: Think about if we really need dataverse.war because it's in dvinstall.zip.
cp ../../target/dataverse*.war dataverse-glassfish/dataverse.war
cp ../../scripts/installer/dvinstall.zip dataverse-glassfish
cp ../../doc/sphinx-guides/source/_static/util/default.config dataverse-glassfish
cp ../../downloads/glassfish-4.1.zip dataverse-glassfish
cp ../../downloads/weld-osgi-bundle-2.2.10.Final-glassfish4.jar dataverse-glassfish
docker build -t iqss/dataverse-glassfish:$GIT_BRANCH dataverse-glassfish
docker push iqss/dataverse-glassfish:$GIT_BRANCH
