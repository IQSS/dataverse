#!/bin/bash

export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH=$JAVA_HOME/bin:$PATH
export PATH=/opt/apache-maven-3.9.16/bin:$PATH

cd /root/docker/dataverse
mvn -Pct clean package -Dapp.image=dataverse-dialnet:local

cd ../platica-carredi-repositorio/
docker compose up -d dataverse
