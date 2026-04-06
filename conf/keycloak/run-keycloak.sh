#!/usr/bin/env bash

DOCKER_IMAGE="quay.io/keycloak/keycloak:26.3.2"
KEYCLOAK_USER="kcadmin"
KEYCLOAK_PASSWORD="kcpassword"
KEYCLOAK_PORT=8090

if [ ! "$(docker ps -q -f name=^/keycloak$)" ]; then
  if [ "$(docker ps -aq -f status=exited -f name=^/keycloak$)" ]; then
    echo "INFO - An exited Keycloak container already exists, restarting..."
    docker start keycloak
    echo "INFO - Keycloak container restarted"
  else
    docker run -d --name keycloak -p $KEYCLOAK_PORT:8080 -e KEYCLOAK_USER=$KEYCLOAK_USER -e KEYCLOAK_PASSWORD=$KEYCLOAK_PASSWORD -e KEYCLOAK_IMPORT=/tmp/test-realm.json -v "$(pwd)"/test-realm.json:/tmp/test-realm.json $DOCKER_IMAGE
    echo "INFO - Keycloak container created and running"
  fi
else
  echo "INFO - Keycloak container is already running"
fi
