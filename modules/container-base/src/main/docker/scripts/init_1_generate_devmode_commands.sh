#!/bin/bash

set -euo pipefail

###### ###### ###### ###### ###### ###### ###### ###### ###### ###### ######
# This script enables different development options, like a JMX connector
# usable with VisualVM, JRebel hot-reload support and JDWP debugger service.
# Enable it by adding env vars on startup (e.g. via ConfigMap)
#
# As this script is "sourced" from entrypoint.sh, we can manipulate env vars
# for the parent shell before executing Payara.
###### ###### ###### ###### ###### ###### ###### ###### ###### ###### ######

if [ -z "$PREBOOT_COMMANDS_FILE" ]; then echo "Variable PREBOOT_COMMANDS_FILE is not set."; exit 1; fi
# Test if preboot file is writeable for us, exit otherwise
touch "$PREBOOT_COMMANDS_FILE" || exit 1

# 0. Init variables
ENABLE_JMX=${ENABLE_JMX:-0}
ENABLE_JDWP=${ENABLE_JDWP:-0}
ENABLE_RELOAD=${ENABLE_RELOAD:-0}

function inject() {
  if [ -z "$1" ]; then echo "No line specified"; exit 1; fi
  # If the line is not yet in the file, try to add it
  if ! grep -q "$1" "$PREBOOT_COMMANDS_FILE"; then
    # Check if the line is still not in the file when splitting at the first =
    if ! grep -q "$(echo "$1" | cut -f1 -d"=")" "$PREBOOT_COMMANDS_FILE"; then
      echo "$1" >> "$PREBOOT_COMMANDS_FILE"
    fi
  fi
}

# 1. Configure JMX (enabled by default on port 8686, but requires SSL)
# See also https://blog.payara.fish/monitoring-payara-server-with-jconsole
# To still use it, you can use a sidecar container proxying or using JMX via localhost without SSL.
if [ "${ENABLE_JMX}" = "1" ]; then
  echo "Enabling unsecured JMX on 0.0.0.0:8686, enabling AMX and tuning monitoring levels to HIGH. You'll need a sidecar for this, as access is allowed from same machine only (without SSL)."
  inject "set configs.config.server-config.amx-configuration.enabled=true"
  inject "set configs.config.server-config.monitoring-service.module-monitoring-levels.jvm=HIGH"
  inject "set configs.config.server-config.monitoring-service.module-monitoring-levels.connector-service=HIGH"
  inject "set configs.config.server-config.monitoring-service.module-monitoring-levels.connector-connection-pool=HIGH"
  inject "set configs.config.server-config.monitoring-service.module-monitoring-levels.jdbc-connection-pool=HIGH"
  inject "set configs.config.server-config.monitoring-service.module-monitoring-levels.web-services-container=HIGH"
  inject "set configs.config.server-config.monitoring-service.module-monitoring-levels.ejb-container=HIGH"
  inject "set configs.config.server-config.monitoring-service.module-monitoring-levels.thread-pool=HIGH"
  inject "set configs.config.server-config.monitoring-service.module-monitoring-levels.http-service=HIGH"
  inject "set configs.config.server-config.monitoring-service.module-monitoring-levels.security=HIGH"
  inject "set configs.config.server-config.monitoring-service.module-monitoring-levels.jms-service=HIGH"
  inject "set configs.config.server-config.monitoring-service.module-monitoring-levels.jersey=HIGH"
  inject "set configs.config.server-config.monitoring-service.module-monitoring-levels.transaction-service=HIGH"
  inject "set configs.config.server-config.monitoring-service.module-monitoring-levels.jpa=HIGH"
  inject "set configs.config.server-config.monitoring-service.module-monitoring-levels.web-container=HIGH"
  inject "set configs.config.server-config.monitoring-service.module-monitoring-levels.orb=HIGH"
  inject "set configs.config.server-config.monitoring-service.module-monitoring-levels.deployment=HIGH"
  inject "set configs.config.server-config.admin-service.jmx-connector.system.security-enabled=false"
fi

# 2. Enable JDWP via debugging switch
if [ "${ENABLE_JDWP}" = "1" ]; then
  echo "Enabling JDWP remote debugging support via asadmin debugging switch."
  export PAYARA_ARGS="${PAYARA_ARGS} --debug=true"
fi

# 3. Enable hot reload
if [ "${ENABLE_RELOAD}" = "1" ]; then
  echo "Enabling hot reload of deployments."
  inject "set configs.config.server-config.admin-service.das-config.dynamic-reload-enabled=true"
  inject "set configs.config.server-config.admin-service.das-config.autodeploy-enabled=true"
fi

# 4. Add the commands to the existing preboot file, but insert BEFORE deployment
echo "DEBUG: preboot contains now the following commands:"
echo "--------------------------------------------------"
cat "${PREBOOT_COMMANDS_FILE}"
echo "--------------------------------------------------"
