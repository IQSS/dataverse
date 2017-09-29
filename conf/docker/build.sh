#!/bin/sh
# Creates images and pushes them to Docker Hub.
# The "kick-the-tires" tag should be relatively stable. No breaking changes.
# Push to custom tags or tags based on branch names to iterate on the images.
if [ -z "$1" ]; then
  echo "No argument supplied. Please specify \"branch\" or \"custom my-custom-tag\" for experiments or \"stable\" if your change won't break anything."
  exit 1
fi

if [ "$1" == 'branch' ]; then
  echo "We'll push a tag to the branch you're on."
  GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
  TAG=$GIT_BRANCH
elif [ "$1" == 'stable' ]; then
  echo "We'll push a tag to the most stable tag (which isn't saying much!)."
  TAG=kick-the-tires
elif [ "$1" == 'custom' ]; then
  if [ -z "$1" ]; then
    echo "You must provide a custom tag as the second argument."
    exit 1
  else
    echo "We'll push a custom tag."
    TAG=$2
  fi
else
  echo "Unexpected argument: $1. Exiting. Run with no arguments for help."
  exit 1
fi
echo Images will be pushed to Docker Hub with the tag \"$TAG\".
# Use "conf" directory as context so we can copy schema.xml into Solr image.
docker build -t iqss/dataverse-solr:$TAG -f solr/Dockerfile ../../conf
docker push iqss/dataverse-solr:$TAG
# TODO: Think about if we really need dataverse.war because it's in dvinstall.zip.
# FIXME: Automate the building of dataverse.war and dvinstall.zip. Think about https://github.com/IQSS/dataverse/issues/3974 and https://github.com/IQSS/dataverse/pull/3975
cp ../../target/dataverse*.war dataverse-glassfish/dataverse.war
cp ../../scripts/installer/dvinstall.zip dataverse-glassfish
cp ../../doc/sphinx-guides/source/_static/util/default.config dataverse-glassfish
cp ../../downloads/glassfish-4.1.zip dataverse-glassfish
cp ../../downloads/weld-osgi-bundle-2.2.10.Final-glassfish4.jar dataverse-glassfish
docker build -t iqss/dataverse-glassfish:$TAG dataverse-glassfish
# FIXME: Check the output of `docker build` and only push on success.
docker push iqss/dataverse-glassfish:$TAG
