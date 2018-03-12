#!/bin/bash

# Add the passed group to the system.
curl -X POST -H"Content-Type:application/json" -d@../../api/data/$1  localhost:8080/api/admin/groups/ip
