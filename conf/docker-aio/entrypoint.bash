#!/usr/bin/env bash
export LANG=en_US.UTF-8
sudo -u postgres /usr/pgsql-13/bin/pg_ctl start -D /var/lib/pgsql/13/data &
cd /opt/solr-8.11.1/
# TODO: Run Solr as non-root and remove "-force".
bin/solr start -force
bin/solr create_core -c collection1 -d server/solr/collection1/conf -force

# start apache, in both foreground and background...
apachectl -DFOREGROUND &

# TODO: Run Payara as non-root.
cd /opt/payara5
bin/asadmin start-domain --debug
sleep infinity

