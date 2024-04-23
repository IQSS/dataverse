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

# 0. Init variables
ENABLE_JMX=${ENABLE_JMX:-0}
ENABLE_JDWP=${ENABLE_JDWP:-0}
ENABLE_RELOAD=${ENABLE_RELOAD:-0}

DV_PREBOOT=${CONFIG_DIR}/dataverse_preboot
echo "# Dataverse preboot configuration for Payara" > "${DV_PREBOOT}"

# 1. Configure JMX (enabled by default on port 8686, but requires SSL)
# See also https://blog.payara.fish/monitoring-payara-server-with-jconsole
# To still use it, you can use a sidecar container proxying or using JMX via localhost without SSL.
if [ "${ENABLE_JMX}" = "1" ]; then
  echo "Enabling unsecured JMX on 0.0.0.0:8686, enabling AMX and tuning monitoring levels to HIGH. You'll need a sidecar for this, as access is allowed from same machine only (without SSL)."
  { \
    echo "set configs.config.server-config.amx-configuration.enabled=true"
    echo "set configs.config.server-config.monitoring-service.module-monitoring-levels.jvm=HIGH"
    echo "set configs.config.server-config.monitoring-service.module-monitoring-levels.connector-service=HIGH"
    echo "set configs.config.server-config.monitoring-service.module-monitoring-levels.connector-connection-pool=HIGH"
    echo "set configs.config.server-config.monitoring-service.module-monitoring-levels.jdbc-connection-pool=HIGH"
    echo "set configs.config.server-config.monitoring-service.module-monitoring-levels.web-services-container=HIGH"
    echo "set configs.config.server-config.monitoring-service.module-monitoring-levels.ejb-container=HIGH"
    echo "set configs.config.server-config.monitoring-service.module-monitoring-levels.thread-pool=HIGH"
    echo "set configs.config.server-config.monitoring-service.module-monitoring-levels.http-service=HIGH"
    echo "set configs.config.server-config.monitoring-service.module-monitoring-levels.security=HIGH"
    echo "set configs.config.server-config.monitoring-service.module-monitoring-levels.jms-service=HIGH"
    echo "set configs.config.server-config.monitoring-service.module-monitoring-levels.jersey=HIGH"
    echo "set configs.config.server-config.monitoring-service.module-monitoring-levels.transaction-service=HIGH"
    echo "set configs.config.server-config.monitoring-service.module-monitoring-levels.jpa=HIGH"
    echo "set configs.config.server-config.monitoring-service.module-monitoring-levels.web-container=HIGH"
    echo "set configs.config.server-config.monitoring-service.module-monitoring-levels.orb=HIGH"
    echo "set configs.config.server-config.monitoring-service.module-monitoring-levels.deployment=HIGH"
    echo "set configs.config.server-config.admin-service.jmx-connector.system.security-enabled=false"
  } >> "${DV_PREBOOT}"
fi

# 2. Enable JDWP via debugging switch
if [ "${ENABLE_JDWP}" = "1" ]; then
  echo "Enabling JDWP remote debugging support via asadmin debugging switch."
  export PAYARA_ARGS="${PAYARA_ARGS} --debug=true"
fi

# 3. Enable hot reload
if [ "${ENABLE_RELOAD}" = "1" ]; then
  echo "Enabling hot reload of deployments."
  echo "set configs.config.server-config.admin-service.das-config.dynamic-reload-enabled=true" >> "${DV_PREBOOT}"
  echo "set configs.config.server-config.admin-service.das-config.autodeploy-enabled=true" >> "${DV_PREBOOT}"
  export DATAVERSE_JSF_PROJECT_STAGE=${DATAVERSE_JSF_PROJECT_STAGE:-"Development"}
  export DATAVERSE_JSF_REFRESH_PERIOD=${DATAVERSE_JSF_REFRESH_PERIOD:-"0"}
fi

# 4. Add the commands to the existing preboot file, but insert BEFORE deployment
TMP_PREBOOT=$(mktemp)
cat "${DV_PREBOOT}" "${PREBOOT_COMMANDS}" > "${TMP_PREBOOT}"
mv "${TMP_PREBOOT}" "${PREBOOT_COMMANDS}"
echo "DEBUG: preboot contains the following commands:"
echo "--------------------------------------------------"
cat "${PREBOOT_COMMANDS}"
echo "--------------------------------------------------"