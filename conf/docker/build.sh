#!/bin/sh
# Creates images and pushes them to Docker Hub.
GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
# Use "conf" directory as context so we can copy schema.xml into Solr image.
docker build -t iqss/dataverse-solr:$GIT_BRANCH -f solr/Dockerfile ../../conf
docker push iqss/dataverse-solr:$GIT_BRANCH
