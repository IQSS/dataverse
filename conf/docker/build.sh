#!/bin/sh
# Creates images and pushes them to Docker Hub.
GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
# FIXME: Make this script dynamic so you can switch the tag to the branch you're on or a tagged release.
TAG=kick-the-tires
# kick-the-tires should be relatively stable. Push to tags with branch names to iterate on the images.
#TAG=$GIT_BRANCH
echo Images will be pushed to Docker Hub with the tag $TAG
# Use "conf" directory as context so we can copy schema.xml into Solr image.
docker build -t iqss/dataverse-solr:$TAG -f solr/Dockerfile ../../conf
docker push iqss/dataverse-solr:$TAG
# TODO: Think about if we really need dataverse.war because it's in dvinstall.zip.
cp ../../target/dataverse*.war dataverse-glassfish/dataverse.war
cp ../../scripts/installer/dvinstall.zip dataverse-glassfish
cp ../../doc/sphinx-guides/source/_static/util/default.config dataverse-glassfish
cp ../../downloads/glassfish-4.1.zip dataverse-glassfish
cp ../../downloads/weld-osgi-bundle-2.2.10.Final-glassfish4.jar dataverse-glassfish
docker build -t iqss/dataverse-glassfish:$TAG dataverse-glassfish
# FIXME: Check the output of `docker build` and only push on success.
docker push iqss/dataverse-glassfish:$TAG
