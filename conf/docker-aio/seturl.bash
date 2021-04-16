#!/usr/bin/env bash

docker exec dv /usr/local/glassfish4/bin/asadmin create-jvm-options "\"-Ddataverse.siteUrl=http\://localhost\:8084\""
