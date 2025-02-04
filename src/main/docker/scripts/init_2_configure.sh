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

# If reload is enable via ENABLE_RELOAD=1, set according Jakarta Faces options
ENABLE_RELOAD=${ENABLE_RELOAD:-0}
if [ "${ENABLE_RELOAD}" = "1" ]; then
  export DATAVERSE_JSF_PROJECT_STAGE=${DATAVERSE_JSF_PROJECT_STAGE:-"Development"}
  export DATAVERSE_JSF_REFRESH_PERIOD=${DATAVERSE_JSF_REFRESH_PERIOD:-"0"}
fi

# Check prerequisites for commands handling
if [ -z "$POSTBOOT_COMMANDS_FILE" ]; then echo "Variable POSTBOOT_COMMANDS_FILE is not set."; exit 1; fi
# Test if postboot file is writeable for us, exit otherwise
touch "$POSTBOOT_COMMANDS_FILE" || exit 1
# Copy and split the postboot contents to manipulate them
EXISTING_DEPLOY_COMMANDS=$(mktemp)
NEW_POSTBOOT_COMMANDS=$(mktemp)
grep -e "^deploy " "$POSTBOOT_COMMANDS_FILE" > "$EXISTING_DEPLOY_COMMANDS" || true
grep -v -e "^deploy" "$POSTBOOT_COMMANDS_FILE" > "$NEW_POSTBOOT_COMMANDS" || true

function inject() {
  if [ -z "$1" ]; then echo "No line specified"; exit 1; fi
  # If the line is not yet in the file, try to add it
  if ! grep -q "$1" "$NEW_POSTBOOT_COMMANDS"; then
    # Check if the line is still not in the file when splitting at the first =
    if ! grep -q "$(echo "$1" | cut -f1 -d"=")" "$NEW_POSTBOOT_COMMANDS"; then
      echo "$1" >> "$NEW_POSTBOOT_COMMANDS"
    fi
  fi
}

# Domain based configuration options - set from Dataverse environment variables
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
    inject "create-system-properties ${KEY}=${v}"
done

# 4. Add the commands to the existing postboot file, but insert BEFORE deployment
cat "$NEW_POSTBOOT_COMMANDS" "$EXISTING_DEPLOY_COMMANDS" > "${POSTBOOT_COMMANDS_FILE}"
echo "DEBUG: postboot contains the following commands:"
echo "--------------------------------------------------"
cat "${POSTBOOT_COMMANDS_FILE}"
echo "--------------------------------------------------"
