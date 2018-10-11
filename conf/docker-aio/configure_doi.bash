#!/usr/bin/env bash

cd /usr/local/glassfish4

# if appropriate; reconfigure PID provider on the basis of environmental variables.
if [ ! -z "${DoiProvider}" ]; then
        curl -X PUT -d ${DoiProvider} http://localhost:8080/api/admin/settings/:DoiProvider
fi
if [ ! -z "${doi_username}" ]; then
        bin/asadmin create-jvm-options "-Ddoi.username=${doi_password}"
fi
if [ ! -z "${doi_password}" ]; then
        bin/asadmin create-jvm-options "-Ddoi.password=${doi_password}"
fi
if [ ! -z "${doi_baseurl}" ]; then
        bin/asadmin delete-jvm-options "-Ddoi.baseurlstring=https\://mds.test.datacite.org"
        doi_baseurl_esc=`echo ${doi_baseurl} | sed -e 's/:/\\:/'`
        bin/asadmin create-jvm-options "\"-Ddoi.baseurlstring=${doi_baseurl_esc}\""
fi
