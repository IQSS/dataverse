#!/bin/sh

echo "Waiting for Keycloak to be fully up..."

# Loop until the health check returns 200
while true; do
  RESPONSE=$(curl -s -w "\n%{http_code}" "http://keycloak:9000/health")
  HTTP_BODY=$(echo "$RESPONSE" | head -n -1)   # Extract response body
  HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)    # Extract HTTP status code

  if [ "$HTTP_CODE" -eq 200 ]; then
    echo "Keycloak is up! (HTTP $HTTP_CODE)"
    break
  else
    echo "Health check failed (HTTP $HTTP_CODE). Response: $HTTP_BODY"
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
    "parentId": null,
    "config": {
      "datasource": ["user-store"]
    }
  }'

echo "Keycloak SPI configured in realm."
