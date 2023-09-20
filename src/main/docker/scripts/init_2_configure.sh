#!/bin/bash
################################################################################
# Configure Payara
#
# BEWARE: As this is done for Kubernetes, we will ALWAYS start with a fresh container!
#         When moving to Payara 5+ the option commands are idempotent.
#         The resources are to be created by the application on deployment,
#         once Dataverse has proper refactoring, etc.
################################################################################

# Fail on any error
set -euo pipefail

# Include some sane defaults (which are currently not settable via MicroProfile Config).
# This is an ugly hack and shall be removed once #7000 is resolved.
export dataverse_auth_password__reset__timeout__in__minutes="${dataverse_auth_password__reset__timeout__in__minutes:-60}"
export dataverse_timerServer="${dataverse_timerServer:-true}"
export dataverse_files_storage__driver__id="${dataverse_files_storage__driver__id:-local}"
if [ "${dataverse_files_storage__driver__id}" = "local" ]; then
  export dataverse_files_local_type="${dataverse_files_local_type:-file}"
  export dataverse_files_local_label="${dataverse_files_local_label:-Local}"
  export dataverse_files_local_directory="${dataverse_files_local_directory:-${STORAGE_DIR}/store}"
fi

# 0. Define postboot commands file to be read by Payara and clear it
DV_POSTBOOT=${PAYARA_DIR}/dataverse_postboot
echo "# Dataverse postboot configuration for Payara" > "${DV_POSTBOOT}"

# 2. Domain-spaced resources (JDBC, JMS, ...)
# TODO: This is ugly and dirty. It should be replaced with resources from
#       EE 8 code annotations or at least glassfish-resources.xml
# NOTE: postboot commands is not multi-line capable, thus spaghetti needed.

# 3. Domain based configuration options
# Set Dataverse environment variables
echo "INFO: Defining system properties for Dataverse configuration options."
#env | grep -Ee "^(dataverse|doi)_" | sort -fd
env -0 | grep -z -Ee "^(dataverse|doi)_" | while IFS='=' read -r -d '' k v; do
    # transform __ to -
    # shellcheck disable=SC2001
    KEY=$(echo "${k}" | sed -e "s#__#-#g")
    # transform remaining single _ to .
    KEY=$(echo "${KEY}" | tr '_' '.')

    # escape colons in values
    # shellcheck disable=SC2001
    v=$(echo "${v}" | sed -e 's/:/\\\:/g')

    echo "DEBUG: Handling ${KEY}=${v}."
    echo "create-system-properties ${KEY}=${v}" >> "${DV_POSTBOOT}"
done

# 4. Add the commands to the existing postboot file, but insert BEFORE deployment
TMPFILE=$(mktemp)
cat "${DV_POSTBOOT}" "${POSTBOOT_COMMANDS}" > "${TMPFILE}" && mv "${TMPFILE}" "${POSTBOOT_COMMANDS}"
echo "DEBUG: postboot contains the following commands:"
echo "--------------------------------------------------"
cat "${POSTBOOT_COMMANDS}"
echo "--------------------------------------------------"

