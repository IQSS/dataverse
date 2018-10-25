#!/bin/bash
# Creates images and pushes them to Docker Hub.
# The "latest" tag under "iqss" should be relatively stable. Don't push breaking changes there.
# None of the tags are suitable for production use. See https://github.com/IQSS/dataverse/issues/4040
# To iterate on images, push to custom tags or tags based on branch names or a non-iqss Docker Hub org/username. Consider trying "internal" to push to the internal Minishift registry.

# Docker Hub organization or username
HUBORG=iqss
# The most stable tag we have.
STABLE=latest
#FIXME: Use a real flag/argument parser. download-files.sh uses "getopts" for example.
if [ -z "$1" ]; then
  echo "No argument supplied. For experiments, specify \"branch\" or \"custom my-custom-tag\" or \"huborg <USERNAME/ORG>\" or \"internal\". Specify \"stable\" to push to the \"$STABLE\" tag under \"$HUBORG\" if your change won't break anything."
  exit 1
fi

if [ "$1" == 'branch' ]; then
  echo "We'll push a tag to the branch you're on."
  GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
  TAG=$GIT_BRANCH
elif [ "$1" == 'stable' ]; then
  echo "We'll push a tag to the most stable tag (which isn't saying much!)."
  TAG=$STABLE
elif [ "$1" == 'custom' ]; then
  if [ -z "$2" ]; then
    echo "You must provide a custom tag as the second argument. Something other than \"$STABLE\"."
    exit 1
  else
    echo "We'll push a custom tag."
    TAG=$2
  fi
elif [ "$1" == 'huborg' ]; then
  if [ -z "$2" ]; then
    echo "You must provide your Docker Hub organization or username as the second argument. \"$USER\" or whatever."
    exit 1
  else
    HUBORG=$2
    TAG=$STABLE
    echo "We'll push to the Docker Hub organization or username you specified: $HUBORG."
  fi
elif [ "$1" == 'internal' ]; then
  echo "Building for internal Minishift registry."
  TAG=$STABLE
else
  echo "Unexpected argument: $1. Exiting. Run with no arguments for help."
  TAG=$STABLE
  exit 1
fi
echo Images are being built for registry org/username \"$HUBORG\" with the tag \"$TAG\".
#
# Build dataverse-solr
#
cp ../solr/7.3.0/schema.xml solr/
# move solr*.tgz to the solr image
cp ../../downloads/solr-7.3.0.tgz solr/
docker build -t $HUBORG/dataverse-solr:$TAG  solr/
if [ "$1" == 'internal' ]; then
  echo "Skipping docker push because we're using the internal Minishift registry."
else
  # FIXME: Check the output of "docker build" and only push on success.
  docker push $HUBORG/dataverse-solr:$TAG
fi
#
# Build dataverse-glassfish
#
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
docker build -t $HUBORG/dataverse-glassfish:$TAG dataverse-glassfish
if [ "$1" == 'internal' ]; then
  echo "Skipping docker push because we're using the internal Minishift registry."
else
  # FIXME: Check the output of "docker build" and only push on success.
  docker push $HUBORG/dataverse-glassfish:$TAG
fi
#
# Build init-container
#
cp ../../scripts/installer/install dataverse-glassfish/init-container
docker build -t $HUBORG/init-container:$TAG dataverse-glassfish/init-container
if [ "$1" == 'internal' ]; then
  echo "Skipping docker push because we're using the internal Minishift registry."
else
  # FIXME: Check the output of "docker build" and only push on success.
  docker push $HUBORG/init-container:$TAG
fi
