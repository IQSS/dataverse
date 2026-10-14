#!/bin/bash
################################################################################
# Hot-redeploy the application from the exploded WAR at ${DEPLOY_DIR}/dataverse.
#
# Meant to be run inside a running application container, e.g. via
# "docker exec dev_dataverse redeploy.sh" - which is what "mvn -Pfrd package"
# does after refreshing the (bind mounted) exploded WAR. See the "Fast Redeploy"
# section of the container guide for details.
################################################################################

# Fail on any error
set -euo pipefail

# These env vars are provided by the (base) image with sane defaults.
PASSWORD_FILE=$(mktemp)
trap 'rm -f "${PASSWORD_FILE}"' EXIT
echo "AS_ADMIN_PASSWORD=${PAYARA_ADMIN_PASSWORD}" > "${PASSWORD_FILE}"

echo "Redeploying application from ${DEPLOY_DIR}/dataverse..."
# Include ${DEPLOY_PROPS} (unquoted, may hold multiple options) so the redeploy uses the same
# deployment options as the initial deployment, see init_1_generate_deploy_commands.sh.
# shellcheck disable=SC2086
"${PAYARA_DIR}/bin/asadmin" --user="${PAYARA_ADMIN_USER}" --passwordfile="${PASSWORD_FILE}" \
    deploy ${DEPLOY_PROPS:-} --force --upload=false "${DEPLOY_DIR}/dataverse"
