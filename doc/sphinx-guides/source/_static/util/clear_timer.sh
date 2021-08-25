#!/bin/sh

# EBJ timers sometimes cause problems; utility to clear generated directories and database rows

# assumes this script is being run as root, and that the postgres user had passwordless 
# access to the database (local sockets, or appropriate environmental variables).

# will restart glassfish if it's stopped; comment out the `start-domain` command at the end
# if you'd like to avoid that.

# directory where glassfish is installed
GLASSFISH_DIR=/usr/local/glassfish4

# directory within glassfish (defaults)
DV_DIR=${GLASSFISH_DIR}/glassfish/domains/domain1

# stop the glassfish domain (generates a warning if glassfish is stopped)
${GLASSFISH_DIR}/bin/asadmin stop-domain

rm -rf ${GLASSFISH_DIR}/${DV_DIR}/generated/
rm -rf ${GLASSFISH_DIR}/${DV_DIR}/osgi-cache/felix

# restart the domain (also generates a warning if glassfish is stopped)
${GLASSFISH_DIR}/bin/asadmin start-domain

