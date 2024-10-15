#!/bin/bash
set -euo pipefail

# NOTE: ALL PASSWORD ENV VARS WILL BE SCRAMBLED IN startInForeground.sh FOR SECURITY!
#       This is to avoid possible attack vectors where someone could extract the sensitive information
#       from within an env var dump inside an application!

# Someone set the env var for passwords - get the new password in. Otherwise print warning.
# https://docs.openshift.com/container-platform/4.14/openshift_images/create-images.html#avoid-default-passwords
if [ "$LINUX_PASSWORD" != "payara" ]; then
  echo -e "$LINUX_USER\n$LINUX_PASSWORD\n$LINUX_PASSWORD" | passwd
else
  echo "IMPORTANT: THIS CONTAINER USES THE DEFAULT PASSWORD FOR USER \"${LINUX_USER}\"! ('payara')"
  echo "           To change the password, set the LINUX_PASSWORD env var."
fi

# Change the domain admin password if necessary
if [ "$PAYARA_ADMIN_PASSWORD" != "admin" ]; then
  PASSWORD_FILE=$(mktemp)
  echo "AS_ADMIN_PASSWORD=admin" > "$PASSWORD_FILE"
  echo "AS_ADMIN_NEWPASSWORD=${PAYARA_ADMIN_PASSWORD}" >> "$PASSWORD_FILE"
  asadmin --user="${PAYARA_ADMIN_USER}" --passwordfile="$PASSWORD_FILE" change-admin-password --domain_name="${DOMAIN_NAME}"
  rm "$PASSWORD_FILE"
else
  echo "IMPORTANT: THIS CONTAINER USES THE DEFAULT PASSWORD FOR PAYARA ADMIN \"${PAYARA_ADMIN_USER}\"! ('admin')"
  echo "           To change the password, set the PAYARA_ADMIN_PASSWORD env var."
fi

# Change the domain master password if necessary
# > The master password is not tied to a user account, and it is not used for authentication.
# > Instead, Payara Server strictly uses the master password to ONLY encrypt the keystore and truststore used to store keys and certificates for the DAS and instances usage.
# It will be requested when booting the application server!
# https://docs.payara.fish/community/docs/Technical%20Documentation/Payara%20Server%20Documentation/Security%20Guide/Administering%20System%20Security.html#to-change-the-master-password
if [ "$DOMAIN_PASSWORD" != "changeit" ]; then
  PASSWORD_FILE=$(mktemp)
  echo "AS_ADMIN_MASTERPASSWORD=changeit" >> "$PASSWORD_FILE"
  echo "AS_ADMIN_NEWMASTERPASSWORD=${DOMAIN_PASSWORD}" >> "$PASSWORD_FILE"
  asadmin --user="${PAYARA_ADMIN_USER}" --passwordfile="$PASSWORD_FILE" change-master-password --savemasterpassword false "${DOMAIN_NAME}"
  rm "$PASSWORD_FILE"
else
  echo "IMPORTANT: THIS CONTAINER USES THE DEFAULT DOMAIN \"MASTER\" PASSWORD! ('changeit')"
  echo "           To change the password, set the DOMAIN_PASSWORD env var."
fi
