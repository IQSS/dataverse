#!/bin/bash

# create the config file that has all our environment settings

echo -e "
[glassfish]
HOST_DNS_ADDRESS=${HOST_DNS_ADDRESS}
GLASSFISH_USER = ${GLASSFISH_USER}
GLASSFISH_DIRECTORY = /usr/local/payara5/
GLASSFISH_ADMIN_USER = ${GLASSFISH_ADMIN_USER}
GLASSFISH_ADMIN_PASSWORD = ${GLASSFISH_ADMIN_PASSWORD}
GLASSFISH_HEAP = 2048
GLASSFISH_REQUEST_TIMEOUT = 1800

[database]
POSTGRES_ADMIN_PASSWORD=${POSTGRES_ADMIN_PASSWORD}
POSTGRES_SERVER=${POSTGRES_SERVER}
POSTGRES_PORT=${POSTGRES_PORT}
POSTGRES_DATABASE=${POSTGRES_DATABASE}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
POSTGRES_USER=${POSTGRES_USER}

[system]
ADMIN_EMAIL=${ADMIN_EMAIL}
MAIL_SERVER=${MAIL_SERVER}
SOLR_LOCATION=${SOLR_LOCATION}

[rserve]
RSERVE_HOST=${RSERVE_HOST}
RSERVE_PORT=${RSERVE_PORT}
RSERVE_USER=${RSERVE_USER}
RSERVE_PASSWORD=${RSERVE_PASSWORD}

[doi]
DOI_USERNAME = dataciteuser
DOI_PASSWORD = datacitepassword
DOI_BASEURL = https://mds.test.datacite.org
DOI_DATACITERESTAPIURL = https://api.test.datacite.org
" > /dataverse/scripts/installer/default.config

# https://github.com/poikilotherm/dataverse/blob/ct-mvn-mod/modules/container-base/src/main/docker/Dockerfile
# https://guides.dataverse.org/en/latest/installation/config.html#amazon-s3-storage-or-compatible
# set s3 storage settings
if ! grep -q "Ddataverse.files.s3.type=s3" "/usr/local/payara5/glassfish/domains/domain1/config/domain.xml"; then
  # use : as delimiter
  sed -i "s:</java-config>:<jvm-options>-Ddataverse.files.s3.type=s3</jvm-options>\n</java-config>:" /usr/local/payara5/glassfish/domains/domain1/config/domain.xml
  sed -i "s:</java-config>:<jvm-options>-Ddataverse.files.s3.label=s3</jvm-options>\n</java-config>:" /usr/local/payara5/glassfish/domains/domain1/config/domain.xml
  sed -i "s:</java-config>:<jvm-options>-Ddataverse.files.s3.access-key=${S3_ACCESS_KEY}</jvm-options>\n</java-config>:" /usr/local/payara5/glassfish/domains/domain1/config/domain.xml
  sed -i "s:</java-config>:<jvm-options>-Ddataverse.files.s3.secret-key=${S3_SECRET_KEY}</jvm-options>\n</java-config>:" /usr/local/payara5/glassfish/domains/domain1/config/domain.xml
  sed -i "s:</java-config>:<jvm-options>-Ddataverse.files.s3.custom-endpoint-url=http\:\/\/seaweedfs\:8333</jvm-options>\n</java-config>:" /usr/local/payara5/glassfish/domains/domain1/config/domain.xml
  # keep this as dataverse as it's hardcoded elsewhere
  sed -i "s:</java-config>:<jvm-options>-Ddataverse.files.s3.bucket-name=dataverse</jvm-options>\n</java-config>:" /usr/local/payara5/glassfish/domains/domain1/config/domain.xml
  sed -i "s:</java-config>:<jvm-options>-Ddataverse.files.s3.custom-endpoint-region=us-east-1</jvm-options>\n</java-config>:" /usr/local/payara5/glassfish/domains/domain1/config/domain.xml
  # # Use path style buckets instead of subdomains
  sed -i "s:</java-config>:<jvm-options>-Ddataverse.files.s3.path-style-access=true</jvm-options>\n</java-config>:" /usr/local/payara5/glassfish/domains/domain1/config/domain.xml
fi

cd /dataverse/scripts/installer/

# the installer needs to run from within the directory, it cannot be run from / for example
# this can take some time to run, be patient
python3 install.py --noninteractive --force

# check if we should disable DOI validation
if [[ ! -z "${DISABLE_DOI}" ]] && [[ "true" = "${DISABLE_DOI}" ]]; then
  echo "Disabling DOI validation"
  curl -X PUT -d FAKE http://localhost:8080/api/admin/settings/:DoiProvider
fi

# check if we should exclude emails from exports
if [[ ! -z "${EXCLUDE_EMAIL_EXPORTS}" ]] && [[ "true" = "${EXCLUDE_EMAIL_EXPORTS}" ]]; then
  echo "Excluding emails in exports"
  curl -X PUT -d true http://localhost:8080/api/admin/settings/:ExcludeEmailFromExport
fi

# create an empty s3 bucket in seaweedfs if it doesn't already exist
curl -X POST "http://seaweedfs:8888/buckets/"
curl -X POST "http://seaweedfs:8888/buckets/dataverse/"

wait-for-it localhost:8080 -- tail -f /usr/local/payara5/glassfish/domains/domain1/logs/server.log