#!/bin/bash
set -euo pipefail

# NOTE: ALL PASSWORD ENV VARS WILL BE SCRAMBLED IN startInForeground.sh FOR SECURITY!
#       This is to avoid possible attack vectors where someone could extract the sensitive information
#       from within an env var dump inside an application!

# Someone set the env var for passwords - get the new password in. Otherwise print warning.
# https://docs.openshift.com/container-platform/4.14/openshift_images/create-images.html#avoid-default-passwords
if [ "$LINUX_PASSWORD" != "payara" ]; then
  echo -e "$LINUX_USER\n$LINUX_PASSWORD\n$LINUX_PASSWORD" | passwd || true
else
  echo "IMPORTANT: THIS CONTAINER USES THE DEFAULT PASSWORD FOR USER \"${LINUX_USER}\"! ('payara')"
  echo "           To change the password, set the LINUX_PASSWORD env var."
fi

# Change the domain admin password if necessary
if [ "$PAYARA_ADMIN_PASSWORD" != "admin" ]; then
  TEMP_PASSWORD_FILE=$(mktemp)
  echo "AS_ADMIN_PASSWORD=admin" > "$TEMP_PASSWORD_FILE"
  echo "AS_ADMIN_NEWPASSWORD=${PAYARA_ADMIN_PASSWORD}" >> "$TEMP_PASSWORD_FILE"
  echo "AS_ADMIN_PASSWORD=${ADMIN_PASSWORD}" >> ${PASSWORD_FILE}
  asadmin --user="${PAYARA_ADMIN_USER}" --passwordfile="$TEMP_PASSWORD_FILE" change-admin-password --domain_name="${DOMAIN_NAME}" || true
  rm "$TEMP_PASSWORD_FILE"
else
  echo "IMPORTANT: THIS CONTAINER USES THE DEFAULT PASSWORD FOR PAYARA ADMIN \"${PAYARA_ADMIN_USER}\"! ('admin')"
  echo "           To change the password, set the PAYARA_ADMIN_PASSWORD env var."
fi
