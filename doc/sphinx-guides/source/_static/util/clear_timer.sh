#!/bin/sh

# EBJ timers sometimes cause problems; utility to clear generated directories and database rows

# assumes that glassfish is not running, and 
# also assumes this script is being run as root, and that the postgres user had passwordless 
# access to the database (local sockets, or appropriate environmental variables).

# directory where glassfish is installed
GLASSFISH_DIR=/usr/local/glassfish4

# directory within glassfish (defaults)
DV_DIR=${GLASSFISH_DIR}/glassfish/domains/domain1

# name of dataverse database
DV_DB=dvndb

# OS user for the database
DB_USER=postgres

rm -rf ${GLASSFISH_DIR}/${DV_DIR}/generated/
rm -rf ${GLASSFISH_DIR}/${DV_DIR}/osgi-cache/felix

sudo -u ${DB_USER} psql ${DV_DB} -c 'delete from "EJB__TIMER__TBL"';


