#!/usr/bin/env bash

sudo -u postgres /usr/bin/postgres -D /var/lib/pgsql/data &
cd /opt/solr-7.2.1/
bin/solr stop &

cd /opt/glassfish4
bin/asadmin start-domain
sleep infinity

