#!/bin/bash

cp ../solr/8.11.1/*.xml ./solr/

mkdir -p ./dataverse/dataverse/

rsync -a --exclude '.m2*' --exclude '*-bind' ../../../dataverse/ ./dataverse/dataverse/
