#!/bin/bash

# Run this script post-installation, to block all the settings that 
# should not be available to the general public in a production Dataverse installation.

curl -X PUT -d groups,s,index,datasetfield http://localhost:8080/api/s/settings/:BlockedApiEndpoints
