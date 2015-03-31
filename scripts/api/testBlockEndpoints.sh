#!/bin/bash

ADMIN_KEY=$1

echo Testing Groups
curl http://localhost:8080/api/admin/groups/ip/?key=$ADMIN_KEY
echo

echo blocking groups
curl -X PUT -d groups http://localhost:8080/api/admin/settings/:BlockedApiEndpoints
echo

echo Testing Groups again - expecting 503 Unavailable
curl -v http://localhost:8080/api/admin/groups/ip/?key=$ADMIN_KEY
echo

echo Unblocking groups
curl -X DELETE http://localhost:8080/api/admin/settings/:BlockedApiEndpoints
echo

echo Testing Groups
curl http://localhost:8080/api/admin/groups/ip/?key=$ADMIN_KEY
echo

echo blocking groups, Roles
curl -X PUT -d groups,roles http://localhost:8080/api/admin/settings/:BlockedApiEndpoints
echo

echo Testing Groups again - expecting 503 Unavailable
curl -v http://localhost:8080/api/admin/groups/ip/?key=$ADMIN_KEY
echo

echo Testing Roles - expecting 503 Unavailable
curl -v http://localhost:8080/api/roles/?key=$ADMIN_KEY
echo

echo blocking Roles only
curl -X PUT -d roles http://localhost:8080/api/admin/settings/:BlockedApiEndpoints
echo

echo Testing Groups again 
curl -v http://localhost:8080/api/admin/groups/ip/?key=$ADMIN_KEY
echo

echo Testing Roles - expecting 503 Unavailable
curl -v http://localhost:8080/api/roles/?key=$ADMIN_KEY
echo

echo Unblocking all
curl -X DELETE http://localhost:8080/api/admin/settings/:BlockedApiEndpoints
echo

echo DONE
