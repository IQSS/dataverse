#!/bin/sh

echo "Waiting for Keycloak to be fully up..."

# Loop until the health check returns 200
while true; do
  HTTP_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "http://keycloak:8090/health")
  if [ "$HTTP_RESPONSE" -eq 200 ]; then
    echo "Keycloak is up! (HTTP $HTTP_RESPONSE)"
    break
  else
    echo "Health check failed. Waiting..."
    sleep 5
  fi
done

echo "Keycloak is up and running! Executing SPI setup script..."

# Obtain admin token
ADMIN_TOKEN=$(curl -s -X POST "http://keycloak:8090/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=$KEYCLOAK_ADMIN" \
  -d "password=$KEYCLOAK_ADMIN_PASSWORD" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r .access_token)

# Create user storage provider using the components endpoint
curl -X POST "http://keycloak:8090/admin/realms/test/components" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Dataverse built-in users authentication",
    "providerId": "dv-builtin-users-authenticator",
    "providerType": "org.keycloak.storage.UserStorageProvider",
    "parentId": null
  }'

echo "Keycloak SPI configured in realm."
