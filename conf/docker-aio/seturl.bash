#!/usr/bin/env bash

docker exec dv /opt/payara6/bin/asadmin create-jvm-options "\"-Ddataverse.siteUrl=http\://localhost\:8084\""
