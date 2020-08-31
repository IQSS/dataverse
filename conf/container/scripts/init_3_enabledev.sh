#!/bin/bash

set -e

###### ###### ###### ###### ###### ###### ###### ###### ###### ###### ######
# This script enables different development options, like a JMX connector
# usable with VisualVM, JRebel hot-reload support and JDWP debugger service.
# Enable it by adding env vars on startup (e.g. via ConfigMap)
###### ###### ###### ###### ###### ###### ###### ###### ###### ###### ######

# 0. Init variables
ENABLE_JMX=${ENABLE_JMX:-0}
ENABLE_JDWP=${ENABLE_JDWP:-0}
ENABLE_JREBEL=${ENABLE_JREBEL:-0}
JDWP_PORT=${JDWP_PORT:-9009}

DV_PREBOOT=${PAYARA_DIR}/dataverse_preboot
echo "# Dataverse preboot configuration for Payara" > ${DV_PREBOOT}

# 1. Configure JMX (enabled by default on port 8686, but requires SSL)
# See also https://blog.payara.fish/monitoring-payara-server-with-jconsole
# To still use it, you can use a sidecar container proxying or using JMX via localhost without SSL.
if [ "x${ENABLE_JMX}" = "x1" ]; then
  echo "Enabling JMX on 127.0.0.1:8686. You'll need a sidecar for this."
  echo "set configs.config.server-config.admin-service.jmx-connector.system.address=127.0.0.1" >> ${DV_PREBOOT}
fi

# 2. Enable JDWP (debugger)
if [ "x${ENABLE_JDWP}" = "x1" ]; then
  echo "Enabling JDWP debugger, listening on port ${JDWP_PORT} of this container/pod."
  echo "create-jvm-options --target=server-config \"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${JDWP_PORT}\"" >> ${DV_PREBOOT}
fi

# 3. Enable JRebel (hot-redeploy)
if [ "x${ENABLE_JREBEL}" = "x1" ] && [ -s "${JREBEL_LIB}" ]; then
  echo "Enabling JRebel support with enabled remoting_plugin option."
  echo "create-jvm-options --target=server-config \"-agentpath:${JREBEL_LIB}\"" >> ${DV_PREBOOT}
  echo "create-system-properties rebel.remoting_plugin=true" >> ${DV_PREBOOT}
fi

# 4. Add the commands to the existing postboot file, but insert BEFORE deployment
echo "$(cat ${DV_PREBOOT} | cat - ${PREBOOT_COMMANDS} )" > ${PREBOOT_COMMANDS}
echo "DEBUG: preboot contains the following commands:"
echo "--------------------------------------------------"
cat ${PREBOOT_COMMANDS}
echo "--------------------------------------------------"
