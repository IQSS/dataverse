#!/usr/bin/env bash

if [ "$(docker ps -aq -f name=^/keycloak$)" ]; then
  if [ "$(docker ps -aq -f status=running -f name=^/keycloak$)" ]; then
    docker kill keycloak
  fi
  docker rm keycloak
  echo "INFO - Keycloak container removed"
else
  echo "INFO - No Keycloak container available to remove"
fi
