#!/bin/sh

# EBJ timers sometimes cause problems; utility to clear generated directories

# assumes this script is being run as root

# will restart Payara if it's stopped; comment out the `start-domain` command at the end
# if you'd like to avoid that.

# directory where Payara is installed
PAYARA_DIR=/usr/local/payara6

# directory within Payara (defaults)
DV_DIR=${PAYARA_DIR}/glassfish/domains/domain1

# stop the domain (generates a warning if app server is stopped)
${PAYARA_DIR}/bin/asadmin stop-domain

rm -rf ${PAYARA_DIR}/${DV_DIR}/generated/
rm -rf ${PAYARA_DIR}/${DV_DIR}/osgi-cache/

# restart the domain (also generates a warning if app server is stopped)
${PAYARA_DIR}/bin/asadmin start-domain
