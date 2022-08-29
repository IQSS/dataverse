#!/bin/bash
##########################################################################################################
#
#  This script is a fork of https://github.com/payara/Payara/blob/master/appserver/extras/docker-images/
#  server-full/src/main/docker/bin/entrypoint.sh and licensed under CDDL 1.1 by the Payara Foundation.
#
##########################################################################################################

for f in "${SCRIPT_DIR}"/init_* "${SCRIPT_DIR}"/init.d/*; do
      # shellcheck disable=SC1090
      case "$f" in
        *.sh)  echo "[Entrypoint] running $f"; . "$f" ;;
        *)     echo "[Entrypoint] ignoring $f" ;;
      esac
      echo
done

exec "${SCRIPT_DIR}"/startInForeground.sh "${PAYARA_ARGS}"
