#!/usr/bin/env bash
set -a
. $1
set +a

echo "SITE_URL=${SITE_URL}"
echo "DOI_USERNAME=${DOI_USERNAME}"
echo "DOI_PASSWORD=${DOI_PASSWORD}"
echo "DOI_BASEURL=${DOI_BASEURL}"


docker exec -it dv /usr/local/glassfish4/bin/asadmin create-jvm-options "\"-Ddataverse.siteUrl=${SITE_URL}\""
sleep 15
docker exec -it dv /usr/local/glassfish4/bin/asadmin create-jvm-options "\"-Ddoi.username=${DOI_USERNAME}\""
sleep 15
docker exec -it dv /usr/local/glassfish4/bin/asadmin create-jvm-options "\"-Ddoi.password=${DOI_PASSWORD}\""
sleep 15
docker exec -it dv /usr/local/glassfish4/bin/asadmin create-jvm-options "\"-Ddoi.baseurlstring=${DOI_BASEURL}\""
