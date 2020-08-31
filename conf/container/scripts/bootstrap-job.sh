#!/bin/bash
################################################################################
# This script is used to bootstrap a Dataverse installation.
#
# It runs all necessary database foo that cannot be done from EclipseLink.
# It initializes the most basic settings and
# creates root dataverse and admin account.
################################################################################

# Fail on any error
set -euo pipefail
# Include some sane defaults
. ${SCRIPT_DIR}/default.config
DATAVERSE_SERVICE_HOST=${DATAVERSE_SERVICE_HOST:-"dataverse"}
DATAVERSE_SERVICE_PORT_HTTP=${DATAVERSE_SERVICE_PORT_HTTP:-"8080"}
DATAVERSE_URL=${DATAVERSE_URL:-"http://${DATAVERSE_SERVICE_HOST}:${DATAVERSE_SERVICE_PORT_HTTP}"}
# The Solr Service IP is always available under its name within the same namespace.
# If people want to use a different Solr than we normally deploy, they have the
# option to override.
SOLR_K8S_HOST=${SOLR_K8S_HOST:-"solr"}

# Check postgres and API key secrets are available
if [ ! -s "${SECRETS_DIR}/db/password" ]; then
  echo "No database password present. Failing."
  exit 126
fi
if [ ! -s "${SECRETS_DIR}/api/key" ]; then
  echo "No API key present. Failing."
  exit 126
fi

# Load dataverseAdmin password if present
if [ -s "${SECRETS_DIR}/admin/password" ]; then
  echo "Loading admin password from secret file."
  ADMIN_PASSWORD=`cat ${SECRETS_DIR}/admin/password`
fi

# Drop the Postgres credentials into .pgpass
echo "${POSTGRES_SERVER}:*:*:${POSTGRES_USER}:`cat ${SECRETS_DIR}/db/password`" > ${HOME_DIR}/.pgpass
chmod 0600 ${HOME_DIR}/.pgpass

# 1.) Load SQL data
psql -h ${POSTGRES_SERVER} -U ${POSTGRES_USER} ${POSTGRES_DATABASE} < ${DEPLOY_DIR}/dataverse/supplements/reference_data.sql

# 2) Initialize common data structures to make Dataverse usable
cd ${DEPLOY_DIR}/dataverse/supplements
# 2a) Patch load scripts with k8s based URL
sed -i -e "s#localhost:8080#${DATAVERSE_SERVICE_HOST}:${DATAVERSE_SERVICE_PORT_HTTP}#" setup-*.sh
# 2b) Patch user and root dataverse JSON with contact email
sed -i -e "s#root@mailinator.com#${CONTACT_MAIL}#" data/dv-root.json
sed -i -e "s#dataverse@mailinator.com#${CONTACT_MAIL}#" data/user-admin.json
# 2c) Use script(s) to bootstrap the instance.
./setup-all.sh --insecure -p="${ADMIN_PASSWORD:-admin}"

# 4.) Configure Solr location
curl -sS -X PUT -d "${SOLR_K8S_HOST}:8983" "${DATAVERSE_URL}/api/admin/settings/:SolrHostColonPort"

# 5.) Provision builtin users key to enable creation of more builtin users
if [ -s "${SECRETS_DIR}/api/userskey" ]; then
  curl -sS -X PUT -d "`cat ${SECRETS_DIR}/api/userskey`" "${DATAVERSE_URL}/api/admin/settings/BuiltinUsers.KEY"
else
  curl -sS -X DELETE "${DATAVERSE_URL}/api/admin/settings/BuiltinUsers.KEY"
fi

# 6.) Block access to the API endpoints, but allow for request with key from secret
curl -sS -X PUT -d "`cat ${SECRETS_DIR}/api/key`" "${DATAVERSE_URL}/api/admin/settings/:BlockedApiKey"
curl -sS -X PUT -d unblock-key "${DATAVERSE_URL}/api/admin/settings/:BlockedApiPolicy"
curl -sS -X PUT -d admin,test "${DATAVERSE_URL}/api/admin/settings/:BlockedApiEndpoints"

# Initial configuration of Dataverse
exec ${SCRIPT_DIR}/config-job.sh
