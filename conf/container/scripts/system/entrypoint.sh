#!/bin/bash

for f in ${SCRIPT_DIR}/init_* ${SCRIPT_DIR}/init.d/*; do
      case "$f" in
        *.sh)  echo "[Entrypoint] running $f"; . "$f" ;;
        *)     echo "[Entrypoint] ignoring $f" ;;
      esac
      echo
done

exec ${SCRIPT_DIR}/startInForeground.sh $PAYARA_ARGS
