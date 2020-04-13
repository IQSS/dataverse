#!/bin/sh

# EBJ timers sometimes cause problems; utility to clear generated directories and database rows

# assumes this script is being run as root, and that the postgres user had passwordless 
# access to the database (local sockets, or appropriate environmental variables).

# will restart Payara if it's stopped; comment out the `start-domain` command at the end
# if you'd like to avoid that.

# directory where Payara is installed
PAYARA_DIR=/usr/local/payara5

# directory within Payara (defaults)
DV_DIR=${PAYARA_DIR}/glassfish/domains/domain1

# name of dataverse database
DV_DB=dvndb

# OS user for the database
DB_USER=postgres

# stop the domain (generates a warning if app server is stopped)
${PAYARA_DIR}/bin/asadmin stop-domain

rm -rf ${PAYARA_DIR}/${DV_DIR}/generated/
rm -rf ${PAYARA_DIR}/${DV_DIR}/osgi-cache/felix

sudo -u ${DB_USER} psql ${DV_DB} -c 'delete from "EJB__TIMER__TBL"';

# restart the domain (also generates a warning if app server is stopped)
${PAYARA_DIR}/bin/asadmin start-domain
