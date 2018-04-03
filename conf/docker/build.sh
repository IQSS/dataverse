#!/bin/sh
# Creates images and pushes them to Docker Hub.
# The "latest" tag should be relatively stable. Don't push breaking changes there.
# None of the tags are suitable for production use. See https://github.com/IQSS/dataverse/issues/4040
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
cd ../..
mvn clean
scripts/installer/custom-build-number
mvn package
cd conf/docker
cp ../../target/dataverse*.war dataverse-glassfish/dataverse.war
if [[ "$?" -ne 0 ]]; then
  echo "Unable to copy war file into place. Did 'mvn package' work?"
  exit 1
fi
cd ../../scripts/installer
make clean
make
cd ../../conf/docker
cp ../../scripts/installer/dvinstall.zip dataverse-glassfish
if [[ "$?" -ne 0 ]]; then
  echo "Unable to copy dvinstall.zip file into place. Did 'make' work?"
  exit 1
fi
cp ../../downloads/glassfish-4.1.zip dataverse-glassfish
if [[ "$?" -ne 0 ]]; then
  echo "Unable to copy Glassfish zip file into place. You must run the download script in that directory once. "
  exit 1
fi
# We'll assume at this point that the download script has been run.
cp ../../downloads/weld-osgi-bundle-2.2.10.Final-glassfish4.jar dataverse-glassfish
docker build -t iqss/dataverse-glassfish:$TAG dataverse-glassfish
# FIXME: Check the output of `docker build` and only push on success.
docker push iqss/dataverse-glassfish:$TAG
