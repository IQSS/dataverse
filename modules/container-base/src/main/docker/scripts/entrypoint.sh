#!/usr/bin/dumb-init /bin/bash
##########################################################################################################
#
#  This script is a fork of https://github.com/payara/Payara/blob/master/appserver/extras/docker-images/
#  server-full/src/main/docker/bin/entrypoint.sh and licensed under CDDL 1.1 by the Payara Foundation.
#
##########################################################################################################

# This shellscript is supposed to be executed by https://github.com/Yelp/dumb-init to keep subprocesses
# and zombies under control. If the ENTRYPOINT command is changed, it will still use dumb-init because shebang.
# dumb-init takes care to send any signals to subshells, too! (Which might run in the background...)

# We do not define these variables within our Dockerfile so the location can be changed when trying to avoid
# writes to the overlay filesystem. (CONFIG_DIR is defined within the Dockerfile, but might be overridden.)
${PREBOOT_COMMANDS:="${CONFIG_DIR}/pre-boot-commands.asadmin"}
export PREBOOT_COMMANDS
${POSTBOOT_COMMANDS:="${CONFIG_DIR}/post-boot-commands.asadmin"}
export POSTBOOT_COMMANDS

# Execute any scripts BEFORE the appserver starts
for f in "${SCRIPT_DIR}"/init_* "${SCRIPT_DIR}"/init.d/*; do
      # shellcheck disable=SC1090
      case "$f" in
        *.sh)  echo "[Entrypoint] running $f"; . "$f" ;;
        *)     echo "[Entrypoint] ignoring $f" ;;
      esac
      echo
done

# If present, run a startInBackground.sh in the background (e.g. to run tasks AFTER the application server starts)
if [ -x "${SCRIPT_DIR}/startInBackground.sh" ]; then
    echo "[Entrypoint] running ${SCRIPT_DIR}/startInBackground.sh in background"
    "${SCRIPT_DIR}"/startInBackground.sh &
fi

# Start the application server and make it REPLACE this shell, so init system and Java directly interact
# Remember - this means no code below this statement will be run!
echo "[Entrypoint] running ${SCRIPT_DIR}/startInForeground.sh in foreground"
exec "${SCRIPT_DIR}"/startInForeground.sh "${PAYARA_ARGS}"
