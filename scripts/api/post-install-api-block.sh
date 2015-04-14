#!/bin/bash

# Run this script post-installation, to block all the settings that 
# should not be available to the general public in a production Dataverse installation.
# Relevant settings:
#  - :BlockedApiPolicy - one of allow, drop, localhost-only, unblock-key
#  - :BlockedApiKey - when using the unblock-key policy, pass this key in the unblock-key query param to allow the call to a blocked endpoint
#  - :BlockedApiEndpoints - comma separated list of blocked api endpoints.

curl -X PUT -d localhost-only http://localhost:8080/api/admin/settings/:BlockedApiPolicy
curl -X PUT -d admin,test http://localhost:8080/api/admin/settings/:BlockedApiEndpoints
