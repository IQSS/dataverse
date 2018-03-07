#!/usr/bin/env bash

sudo -u postgres /usr/bin/postgres -D /var/lib/pgsql/data &
cd /opt/solr-4.6.0/example/
java -DSTOP.PORT=8079 -DSTOP.KEY=a09df7a0d -jar start.jar &

cd /opt/glassfish4
bin/asadmin start-domain
sleep infinity

