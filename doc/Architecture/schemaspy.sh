#!/bin/sh
java -jar /tmp/schemaSpy_5.0.0.jar -t pgsql -host localhost -db dvnDb -u postgres -p secret -dp /tmp/postgresql-9.3-1100.jdbc41.jar -o /tmp/schemaspy.out -s public
