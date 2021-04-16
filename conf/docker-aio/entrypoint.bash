#!/usr/bin/env bash
export LANG=en_US.UTF-8
sudo -u postgres /usr/bin/pg_ctl start -D /var/lib/pgsql/data &
cd /opt/solr-8.8.1/
# TODO: Run Solr as non-root and remove "-force".
bin/solr start -force
bin/solr create_core -c collection1 -d server/solr/collection1/conf -force

# start apache, in both foreground and background...
apachectl -DFOREGROUND &

# TODO: Run Glassfish as non-root.
cd /opt/glassfish4
bin/asadmin start-domain --debug
sleep infinity

