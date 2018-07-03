#!/usr/bin/env bash

#sudo -u postgres /usr/bin/postgres -D /var/lib/pgsql/data &
sudo -u postgres /usr/pgsql-9.6/bin/postgres -D /var/lib/pgsql/data &
cd /opt/solr-7.3.0/
# TODO: Run Solr as non-root and remove "-force".
bin/solr start -force
bin/solr create_core -c collection1 -d server/solr/collection1/conf -force

# start apache, in both foreground and background...
apachectl -DFOREGROUND &

# TODO: Run Glassfish as non-root.
cd /opt/glassfish4
bin/asadmin start-domain --debug
sleep infinity

