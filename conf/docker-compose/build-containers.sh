#!/bin/bash

# Solr XML schema files
cp ../solr/8.11.1/*.xml ../../modules/container-solr/src/main/docker/

# go back to git root directory
cd ../../

# prep Solr beforehand so it has the appropriate permissions
# 8983 is the UID hard-coded in the stock Solr Dockerfile
mkdir -p ./conf/docker-compose/solr-bind/
sudo chown 8983:8983 ./conf/docker-compose/solr-bind/

# copy sourcecode and installer files over
cp pom.xml modules/container-dataverse/src/main/docker/
cp -R ./src/ modules/container-dataverse/src/main/docker/src/
cp -R ./modules/dataverse-parent/ modules/container-dataverse/src/main/docker/modules/dataverse-parent/
cp -R ./scripts/ modules/container-dataverse/src/main/docker/scripts/
cp -R ./conf/ modules/container-dataverse/src/main/docker/conf/
cp -R ./local_lib/ modules/container-dataverse/src/main/docker/local_lib/

# build out each of the images
mvn -Pct -f modules/container-base clean install -Dmaven.test.skip -Ddocker.verbose=true
mvn -Pct -f modules/container-postgresql clean install -Dmaven.test.skip -Ddocker.verbose=true
mvn -Pct -f modules/container-rserve clean install -Dmaven.test.skip -Ddocker.verbose=true
mvn -Pct -f modules/container-seaweedfs clean install -Dmaven.test.skip -Ddocker.verbose=true
mvn -Pct -f modules/container-solr clean install -Dmaven.test.skip -Ddocker.verbose=true
mvn -Pct -f modules/container-traefik clean install -Dmaven.test.skip -Ddocker.verbose=true
mvn -Pct -f modules/container-dataverse clean install -Dmaven.test.skip -Ddocker.verbose=true
