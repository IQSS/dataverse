#!/bin/bash
curl -X GET http://localhost:8080/api/admin/groups/ip | jq .
