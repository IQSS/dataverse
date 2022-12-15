#!/usr/bin/env bash

DOCKER_IMAGE="jboss/keycloak:16.1.1"
KEYCLOAK_USER="keycloakadmin"
KEYCLOAK_PASSWORD="keycloakadminpassword"

if [ ! "$(docker ps -q -f name=keycloak)" ]; then
  if [ "$(docker ps -aq -f status=exited -f name=keycloak)" ]; then
    echo "INFO - An exited Keycloak container already exists, please select an option:"
    options=("Recreate container" "Restart container" "Quit")
    select opt in "${options[@]}"; do
      case $opt in
      "Recreate container")
        docker rm keycloak
        docker run -d --name keycloak -p 8090:8080 -e KEYCLOAK_USER=$KEYCLOAK_USER -e KEYCLOAK_PASSWORD=$KEYCLOAK_PASSWORD -e KEYCLOAK_IMPORT=/tmp/oidc-realm.json -v "$(pwd)"/oidc-realm.json:/tmp/oidc-realm.json $DOCKER_IMAGE
        echo "INFO - Keycloak container recreated"
        break
        ;;
      "Restart container")
        docker start keycloak
        echo "INFO - Keycloak container restarted"
        break
        ;;
      "Quit")
        break
        ;;
      *) echo "invalid option $REPLY" ;;
      esac
    done
  else
    docker run -d --name keycloak -p 8090:8080 -e KEYCLOAK_USER=$KEYCLOAK_USER -e KEYCLOAK_PASSWORD=$KEYCLOAK_PASSWORD -e KEYCLOAK_IMPORT=/tmp/oidc-realm.json -v "$(pwd)"/oidc-realm.json:/tmp/oidc-realm.json $DOCKER_IMAGE
    echo "INFO - Keycloak container created"
  fi
else
  echo "INFO - Keycloak container is already running"
fi
