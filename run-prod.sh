#!/bin/bash

mvn -DskipTests=true clean package
docker compose -f docker-compose-prod.yml up -d --build